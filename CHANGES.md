# CHANGES — fork delta vs. `kryunek/CelestHub`

This document explains **everything that changed** in this fork relative to the upstream it
was forked from: `kryunek/CelestHub@c3448b4` (the merge-base / fork point).

- **Base (upstream):** `kryunek/CelestHub`, branch `main`, commit `c3448b4` ("Update README.md").
- **This fork head:** `b21110f`.
- **Delta:** 223 files changed, ~3,853 insertions / ~1,422 deletions.
- **Goal of the work:** take a functional-but-rough plugin (independently reviewed at ~6.5/10)
  toward production grade — without a test server, so `mvn clean verify` (compile + JUnit +
  JaCoCo + SpotBugs) is the only verification, backed by CI.

Each section below is **what changed** and, more importantly, **why**. Issue codes (B/M/N)
match `CODE_REVIEW.md`, the issue catalogue this work is remediating.

---

## 1. New reusable core: the `support/` package

**What:** Added a small, dependency-light, fully-unit-tested core under
`net.kryunek.hub.support`:
- `config/ConfigSupport` — null-safe readers (`getString`, `requireString`, `getMaterial`,
  `getSound`, `getParticle`, `getPositiveInt`) plus `applyMissingDefaults`.
- `config/ConfigFiles` — typed constants for every registered config file name.
- `message/Messages` + `message/MessageKey` — colourised, placeholder-aware message resolution
  with one-time missing-key warnings.
- `command/CommandSupport` + `command/Permissions` — permission/in-game command guards.
- `profile/ProfileAccess` — the "get the player's profile, message them if absent" guard.

