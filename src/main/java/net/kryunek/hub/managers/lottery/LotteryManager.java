package net.kryunek.hub.managers.lottery;

import net.kryunek.hub.support.config.ConfigSupport;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.hotbar.Hotbar;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.TaskUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Runs timed lotteries — creation, joining, winner selection and reward payout — persisting to the
 * injected lottery config and mirroring state across hubs via {@code NetworkSyncManager}.
 */
public class LotteryManager {

    private final Map<String, Lottery> lotteries = new LinkedHashMap<>();
    private final FileConfig lotteryConfig;
    private final FileConfig messages;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
    private BukkitTask tickerTask;
    private BukkitTask reminderTask;

    public LotteryManager(FileConfig lotteryConfig, FileConfig messages) {
        this.lotteryConfig = lotteryConfig;
        this.messages = messages;
        ensureDefaults();
        loadLotteries();
        startTickerTask();
        startReminderTask();
    }

    private void ensureDefaults() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("LOTTERY.JOIN_ITEM.ENABLED", true);
        defaults.put("LOTTERY.JOIN_ITEM.MATERIAL", "PAPER");
        defaults.put("LOTTERY.JOIN_ITEM.NAME", "&d&lLOTTERY TICKET &7(Right Click)");
        defaults.put("LOTTERY.JOIN_ITEM.LORE", List.of(
                "&7A lottery is active right now.",
                "&eRight click to join instantly."
        ));
        defaults.put("LOTTERY.JOIN_ITEM.SLOT", 7);
        defaults.put("LOTTERY.JOIN_ITEM.AMOUNT", 1);
        defaults.put("LOTTERY.DEFAULT_WINNERS", 1);

