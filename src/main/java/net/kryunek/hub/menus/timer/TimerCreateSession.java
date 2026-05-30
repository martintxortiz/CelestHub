package net.kryunek.hub.menus.timer;

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

public class TimerCreateSession {

    private static final long TIMEOUT_TICKS = 20L * 60L;
    private static final Map<UUID, Long> active = new HashMap<>();

    public static void start(Player player) {
        if (!SessionGuard.canStart(player, "TIMER_CREATE")) {
            return;
        }
        UUID uuid = player.getUniqueId();
        long startedAt = System.currentTimeMillis();
        active.put(uuid, startedAt);
        scheduleTimeout(uuid, startedAt);
    }

    public static boolean isActive(Player player) {
        return active.containsKey(player.getUniqueId());
    }

    public static int activeCount() {
        return active.size();
    }

    public static void handleChat(Player player, String message) {
        if (!isActive(player)) {
            return;
        }

        active.remove(player.getUniqueId());

        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(CC.translate(ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES).getString("TIMER.SESSION.CANCELLED")));
            return;
        }

        String[] args = message.split(" ");
        if (args.length < 2) {
            player.sendMessage(CC.translate(ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES).getString("TIMER.SESSION.USAGE")));
            return;
        }

        String name = args[0];
        long seconds;

        try {
            seconds = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate(ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES).getString("TIMER.SESSION.INVALID_NUMBER")));
            return;
        }

        String prefix = args.length >= 3 ? args[2] : name;
        ModuleService.getManagerModule().getTimerManager().createTimer(player, name, seconds, prefix);
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
