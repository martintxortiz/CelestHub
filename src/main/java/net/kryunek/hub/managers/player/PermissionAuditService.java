package net.kryunek.hub.managers.player;

import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.menus.gadgets.GadgetService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.PlayerUtil;
import net.kryunek.hub.utils.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

/**
 * Periodically revokes cosmetics and abilities a player no longer has permission for — outfit,
 * trail, gadget, build mode and fly-on-join — on an interval from the injected settings config.
 */
public class PermissionAuditService {

    private final FileConfig settings;
    private final FileConfig messages;
    private BukkitTask task;

    public PermissionAuditService(FileConfig settings, FileConfig messages) {
        this.settings = settings;
        this.messages = messages;
    }

    public void start() {
        if (!settings.getConfiguration().contains("PERMISSION_AUDIT.ENABLED")) {
            settings.getConfiguration().set("PERMISSION_AUDIT.ENABLED", true);
            settings.getConfiguration().set("PERMISSION_AUDIT.INTERVAL_SECONDS", 30);
            settings.save();
        }

        if (!settings.getBoolean("PERMISSION_AUDIT.ENABLED")) {
            return;
        }

        int intervalSeconds = Math.max(5, settings.getInt("PERMISSION_AUDIT.INTERVAL_SECONDS"));
        this.task = TaskUtil.runSyncTimer(this::runAuditTick, intervalSeconds * 20L, intervalSeconds * 20L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void runAuditTick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Profile profile = ModuleService.getManagerModule().getProfileManager().getProfile(player.getUniqueId());
            if (profile == null) {
                continue;
            }

            boolean changed = false;
            boolean needsHotbarReload = false;

            if (profile.getOutfit() != null && !hasOutfitPermission(player, profile)) {
                profile.setOutfit(null);
                if (!profile.isBuildModeEnabled()) {
                    player.getInventory().setArmorContents(new ItemStack[4]);
                }
                notifyPlayer(player, "PERMISSION_AUDIT.REMOVED_OUTFIT", "&eYour outfit was removed due to missing permission.");
                changed = true;
            }

            if (profile.getTrail() != null && !hasTrailPermission(player, profile)) {
                profile.setTrail(null);
                notifyPlayer(player, "PERMISSION_AUDIT.REMOVED_TRAIL", "&eYour trail was removed due to missing permission.");
                changed = true;
            }

            String gadgetType = profile.getSelectedGadgetType();
            if (gadgetType != null
                    && !gadgetType.equalsIgnoreCase("NONE")
                    && !GadgetService.hasPermission(player, gadgetType)) {
                profile.setSelectedGadgetType("NONE");
                GadgetService.deactivatePersistentEffects(player);
                notifyPlayer(player, "PERMISSION_AUDIT.REMOVED_GADGET", "&eYour selected gadget was removed due to missing permission.");
                changed = true;
                needsHotbarReload = true;
            }

            if (profile.isBuildModeEnabled() && !player.hasPermission("celest.command.buildmode")) {
                profile.setBuildModeEnabled(false);
                player.setGameMode(GameMode.SURVIVAL);
                ModuleService.getManagerModule().getHotbarManager().setHotbar(player);
                ModuleService.getManagerModule().getOutfitManager().applySelectedOutfit(player, profile);
                if (profile.isFlyOnJoin() && player.hasPermission("celest.command.fly")) {
                    PlayerUtil.applyHubFlyState(player, true, false);
                } else {
                    PlayerUtil.applyHubFlyState(player, false, false);
                }
                notifyPlayer(player, "PERMISSION_AUDIT.REMOVED_BUILDMODE", "&eBuild mode was disabled due to missing permission.");
                changed = true;
                needsHotbarReload = false;
            }

            if (profile.isFlyOnJoin() && !player.hasPermission("celest.command.fly")) {
                profile.setFlyOnJoin(false);
                if (!profile.isBuildModeEnabled()) {
                    PlayerUtil.applyHubFlyState(player, false, false);
                }
                notifyPlayer(player, "PERMISSION_AUDIT.REMOVED_FLY", "&eFly-on-join was disabled due to missing permission.");
                changed = true;
            }

            if (needsHotbarReload && !profile.isBuildModeEnabled()) {
                ModuleService.getManagerModule().getHotbarManager().setHotbar(player);
            }

            if (changed) {
                ModuleService.getManagerModule().getProfileManager().saveProfile(profile, false);
            }
        }
    }

    private boolean hasOutfitPermission(Player player, Profile profile) {
        String permission = profile.getOutfit().getPermission();
        return player.hasPermission(permission) || player.hasPermission("celest.cosmetics.outfit.*");
    }

    private boolean hasTrailPermission(Player player, Profile profile) {
        String permission = profile.getTrail().getPermission();
        return player.hasPermission(permission) || player.hasPermission("celest.cosmetics.trail.*");
    }

    private void notifyPlayer(Player player, String path, String fallback) {
        player.sendMessage(CC.translate(messages.getString(path, fallback, true)));
    }
}