        if (ConfigSupport.applyMissingDefaults(lotteryConfig.getConfiguration(), defaults)) {
            lotteryConfig.save();
        }
    }

    public void loadLotteries() {
        lotteries.clear();
        ConfigurationSection section = lotteryConfig.getConfiguration().getConfigurationSection("LOTTERY.LIST");
        if (section == null) {
            refreshJoinItems();
            return;
        }

        long now = System.currentTimeMillis();
        boolean changed = false;

        for (String key : section.getKeys(false)) {
            String base = "LOTTERY.LIST." + key;
            int duration = Math.max(1, lotteryConfig.getInt(base + ".DURATION_SECONDS"));
            int defaultWinners = Math.max(1, lotteryConfig.getInt("LOTTERY.DEFAULT_WINNERS"));
            int winnersCount = lotteryConfig.getConfiguration().contains(base + ".WINNERS")
                    ? Math.max(1, lotteryConfig.getInt(base + ".WINNERS"))
                    : defaultWinners;
            List<String> rewards = new ArrayList<>();
            for (String reward : lotteryConfig.getConfiguration().getStringList(base + ".REWARDS")) {
                if (reward == null || reward.isBlank()) {
                    changed = true;
                    continue;
                }
                if (reward.regionMatches(true, 0, "COSMETIC:", 0, 9)) {
                    changed = true;
                    continue;
                }
                rewards.add(reward);
            }
            Lottery lottery = new Lottery(key, duration, winnersCount, rewards);
            for (String rawUuid : lotteryConfig.getConfiguration().getStringList(base + ".PARTICIPANTS")) {
                try {
                    lottery.addParticipant(UUID.fromString(rawUuid));
                } catch (IllegalArgumentException ex) {
                    Bukkit.getLogger().warning("[Celest] Ignoring invalid lottery participant UUID for " + key + ": " + rawUuid);
                    changed = true;
                }
            }

            boolean active = lotteryConfig.getBoolean(base + ".ACTIVE");
            long endAt = lotteryConfig.getLong(base + ".END_AT");
            if (active && endAt > now) {
                lottery.setActive(true);
                lottery.setEndAt(endAt);
            } else if (active) {
                changed = true;
            }

            lotteries.put(key.toLowerCase(), lottery);
        }

        if (changed) {
            saveAll();
        }
        refreshJoinItems();
    }

    public Collection<Lottery> getLotteries() {
        return lotteries.values();
    }

    public Lottery getLottery(String name) {
        if (name == null) {
            return null;
        }
        return lotteries.get(name.toLowerCase());
    }

    public boolean createLottery(String name, int durationSeconds) {
        if (name == null || name.isBlank() || getLottery(name) != null) {
            return false;
        }

        Lottery lottery = new Lottery(name, durationSeconds, Math.max(1, lotteryConfig.getInt("LOTTERY.DEFAULT_WINNERS")), new ArrayList<>());
        lotteries.put(name.toLowerCase(), lottery);
        saveLottery(lottery);
        notifyLotterySync();
        return true;
    }

    public boolean updateWinnersCount(String name, int winnersCount) {
        Lottery lottery = getLottery(name);
        if (lottery == null) {
            return false;
        }
        lottery.setWinnersCount(Math.max(1, winnersCount));
        saveLottery(lottery);
        notifyLotterySync();
        return true;
    }

    public boolean deleteLottery(String name) {
        Lottery removed = lotteries.remove(name.toLowerCase());
        if (removed == null) {
            return false;
        }

        lotteryConfig.getConfiguration().set("LOTTERY.LIST." + removed.getName(), null);
        lotteryConfig.save();
        refreshJoinItems();
        notifyLotterySync();
        return true;
    }

    public boolean addReward(String name, String rewardCommand) {
        Lottery lottery = getLottery(name);
        if (lottery == null || rewardCommand == null || rewardCommand.isBlank()) {
            return false;
        }

        String normalized = rewardCommand.trim();
        if (!normalized.regionMatches(true, 0, "COMMAND:", 0, 8)) {
            normalized = "COMMAND:" + normalized;
        }

        lottery.getRewards().add(normalized);
        saveLottery(lottery);
        notifyLotterySync();
        return true;
    }

    public boolean clearRewards(String name) {
        Lottery lottery = getLottery(name);
        if (lottery == null) {
            return false;
        }

        lottery.getRewards().clear();
        saveLottery(lottery);
        notifyLotterySync();
        return true;
    }

    public boolean removeReward(String name, int rewardIndex) {
        Lottery lottery = getLottery(name);
        if (lottery == null) {
            return false;
        }
        if (rewardIndex < 0 || rewardIndex >= lottery.getRewards().size()) {
            return false;
        }

        lottery.getRewards().remove(rewardIndex);
        saveLottery(lottery);
        notifyLotterySync();
        return true;
    }

    public boolean startLottery(String name, CommandSender starter) {
        Lottery lottery = getLottery(name);
        if (lottery == null || lottery.isActive()) {
            return false;
        }

        lottery.getParticipants().clear();
        lottery.setActive(true);
        lottery.setEndAt(System.currentTimeMillis() + (lottery.getDurationSeconds() * 1000L));
        saveLottery(lottery);
        broadcastStart(lottery, starter.getName());
        refreshJoinItems();
        notifyLotterySync();
        return true;
    }

    public boolean endLottery(String name, String endedBy) {
        Lottery lottery = getLottery(name);
        if (lottery == null || !lottery.isActive()) {
            return false;
        }

        finishLottery(lottery, endedBy);
        return true;
    }

    public JoinResult joinLottery(Player player, String name) {
        Lottery lottery = getLottery(name);
        if (lottery == null) {
            return JoinResult.NOT_FOUND;
        }
        if (!lottery.isActive()) {
            return JoinResult.NOT_ACTIVE;
        }
        if (!lottery.addParticipant(player.getUniqueId())) {
            return JoinResult.ALREADY_JOINED;
        }
        saveLottery(lottery);
        updateJoinItem(player);
        notifyLotterySync();
        return JoinResult.JOINED;
    }

    private void startTickerTask() {
        if (tickerTask != null) {
            tickerTask.cancel();
        }

        tickerTask = TaskUtil.runSyncTimer(() -> {
            long now = System.currentTimeMillis();
            List<Lottery> finished = new ArrayList<>();

            for (Lottery lottery : lotteries.values()) {
                if (!lottery.isActive()) {
                    continue;
                }
                if (lottery.getEndAt() <= now) {
                    finished.add(lottery);
                }
            }

            for (Lottery lottery : finished) {
                finishLottery(lottery, "System");
            }
        }, 20L, 20L);
    }

    private void startReminderTask() {
        if (reminderTask != null) {
            reminderTask.cancel();
        }

        int intervalSeconds = Math.max(5, lotteryConfig.getInt("LOTTERY.BROADCAST_REMINDER_SECONDS"));
        reminderTask = TaskUtil.runSyncTimer(() -> {
            for (Lottery lottery : lotteries.values()) {
                if (!lottery.isActive()) {
                    continue;
                }
                sendJoinBroadcast(lottery, "LOTTERY.BROADCAST.REMINDER_LINES");
            }
        }, intervalSeconds * 20L, intervalSeconds * 20L);
    }

    public void shutdown() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }
        if (reminderTask != null) {
            reminderTask.cancel();
            reminderTask = null;
        }
    }

    public int getReminderIntervalSeconds() {
        return Math.max(5, lotteryConfig.getInt("LOTTERY.BROADCAST_REMINDER_SECONDS"));
    }

    public void updateReminderIntervalSeconds(int seconds) {
        int value = Math.max(5, seconds);
        lotteryConfig.getConfiguration().set("LOTTERY.BROADCAST_REMINDER_SECONDS", value);
        lotteryConfig.save();
        startReminderTask();
        notifyLotterySync();
    }

    private void finishLottery(Lottery lottery, String endedBy) {
        List<UUID> winnerIds = pickWinners(lottery);
        List<String> winnerNames = new ArrayList<>();
        for (UUID winnerId : winnerIds) {
            winnerNames.add(offlineName(winnerId));
        }

        if (winnerNames.isEmpty()) {
            for (String line : messages.getStringList("LOTTERY.BROADCAST.NO_PARTICIPANTS")) {
                Bukkit.broadcastMessage(CC.translate(line
                        .replace("%lottery%", lottery.getName())
                        .replace("%by%", endedBy)));
            }
        } else {
            executeRewards(lottery, winnerNames);
            String winnerDisplay = String.join(", ", winnerNames);
            for (String line : messages.getStringList("LOTTERY.BROADCAST.WINNER_LINES")) {
                Bukkit.broadcastMessage(CC.translate(line
                        .replace("%lottery%", lottery.getName())
                        .replace("%winner%", winnerDisplay)
                        .replace("%by%", endedBy)));
            }
        }

        lottery.setActive(false);
        lottery.setEndAt(0L);
        lottery.getParticipants().clear();
        saveLottery(lottery);
        refreshJoinItems();
        notifyLotterySync();
    }

    private List<UUID> pickWinners(Lottery lottery) {
        if (lottery.getParticipants().isEmpty()) {
            return List.of();
        }

        List<UUID> entries = new ArrayList<>(lottery.getParticipants());
        Collections.shuffle(entries, ThreadLocalRandom.current());
        int winnerCount = Math.max(1, Math.min(entries.size(), lottery.getWinnersCount()));
        return new ArrayList<>(entries.subList(0, winnerCount));
    }

    private void executeRewards(Lottery lottery, List<String> winners) {
        if (lottery.getRewards().isEmpty()) {
            return;
        }

        for (String winnerName : winners) {
            for (String reward : lottery.getRewards()) {
                if (reward == null || reward.isBlank()) {
                    continue;
                }

                String raw = reward.trim();
                if (raw.regionMatches(true, 0, "COMMAND:", 0, 8)) {
                    raw = raw.substring(8).trim();
                } else if (raw.contains(":")) {
                    continue;
                }

                String command = raw.replace("%player%", winnerName).trim();
                if (command.startsWith("/")) {
                    command = command.substring(1);
                }
                if (!command.isBlank()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            }
        }
    }

    private String offlineName(UUID uuid) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        if (offline.getName() != null && !offline.getName().isBlank()) {
            return offline.getName();
        }
        return uuid.toString();
    }

    private void broadcastStart(Lottery lottery, String startedBy) {
        Bukkit.broadcastMessage(" ");
        for (String line : messages.getStringList("LOTTERY.BROADCAST.START_LINES")) {
            Bukkit.broadcastMessage(CC.translate(line
                    .replace("%lottery%", lottery.getName())
                    .replace("%seconds%", String.valueOf(lottery.getDurationSeconds()))
                    .replace("%by%", startedBy)));
        }
        Bukkit.broadcastMessage(" ");
        sendJoinBroadcast(lottery, "LOTTERY.BROADCAST.JOIN_LINES");
    }

    private void sendJoinBroadcast(Lottery lottery, String path) {
        for (String line : messages.getStringList(path)) {
            String parsed = CC.translate(line
                    .replace("%lottery%", lottery.getName())
                    .replace("%seconds%", String.valueOf(lottery.getRemainingSeconds()))
                    .replace("%count%", String.valueOf(lottery.getParticipantCount())));

            Component component = serializer.deserialize(parsed)
                    .clickEvent(ClickEvent.runCommand("/lottery join " + lottery.getName()))
                    .hoverEvent(HoverEvent.showText(serializer.deserialize(CC.translate("&aClick to join &f" + lottery.getName()))));

            Bukkit.getOnlinePlayers().forEach(player -> {
                player.sendMessage(Component.empty());
                player.sendMessage(component);
                player.sendMessage(Component.empty());
            });
            Bukkit.getConsoleSender().sendMessage(serializer.serialize(component));
        }
    }

    private void saveLottery(Lottery lottery) {
        String base = "LOTTERY.LIST." + lottery.getName();
        lotteryConfig.getConfiguration().set(base + ".DURATION_SECONDS", Math.max(1, lottery.getDurationSeconds()));
        lotteryConfig.getConfiguration().set(base + ".WINNERS", Math.max(1, lottery.getWinnersCount()));
        lotteryConfig.getConfiguration().set(base + ".REWARDS", new ArrayList<>(lottery.getRewards()));
        lotteryConfig.getConfiguration().set(base + ".PARTICIPANTS", lottery.getParticipants().stream()
                .map(UUID::toString)
                .toList());
        lotteryConfig.getConfiguration().set(base + ".ACTIVE", lottery.isActive());
        lotteryConfig.getConfiguration().set(base + ".END_AT", lottery.getEndAt());
        lotteryConfig.save();
    }

    private void saveAll() {
        lotteryConfig.getConfiguration().set("LOTTERY.LIST", null);
        for (Lottery lottery : lotteries.values()) {
            String base = "LOTTERY.LIST." + lottery.getName();
            lotteryConfig.getConfiguration().set(base + ".DURATION_SECONDS", Math.max(1, lottery.getDurationSeconds()));
            lotteryConfig.getConfiguration().set(base + ".WINNERS", Math.max(1, lottery.getWinnersCount()));
            lotteryConfig.getConfiguration().set(base + ".REWARDS", new ArrayList<>(lottery.getRewards()));
            lotteryConfig.getConfiguration().set(base + ".PARTICIPANTS", lottery.getParticipants().stream()
                    .map(UUID::toString)
                    .toList());
            lotteryConfig.getConfiguration().set(base + ".ACTIVE", lottery.isActive());
            lotteryConfig.getConfiguration().set(base + ".END_AT", lottery.getEndAt());
        }
        lotteryConfig.save();
    }

    public Lottery getFirstActiveLottery() {
        for (Lottery lottery : lotteries.values()) {
            if (lottery.isActive()) {
                return lottery;
            }
        }
        return null;
    }

    public Lottery getSingleActiveLottery() {
        Lottery found = null;
        for (Lottery lottery : lotteries.values()) {
            if (!lottery.isActive()) {
                continue;
            }
            if (found != null) {
                return null;
            }
            found = lottery;
        }
        return found;
    }

    public boolean isJoinItem(ItemStack stack) {
        Hotbar joinHotbar = getJoinHotbar();
        if (joinHotbar == null || joinHotbar.getItem() == null) {
            return false;
        }
        return joinHotbar.isHotbarItem(stack);
    }

    public void handleJoin(Player player) {
        updateJoinItem(player);
    }

    private void refreshJoinItems() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            updateJoinItem(online);
        }
    }

    private void updateJoinItem(Player player) {
        Lottery active = getSingleActiveLottery();
        Hotbar joinHotbar = getJoinHotbar();
        if (active == null
                || active.getParticipants().contains(player.getUniqueId())
                || joinHotbar == null
                || !joinHotbar.isEnabled()
                || joinHotbar.getItem() == null) {
            removeJoinItems(player);
            return;
        }

        if (containsJoinItem(player)) {
            return;
        }

        ItemStack joinItem = joinHotbar.getItem().clone();
        int preferredSlot = Math.max(0, Math.min(35, joinHotbar.getSlot()));
        if (player.getInventory().getItem(preferredSlot) == null || player.getInventory().getItem(preferredSlot).getType() == Material.AIR) {
            player.getInventory().setItem(preferredSlot, joinItem);
            return;
        }

        int firstEmpty = player.getInventory().firstEmpty();
        if (firstEmpty >= 0) {
            player.getInventory().setItem(firstEmpty, joinItem);
            return;
        }

        player.getInventory().setItem(preferredSlot, joinItem);
    }

    private boolean containsJoinItem(HumanEntity player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (isJoinItem(stack)) {
                return true;
            }
        }
        return false;
    }

    private void removeJoinItems(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isJoinItem(contents[i])) {
                contents[i] = null;
            }
        }
        player.getInventory().setContents(contents);
    }

    private Hotbar getJoinHotbar() {
        if (ModuleService.getManagerModule() == null || ModuleService.getManagerModule().getHotbarManager() == null) {
            return null;
        }
        return ModuleService.getManagerModule().getHotbarManager().getHotbar("LOTTERY_JOIN");
    }

    private void notifyLotterySync() {
        if (ModuleService.getManagerModule() != null && ModuleService.getManagerModule().getNetworkSyncManager() != null) {
            ModuleService.getManagerModule().getNetworkSyncManager().publishLotteryState(lotteryConfig.getConfiguration().saveToString());
        }
    }

    public void applyRemoteSnapshot(String yaml) {
        if (yaml == null || yaml.isBlank()) {
            return;
        }
        try {
            lotteryConfig.getConfiguration().loadFromString(yaml);
            lotteryConfig.save();
            loadLotteries();
            startReminderTask();
        } catch (InvalidConfigurationException e) {
            Bukkit.getLogger().warning("[Celest] Invalid remote lottery snapshot.");
        }
    }

    public enum JoinResult {
        JOINED,
        ALREADY_JOINED,
        NOT_ACTIVE,
        NOT_FOUND
    }
}
