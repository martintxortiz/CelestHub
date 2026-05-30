package net.kryunek.hub.managers.player;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.FileConfig;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LocalProfileStorage implements ProfileStorage {

    private final FileConfig playersConfig = ModuleService.getFileModule().getFile(ConfigFiles.PLAYERS);

    @Override
    public synchronized ProfileData load(UUID uuid) {
        ConfigurationSection section = playersConfig.getConfiguration().getConfigurationSection("players." + uuid);
        if (section == null) {
            return null;
        }
        ProfileData data = new ProfileData();
        data.setName(section.getString("name"));
        data.setVisibilityOn(section.getBoolean("visibility", true));
        data.setBuildModeEnabled(section.getBoolean("buildmode", false));
        data.setFlyOnJoin(section.getBoolean("flyjoin", false));
        data.setJukeboxEnabled(!section.contains("jukebox") || section.getBoolean("jukebox"));
        data.setJukeboxVolume(section.getDouble("jukebox_volume", 1.0D));
        data.setShowScoreboard(section.getBoolean("scoreboard", true));
        data.setShowTablist(!section.contains("tablist") || section.getBoolean("tablist"));
        data.setTrail(section.getString("trail", "None"));
        data.setOutfit(section.getString("outfit", "None"));
        String gadget = section.getString("gadget", "NONE");
        if ("ENDERBUTT_VELOCITY".equalsIgnoreCase(gadget)) {
            gadget = "SNOWBALL_VELOCITY";
        }
        data.setSelectedGadgetType(gadget);
        data.setPvpArenaKitSerialized(section.getString("pvp_kit", ""));
        data.setPvpKills(section.getInt("pvp_kills", 0));
        data.setPvpDeaths(section.getInt("pvp_deaths", 0));
        data.setPvpKillstreak(section.getInt("pvp_killstreak", 0));
        data.setPvpMaxKillstreak(section.getInt("pvp_max_killstreak", 0));
        data.setFirstJoinAt(section.contains("first_join") ? section.getLong("first_join") : System.currentTimeMillis());
        data.setTimePreference(section.getString("time_preference", "SERVER"));
        return data;
    }

    @Override
    public synchronized Map<UUID, ProfileData> loadAll() {
        Map<UUID, ProfileData> result = new HashMap<>();
        ConfigurationSection players = playersConfig.getConfiguration().getConfigurationSection("players");
        if (players == null) {
            return result;
        }

        for (String key : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ProfileData data = load(uuid);
                if (data != null) {
                    result.put(uuid, data);
                }
            } catch (IllegalArgumentException ex) {
                Bukkit.getLogger().warning("[Celest] Ignoring local profile with invalid UUID: " + key);
            }
        }
        return result;
    }

    @Override
    public synchronized void save(UUID uuid, ProfileData data) {
        ConfigurationSection section = playersConfig.getConfiguration().getConfigurationSection("players." + uuid);
        if (section == null) {
            section = playersConfig.getConfiguration().createSection("players." + uuid);
        }
        section.set("name", data.getName());
        section.set("scoreboard", data.isShowScoreboard());
        section.set("tablist", data.isShowTablist());
        section.set("buildmode", data.isBuildModeEnabled());
        section.set("flyjoin", data.isFlyOnJoin());
        section.set("jukebox", data.isJukeboxEnabled());
        section.set("jukebox_volume", data.getJukeboxVolume());
        section.set("visibility", data.isVisibilityOn());
        section.set("trail", data.getTrail());
        section.set("outfit", data.getOutfit());
        section.set("gadget", data.getSelectedGadgetType());
        section.set("pvp_kit", data.getPvpArenaKitSerialized());
        section.set("pvp_kills", data.getPvpKills());
        section.set("pvp_deaths", data.getPvpDeaths());
        section.set("pvp_killstreak", data.getPvpKillstreak());
        section.set("pvp_max_killstreak", data.getPvpMaxKillstreak());
        section.set("first_join", data.getFirstJoinAt());
        section.set("time_preference", data.getTimePreference());
        playersConfig.save();
    }

    @Override
    public void close() {
    }
}
