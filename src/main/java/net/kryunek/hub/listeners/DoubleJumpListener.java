package net.kryunek.hub.listeners;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.managers.player.ProfileManager;
import net.kryunek.hub.support.config.ConfigSupport;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.PvpArenaUtil;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public class DoubleJumpListener implements Listener {

    private final ProfileManager profileManager;
    private final Logger logger;
    private FileConfig settingsConfig;
    private final Set<UUID> jumped = new HashSet<>();

    public DoubleJumpListener(Celest hub) {
        Bukkit.getPluginManager().registerEvents(this, hub);
        this.profileManager = ModuleService.getManagerModule().getProfileManager();
        this.logger = hub.getLogger();
        this.settingsConfig = ModuleService.getFileModule().getFile(ConfigFiles.SETTINGS);
    }

    @EventHandler
    public void onJump(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        Profile profile = profileManager.getProfile(player.getUniqueId());

        if (profile == null) return;
        if (profile.isBuildModeEnabled()) {
            return;
        }
        if (PvpArenaUtil.isInsideArena(settingsConfig, player.getLocation())) {
            event.setCancelled(true);
            player.setAllowFlight(false);
            player.setFlying(false);
            return;
        }

        if (player.getGameMode() == GameMode.CREATIVE || profile.isFlyOnJoin()) {
            return;
        }

        if (player.getVehicle() != null) {
            player.getVehicle().remove();
            player.eject();
        }

        event.setCancelled(true);

        if (settingsConfig.getBoolean("DOUBLE_JUMP.ENABLED")) {

            player.setAllowFlight(false);
            player.setFlying(false);
            jumped.add(player.getUniqueId());

            player.setVelocity(
                    player.getLocation().getDirection().normalize()
                            .multiply(settingsConfig.getInt("DOUBLE_JUMP.MULTIPLY"))
                            .setY(settingsConfig.getInt("DOUBLE_JUMP.SET-Y"))
            );

            // 🎇 PARTÍCULAS MODERNAS
            Particle particle = ConfigSupport.getParticle(settingsConfig.getConfiguration(),
                    "DOUBLE_JUMP.PARTICLE", Particle.CLOUD, logger);

            for (Player viewer : player.getWorld().getPlayers()) {
                if (viewer.equals(player) || viewer.canSee(player)) {
                    viewer.spawnParticle(
                            particle,
                            player.getLocation(),
                            20,
                            0.3, 0.3, 0.3,
                            0.1
                    );
                }
            }

            // 🔊 SONIDO
            Sound sound = ConfigSupport.getSound(settingsConfig.getConfiguration(),
                    "DOUBLE_JUMP.SOUND", Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, logger);
            player.playSound(
                    player.getLocation(),
                    sound,
                    (float) settingsConfig.getDouble("DOUBLE_JUMP.VOLUME"),
                    (float) settingsConfig.getDouble("DOUBLE_JUMP.PITCH")

            );
        }
    }

    @EventHandler
    public void onPlayerGround(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Profile profile = profileManager.getProfile(player.getUniqueId());

        if (profile == null) return;
        if (profile.isBuildModeEnabled()) {
            return;
        }
        if (PvpArenaUtil.isInsideArena(settingsConfig, player.getLocation())) {
            player.setAllowFlight(false);
            player.setFlying(false);
            jumped.remove(player.getUniqueId());
            return;
        }

        // 🚀 optimización PRO
        if (event.getFrom().getBlockY() == event.getTo().getBlockY()) return;

        if (player.getGameMode() == GameMode.CREATIVE || profile.isFlyOnJoin()) {
            return;
        }

        if (settingsConfig.getBoolean("DOUBLE_JUMP.ENABLED")) {
            boolean wasOnGround = event.getFrom().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR;
            boolean isOnGround = event.getTo().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR;
            if (!wasOnGround && isOnGround) {
                jumped.remove(player.getUniqueId());
            }

            boolean infinite = settingsConfig.getBoolean("DOUBLE_JUMP.INFINITE");

            if (infinite) {
                if (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
                    player.setAllowFlight(true);
                }
            } else {
                if (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR) {
                    player.setAllowFlight(true);
                }
            }
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        Profile profile = profileManager.getProfile(player.getUniqueId());
        if (profile == null || profile.isBuildModeEnabled() || player.getGameMode() == GameMode.CREATIVE || profile.isFlyOnJoin()) {
            return;
        }
        if (PvpArenaUtil.isInsideArena(settingsConfig, player.getLocation())) {
            return;
        }
        if (!settingsConfig.getBoolean("DOUBLE_JUMP.ENABLED")) {
            return;
        }
        if (!settingsConfig.getConfiguration().getBoolean("DOUBLE_JUMP.SHIFT_BOOST.ENABLED", true)) {
            return;
        }
        if (!event.isSneaking()) {
            return;
        }
        if (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (!jumped.remove(uuid)) {
            return;
        }

        double multiply = settingsConfig.getConfiguration().getDouble("DOUBLE_JUMP.SHIFT_BOOST.MULTIPLY", 2.4D);
        double y = settingsConfig.getConfiguration().getDouble("DOUBLE_JUMP.SHIFT_BOOST.SET-Y", 0.35D);
        player.setVelocity(player.getLocation().getDirection().normalize().multiply(multiply).setY(y));

        Particle particle = ConfigSupport.getParticle(settingsConfig.getConfiguration(),
                "DOUBLE_JUMP.SHIFT_BOOST.PARTICLE", Particle.CLOUD, logger);
        player.getWorld().spawnParticle(particle, player.getLocation(), 20, 0.25, 0.25, 0.25, 0.1);

        float volume = (float) settingsConfig.getConfiguration().getDouble("DOUBLE_JUMP.SHIFT_BOOST.VOLUME", 1.0D);
        float pitch = (float) settingsConfig.getConfiguration().getDouble("DOUBLE_JUMP.SHIFT_BOOST.PITCH", 1.2D);
        Sound sound = ConfigSupport.getSound(settingsConfig.getConfiguration(),
                "DOUBLE_JUMP.SHIFT_BOOST.SOUND", Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, logger);
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
