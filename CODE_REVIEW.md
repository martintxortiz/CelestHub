# CelestHub — Production-Readiness Code Review

**Scope:** entire `src/main/java` (318 files, ~24,300 lines) plus build/test config.
**Verdict:** functional and feature-rich, but **not yet production-grade (~6.5/10)**. The
recent DI work is correct but skin-deep. The codebase is dominated by a global static
service-locator, has several latent crash/NPE landmines, mixed-language and unprofessional
naming, near-zero automated safety net, and no CI/quality gates.

This document lists **every category of issue** with concrete evidence (file:line) and
instance counts, ordered by severity. Fix top-down.

---

## Remediation status (last updated 2026-05-29)

A first remediation pass has landed. Build + 31 tests green; shaded jar builds.

**✅ Fixed**
- **B2** — `TaskUtil` static-init landmine removed; plugin now resolved lazily + cached (no more class-load crash). Tick/second method semantics documented; class made `final` with private ctor.
- **B3** — `JoinLeaveListener.onJoin` null-guards the profile (fails safe with a kick instead of NPE); the other-player profile in the visibility loop is null-guarded too.
- **B4 (partial)** — GitHub Actions CI added (`.github/workflows/ci.yml`) running `mvn clean verify` on push/PR and archiving the jar. *(Still TODO: JaCoCo coverage floor + SpotBugs/Error Prone.)*
- **M1** — vulgar variable `tetas` renamed to a proper name.
- **M2** — Spanish identifiers translated: queue methods (`iniciar/detener*` → `start/stop*`) and the entire `TablistManager` (fields, methods, locals) is now English.
- **M3** — `IRankManager` → `RankManager` (it's a concrete class; the `I` was misleading).
- **M4** — `catch (Throwable)` in the rank manager narrowed to `catch (Exception)`.
- **M5** — Redis subscriber now reconnects with exponential backoff (capped) instead of dying permanently on a transient outage; sync topics are constants, not duplicated magic strings.
- **M6** — `MongoProfileStorage` document→model mapping de-duplicated into a single `fromDocument`.
- **M7 (partial)** — the 110-line `onJoin` god-method is decomposed into focused private methods; a redundant per-iteration inventory write was hoisted out of the visibility loop.
- **N2** — dropped class-level `@Setter` on the rank manager (no more public mutators for internal state).
- **N3/N4** — `load(boolean b)` → `load(boolean reload)`; redundant manual list copy in `getOnlinePlayers()` simplified.

**⏳ Remaining (largest-value, not yet done)**
- **B1** — the global static service-locator (`ModuleService` 416×, `Celest.get()` 53×) still dominates. This is the long-game migration to constructor injection, package by package.
- **B4 (rest)** — add JaCoCo coverage gate + SpotBugs/Error Prone; broaden tests to listeners/commands/menus and the network/persistence layers (MockBukkit + Testcontainers).
- **N1** — 997 inline config-key string literals across 160 files still want typed constants/binding.
- **M8 / N7 / N8** — resource-filtering scope, `FileConfig` sentinel-string contract, committed `MANIFEST.MF`.

---

## Severity legend
- 🔴 **Blocker** — can crash the server, lose data, or makes the code untestable/unmaintainable at scale.
- 🟠 **Major** — real bug risk or systemic design flaw; fix before calling this production-grade.
- 🟡 **Minor** — quality/consistency; fix opportunistically.

---

## 🔴 Blockers

### B1. Global static service-locator is the de-facto architecture
- `ModuleService.getXxx()` is called **416 times across 191 files**.
- `Celest.get()` is called **53 times across 28 files**.
- Every manager constructs its *own* dependencies by reaching into the static locator
  (e.g. `IRankManager` ctor calls `ModuleService.getFileModule()`,
  `IRankManager.java:29`), so the constructor-injection added to `QueueManager` /
  `PvpArenaKitManager` is the exception, not the rule.

**Why it's a blocker:** nothing can be unit-tested without `mockStatic(...)` gymnastics
(see `QueueTest`/`IRankManagerTest` — every test needs 1–2 static mocks just to *construct*
the object). Hidden global coupling makes load order fragile and refactors dangerous.

**Fix:** introduce a single composition root (`CelestApplication`/`ManagerModule` already is
one) that constructs managers and **passes** dependencies via constructors. Delete static
accessors as call sites migrate. This is the same pattern already proven on `QueueManager`,
applied consistently.

### B2. `TaskUtil` static initializer can hard-fail at class load
`src/main/java/.../utils/TaskUtil.java:18-20`
```java
static { plugin = Celest.get(); }   // JavaPlugin.getPlugin(Celest.class) throws if not yet registered
```
The **first reference** to `TaskUtil` triggers this. If the plugin instance isn't registered
(early load, reload edge cases, tests), the class fails to initialize with
`ExceptionInInitializerError` and *every* feature touching `TaskUtil` dies. This is why
`QueueTest` must wrap construction in `mockStatic(Celest.class)`.

**Fix:** remove the static field; pass the plugin in, or read `Bukkit.getPluginManager()`
lazily inside each method. Make `TaskUtil` an injected instance, not a static singleton.

### B3. NPE on player join when profile is absent
`src/main/java/.../listeners/JoinLeaveListener.java:54-58`
```java
Profile profile = profileManager.getProfile(event.getPlayer().getUniqueId());
...
player.setGameMode(profile.isBuildModeEnabled() ? ... );   // profile may be null
```
`getProfile` returns `null` when no profile is cached (load failure, async race, mid-shutdown).
`onJoin` dereferences it immediately with **no null check**, yet `onLeave` (line 177) *does*
null-guard the same call — proving the author knows it can be null. A failed/slow profile load
crashes the join handler and leaves the player in a broken state.

**Fix:** guarantee a profile exists before this listener runs (load synchronously in an
earlier-priority handler, or create-on-miss), and null-guard defensively.

### B4. No automated safety net for a server you can't test on
- **8 test files for 318 source files (~2.5%).** Zero tests for: 145 menu classes, 22
  listeners, all 46 command classes, the network/persistence layers.
- No `.github/` — **no CI** at all. Nothing runs the tests on push.
- No coverage tool (JaCoCo), no static analysis (SpotBugs/Error Prone/Checkstyle/PMD), no
  Maven Enforcer. `pom.xml` has only compiler + shade + surefire.

**Why it's a blocker (per your stated goal):** you explicitly cannot test on the server, so the
build *is* your only verification. Right now it verifies almost nothing.

**Fix:** add GitHub Actions running `mvn verify`; add JaCoCo with a coverage floor; add
SpotBugs + Error Prone to catch NPE/resource-leak classes of bug at compile time; add
MockBukkit + Testcontainers (Mongo/Redis) to test the join flow, persistence round-trips, and
the Redis protocol end-to-end.

---

## 🟠 Major

### M1. Unprofessional / vulgar variable name in shipped code
`src/main/java/.../listeners/JoinLeaveListener.java:76-77`
```java
Hotbar tetas = hotbarManager.getHotbar("HIDE_PLAYER");
player.getInventory().setItem(tetas.getSlot(), tetas.getItem());
```
`tetas` is Spanish vulgar slang. This must not exist in production code. Rename to
`hidePlayerItem`.

### M2. Mixed-language (Spanish/English) identifiers
Public API methods are in Spanish, breaking the otherwise-English codebase:
- `Queue.iniciarTaskPosicion()` / `Queue.detenerTaskPosicion()` (`Queue.java:44,84`)
- `QueueManager.iniciarSendTask()` / `detenerSendTask()` (`QueueManager.java:37,54`)
- Called from **14 sites** in the queue package.

**Fix:** rename to `startPositionTask` / `stopPositionTask` / `startSendTask` / `stopSendTask`
(IDE rename-symbol, single commit).

### M3. Misleading `I`-prefix on a concrete class
`IRankManager` is a **concrete class** (`IRankManager.java:19`), but `I` universally connotes
an interface — and the real interface here is `IRank`. This actively misleads readers.
**Fix:** rename `IRankManager` → `RankManager`.

### M4. Broad `catch (Throwable)` / `catch (Exception)` swallowing
- `catch (Throwable ex)` at `IRankManager.java:93` (catches `Error`, e.g. `OutOfMemoryError`).
- **18 broad `catch (Exception|Throwable)`** across 8 files.

Catching `Throwable` hides JVM-level errors; broad catches hide bugs. **Fix:** catch the
narrowest type, log with context, and never catch `Error`/`Throwable`.

### M5. Network layer is fragile and unmanaged
`src/main/java/.../managers/network/NetworkSyncManager.java`
- Raw `new Thread(...)` for the Redis subscriber (line 51) instead of an `ExecutorService`.
- **No reconnect:** if `subscribe` throws (network blip), the thread logs once and dies; the
  manager stays `enabled` but is silently deaf forever (lines 61-69).
- Hand-rolled wire protocol `serverId|topic|base64` (line 125) with **no version field** and
  string-split parsing (line 135) — brittle and unversioned. Topic strings (`"QUEUE_STATE"`
  etc.) are duplicated magic literals.

**Fix:** use a managed executor with backoff-reconnect; define a small versioned message type
(record + JSON) instead of pipe-delimited strings; centralize topic constants.

### M6. Duplicated document→model mapping in Mongo storage
`MongoProfileStorage.java` — `load()` (lines 37-57) and `loadAll()` (lines 70-89) contain the
**same ~20-line field-by-field mapping**, copy-pasted. Any new profile field must be edited in
two places; they *will* drift.
**Fix:** extract `private ProfileData fromDocument(Document doc)` and call it from both.

### M7. God-method listener with embedded business logic
`JoinLeaveListener.onJoin` is **~110 lines** (lines 53-166) doing: health/food reset, gamemode,
walk speed, spawn teleport, hotbar setup, a full online-player visibility loop, clear-chat,
join message, title timing math, join sound, and two nested delayed tasks. Listeners should
delegate to managers/services, not implement features inline. This is untestable and a magnet
for the kind of NPE in B3.
**Fix:** extract a `PlayerJoinService` (or per-concern methods) and have the listener call it.

### M8. Resource filtering enabled on all resources
`pom.xml:99-104` sets `<filtering>true</filtering>` for `src/main/resources/**`. This runs
Maven property substitution over **every YAML and the manifest**. Any literal `${...}` in a
config value (common in message templates / placeholders) gets mangled at build time, and
binary resources can be corrupted.
**Fix:** scope filtering to only `plugin.yml` (or a dedicated filtered folder); leave config
YAML unfiltered.

---

## 🟡 Minor (consistency, encapsulation, polish)

### N1. Magic config-key strings everywhere
Inline config literals (`getString("...")`, `getFile("...")`, `getConfiguration().get/set(...)`)
appear **997 times across 160 files**. A typo (`"WALK_SPEED"` vs `"WALKSPEED"`) is a silent
runtime default-to-zero, never caught by the compiler.
**Fix:** introduce typed config keys / a config-binding object per feature. At minimum, hoist
repeated keys into `static final` constants near each manager.

### N2. `@Getter @Setter` over-exposure breaks encapsulation
`IRankManager` is annotated `@Getter @Setter` (line 18), generating public setters for
`rank`, `config`, `rankSystem`, etc. External code can swap the rank implementation or null out
config at will. Same pattern likely repeats on other managers.
**Fix:** expose only the getters that are actually needed; drop class-level `@Setter` on
managers.

### N3. Poor parameter / local names
- `ManagerModule.load(boolean b)` (line 49) — parameter named `b`; should be `reload`.
- `JoinLeaveListener` uses `profiles` (plural) for a single profile (line 73) next to `profile`.
**Fix:** rename for intent.

### N4. Redundant collection copy
`ManagerModule.getOnlinePlayers()` (lines 96-102) manually copies `getOnlinePlayers()` into a
new `ArrayList` element-by-element; the source is already a `Collection`. Either return the
view directly or `new ArrayList<>(server.getOnlinePlayers())`.

### N5. `TaskUtil` API is inconsistent and surprising
Two overloads named `runTimer` (lines 26, 42) take the same shape but one multiplies by `20`
and one doesn't (`runTaskTimer` at line 22 multiplies; `runTimer` at line 26 doesn't). Callers
can't tell ticks from seconds from the name. **Fix:** name by unit (`runTimerTicks` /
`runTimerSeconds`) and document.

