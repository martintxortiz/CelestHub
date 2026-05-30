package net.kryunek.hub.managers.queue;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.session.SessionGuard;
import net.kryunek.hub.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class QueueEditSession {

    private static final long TIMEOUT_TICKS = 20L * 60L;
    private static final Map<UUID, QueueEditSession> SESSIONS = new HashMap<>();

    @Getter
    private final Type type;
    @Getter
    private final String server;
    @Getter
    private final long createdAt;

    private QueueEditSession(Type type, String server, long createdAt) {
        this.type = type;
        this.server = server;
        this.createdAt = createdAt;
    }

    public static void start(Player player, Type type, String server) {
        if (!SessionGuard.canStart(player, "QUEUE_EDIT")) {
            return;
        }
        UUID uuid = player.getUniqueId();
        long startedAt = System.currentTimeMillis();
        SESSIONS.put(uuid, new QueueEditSession(type, server, startedAt));
        scheduleTimeout(uuid, startedAt);
    }

    public static void startGlobal(Player player, Type type) {
        start(player, type, null);
    }

    public static QueueEditSession get(Player player) {
        return SESSIONS.get(player.getUniqueId());
    }

    public static boolean isActive(Player player) {
        return SESSIONS.containsKey(player.getUniqueId());
    }

    public static void stop(Player player) {
        SESSIONS.remove(player.getUniqueId());
    }

    public static int activeCount() {
        return SESSIONS.size();
    }

    public boolean isGlobal() {
        return server == null || server.isEmpty();
    }

    private static void scheduleTimeout(UUID uuid, long startedAt) {
        Bukkit.getScheduler().runTaskLater(Celest.get(), () -> {
            QueueEditSession session = SESSIONS.get(uuid);
            if (session == null || session.getCreatedAt() != startedAt) {
                return;
            }

            SESSIONS.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(CC.translate(ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES)
                        .getString("SESSION.EXPIRED", "&cEditor session expired after 60 seconds.", true)));
            }
        }, TIMEOUT_TICKS);
    }

    @Getter
    @RequiredArgsConstructor
    public enum Type {
        QUEUE_DELAY("QUEUE.EDITOR.BUTTONS.QUEUE_DELAY"),
        MESSAGE_DELAY("QUEUE.EDITOR.BUTTONS.MESSAGE_DELAY");

        private final String configPath;
    }
}
