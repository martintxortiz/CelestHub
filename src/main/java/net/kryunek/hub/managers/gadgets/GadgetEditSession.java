package net.kryunek.hub.managers.gadgets;

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

public final class GadgetEditSession {

    private static final long TIMEOUT_TICKS = 20L * 60L;
    private static final Map<UUID, GadgetEditSession> SESSIONS = new HashMap<>();

    @Getter
    private final Type type;
    @Getter
    private final String key;
    @Getter
    private final long createdAt;

    private GadgetEditSession(Type type, String key, long createdAt) {
        this.type = type;
        this.key = key;
        this.createdAt = createdAt;
    }

    public static void start(Player player, Type type, String key) {
        if (!SessionGuard.canStart(player, "GADGET_EDIT")) {
            return;
        }
        UUID uuid = player.getUniqueId();
        long startedAt = System.currentTimeMillis();
        SESSIONS.put(uuid, new GadgetEditSession(type, key, startedAt));
        scheduleTimeout(uuid, startedAt);
    }

    public static boolean isActive(Player player) {
        return SESSIONS.containsKey(player.getUniqueId());
    }

    public static GadgetEditSession get(Player player) {
        return SESSIONS.get(player.getUniqueId());
    }

    public static void stop(Player player) {
        SESSIONS.remove(player.getUniqueId());
    }

    private static void scheduleTimeout(UUID uuid, long startedAt) {
        Bukkit.getScheduler().runTaskLater(Celest.get(), () -> {
            GadgetEditSession session = SESSIONS.get(uuid);
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
        COOLDOWN,
        SLOT
    }
}
