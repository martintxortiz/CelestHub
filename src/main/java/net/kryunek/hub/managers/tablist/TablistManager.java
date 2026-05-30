package net.kryunek.hub.managers.tablist;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.managers.rank.IRank;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TablistManager {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private final Celest plugin;
    private final FileConfig config;
    private final IRank rank;

    private BukkitTask updateTask;

    private final long updateIntervalTicks;
    private final List<String> headerLines;
    private final List<String> footerLines;
    private final String nameFormat;
    private final String nametagPrefixFormat;
    private final String nametagSuffixFormat;
    private final List<String> groupOrder;
    private final boolean headerFooterEnabled;
    private final boolean nameFormatEnabled;
    private final boolean showNametag;
    private final boolean sortByGroup;

    public TablistManager(Celest plugin) {
        this.plugin = plugin;
        this.config = ModuleService.getFileModule().getFile(ConfigFiles.TAB);
        this.rank = ModuleService.getManagerModule().getRankManager().getRank();
        this.updateIntervalTicks = Math.max(1L, config.getLong("update-interval"));
        this.headerFooterEnabled = config.getBoolean("header-footer.enabled");
        this.nameFormatEnabled = config.getBoolean("tablist-name-formatting.enabled");
        this.headerLines = new ArrayList<>(getListWithFallback("header-footer.header", "header"));
        this.footerLines = new ArrayList<>(getListWithFallback("header-footer.footer", "footer"));
        this.nameFormat = getText("tablist-name-formatting.format", "{lp_prefix}%player%{lp_suffix}");
        this.nametagPrefixFormat = getText("nametag.prefix", "{lp_prefix}");
        this.nametagSuffixFormat = getText("nametag.suffix", "{lp_suffix}");
        this.showNametag = config.getBoolean("nametag.enabled");
        this.sortByGroup = config.getBoolean("group-sorting.enabled");
        this.groupOrder = new ArrayList<>(getList("group-sorting.groups"));
    }

    public void start() {
        stop();

        this.updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            if (players.isEmpty()) {
                return;
            }

            for (Player player : players) {
                if (!shouldSeeTablist(player)) {
                    clearTablist(player);
                } else if (headerFooterEnabled) {
                    updateHeaderAndFooter(player);
                }

                if (nameFormatEnabled) {
                    updateTabName(player);
                }
            }

            if (showNametag || sortByGroup) {
                for (Player viewer : players) {
                    syncTeams(viewer, players);
                }
            }
        }, 20L, updateIntervalTicks);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void updateHeaderAndFooter(Player player) {
        Component header = deserialize(joinLines(headerLines, player));
        Component footer = deserialize(joinLines(footerLines, player));
        player.sendPlayerListHeaderAndFooter(header, footer);
    }

    private void updateTabName(Player player) {
        String text = resolvePlaceholders(nameFormat, player);
        player.playerListName(deserialize(text));
    }

    public void clearTablist(Player player) {
        player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
    }

    public void clearVisuals(Player player) {
        clearTablist(player);
        clearNametag(player);
    }

    public void clearNametag(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard == null) {
            return;
        }

        for (Team team : new ArrayList<>(scoreboard.getTeams())) {
            if (team.getName().startsWith("tab")) {
                team.unregister();
            }
        }
    }

    private void syncTeams(Player viewer, Collection<? extends Player> players) {
        Scoreboard scoreboard = safeScoreboard(viewer);
        clearTabTeams(scoreboard, players);

        for (Player target : players) {
            String teamName = buildTeamName(target);
            Team team = scoreboard.getTeam(teamName);

            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }

            for (String entry : new ArrayList<>(team.getEntries())) {
                if (!entry.equals(target.getName())) {
                    team.removeEntry(entry);
                }
            }

            if (!team.hasEntry(target.getName())) {
                team.addEntry(target.getName());
            }

            if (showNametag) {
                String prefixText = resolvePlaceholders(nametagPrefixFormat, target, true);
                String suffixText = resolvePlaceholders(nametagSuffixFormat, target, true);

                team.prefix(deserialize(prefixText));
                team.suffix(deserialize(suffixText));
                team.setColor(nameColorFromPrefix(prefixText));
            } else {
                team.prefix(Component.empty());
                team.suffix(Component.empty());
                team.setColor(ChatColor.RESET);
            }
        }
    }

    private void clearTabTeams(Scoreboard scoreboard, Collection<? extends Player> players) {
        List<String> currentNames = players.stream().map(this::buildTeamName).collect(Collectors.toList());
        for (Team team : new ArrayList<>(scoreboard.getTeams())) {
            if (team.getName().startsWith("tab") && !currentNames.contains(team.getName())) {
                team.unregister();
            }
        }
    }

    private Scoreboard safeScoreboard(Player player) {
        Scoreboard current = player.getScoreboard();
        if (current == null || current == Bukkit.getScoreboardManager().getMainScoreboard()) {
            current = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(current);
        }
        return current;
    }

    private String buildTeamName(Player player) {
        int priority = groupPriority(player);
        String identifier = player.getUniqueId().toString().replace("-", "");
        identifier = identifier.substring(0, 8);
        return String.format("tab%05d%s", priority, identifier);
    }

    private int groupPriority(Player player) {
        if (!sortByGroup) {
            return 99999;
        }

        String group = resolveGroup(player).toLowerCase(Locale.ROOT);
        for (int i = 0; i < groupOrder.size(); i++) {
            if (groupOrder.get(i).equalsIgnoreCase(group)) {
                return i;
            }
        }

        return 99999;
    }

    private String resolveGroup(Player player) {
        try {
            return safeValue(rank.getName(player.getUniqueId()));
        } catch (Exception exception) {
            return "default";
        }
    }

    private String joinLines(List<String> lines, Player player) {
        if (lines.isEmpty()) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                text.append('\n');
            }
            text.append(resolvePlaceholders(lines.get(i), player));
        }
        return text.toString();
    }

    private String resolvePlaceholders(String text, Player player) {
        return resolvePlaceholders(text, player, false);
    }

    private String resolvePlaceholders(String text, Player player, boolean stripResetMeta) {
        String prefix = "";
        String suffix = "";
        String rankName = "default";

        try {
            rankName = safeValue(rank.getName(player.getUniqueId()));
            prefix = safeValue(rank.getPrefix(player.getUniqueId()));
            suffix = safeValue(rank.getSuffix(player.getUniqueId()));
        } catch (Exception ex) {
            Bukkit.getLogger().fine("[Celest] Failed to resolve rank placeholders for " + player.getName() + ": " + ex.getMessage());
        }

        if (stripResetMeta) {
            prefix = normalizeNametagMeta(prefix);
            prefix = keepColorForName(prefix);
            suffix = stripResetCode(suffix);
        }

        return colorizeHex(text
                .replace("%player%", player.getName())
                .replace("%displayname%", player.getDisplayName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%max_online%", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("%rank%", rankName)
                .replace("{lp_prefix}", prefix)
                .replace("{lp_suffix}", suffix)
                .replace("%prefix%", prefix)
                .replace("%suffix%", suffix));
    }

    private String stripResetCode(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replaceAll("(?i)(?:&|\\u00A7)r", "");
    }

    private String normalizeNametagMeta(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String normalized = text
                .replaceAll("(?i)<\\s*reset\\s*>", "")
                .replaceAll("(?i)<\\s*r\\s*>", "");
        return stripResetCode(normalized);
    }

    private String keepColorForName(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return "";
        }

        String translated = CC.translate(colorizeHex(prefix));
        String lastColors = ChatColor.getLastColors(translated);
        if (lastColors == null || lastColors.isEmpty()) {
            return prefix;
        }

        return prefix + lastColors;
    }

    private ChatColor nameColorFromPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return ChatColor.WHITE;
        }

        String translated = CC.translate(colorizeHex(prefix));
        String lastColors = ChatColor.getLastColors(translated);
        if (lastColors == null || lastColors.isEmpty()) {
            return ChatColor.WHITE;
        }

        for (int i = lastColors.length() - 1; i >= 1; i--) {
            if (lastColors.charAt(i - 1) != '§') {
                continue;
            }

            ChatColor candidate = ChatColor.getByChar(lastColors.charAt(i));
            if (candidate == null) {
                continue;
            }
            if (candidate == ChatColor.RESET) {
                return ChatColor.WHITE;
            }
            if (candidate.isColor()) {
                return candidate;
            }
        }

        return ChatColor.WHITE;
    }

    private String safeValue(String text) {
        return text == null ? "" : text;
    }

    private String getText(String path, String fallback) {
        return config.getString(path, fallback, true);
    }

    private List<String> getList(String path) {
        return config.getStringList(path);
    }

    private List<String> getListWithFallback(String primaryPath, String fallbackPath) {
        List<String> list = getList(primaryPath);
        if (!list.isEmpty()) {
            return list;
        }
        return getList(fallbackPath);
    }

    private String colorizeHex(String text) {
        String translated = CC.translate(text == null ? "" : text);
        Matcher matcher = HEX_PATTERN.matcher(translated);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(result, Matcher.quoteReplacement(toLegacyHex(hex)));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private String toLegacyHex(String hex) {
        StringBuilder builder = new StringBuilder("§x");
        for (char character : hex.toCharArray()) {
            builder.append('§').append(character);
        }
        return builder.toString();
    }

    private boolean shouldSeeTablist(Player player) {
        Profile profile = ModuleService.getManagerModule().getProfileManager().getProfile(player.getUniqueId());
        return profile == null || profile.isShowTablist();
    }

    private Component deserialize(String text) {
        return SERIALIZER.deserialize(colorizeHex(text));
    }
}
