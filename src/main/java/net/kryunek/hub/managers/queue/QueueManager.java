package net.kryunek.hub.managers.queue;

import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.rank.IRankManager;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.TaskUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QueueManager {

    private final List<Queue> queues = new ArrayList<>();
    private final Map<UUID, Queue> playerQueueMap = new HashMap<>();
    private final FileConfig config;
    private final IRankManager rankManager;
    private BukkitTask sendTask;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    public QueueManager(IRankManager rankManager) {
        this.config = ModuleService.getFileModule().getFile("queue");
        this.rankManager = rankManager;
        migrateLegacyTickValuesIfNeeded();
        loadQueues();
        iniciarSendTask();
    }

    public void iniciarSendTask() {
        detenerSendTask();
        long delayTicks = toTicks(config.getInt("QUEUE.DELAY"));
        this.sendTask = TaskUtil.runSyncTimer(() -> {
            for (Queue queue : queues) {
                if (queue.isPaused() || queue.getPlayerList().isEmpty()) {
                    continue;
                }

                Player player = queue.getPlayerAt(0);
                if (player != null && player.isOnline()) {
                    queue.sendFirst();
                }
            }
        }, delayTicks, delayTicks);
    }

    public void detenerSendTask() {
        if (sendTask != null) {
            sendTask.cancel();
            sendTask = null;
        }
    }

    public void shutdown() {
        detenerSendTask();
        for (Queue queue : queues) {
            queue.detenerTaskPosicion();
        }
    }

    public void loadQueues() {
        for (Queue queue : queues) {
            queue.detenerTaskPosicion();
        }
        queues.clear();

        ConfigurationSection section = config.getConfiguration().getConfigurationSection("QUEUE.SERVERS");
        if (section == null) {
            return;
        }

        for (String server : section.getKeys(false)) {
            boolean paused = section.getBoolean(server + ".paused");
            Queue queue = new Queue(server, this, rankManager, config);
            queue.setPausedSilently(paused);
            queue.loadEntries(section.getStringList(server + ".entries"));
            queues.add(queue);
        }
        rebuildPlayerQueueMap();
    }

    public void createQueue(String name) {
        if (getQueue(name) != null) {
            return;
        }

        Queue queue = new Queue(name, this, rankManager, config);
        queues.add(queue);

        config.getConfiguration().set("QUEUE.SERVERS." + name + ".paused", false);
        persistAndSyncQueues(true);
    }

    public void deleteQueue(String name) {
        Queue queue = getQueue(name);
        if (queue == null) {
            return;
        }

        queues.remove(queue);
        config.getConfiguration().set("QUEUE.SERVERS." + name, null);
        persistAndSyncQueues(true);
    }

    public void addToQueue(Player player, String name) {
        Queue queue = getQueue(name);

        if (queue == null) {
            player.sendMessage(CC.translate(config.getString("QUEUE_NOT_FOUND").replace("%queue%", name)));
            return;
        }

        if (playerQueueMap.containsKey(player.getUniqueId())) {
            Queue old = playerQueueMap.get(player.getUniqueId());
            sendActionBar(player, "ACTIONBAR.QUEUE_LEAVE", Map.of("%queue%", old.getServer()));
            old.removeEntry(player);
        }

        boolean queued = queue.addEntry(player);
        if (queued) {
            playerQueueMap.put(player.getUniqueId(), queue);
        } else {
            playerQueueMap.remove(player.getUniqueId());
        }

        player.sendMessage(CC.translate(config.getString("QUEUE_JOIN").replace("%queue%", name)));
        sendActionBar(player, "ACTIONBAR.QUEUE_JOIN", Map.of("%queue%", name));
        persistAndSyncQueues(true);
    }

    public void removeFromMap(Player player) {
        playerQueueMap.remove(player.getUniqueId());
    }

    public Queue getQueue(String name) {
        return queues.stream()
                .filter(q -> q.getServer().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public Queue getQueue(Player player) {
        if (player == null) {
            return null;
        }
        UUID playerId = player.getUniqueId();

        Queue queue = playerQueueMap.get(player.getUniqueId());
        if (queue == null) {
            for (Queue candidate : queues) {
                if (candidate.contains(playerId)) {
                    playerQueueMap.put(playerId, candidate);
                    return candidate;
                }
            }
            return null;
        }
        if (!queue.contains(playerId)) {
            playerQueueMap.remove(playerId);
            return null;
        }
        return queue;
    }

    public boolean isInQueue(Player player) {
        return getQueue(player) != null;
    }

    public int getQueueSize(String name) {
        Queue queue = getQueue(name);
        return queue != null ? queue.getSize() : 0;
    }

    public List<Queue> getQueues() {
        return queues;
    }

    public void updateQueueDelay(int delaySeconds) {
        config.getConfiguration().set("QUEUE.DELAY", delaySeconds);
        config.save();
        iniciarSendTask();
        notifyQueueSync();
    }

    public void updatePositionMessageDelay(int delaySeconds) {
        config.getConfiguration().set("QUEUE.POSITION_MESSAGE_DELAY", delaySeconds);
        config.save();
        for (Queue queue : queues) {
            queue.iniciarTaskPosicion();
        }
        notifyQueueSync();
    }

    public void persistAndSyncQueues(boolean publish) {
        ConfigurationSection section = config.getConfiguration().getConfigurationSection("QUEUE.SERVERS");
        if (section == null) {
            section = config.getConfiguration().createSection("QUEUE.SERVERS");
        }
        for (Queue queue : queues) {
            String base = "QUEUE.SERVERS." + queue.getServer();
            config.getConfiguration().set(base + ".paused", queue.isPaused());
            config.getConfiguration().set(base + ".entries", queue.serializeEntries());
        }
        config.save();
        if (publish) {
            notifyQueueSync();
        }
    }

    private void rebuildPlayerQueueMap() {
        playerQueueMap.clear();
        for (Queue queue : queues) {
            for (UUID uuid : queue.getPlayerList()) {
                Player online = org.bukkit.Bukkit.getPlayer(uuid);
                if (online != null) {
                    playerQueueMap.put(uuid, queue);
                }
            }
        }
    }

    private void notifyQueueSync() {
        if (ModuleService.getManagerModule() != null && ModuleService.getManagerModule().getNetworkSyncManager() != null) {
            ModuleService.getManagerModule().getNetworkSyncManager().publishQueueState(config.getConfiguration().saveToString());
        }
    }

    public void applyRemoteSnapshot(String yaml) {
        if (yaml == null || yaml.isBlank()) {
            return;
        }
        try {
            config.getConfiguration().loadFromString(yaml);
            config.save();
            loadQueues();
            iniciarSendTask();
        } catch (InvalidConfigurationException e) {
            org.bukkit.Bukkit.getLogger().warning("[Celest] Invalid remote queue snapshot.");
        }
    }

    private long toTicks(int seconds) {
        return Math.max(1L, seconds) * 20L;
    }

    private void migrateLegacyTickValuesIfNeeded() {
        if (config.getConfiguration().getBoolean("QUEUE.LEGACY_TICKS_MIGRATED")) {
            return;
        }

        int oldDelayTicks = Math.max(1, config.getInt("QUEUE.DELAY"));
        int oldMessageDelayTicks = Math.max(1, config.getInt("QUEUE.POSITION_MESSAGE_DELAY"));

        int delaySeconds = (int) Math.ceil(oldDelayTicks / 20.0D);
        int messageDelaySeconds = (int) Math.ceil(oldMessageDelayTicks / 20.0D);

        config.getConfiguration().set("QUEUE.DELAY", Math.max(1, delaySeconds));
        config.getConfiguration().set("QUEUE.POSITION_MESSAGE_DELAY", Math.max(1, messageDelaySeconds));
        config.getConfiguration().set("QUEUE.LEGACY_TICKS_MIGRATED", true);
        config.save();
    }

    private void sendActionBar(Player player, String path, Map<String, String> placeholders) {
        FileConfig messages = ModuleService.getFileModule().getFile("messages");
        String prefix = messages.getString("ACTIONBAR.PREFIX", "", true);
        String message = messages.getString(path, "", true);
        if (message == null || message.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }

        player.sendActionBar(serializer.deserialize(CC.translate((prefix == null ? "" : prefix) + message)));
    }
}
