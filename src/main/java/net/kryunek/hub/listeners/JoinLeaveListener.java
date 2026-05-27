package net.kryunek.hub.listeners;

import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.hotbar.Hotbar;
import net.kryunek.hub.managers.hotbar.HotbarManager;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.managers.player.ProfileManager;
import net.kryunek.hub.menus.settings.SettingsButton;
import net.kryunek.hub.menus.gadgets.GadgetService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.PlayerUtil;
import net.kryunek.hub.utils.PvpArenaUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinLeaveListener implements Listener {

    private final ProfileManager profileManager;
    private final HotbarManager hotbarManager;
    private FileConfig settingsConfig;

    public JoinLeaveListener(Celest hub) {
        Bukkit.getPluginManager().registerEvents(this, hub);
        this.profileManager = ModuleService.getManagerModule().getProfileManager();
        this.hotbarManager = ModuleService.getManagerModule().getHotbarManager();
        this.settingsConfig = ModuleService.getFileModule().getFile("settings");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Profile profile = profileManager.getProfile(event.getPlayer().getUniqueId());
        Player player = event.getPlayer();
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setGameMode(profile.isBuildModeEnabled() ? GameMode.CREATIVE : GameMode.SURVIVAL);
        player.setWalkSpeed((float) settingsConfig.getDouble("WALK_SPEED"));
        ModuleService.getManagerModule().getSpawnManager().toSpawn(event.getPlayer(), false);
        if (profile.isBuildModeEnabled()) {
            PlayerUtil.clear(player, true, true);
        } else {
            hotbarManager.setHotbar(event.getPlayer());
        }
        event.setJoinMessage(null);
        SettingsButton.applyTimePreference(player, profile.getTimePreference());
        if (ModuleService.getManagerModule().getJukeboxManager() != null) {
            ModuleService.getManagerModule().getJukeboxManager().handleJoin(player);
        }

        for (Player players : Bukkit.getServer().getOnlinePlayers()) {
            Profile profiles = profileManager.getProfile(players.getUniqueId());
            if (profile.isVisibilityOn()) {
                if (!profile.isBuildModeEnabled()) {
                    Hotbar tetas = hotbarManager.getHotbar("HIDE_PLAYER");
                    player.getInventory().setItem(tetas.getSlot(), tetas.getItem());
                }
                player.showPlayer(players);
            } else {
                if (!profile.isBuildModeEnabled()) {
                    Hotbar hotbar = hotbarManager.getHotbar("SHOW_PLAYER");
                    player.getInventory().setItem(hotbar.getSlot(), hotbar.getItem());
                }
                player.hidePlayer(players);
            }
            if (profiles.isVisibilityOn()) {
                players.showPlayer(player);
            } else {
                players.hidePlayer(player);
            }
        }
        if (settingsConfig.getBoolean("JOIN_CLEARCHAT.ENABLED")) {
            for (int i = 0; i < settingsConfig.getInt("JOIN_CLEARCHAT.LINES"); i++) {
                player.sendMessage("");
                player.sendMessage(CC.translate("&c &f"));
            }
        }
        if (settingsConfig.getBoolean("JOIN_MESSAGE.ENABLED")) {
                event.setJoinMessage(CC.translate(settingsConfig.getString("JOIN_MESSAGE.MESSAGE")).replace("%player%", event.getPlayer().getDisplayName()));

        }
        if (settingsConfig.getBoolean("JOIN_TITLE.ENABLED")) {
            boolean useTicks = settingsConfig.getBoolean("JOIN_TITLE.SETTIMINGTOTICKS");
            int fadeIn = settingsConfig.getInt("JOIN_TITLE.FADEINTIME");
            int stay = settingsConfig.getInt("JOIN_TITLE.STAYTIME");
            int fadeOut = settingsConfig.getInt("JOIN_TITLE.FADEOUTTIME");

            if (!useTicks) {
                fadeIn *= 20;
                stay *= 20;
                fadeOut *= 20;
            }

            String title = settingsConfig.getString("JOIN_TITLE.TITLE");
            String subtitle = settingsConfig.getString("JOIN_TITLE.SUBTITLE");

            player.sendTitle(
                    title == null ? "" : title.replace("%player%", player.getName()),
                    subtitle == null ? "" : subtitle.replace("%player%", player.getName()),
                    fadeIn,
                    stay,
                    fadeOut
            );
        }
        if (settingsConfig.getBoolean("JOIN_SOUND.ENABLED")) {
            player.playSound(player.getLocation(), Sound.valueOf(settingsConfig.getString("JOIN_SOUND.SOUND")),
                    (float) settingsConfig.getDouble("JOIN_SOUND.VOLUME"),
                    (float) settingsConfig.getDouble("JOIN_SOUND.PITCH"));
        }

        Bukkit.getScheduler().runTaskLater(Celest.get(), () -> {
            if (!player.isOnline()) {
                return;
            }

            if (profile.isBuildModeEnabled()) {
                player.setGameMode(GameMode.CREATIVE);
                player.setAllowFlight(true);
                player.setFlying(false);
            } else if (profile.isFlyOnJoin()) {
                player.setGameMode(GameMode.SURVIVAL);
                PlayerUtil.applyHubFlyState(player, true, true);
            } else {
                player.setGameMode(GameMode.SURVIVAL);
                PlayerUtil.applyHubFlyState(player, false, false);
            }

            if (PvpArenaUtil.isInsideArena(settingsConfig, player.getLocation())) {
                return;
            }
            ModuleService.getManagerModule().getOutfitManager().applySelectedOutfit(player, profile);
        }, 1L);

        Bukkit.getScheduler().runTaskLater(Celest.get(), () -> {
            if (!player.isOnline()) {
                return;
            }
            var pvpManager = ModuleService.getManagerModule().getPvpArenaKitManager();
            pvpManager.enforceArenaVisibility(player);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (pvpManager.isInArenaSession(online.getUniqueId())) {
                    pvpManager.enforceArenaVisibility(online);
                }
            }
        }, 2L);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Profile profile = profileManager.getProfile(event.getPlayer().getUniqueId());
        event.setQuitMessage(null);
        event.getPlayer().setWalkSpeed(0.2f);
        GadgetService.deactivatePersistentEffects(event.getPlayer());
        if (ModuleService.getManagerModule().getJukeboxManager() != null) {
            ModuleService.getManagerModule().getJukeboxManager().stop(event.getPlayer());
        }
        PlayerUtil.clear(profile == null ? event.getPlayer() : profile.getPlayer(), true, true);

    }
}
