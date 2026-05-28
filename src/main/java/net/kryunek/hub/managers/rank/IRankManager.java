package net.kryunek.hub.managers.rank;

import lombok.Getter;
import lombok.Setter;
import net.kryunek.hub.managers.module.ModuleService;
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

@Getter @Setter
public class IRankManager {

    private final FileConfig config;
    private final FileConfig queueConfig;
    private final FileConfig tabConfig;
    private String rankSystem;
    private String configuredSystemMode;
    private IRank rank;

    public IRankManager() {
        var files = ModuleService.getFileModule();
        this.config = files.getFile("config");
        this.queueConfig = files.getFile("queue");
        this.tabConfig = files.getFile("tab");
    }

    public void loadRank() {
        ensureDefaults();
        String mode = config.getString("RANK.SYSTEM", "AUTO", false);
        this.configuredSystemMode = mode == null ? "AUTO" : mode.toUpperCase(Locale.ROOT);

        if ("DEFAULT".equalsIgnoreCase(configuredSystemMode)) {
            this.setRank(new Default());
            this.setRankSystem("Default");
            return;
        }

        if ("LUCKPERMS".equalsIgnoreCase(configuredSystemMode) && Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            this.setRank(new Default());
            this.setRankSystem("Default");
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            this.setRank(new LuckPerms());
            this.setRankSystem("LuckPerms");
        }
        else {
            this.setRank(new Default());
            this.setRankSystem("Default");
        }
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

        List<String> tabGroups = tabConfig.getStringList("group-sorting.groups");
        if (!(tabGroups.size() == 1 && "ERROR: STRING LIST NOT FOUND!".equals(tabGroups.get(0)))) {
            ranks.addAll(tabGroups);
        }

        boolean allowLuckPermsSource = !"DEFAULT".equalsIgnoreCase(configuredSystemMode);
        if (allowLuckPermsSource && isLuckPermsActive()) {
            try {
                net.luckperms.api.LuckPerms api = Bukkit.getServicesManager().load(net.luckperms.api.LuckPerms.class);
                if (api != null) {
                    api.getGroupManager().getLoadedGroups().forEach(group -> ranks.add(group.getName()));
                }
            } catch (Throwable ex) {
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
        List<String> groups = new ArrayList<>(tabConfig.getStringList("group-sorting.groups"));
        if (groups.size() == 1 && "ERROR: STRING LIST NOT FOUND!".equals(groups.get(0))) {
            groups.clear();
        }
        return groups;
    }

    private void ensureDefaults() {
        if (!config.getConfiguration().contains("RANK.SYSTEM")) {
            config.getConfiguration().set("RANK.SYSTEM", "AUTO");
            config.save();
        }
    }

}
