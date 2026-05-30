package net.kryunek.hub.managers.queue;

import lombok.Getter;
import lombok.Setter;
import net.kryunek.hub.managers.rank.RankManager;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.TaskUtil;
import net.kryunek.hub.utils.bungee.BungeeUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Getter
@Setter
public class Queue {

    private final List<UUID> playerList = new ArrayList<>();
    private boolean paused;
    private final String server;
    private final QueueManager queueManager;
    private final RankManager rankManager;
    private final FileConfig config;
    private BukkitTask positionTask;

    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    public Queue(String server, QueueManager queueManager, RankManager rankManager, FileConfig config) {
        this.server = server;
        this.queueManager = queueManager;
        this.rankManager = rankManager;
        this.config = config;
        startPositionTask();
    }

    public void startPositionTask() {
        stopPositionTask();
        long delayTicks = toTicks(config.getInt("QUEUE.POSITION_MESSAGE_DELAY"));
        this.positionTask = TaskUtil.runSyncTimer(() -> {
            Iterator<UUID> iterator = playerList.iterator();

            while (iterator.hasNext()) {
                UUID playerId = iterator.next();
                Player player = Bukkit.getPlayer(playerId);

                if (getSize() <= 0) {
                    continue;
                }

                if (player == null || !player.isOnline()) {
                    continue;
                }

                int position = getPosition(player);
                if (position <= 0) {
                    queueManager.removeFromMap(player);
                    continue;
                }

                for (String s : config.getStringList("QUEUE_POSITION")) {
                    String estimated = getEstimatedTimeFormatted(position);
                    String parsed = CC.translate(
                            s.replace("%queue_pos%", String.valueOf(position))
                                    .replace("%queue_size%", String.valueOf(getSize()))
                                    .replace("%queue%", server)
                                    .replace("%estimated_time%", estimated)
                    );

                    player.sendMessage(parsed);
                    player.sendActionBar(serializer.deserialize(parsed));
                }
            }
        }, delayTicks, delayTicks);
    }

    public void stopPositionTask() {
        if (positionTask != null) {
            positionTask.cancel();
            positionTask = null;
        }
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        config.getConfiguration().set("QUEUE.SERVERS." + server + ".paused", paused);
        config.save();
        queueManager.persistAndSyncQueues(true);
    }

    public void setPausedSilently(boolean paused) {
        this.paused = paused;
    }

    public void removeEntry(Player player) {
        if (player == null) {
            return;
        }

        removeEntry(player.getUniqueId());
    }

    public void removeEntry(UUID playerId) {
        playerList.remove(playerId);
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            queueManager.removeFromMap(player);
        }
        queueManager.persistAndSyncQueues(true);
    }

    public void loadEntries(List<String> rawEntries) {
        playerList.clear();
        if (rawEntries == null) {
            return;
        }
        for (String raw : rawEntries) {
            try {
                UUID uuid = UUID.fromString(raw);
                if (!playerList.contains(uuid)) {
                    playerList.add(uuid);
                }
            } catch (IllegalArgumentException ex) {
                Bukkit.getLogger().warning("[Celest] Ignoring invalid queue entry UUID for " + server + ": " + raw);
            }
        }
    }

    public List<String> serializeEntries() {
        List<String> serialized = new ArrayList<>();
        for (UUID uuid : playerList) {
            serialized.add(uuid.toString());
        }
        return serialized;
    }

    public boolean contains(UUID uuid) {
        return playerList.contains(uuid);
    }

    public boolean addEntry(UUID uuid) {
        if (playerList.contains(uuid)) {
            return true;
        }
        playerList.add(uuid);
        playerList.sort(Comparator.comparingInt(this::getPriority).reversed());
        queueManager.persistAndSyncQueues(true);
        return true;
    }

    public UUID getPlayerIdAt(int i) {
        return playerList.get(i);
    }

    public Player getPlayerAt(int i) {
        return Bukkit.getPlayer(getPlayerIdAt(i));
    }

    public int getPosition(UUID playerId) {
        int index = playerList.indexOf(playerId);
        return index < 0 ? 0 : index + 1;
    }

    public boolean addEntry(Player player) {
        UUID playerId = player.getUniqueId();
        if (playerList.contains(playerId)) {
            return true;
        }

        if (getPriority(playerId) == 0) {
            sendDirect(player);
            return false;
        }

        playerList.add(playerId);
        playerList.sort(Comparator.comparingInt(this::getPriority).reversed());
        queueManager.persistAndSyncQueues(true);
        return true;
    }

    public int getPosition(Player player) {
        return getPosition(player.getUniqueId());
    }

    public void sendFirst() {
        if (playerList.isEmpty()) {
            return;
        }

        UUID playerId = playerList.get(0);
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            // Only remove offline players when it's their turn to be sent.
            removeEntry(playerId);
            return;
        }

        BungeeUtils.sendToServer(player, server);
        removeEntry(playerId);
    }

    public int getSize() {
        return playerList.size();
    }

    public void sendDirect(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        BungeeUtils.sendToServer(player, server);
        queueManager.removeFromMap(player);
    }

    public int getPriority(UUID playerId) {
        String rankName = rankManager.getRank().getName(playerId);
        if (rankName == null || rankName.isBlank()) {
            rankName = "default";
        }
        return config.getInt("QUEUE.PRIORITY." +
                rankName.toLowerCase(Locale.ROOT));
    }

    private String getEstimatedTimeFormatted(int position) {
        if (paused) {
            return "paused";
        }

        int delaySeconds = Math.max(1, config.getInt("QUEUE.DELAY"));
        int waitSeconds = position * delaySeconds;
        if (waitSeconds <= 0) {
            waitSeconds = 1;
        }

        if (waitSeconds < 60) {
            return waitSeconds + "s";
        }

        int minutes = waitSeconds / 60;
        int seconds = waitSeconds % 60;
        return minutes + "m " + seconds + "s";
    }

    private long toTicks(int seconds) {
        return Math.max(1L, seconds) * 20L;
    }
}