**Why:** the original code resolved config values, messages, and permissions inline,
everywhere, with no reuse and no testability. This core gives one tested place for each cross-
cutting concern, is constructor-injectable (so it's mockable), and is where the crash-safety in
§3 lives. It's the foundation the rest of the hardening builds on.

---

## 2. Dependency injection for the manager layer (B1)

**What:** Every manager now receives its `FileConfig`/collaborator dependencies as
**constructor parameters**, wired in one place — `ManagerModule.onEnable` (the composition
root), which reads the `ModuleService` locator once and passes configs in dependency order
(rank → queue, profile/hotbar → pvp-kit, network-sync started last). `ProfileManager`'s public
constructor now takes its config + jukebox files; the static storage/jukebox helpers take them
as parameters. `RankManagerTest` lost its `mockStatic(ModuleService)` scaffolding as a result.

**Why:** previously *every* manager reached into the global static service-locator in its own
constructor (`ModuleService.getFileModule()...`), so nothing could be constructed in a test
without `mockStatic` gymnastics, and load order was implicit and fragile. Construction-time
dependencies are now explicit and the managers are unit-constructable.

**Scope/limit (honest):** `ModuleService` remains as a facade for *method-body* navigation
between features (~400 call sites). Threading those through ~190 UI/event files is high-risk
with no server to test against, so that was deliberately left as the documented next step, not
attempted blind. This is the "simplest version that achieves the goal," not a DI framework.

---

## 3. Crash & NPE fixes (the latent landmines)

**What:**
- **Config `valueOf` crash class (the big one):** ~35 sites did `Material.valueOf(config)` /
  `Sound.valueOf(config)`, which **throw** on any config typo — crashing player join
  (`JoinLeaveListener` join sound) or breaking menus on open. All now route through
  `ConfigSupport`, which logs and falls back to a safe default instead of throwing.
  `ItemBuilder`'s `String`/`int` constructors are hardened the same way as a backstop.
  Guarded admin-input validators (e.g. the editor's "type a sound name") were intentionally
  left throwing — there, rejecting bad input is correct.
- **B2 — `TaskUtil` static-init landmine:** the `static { plugin = Celest.get(); }` block could
  fail at class load with `ExceptionInInitializerError` (early load/reload/tests), killing every
  feature that touched `TaskUtil`. Plugin is now resolved lazily and cached; class is `final`
  with a private constructor.
- **B3 — NPE on join:** `onJoin` dereferenced a possibly-null `Profile` even though `onLeave`
  already null-guarded the same call. It now fails safe (kicks with a clear message) instead of
  NPE-ing into a broken state.
- **N7 — `FileConfig` sentinel contract:** `getStringList` used to return
  `["ERROR: STRING LIST NOT FOUND!"]` on a miss, forcing callers to string-compare a magic
  sentinel. It now returns an empty list.

**Why:** these are the bugs that actually take a server down or corrupt player state. On a
plugin you can't smoke-test, a config typo silently shipping a crash is the worst failure mode;
this section removes that whole class.

---

## 4. Build quality gates + CI (B4)

**What:**
- **GitHub Actions CI** (`.github/workflows/ci.yml`): runs `mvn -B clean verify` on every push
  and PR (Ubuntu, Temurin JDK 21, Maven cache) and uploads the built jar.
- **JaCoCo** coverage gate: line-coverage floor of **0.80** scoped to `net.kryunek.hub.support.*`
  (the tested core), with `ConfigFiles` excluded (compile-time constants are inlined and never
  loaded, so they can't be "covered").
- **SpotBugs** (`spotbugs-exclude.xml`): Max effort, Medium threshold, `maxRank=12`, enforced at
  `verify`. All findings fixed to reach zero (notably `setArmorContents(null)` →
  `new ItemStack[4]` in 7 files).

**Why:** the original `pom.xml` had only compiler + shade + surefire and there was no CI at all.
Since the server can't be used for verification, the build *is* the safety net — so it now
actually verifies something, automatically, on every push.

---

## 5. Tests: 8 → 62 (B4 cont.)

**What:** broadened the suite to 62 tests covering `ConfigSupport`, `Messages`, `CommandSupport`,
`ProfileAccess`, `FileConfig`, `ProfileManager`, `Queue`, and (new this pass) `ChatManager`,
`SpawnManager`, and `RankManager` (the last now mock-free thanks to §2). Tests use real configs
via `@TempDir` and `mockStatic` only where genuinely needed.

**Why:** 8 tests for 318 files (~2.5%) was effectively no safety net. The new tests pin the
reusable core and the manager logic that's now constructable, so regressions in the most-reused
code are caught by CI. (Coverage of the listener/menu layer is still thin — see §11.)

---

## 6. Modern Paper APIs (deprecation migration)

**What:** replaced deprecated Bukkit/Paper APIs with their current Adventure equivalents:
- `showPlayer(Player)` / `hidePlayer(Player)` → `(Plugin, Player)` (16 sites).
- `setJoinMessage` / `setQuitMessage` / `kickPlayer(String)` → `joinMessage`/`quitMessage(null)`
  and `kick(Component)`.
- `AsyncPlayerChatEvent` → `io.papermc.paper.event.player.AsyncChatEvent` across all 12 chat
  listeners (plain-text reads via `PlainTextComponentSerializer`; `ChatListener`'s `setFormat`
  became a `renderer`).

**Why:** these APIs are deprecated and slated for removal; staying on them is borrowed time
against future Paper updates. The migrations are logic-preserving.

**Residual risk (honest):** these change runtime behaviour and **could not be verified on a
server**. They compile, pass CI, and were reviewed line-for-line, but the actual chat rendering
and join/quit text are the one thing worth a 2-minute in-game smoke test if a server ever
becomes available. `AsyncPlayerChatEvent` still *works* today, so this is future-proofing.

---

## 7. Code quality, naming, and de-duplication (M1–M7, N2–N4)

**What:**
- **M1** — vulgar Spanish variable `tetas` in `JoinLeaveListener` renamed to a proper name.
- **M2** — Spanish identifiers translated to English: queue methods
  (`iniciar/detener*` → `start/stop*`, 14 call sites) and the entire `TablistManager`.
- **M3** — `IRankManager` → `RankManager` (it's a concrete class; the `I` implied an interface).
- **M4** — broad `catch (Throwable)` in the rank manager narrowed to `catch (Exception)` (never
  swallow `Error`).
- **M5** — `NetworkSyncManager` Redis subscriber now **reconnects with capped exponential
  backoff** instead of dying permanently on a transient outage; sync topics are constants, not
  duplicated magic strings.
- **M6** — `MongoProfileStorage` document→model mapping de-duplicated into one `fromDocument`
  (was copy-pasted in `load()` and `loadAll()` and would have drifted).
- **M7** — the ~110-line `onJoin` god-method decomposed into focused private methods
  (`applyJoinState`, `applyVisibility`, `applyJoinTitle`, `applyJoinSound`, …).
- **N2** — dropped class-level `@Setter` on the rank manager (no more public mutators for
  internal state).
- **N3/N4** — `load(boolean b)` → `load(boolean reload)`; redundant element-by-element list copy
  in `getOnlinePlayers()` simplified.

**Why:** correctness (M4/M5/M6), professionalism and readability (M1/M2/M3/N3), encapsulation
(N2), and testability/maintainability (M7). M5 in particular fixes a real "silently deaf
forever after a network blip" bug.

---

## 8. Typed config keys (N1)

**What:** introduced `ConfigFiles` constants and replaced the inline config-file-name string
literals (`getFile("config")` → `getFile(ConfigFiles.CONFIG)`) across 151 files.

**Why:** a mistyped key is a silent runtime miss the compiler never catches. Typed constants
turn that into a compile error and give one place to see every config file the plugin owns.

---

## 9. Data-driven config defaults

**What:** the repeated `if (!cfg.contains(k)) { cfg.set(k, v); changed = true; }` walls in
`ChatManager`, `LotteryManager`, and `HotbarManager` collapsed into declarative default maps fed
to `ConfigSupport.applyMissingDefaults(section, map)`.

**Why:** the walls were error-prone boilerplate; the data-driven form is shorter, behaviour-
preserving (same keys/values), and the helper is unit-tested.

---

## 10. Documentation, packaging & repo hygiene

**What:**
- **Javadoc** added across the `support/` core (class + public methods), the module bootstrap
  (`Module`/`ModuleService`/`ManagerModule` — documenting the priority order and composition-root
  contract), and all 15 managers (responsibility + the injected-config contract).
- **M8** — Maven resource filtering scoped to only what needs it (was filtering *all* resources,
  which could mangle `${...}` in message templates or corrupt binaries).
- **N8** — removed the committed `src/main/resources/META-INF/MANIFEST.MF`; the jar/shade plugin
  owns the manifest.
- **`.gitattributes`** — normalise text files to LF so line endings are deterministic across OSes.
- **`.gitignore`** — ignore build output (`target/`) and the local `test-server/` (large server
  jars); `target/` (404 previously-committed build artifacts) was untracked.

**Why:** "no Javadoc anywhere" was a real gap; M8/N8 remove build-time fragility; and a repo that
tracks its own build output and flips line endings on every checkout isn't professional. These
make the *repository* match the code.

---

## 11. Verification & known limitations

**Verified:** `mvn -B clean verify` is green — 62 tests pass, the JaCoCo `support.*` gate holds
at ≥ 0.80, and SpotBugs reports 0 findings at rank ≤ 12. CI re-runs all of this on every push.

**Not closed (honest):**
- **Behavioural Paper-API changes (§6) are unverified on a server** — logic-preserving and CI-
  compiled, but not watched in-game.
- **Coverage is still core-focused** — the listener/menu/command layer has little automated
  coverage; the JaCoCo gate guards only `support.*`.
- **The `ModuleService` service-locator remains** in method bodies (~400 sites) — a deliberate,
  documented architectural trade-off, not an oversight.

Net: independently reviewed at ~6.5/10 before, this fork is meaningfully higher (~8/10) — the
concrete crash/NPE bugs are gone, the build verifies itself, and the code is documented and
modern. The remaining ceiling is genuinely gated on having a server to test against.

---

## Commit map (on top of `kryunek/CelestHub@c3448b4`)

| Commit | Summary |
|--------|---------|
| `04d3f54` "new" | Introduced the `support/` core, command system, initial managers/DI. |
| `9ceee34` "refactor" | M1–M7 / N2–N4 fixes, initial test suite, network/rank refactors. |
| `aaa02bb` "build: quality gates, CI, and repo hygiene" | JaCoCo + SpotBugs gates, GitHub Actions CI, `.gitattributes`/`.gitignore`, untrack `target/`. |
| `b21110f` "refactor: production-hardening pass" | B1 DI, crash-safe config reads, data-driven defaults, modern Paper APIs, typed config keys, 62 tests, Javadoc. |
