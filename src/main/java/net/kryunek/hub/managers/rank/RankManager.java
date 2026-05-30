package net.kryunek.hub.managers.rank;

import lombok.Getter;
import net.kryunek.hub.managers.rank.impl.Default;
import net.kryunek.hub.managers.rank.impl.LuckPerms;
import net.kryunek.hub.utils.FileConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Resolves the active rank provider (LuckPerms when present, otherwise the built-in default) and
 * exposes available ranks plus queue/tab priorities, reading from the injected config/queue/tab files.
 */
@Getter
public class RankManager {

    private final FileConfig config;
    private final FileConfig queueConfig;
    private final FileConfig tabConfig;
    private String rankSystem;
    private String configuredSystemMode;
    private IRank rank;

    public RankManager(FileConfig config, FileConfig queueConfig, FileConfig tabConfig) {
        this.config = config;
        this.queueConfig = queueConfig;
        this.tabConfig = tabConfig;
    }

    public void loadRank() {
        ensureDefaults();
        String mode = config.getString("RANK.SYSTEM", "AUTO", false);
        this.configuredSystemMode = mode == null ? "AUTO" : mode.toUpperCase(Locale.ROOT);

        if ("DEFAULT".equalsIgnoreCase(configuredSystemMode)) {
            useDefaultSystem();
            return;
        }

        if ("LUCKPERMS".equalsIgnoreCase(configuredSystemMode) && !isLuckPermsActive()) {
            useDefaultSystem();
            return;
        }

        if (isLuckPermsActive()) {
            this.rank = new LuckPerms();
            this.rankSystem = "LuckPerms";
        } else {
            useDefaultSystem();
        }
    }

    private void useDefaultSystem() {
        this.rank = new Default();
        this.rankSystem = "Default";
    }

    public void setConfiguredSystemMode(String mode) {
        String normalized = mode == null ? "AUTO" : mode.toUpperCase(Locale.ROOT);
        if (!normalized.equals("AUTO") && !normalized.equals("DEFAULT") && !normalized.equals("LUCKPERMS")) {
            normalized = "AUTO";
        }
        this.configuredSystemMode = normalized;
        config.getConfiguration().set("RANK.SYSTEM", normalized);
        config.save();
        loadRank();
    }

    public List<String> getAvailableRanks() {
        Set<String> ranks = new LinkedHashSet<>();

        ConfigurationSection queuePriority = queueConfig.getConfiguration().getConfigurationSection("QUEUE.PRIORITY");
        if (queuePriority != null) {
            ranks.addAll(queuePriority.getKeys(false));
        }

        ranks.addAll(tabConfig.getStringList("group-sorting.groups"));

        boolean allowLuckPermsSource = !"DEFAULT".equalsIgnoreCase(configuredSystemMode);
        if (allowLuckPermsSource && isLuckPermsActive()) {
            try {
                net.luckperms.api.LuckPerms api = Bukkit.getServicesManager().load(net.luckperms.api.LuckPerms.class);
                if (api != null) {
                    api.getGroupManager().getLoadedGroups().forEach(group -> ranks.add(group.getName()));
                }
            } catch (Exception ex) {
                Bukkit.getLogger().fine("[Celest] Failed to read LuckPerms groups: " + ex.getMessage());
            }
        }

        if (ranks.isEmpty()) {
            ranks.add("default");
        }

        return new ArrayList<>(ranks);
    }

    public int getQueuePriority(String rankName) {
        return queueConfig.getInt("QUEUE.PRIORITY." + rankName.toLowerCase(Locale.ROOT));
    }

    public void setQueuePriority(String rankName, int priority) {
        int value = Math.max(0, priority);
        queueConfig.getConfiguration().set("QUEUE.PRIORITY." + rankName.toLowerCase(Locale.ROOT), value);
        queueConfig.save();
    }

    public int getTabPriority(String rankName) {
        List<String> groups = getTabGroupsMutable();
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).equalsIgnoreCase(rankName)) {
                return i;
            }
        }
        return groups.size();
    }

    public void setTabPriority(String rankName, int priority) {
        List<String> groups = getTabGroupsMutable();
        groups.removeIf(group -> group.equalsIgnoreCase(rankName));
        int index = Math.max(0, Math.min(priority, groups.size()));
        groups.add(index, rankName.toLowerCase(Locale.ROOT));

        tabConfig.getConfiguration().set("group-sorting.groups", groups);
        tabConfig.save();
    }

    public boolean isLuckPermsActive() {
        return Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
    }

    private List<String> getTabGroupsMutable() {
        return new ArrayList<>(tabConfig.getStringList("group-sorting.groups"));
    }

    private void ensureDefaults() {
        if (!config.getConfiguration().contains("RANK.SYSTEM")) {
            config.getConfiguration().set("RANK.SYSTEM", "AUTO");
            config.save();
        }
    }

}
