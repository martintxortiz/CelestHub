package net.kryunek.hub.listeners;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.particles.TrailParticle;
import net.kryunek.hub.managers.particles.TrailParticleCreateSession;
import net.kryunek.hub.managers.particles.TrailParticleManager;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.managers.player.ProfileManager;
import net.kryunek.hub.menus.particles.manage.list.create.TrailParticleEffectMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.PvpArenaUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class TrailParticleListener implements Listener {

    private final ProfileManager profileManager;
    private final TrailParticleManager trailManager;
    private final FileConfig messages;
    private final FileConfig settingsConfig;

    public TrailParticleListener(Celest hub) {
        Bukkit.getPluginManager().registerEvents(this, hub);
        this.profileManager = ModuleService.getManagerModule().getProfileManager();
        this.trailManager = ModuleService.getManagerModule().getTrailParticleManager();
        this.messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);
        this.settingsConfig = ModuleService.getFileModule().getFile(ConfigFiles.SETTINGS);
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (PvpArenaUtil.isInsideArena(settingsConfig, to)) {
            return;
        }
        Profile profile = this.profileManager.getProfile(player.getUniqueId());
        if (profile.getTrail() == null) {
            return;
        }

        profile.getTrail().playEffect(player);
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Profile profile = this.profileManager.getProfile(player.getUniqueId());
        if (profile.getTrail() == null) {
            for (TrailParticle trail : this.trailManager.getTrails().values()) {
                if (!player.isOp() && player.hasPermission(trail.getPermission())) {
                    profile.setTrail(trail);
                    break;
                }
            }
        }
    }

    @EventHandler
    private void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!TrailParticleCreateSession.isActive(player)) {
            return;
        }

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (message.equalsIgnoreCase("cancel")) {
            TrailParticleCreateSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("TRAIL.CREATE.CANCELLED")));
            return;
        }

        String name = message.trim();
        if (name.isEmpty() || name.contains(" ")) {
            player.sendMessage(CC.translate(messages.getString("TRAIL.CREATE.INVALID_NAME")));
            return;
        }
        if (this.trailManager.getTrail(name) != null) {
            player.sendMessage(CC.translate(messages.getString("TRAIL.CREATE.ALREADY_EXISTS")));
            return;
        }

        TrailParticleCreateSession.start(player, name);
        String iconName = Material.BLAZE_POWDER.name();
        if (player.getInventory().getItemInMainHand() != null && player.getInventory().getItemInMainHand().getType() != Material.AIR) {
            iconName = player.getInventory().getItemInMainHand().getType().name();
        }

        player.sendMessage(CC.translate(messages.getString("TRAIL.CREATE.SAVED_NAME").replace("%trail%", name)));
        player.sendMessage(CC.translate(messages.getString("TRAIL.CREATE.SAVED_ICON").replace("%icon%", iconName)));
        player.sendMessage(CC.translate(messages.getString("TRAIL.CREATE.OPEN_EFFECT_MENU")));
        Bukkit.getScheduler().runTask(Celest.get(), () -> new TrailParticleEffectMenu().openMenu(player));
    }
}
