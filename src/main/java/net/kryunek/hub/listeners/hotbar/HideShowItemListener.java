package net.kryunek.hub.listeners.hotbar;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.hotbar.Hotbar;
import net.kryunek.hub.managers.hotbar.HotbarManager;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.managers.player.ProfileManager;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.Cooldown;
import net.kryunek.hub.utils.FileConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class HideShowItemListener implements Listener {

    private final ProfileManager profileManager;
    private final HotbarManager hotbarManager;
    private FileConfig settingsConfig;

    public HideShowItemListener(Celest hub) {
        Bukkit.getPluginManager().registerEvents(this, hub);
        this.profileManager = ModuleService.getManagerModule().getProfileManager();
        this.hotbarManager = ModuleService.getManagerModule().getHotbarManager();
        this.settingsConfig = ModuleService.getFileModule().getFile(ConfigFiles.SETTINGS);
    }


    @EventHandler
    private void onHidePlayer(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (event.getItem() == null) {
                return;
            }
            Hotbar hide = hotbarManager.getHotbar("HIDE_PLAYER");
            if (hide == null) {
                return;
            }
            Player player = event.getPlayer();
            Profile profile = profileManager.getProfile(player.getUniqueId());
            if (hide.isEnabled() && hide.isHotbarItem(event.getItem())) {
                event.setCancelled(true);
                hotbarManager.playClickSound(player, hide);
                Hotbar show = hotbarManager.getHotbar("SHOW_PLAYER");
                if (show == null) {
                    return;
                }
                if (show.isEnabled()) {

                    if (!profile.getVisibilityCooldown().hasExpired()) {
                        player.sendMessage(CC.translate(settingsConfig.getString("HIDE_SHOW_PLAYER.MESSAGE")).replace("%time%", profile.getVisibilityCooldown().getTimeMilisLeft()));
                        return;
                    }
                    for (Player online : Bukkit.getServer().getOnlinePlayers()) {
                        player.hidePlayer(Celest.get(), online);
                    }

                    profile.setVisibilityOn(false);
                    player.getInventory().setItem(show.getSlot(), show.getItem());
                    player.sendMessage(CC.translate(settingsConfig.getString("hide-player")));
                    ModuleService.getManagerModule().getPvpArenaKitManager().enforceArenaVisibility(player);
                    if (settingsConfig.getBoolean("HIDE_SHOW_PLAYER.ENABLED")) {
                        Cooldown cooldown = new Cooldown(settingsConfig.getInt("HIDE_SHOW_PLAYER.TIME"));
                        profile.setVisibilityCooldown(cooldown);
                    }
                }
            }
        }
    }

    @EventHandler
    private void onShowPlayer(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (event.getItem() == null) {
                return;
            }
            Hotbar show = hotbarManager.getHotbar("SHOW_PLAYER");
            if (show == null) {
                return;
            }
            if (show.isEnabled() && show.isHotbarItem(event.getItem())) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                hotbarManager.playClickSound(player, show);
                Profile profile = profileManager.getProfile(player.getUniqueId());
                Hotbar hide = hotbarManager.getHotbar("HIDE_PLAYER");
                if (hide == null) {
                    return;
                }
                if (hide.isEnabled()) {
                    if (!profile.getVisibilityCooldown().hasExpired()) {
                        player.sendMessage(CC.translate(settingsConfig.getString("HIDE_SHOW_PLAYER.MESSAGE")).replace("%time%", profile.getVisibilityCooldown().getTimeMilisLeft()));
                        return;
                    }
                    for (Player online : Bukkit.getServer().getOnlinePlayers()) {
                        player.showPlayer(Celest.get(), online);
                    }
                    profile.setVisibilityOn(true);
                    player.getInventory().setItem(hide.getSlot(), hide.getItem());
                    player.sendMessage(CC.translate(settingsConfig.getString("show-player")));
                    ModuleService.getManagerModule().getPvpArenaKitManager().enforceArenaVisibility(player);
                    if (settingsConfig.getBoolean("HIDE_SHOW_PLAYER.ENABLED")) {
                        Cooldown cooldown = new Cooldown(settingsConfig.getInt("HIDE_SHOW_PLAYER.TIME"));
                        profile.setVisibilityCooldown(cooldown);
                    }
                }
            }
        }
    }


}
