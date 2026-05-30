package net.kryunek.hub.managers.lottery;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.Getter;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.session.SessionGuard;
import net.kryunek.hub.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LotteryRewardSession {

    private static final long TIMEOUT_TICKS = 20L * 60L;
    private static final Map<UUID, LotteryRewardSession> ACTIVE = new HashMap<>();

    @Getter
    private final String lotteryName;
    @Getter
    private final long createdAt;

    private LotteryRewardSession(String lotteryName, long createdAt) {
        this.lotteryName = lotteryName;
        this.createdAt = createdAt;
    }

    public static void start(Player player, String lotteryName) {
        if (!SessionGuard.canStart(player, "LOTTERY_REWARD")) {
            return;
        }
        UUID uuid = player.getUniqueId();
        long startedAt = System.currentTimeMillis();
        ACTIVE.put(uuid, new LotteryRewardSession(lotteryName, startedAt));
        scheduleTimeout(uuid, startedAt);
    }

    public static LotteryRewardSession get(Player player) {
        return ACTIVE.get(player.getUniqueId());
    }

    public static boolean isActive(Player player) {
        return ACTIVE.containsKey(player.getUniqueId());
    }

    public static void stop(Player player) {
        ACTIVE.remove(player.getUniqueId());
    }

    private static void scheduleTimeout(UUID uuid, long startedAt) {
        Bukkit.getScheduler().runTaskLater(Celest.get(), () -> {
            LotteryRewardSession session = ACTIVE.get(uuid);
            if (session == null || session.getCreatedAt() != startedAt) {
                return;
            }

            ACTIVE.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(CC.translate(ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES)
                        .getString("SESSION.EXPIRED", "&cSession expired after 60 seconds without input.", true)));
            }
        }, TIMEOUT_TICKS);
    }
}
