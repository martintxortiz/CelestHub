package net.kryunek.hub.managers.player;

import net.kryunek.hub.Celest;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of player {@link Profile}s with periodic autosave, backed by a pluggable
 * {@link ProfileStorage} (local file or MongoDB) selected from the injected config.
 */
public class ProfileManager {
    private final Map<UUID, Profile> profiles;
    private final ProfileStorage storage;
    private final boolean defaultJukeboxEnabled;
    private final double defaultJukeboxVolume;
    private BukkitTask autosaveTask;

    public ProfileManager(FileConfig config, FileConfig jukebox) {
        this(createStorage(config), true, readDefaultJukeboxEnabled(jukebox), readDefaultJukeboxVolume(jukebox));
    }

    ProfileManager(ProfileStorage storage, boolean startAutosave) {
        this(storage, startAutosave, true, 1.0D);
    }

    ProfileManager(ProfileStorage storage, boolean startAutosave, boolean defaultJukeboxEnabled, double defaultJukeboxVolume) {
        this.profiles = new ConcurrentHashMap<>();
        this.storage = storage;
        this.defaultJukeboxEnabled = defaultJukeboxEnabled;
        this.defaultJukeboxVolume = defaultJukeboxVolume;
        if (startAutosave) {
            startAutosave();
        }
    }

    private static ProfileStorage createStorage(FileConfig config) {
        String type = config.getConfiguration().getString("PERSISTENCE.TYPE", "LOCAL");
        boolean enabled = config.getConfiguration().getBoolean("PERSISTENCE.ENABLED", false);
        if (enabled && "MONGO".equalsIgnoreCase(type)) {
            return new MongoProfileStorage();
        }
        return new LocalProfileStorage();
    }

    private static boolean readDefaultJukeboxEnabled(FileConfig jukebox) {
        return jukebox.getConfiguration().getBoolean("JUKEBOX.DEFAULT_ENABLED", true);
    }

    private static double readDefaultJukeboxVolume(FileConfig jukebox) {
        return jukebox.getConfiguration().getDouble("JUKEBOX.CONTROLS.DEFAULT_VOLUME", 1.0D);
    }

    private void startAutosave() {
        Runnable autosave = this::save;
        if (this.storage instanceof LocalProfileStorage) {
            this.autosaveTask = Bukkit.getScheduler().runTaskTimer(Celest.get(), autosave, 300L, 300L);
            return;
        }
        this.autosaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(Celest.get(), autosave, 300L, 300L);
    }

    public Map<UUID, Profile> getProfiles() {
        return Collections.unmodifiableMap(this.profiles);
    }

    public Collection<Profile> getProfileSnapshot() {
        return new ArrayList<>(this.profiles.values());
    }

    public Profile createProfile(UUID uuid, String name) {
        Profile profile = new Profile(uuid, name, defaultJukeboxEnabled, defaultJukeboxVolume);
        this.profiles.put(uuid, profile);
        return profile;
    }

    public void loadProfile(Profile profile) {
        ProfileData data = storage.load(profile.getUuid());
        if (data == null) {
            saveProfile(profile, false);
            return;
        }

        profile.applyData(data);
    }

    public void save() {
        for (Profile profile : getProfileSnapshot()) {
            if (profile == null) {
                continue;
            }
            saveProfile(profile, false);
        }
    }

    public void saveAndRemove(UUID uuid) {
        Profile profile = this.profiles.remove(uuid);
        if (profile != null) {
            saveProfile(profile, false);
        }
    }

    public void saveProfile(Profile profile, boolean delay) {
        ProfileData data = profile.toData();
        if (delay) {
            TaskUtil.scheduleSyncDelayedTask(() -> storage.save(profile.getUuid(), data));
            return;
        }

        storage.save(profile.getUuid(), data);
    }

    public void removeProfile(UUID uuid) {
        this.profiles.remove(uuid);
    }

    public ProfileStorage getStorage() {
        return storage;
    }

    public Profile getProfile(UUID uuid) {
        return this.profiles.get(uuid);
    }

    public void shutdown() {
        if (this.autosaveTask != null) {
            this.autosaveTask.cancel();
            this.autosaveTask = null;
        }
        storage.close();
    }
}

