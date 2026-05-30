package net.kryunek.hub.listeners;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.support.config.ConfigSupport;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.hotbar.Hotbar;
import net.kryunek.hub.managers.hotbar.HotbarManager;
import net.kryunek.hub.managers.jukebox.JukeboxManager;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.outfit.OutfitManager;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.managers.player.ProfileManager;
import net.kryunek.hub.managers.pvparena.PvpArenaKitManager;
import net.kryunek.hub.managers.spawn.SpawnManager;
import net.kryunek.hub.menus.settings.SettingsButton;
import net.kryunek.hub.menus.gadgets.GadgetService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.PlayerUtil;
import net.kryunek.hub.utils.PvpArenaUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinLeaveListener implements Listener {

    private final Celest hub;
    private final ProfileManager profileManager;
    private final HotbarManager hotbarManager;
    private final SpawnManager spawnManager;
    private final JukeboxManager jukeboxManager;
    private final OutfitManager outfitManager;
    private final PvpArenaKitManager pvpArenaKitManager;
    private final FileConfig settingsConfig;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    public JoinLeaveListener(Celest hub) {
        this.hub = hub;
        var managers = ModuleService.getManagerModule();
        Bukkit.getPluginManager().registerEvents(this, hub);
        this.profileManager = managers.getProfileManager();
        this.hotbarManager = managers.getHotbarManager();
        this.spawnManager = managers.getSpawnManager();
        this.jukeboxManager = managers.getJukeboxManager();
        this.outfitManager = managers.getOutfitManager();
        this.pvpArenaKitManager = managers.getPvpArenaKitManager();
        this.settingsConfig = ModuleService.getFileModule().getFile(ConfigFiles.SETTINGS);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Profile profile = profileManager.getProfile(player.getUniqueId());
        event.joinMessage(null);

        // ProfileListener loads the profile at pre-login and kicks if it is missing, so reaching
        // join with a null profile means something went wrong after login. Fail safe instead of
        // dereferencing null: force a clean reconnect rather than leaving the player half-set-up.
        if (profile == null) {
            hub.getLogger().severe("No profile loaded for " + player.getName() + " on join; kicking to force reconnect.");
            player.kick(LEGACY.deserialize(CC.translate("&cYour profile failed to load. Please reconnect.")));
            return;
        }

        applyJoinState(player, profile);
        applyVisibility(player, profile);
        applyJoinMessage(event, player);
        applyJoinTitle(player);
        applyJoinSound(player);
        scheduleGameModeAndOutfit(player, profile);
        scheduleArenaVisibility(player);
    }

    private void applyJoinState(Player player, Profile profile) {
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setGameMode(profile.isBuildModeEnabled() ? GameMode.CREATIVE : GameMode.SURVIVAL);
        player.setWalkSpeed((float) settingsConfig.getDouble("WALK_SPEED"));
        spawnManager.toSpawn(player, false);
        if (profile.isBuildModeEnabled()) {
            PlayerUtil.clear(player, true, true);
        } else {
            hotbarManager.setHotbar(player);
        }
        SettingsButton.applyTimePreference(player, profile.getTimePreference());
        if (jukeboxManager != null) {
            jukeboxManager.handleJoin(player);
        }
    }

    private void applyVisibility(Player player, Profile profile) {
        // The visibility toggle item depends only on this player's own preference, so set it once.
        if (!profile.isBuildModeEnabled()) {
            Hotbar toggleItem = hotbarManager.getHotbar(profile.isVisibilityOn() ? "HIDE_PLAYER" : "SHOW_PLAYER");
            player.getInventory().setItem(toggleItem.getSlot(), toggleItem.getItem());
        }

        for (Player other : Bukkit.getServer().getOnlinePlayers()) {
            if (profile.isVisibilityOn()) {
                player.showPlayer(Celest.get(), other);
            } else {
                player.hidePlayer(Celest.get(), other);
            }

            Profile otherProfile = profileManager.getProfile(other.getUniqueId());
            if (otherProfile == null || otherProfile.isVisibilityOn()) {
                other.showPlayer(Celest.get(), player);
            } else {
                other.hidePlayer(Celest.get(), player);
            }
        }
    }

    private void applyJoinMessage(PlayerJoinEvent event, Player player) {
        if (settingsConfig.getBoolean("JOIN_CLEARCHAT.ENABLED")) {
            for (int i = 0; i < settingsConfig.getInt("JOIN_CLEARCHAT.LINES"); i++) {
                player.sendMessage("");
                player.sendMessage(CC.translate("&c &f"));
            }
        }
        if (settingsConfig.getBoolean("JOIN_MESSAGE.ENABLED")) {
            event.joinMessage(LEGACY.deserialize(CC.translate(settingsConfig.getString("JOIN_MESSAGE.MESSAGE"))
                    .replace("%player%", player.getDisplayName())));
        }
    }

    private void applyJoinTitle(Player player) {
        if (!settingsConfig.getBoolean("JOIN_TITLE.ENABLED")) {
            return;
        }
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

    private void applyJoinSound(Player player) {
        if (!settingsConfig.getBoolean("JOIN_SOUND.ENABLED")) {
            return;
        }
        Sound sound = ConfigSupport.getSound(settingsConfig.getConfiguration(), "JOIN_SOUND.SOUND",
                Sound.ENTITY_PLAYER_LEVELUP, Bukkit.getLogger());
        player.playSound(player.getLocation(), sound,
                (float) settingsConfig.getDouble("JOIN_SOUND.VOLUME"),
                (float) settingsConfig.getDouble("JOIN_SOUND.PITCH"));
    }

    private void scheduleGameModeAndOutfit(Player player, Profile profile) {
        Bukkit.getScheduler().runTaskLater(hub, () -> {
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
            outfitManager.applySelectedOutfit(player, profile);
        }, 1L);
    }

    private void scheduleArenaVisibility(Player player) {
        Bukkit.getScheduler().runTaskLater(hub, () -> {
            if (!player.isOnline()) {
                return;
            }
            pvpArenaKitManager.enforceArenaVisibility(player);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (pvpArenaKitManager.isInArenaSession(online.getUniqueId())) {
                    pvpArenaKitManager.enforceArenaVisibility(online);
                }
            }
        }, 2L);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Profile profile = profileManager.getProfile(event.getPlayer().getUniqueId());
        event.quitMessage(null);
        event.getPlayer().setWalkSpeed(0.2f);
        GadgetService.deactivatePersistentEffects(event.getPlayer());
        if (jukeboxManager != null) {
            jukeboxManager.stop(event.getPlayer());
        }
        PlayerUtil.clear(profile == null ? event.getPlayer() : profile.getPlayer(), true, true);
    }
}