### N6. Inline fully-qualified names instead of imports
e.g. `IRankManager.java:89` uses `net.luckperms.api.LuckPerms` inline. Minor readability; use
imports (the file already imports cleanly elsewhere).

### N7. Sentinel-string error contract in `FileConfig`
`FileConfig.getStringList` returns `["ERROR: STRING LIST NOT FOUND!"]` on miss
(`FileConfig.java:112`), forcing callers to string-compare that sentinel (see
`IRankManager.java:82,141`). **Fix:** return an empty list or `Optional`, never a magic
sentinel.

### N8. `MANIFEST.MF` committed as a resource
`src/main/resources/META-INF/MANIFEST.MF` is shaded in; combined with N8/filtering this is
fragile. Let the shade/jar plugin own the manifest.

---

## Suggested remediation order (max safety per unit of churn)
1. **B2 + B3 + M1** — remove the `TaskUtil` static landmine, null-guard `onJoin`, rename `tetas`. Small, high-impact, no architecture change.
2. **B4** — add CI (`mvn verify` on push), JaCoCo floor, SpotBugs/Error Prone. This protects every subsequent change since you can't test on the server.
3. **M2 + M3 + N3** — IDE rename pass for Spanish identifiers, `IRankManager`→`RankManager`, `b`→`reload`. Mechanical, one commit.
4. **M4 + M5 + M6** — tighten exception handling, harden Redis (reconnect + versioned messages), dedupe Mongo mapping.
5. **M7 + N1 + N2** — extract join logic into a service; introduce config-key constants/binding; trim Lombok over-exposure.
6. **B1** — the long game: migrate `ModuleService`/`Celest.get()` call sites to constructor injection, package by package, deleting static accessors as you go. Each migrated manager becomes unit-testable without `mockStatic`.

---

## What's already good (keep it)
- Clean module/priority bootstrap (`Module`/`ModuleService`/`CelestApplication`) — a solid
  composition-root foundation to build real DI on.
- Proper async/sync discipline for autosave (`ProfileManager.startAutosave`) and Redis→main-thread
  hop (`NetworkSyncManager.handleMessage` line 150).
- `ConcurrentHashMap` for the profile cache; unmodifiable views on accessors.
- Storage abstraction (`ProfileStorage` with Local/Mongo impls) — the right seam.
- Dependency shading with relocations is correctly configured in `pom.xml`.
- The 8 existing tests use the right techniques (`@TempDir` real configs, `mockStatic`).
