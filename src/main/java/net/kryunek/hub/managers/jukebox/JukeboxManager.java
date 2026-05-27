package net.kryunek.hub.managers.jukebox;

import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.PvpArenaUtil;
import net.kryunek.hub.utils.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JukeboxManager {

    private final FileConfig jukeboxConfig;
    private final FileConfig settingsConfig;
    private final Map<String, Track> tracks = new LinkedHashMap<>();
    private final Map<UUID, String> currentTrack = new LinkedHashMap<>();
    private final Map<UUID, Long> currentTrackEnd = new LinkedHashMap<>();
    private final Map<UUID, Integer> nextTrackIndex = new LinkedHashMap<>();
    private final Map<UUID, String> pausedTrack = new LinkedHashMap<>();
    private final Map<UUID, Long> pausedRemainingMs = new LinkedHashMap<>();
    private final Map<UUID, Boolean> suspendedPlayback = new LinkedHashMap<>();
    private BukkitTask tickerTask;

    public JukeboxManager() {
        this.jukeboxConfig = ModuleService.getFileModule().getFile("jukebox");
        this.settingsConfig = ModuleService.getFileModule().getFile("settings");
        load();
        startTicker();
    }

    public void load() {
        tracks.clear();
        ConfigurationSection section = jukeboxConfig.getConfiguration().getConfigurationSection("JUKEBOX.TRACKS");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            String base = "JUKEBOX.TRACKS." + key;
            String sound = jukeboxConfig.getConfiguration().getString(base + ".SOUND", "").trim();
            if (sound.isBlank()) {
                continue;
            }
            double volume = jukeboxConfig.getConfiguration().getDouble(base + ".VOLUME", 1.0D);
            double pitch = jukeboxConfig.getConfiguration().getDouble(base + ".PITCH", 1.0D);
            int duration = Math.max(1, jukeboxConfig.getConfiguration().getInt(base + ".DURATION_SECONDS", 120));
            tracks.put(key.toLowerCase(), new Track(key, sound, (float) volume, (float) pitch, duration));
        }
    }

    public boolean isEnabled() {
        return jukeboxConfig.getConfiguration().getBoolean("JUKEBOX.ENABLED", true);
    }

    public boolean isDefaultEnabled() {
        return jukeboxConfig.getConfiguration().getBoolean("JUKEBOX.DEFAULT_ENABLED", true);
    }

    public boolean shouldStopInPvp() {
        return jukeboxConfig.getConfiguration().getBoolean("JUKEBOX.STOP_IN_PVP", true);
    }

    public void handleJoin(Player player) {
        if (!isEnabled() || !jukeboxConfig.getConfiguration().getBoolean("JUKEBOX.AUTO_PLAY_ON_JOIN", true)) {
            return;
        }
        Profile profile = ModuleService.getManagerModule().getProfileManager().getProfile(player.getUniqueId());
        if (profile == null || !profile.isJukeboxEnabled()) {
            return;
        }
        if (shouldStopInPvp() && PvpArenaUtil.isInsideArena(settingsConfig, player.getLocation())) {
            return;
        }
        startOrResume(player);
    }

    public void handlePvpState(Player player, boolean insidePvp) {
        if (!isEnabled() || !shouldStopInPvp()) {
            return;
        }
        if (insidePvp) {
            stop(player);
            return;
        }
        Profile profile = ModuleService.getManagerModule().getProfileManager().getProfile(player.getUniqueId());
        if (profile != null && profile.isJukeboxEnabled()) {
            startOrResume(player);
        }
    }

    public boolean toggle(Player player) {
        Profile profile = ModuleService.getManagerModule().getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            return false;
        }
        profile.setJukeboxEnabled(!profile.isJukeboxEnabled());
        if (!profile.isJukeboxEnabled()) {
            stop(player);
        } else {
            suspendedPlayback.put(player.getUniqueId(), false);
            startOrResume(player);
        }
        return profile.isJukeboxEnabled();
    }

    public boolean pause(Player player) {
        Track track = getCurrentTrack(player.getUniqueId());
        if (track == null) {
            return false;
        }
        long remaining = Math.max(1000L, currentTrackEnd.getOrDefault(player.getUniqueId(), System.currentTimeMillis()) - System.currentTimeMillis());
        pausedTrack.put(player.getUniqueId(), track.name());
        pausedRemainingMs.put(player.getUniqueId(), remaining);
        suspendedPlayback.put(player.getUniqueId(), true);
        stopInternal(player, false);
        return true;
    }

    public boolean resume(Player player) {
        String name = pausedTrack.remove(player.getUniqueId());
        Long remaining = pausedRemainingMs.remove(player.getUniqueId());
        if (name == null || remaining == null) {
            return false;
        }
        Track track = tracks.get(name.toLowerCase());
        if (track == null) {
            return false;
        }
        suspendedPlayback.put(player.getUniqueId(), false);
        playTrack(player, track);
        currentTrackEnd.put(player.getUniqueId(), System.currentTimeMillis() + Math.max(1000L, remaining));
        return true;
    }

    public boolean forward(Player player, int seconds) {
        if (!isSeekEnabled()) {
            return false;
        }
        if (seconds <= 0) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        if (getCurrentTrack(uuid) == null) {
            return false;
        }
        long endAt = currentTrackEnd.getOrDefault(uuid, 0L);
        if (endAt <= 0L) {
            return false;
        }
        long newEndAt = endAt - (seconds * 1000L);
        // Never force an instant next-track jump after forwarding.
        long minEnd = System.currentTimeMillis() + Math.max(5000L, getSeekStepSeconds() * 1000L);
        if (newEndAt < minEnd) {
            newEndAt = minEnd;
        }
        currentTrackEnd.put(uuid, newEndAt);
        return true;
    }

    public void stop(Player player) {
        suspendedPlayback.put(player.getUniqueId(), true);
        stopInternal(player, true);
    }

    private void stopInternal(Player player, boolean clearPausedState) {
        Track track = getCurrentTrack(player.getUniqueId());
        if (track != null) {
            player.stopSound(track.soundKey(), SoundCategory.MUSIC);
            player.stopSound(track.soundKey(), SoundCategory.RECORDS);
        } else {
            for (Track value : tracks.values()) {
                player.stopSound(value.soundKey(), SoundCategory.MUSIC);
                player.stopSound(value.soundKey(), SoundCategory.RECORDS);
            }
        }
        currentTrack.remove(player.getUniqueId());
        currentTrackEnd.remove(player.getUniqueId());
        if (clearPausedState) {
            pausedTrack.remove(player.getUniqueId());
            pausedRemainingMs.remove(player.getUniqueId());
        }
    }

    public boolean play(Player player, String trackName) {
        if (!isEnabled()) {
            return false;
        }
        Track track = tracks.get(trackName.toLowerCase());
        if (track == null) {
            return false;
        }
        suspendedPlayback.put(player.getUniqueId(), false);
        pausedTrack.remove(player.getUniqueId());
        pausedRemainingMs.remove(player.getUniqueId());
        playTrack(player, track);
        return true;
    }

    public boolean next(Player player) {
        Track track = getNextTrack(player.getUniqueId());
        if (track == null) {
            return false;
        }
        suspendedPlayback.put(player.getUniqueId(), false);
        pausedTrack.remove(player.getUniqueId());
        pausedRemainingMs.remove(player.getUniqueId());
        playTrack(player, track);
        return true;
    }

    public List<String> getTrackNames() {
        return new ArrayList<>(tracks.keySet());
    }

    public double getVolume(Player player) {
        Profile profile = ModuleService.getManagerModule().getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            return getDefaultVolume();
        }
        return clampVolume(profile.getJukeboxVolume());
    }

    public void setVolume(Player player, double volume) {
        Profile profile = ModuleService.getManagerModule().getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            return;
        }
        profile.setJukeboxVolume(clampVolume(volume));
        Track current = getCurrentTrack(player.getUniqueId());
        if (current != null) {
            playTrack(player, current);
        }
    }

    public String getNowPlaying(UUID uuid) {
        return currentTrack.get(uuid);
    }

    public void startOrResume(Player player) {
        if (!isEnabled()) {
            return;
        }
        suspendedPlayback.put(player.getUniqueId(), false);
        if (resume(player)) {
            return;
        }
        Track existing = getCurrentTrack(player.getUniqueId());
        if (existing != null && currentTrackEnd.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis()) {
            return;
        }
        next(player);
    }

    private Track getCurrentTrack(UUID uuid) {
        String name = currentTrack.get(uuid);
        if (name == null) {
            return null;
        }
        return tracks.get(name.toLowerCase());
    }

    private Track getNextTrack(UUID uuid) {
        if (tracks.isEmpty()) {
            return null;
        }
        List<Track> list = new ArrayList<>(tracks.values());
        if (isShuffleEnabled()) {
            return list.get((int) (Math.random() * list.size()));
        }
        int idx = nextTrackIndex.getOrDefault(uuid, 0);
        if (idx < 0 || idx >= list.size()) {
            idx = 0;
        }
        Track track = list.get(idx);
        nextTrackIndex.put(uuid, (idx + 1) % list.size());
        return track;
    }

    private void playTrack(Player player, Track track) {
        stopInternal(player, false);
        suspendedPlayback.put(player.getUniqueId(), false);
        double profileVolume = getVolume(player);
        float finalVolume = (float) (track.volume() * profileVolume);
        // Reproduce anchored to the player so it follows movement (not static in world position).
        // Dual category call improves compatibility with client volume setups.
        player.playSound(player, track.soundKey(), SoundCategory.MUSIC, finalVolume, track.pitch());
        player.playSound(player, track.soundKey(), SoundCategory.RECORDS, finalVolume, track.pitch());
        currentTrack.put(player.getUniqueId(), track.name());
        currentTrackEnd.put(player.getUniqueId(), System.currentTimeMillis() + track.durationSeconds() * 1000L);
    }

    private void startTicker() {
        if (tickerTask != null) {
            tickerTask.cancel();
        }
        tickerTask = TaskUtil.runSyncTimer(() -> {
            if (!isEnabled()) {
                return;
            }
            long now = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                Profile profile = ModuleService.getManagerModule().getProfileManager().getProfile(player.getUniqueId());
                if (profile == null || !profile.isJukeboxEnabled()) {
                    continue;
                }
                if (suspendedPlayback.getOrDefault(player.getUniqueId(), false)) {
                    continue;
                }
                if (shouldStopInPvp() && PvpArenaUtil.isInsideArena(settingsConfig, player.getLocation())) {
                    continue;
                }
                long endAt = currentTrackEnd.getOrDefault(player.getUniqueId(), 0L);
                if (endAt <= now) {
                    if (isRepeatTrackEnabled()) {
                        Track current = getCurrentTrack(player.getUniqueId());
                        if (current != null) {
                            playTrack(player, current);
                            continue;
                        }
                    }
                    if (isRepeatPlaylistEnabled() || getCurrentTrack(player.getUniqueId()) != null) {
                        next(player);
                    }
                }
            }
        }, 20L, 20L);
    }

    public void shutdown() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            stopInternal(player, true);
        }
        currentTrack.clear();
        currentTrackEnd.clear();
        nextTrackIndex.clear();
        pausedTrack.clear();
        pausedRemainingMs.clear();
        suspendedPlayback.clear();
    }

    public boolean isPauseEnabled() {
        return jukeboxConfig.getConfiguration().getBoolean("JUKEBOX.CONTROLS.ENABLE_PAUSE", true);
    }

    public boolean isSeekEnabled() {
        return jukeboxConfig.getConfiguration().getBoolean("JUKEBOX.CONTROLS.ENABLE_SEEK", true);
    }

    public int getSeekStepSeconds() {
        return Math.max(1, jukeboxConfig.getConfiguration().getInt("JUKEBOX.CONTROLS.SEEK_SECONDS", 15));
    }

    public double getDefaultVolume() {
        return clampVolume(jukeboxConfig.getConfiguration().getDouble("JUKEBOX.CONTROLS.DEFAULT_VOLUME", 1.0D));
    }

    public double clampVolume(double input) {
        double min = jukeboxConfig.getConfiguration().getDouble("JUKEBOX.CONTROLS.MIN_VOLUME", 0.2D);
        double max = jukeboxConfig.getConfiguration().getDouble("JUKEBOX.CONTROLS.MAX_VOLUME", 2.0D);
        if (min > max) {
            double tmp = min;
            min = max;
            max = tmp;
        }
        return Math.max(min, Math.min(max, input));
    }

    public boolean isShuffleEnabled() {
        return jukeboxConfig.getConfiguration().getBoolean("JUKEBOX.PLAYBACK.SHUFFLE", false);
    }

    public boolean isRepeatPlaylistEnabled() {
        return jukeboxConfig.getConfiguration().getBoolean("JUKEBOX.PLAYBACK.REPEAT_PLAYLIST", true);
    }

    public boolean isRepeatTrackEnabled() {
        return jukeboxConfig.getConfiguration().getBoolean("JUKEBOX.PLAYBACK.REPEAT_TRACK", false);
    }

    private record Track(String name, String soundKey, float volume, float pitch, int durationSeconds) {
    }
}
