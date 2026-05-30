package net.kryunek.hub.managers.particles;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.session.SessionGuard;
import net.kryunek.hub.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TrailParticleCreateSession {

    private static final long TIMEOUT_TICKS = 20L * 60L;
    private static final Material DEFAULT_ICON = Material.BLAZE_POWDER;
    private static final Map<UUID, CreationData> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> STARTED = new HashMap<>();

    private TrailParticleCreateSession() {
    }

    public static void start(Player player, String trailName) {
        if (!SessionGuard.canStart(player, "TRAIL_CREATE")) {
            return;
        }
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        Material material = DEFAULT_ICON;
        int data = 0;

        if (itemInHand != null && itemInHand.getType() != Material.AIR) {
            material = itemInHand.getType();
            data = itemInHand.getDurability();
        }

        UUID uuid = player.getUniqueId();
        long startedAt = System.currentTimeMillis();
        ACTIVE.put(uuid, new CreationData(trailName, material.name(), data));
        STARTED.put(uuid, startedAt);
        scheduleTimeout(uuid, startedAt);
    }

    public static void stop(Player player) {
        UUID uuid = player.getUniqueId();
        ACTIVE.remove(uuid);
        STARTED.remove(uuid);
    }

    public static boolean isActive(Player player) {
        return ACTIVE.containsKey(player.getUniqueId());
    }

    public static CreationData get(Player player) {
        return ACTIVE.get(player.getUniqueId());
    }

    public static int activeCount() {
        return ACTIVE.size();
    }

    private static void scheduleTimeout(UUID uuid, long startedAt) {
        Bukkit.getScheduler().runTaskLater(Celest.get(), () -> {
            Long current = STARTED.get(uuid);
            if (current == null || current != startedAt) {
                return;
            }

            ACTIVE.remove(uuid);
            STARTED.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(CC.translate(ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES)
                        .getString("SESSION.EXPIRED", "&cEditor session expired after 60 seconds.", true)));
            }
        }, TIMEOUT_TICKS);
    }

    @Getter
    @AllArgsConstructor
    public static final class CreationData {
        private final String trailName;
        private final String material;
        private final int data;
    }
}
