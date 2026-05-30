package net.kryunek.hub.managers.outfit;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.AllArgsConstructor;
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

public final class OutfitCreateSession {

    private static final long TIMEOUT_TICKS = 20L * 60L;
    private static final Map<UUID, CreationData> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> STARTED = new HashMap<>();

    private OutfitCreateSession() {
    }

    public static void start(Player player, String name, String previousOutfitName) {
        start(player, name, previousOutfitName, 255, 255, 255, false);
    }

    public static void start(Player player, String name, String previousOutfitName, int red, int green, int blue, boolean enchanted) {
        if (!SessionGuard.canStart(player, "OUTFIT_CREATE")) {
            return;
        }
        UUID uuid = player.getUniqueId();
        long startedAt = System.currentTimeMillis();
        ACTIVE.put(uuid, new CreationData(name, red, green, blue, enchanted, previousOutfitName));
        STARTED.put(uuid, startedAt);
        scheduleTimeout(uuid, startedAt);
    }

    public static void set(Player player, CreationData data) {
        UUID uuid = player.getUniqueId();
        long startedAt = System.currentTimeMillis();
        ACTIVE.put(uuid, data);
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
        private final String name;
        private final int red;
        private final int green;
        private final int blue;
        private final boolean enchanted;
        private final String previousOutfitName;

        public CreationData withColor(int red, int green, int blue) {
            return new CreationData(this.name, red, green, blue, this.enchanted, this.previousOutfitName);
        }

        public CreationData withEnchanted(boolean enchanted) {
            return new CreationData(this.name, this.red, this.green, this.blue, enchanted, this.previousOutfitName);
        }
    }
}
