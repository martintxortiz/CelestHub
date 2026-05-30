package net.kryunek.hub.managers.queue;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.session.SessionGuard;
import net.kryunek.hub.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QueueCreateSession {

    private static final long TIMEOUT_TICKS = 20L * 60L;
    private static final Map<UUID, Long> active = new HashMap<>();

    public static void start(Player player) {
        if (!SessionGuard.canStart(player, "QUEUE_CREATE")) {
            return;
        }
        UUID uuid = player.getUniqueId();
        long startedAt = System.currentTimeMillis();
        active.put(uuid, startedAt);
        scheduleTimeout(uuid, startedAt);
    }

    public static void stop(Player player) {
        active.remove(player.getUniqueId());
    }

    public static boolean isActive(Player player) {
        return active.containsKey(player.getUniqueId());
    }

    public static int activeCount() {
        return active.size();
    }

    private static void scheduleTimeout(UUID uuid, long startedAt) {
        Bukkit.getScheduler().runTaskLater(Celest.get(), () -> {
            Long current = active.get(uuid);
            if (current == null || current != startedAt) {
                return;
            }

            active.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(CC.translate(ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES)
                        .getString("SESSION.EXPIRED", "&cEditor session expired after 60 seconds.", true)));
            }
        }, TIMEOUT_TICKS);
    }
}
