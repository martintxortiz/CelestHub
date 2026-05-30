package net.kryunek.hub.managers.module.impl;


import lombok.Getter;
import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.chat.ChatManager;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.hotbar.HotbarManager;
import net.kryunek.hub.managers.module.Module;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.jukebox.JukeboxManager;
import net.kryunek.hub.managers.lottery.LotteryManager;
import net.kryunek.hub.managers.network.NetworkSyncManager;
import net.kryunek.hub.managers.outfit.OutfitManager;
import net.kryunek.hub.managers.particles.TrailParticleManager;
import net.kryunek.hub.managers.pvparena.PvpArenaKitManager;
import net.kryunek.hub.managers.pvparena.PvpArenaSelectionManager;
import net.kryunek.hub.managers.player.PermissionAuditService;
import net.kryunek.hub.managers.player.ProfileManager;
import net.kryunek.hub.managers.queue.QueueManager;
import net.kryunek.hub.managers.rank.RankManager;
import net.kryunek.hub.managers.spawn.SpawnManager;
import net.kryunek.hub.managers.timer.TimerManager;
import net.kryunek.hub.utils.bungee.BungeeUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Composition root for the manager layer (priority 2). {@link #onEnable(Celest)} reads the
 * already-enabled {@link FileModule} once and constructs every manager, injecting each its config
 * and collaborators in dependency order (rank → queue, profile/hotbar → pvp kit), with network
 * sync started last so its subscriber sees a fully built manager set.
 */
@Getter
public class ManagerModule extends Module {
    private HotbarManager hotbarManager;
    private SpawnManager spawnManager;
    private ProfileManager profileManager;
    private RankManager rankManager;
    private QueueManager queueManager;
    private LotteryManager lotteryManager;
    private TrailParticleManager trailParticleManager;
    private OutfitManager outfitManager;
    private ChatManager chatManager;
    private PermissionAuditService permissionAuditService;
    private NetworkSyncManager networkSyncManager;
    private PvpArenaKitManager pvpArenaKitManager;
    private PvpArenaSelectionManager pvpArenaSelectionManager;
    private JukeboxManager jukeboxManager;

    private TimerManager timerManager;

    public void load(boolean reload) {
        this.hotbarManager.load();
        if (reload) {
            this.hotbarManager.reload();
        }
    }
    
    @Override
    public int getPriority() {
        return 2;
    }
    
    @Override
    public void onEnable(Celest hub) {
        new BukkitRunnable() {
            @Override
            public void run() {
                BungeeUtils.refreshGlobalCount();
                BungeeUtils.refreshServerList();
                BungeeUtils.refreshServerCount();
                BungeeUtils.refreshCurrentServer();
            }
        }.runTaskTimer(hub, 20L, 20L);
        hub.getServer().getMessenger().registerOutgoingPluginChannel(hub, "BungeeCord");
        hub.getServer().getMessenger().registerIncomingPluginChannel(hub, "BungeeCord", new BungeeUtils());
        FileModule files = ModuleService.getFileModule();
        this.rankManager = new RankManager(
                files.getFile(ConfigFiles.CONFIG),
                files.getFile(ConfigFiles.QUEUE),
                files.getFile(ConfigFiles.TAB));
        this.rankManager.loadRank();
        this.profileManager = new ProfileManager(
                files.getFile(ConfigFiles.CONFIG),
                files.getFile(ConfigFiles.JUKEBOX));
        this.networkSyncManager = new NetworkSyncManager(hub, this, files.getFile(ConfigFiles.CONFIG));
        this.queueManager = new QueueManager(rankManager, files.getFile(ConfigFiles.QUEUE));
        this.lotteryManager = new LotteryManager(
                files.getFile(ConfigFiles.LOTTERY),
                files.getFile(ConfigFiles.MESSAGES));
        this.spawnManager = new SpawnManager(files.getFile(ConfigFiles.SETTINGS));
        this.hotbarManager = new HotbarManager(files.getFile(ConfigFiles.HOTBAR));
        this.pvpArenaKitManager = new PvpArenaKitManager(
                profileManager, hotbarManager,
                files.getFile(ConfigFiles.SETTINGS),
                files.getFile(ConfigFiles.SETTINGS_MENU));
        this.pvpArenaSelectionManager = new PvpArenaSelectionManager(files.getFile(ConfigFiles.SETTINGS));
        this.jukeboxManager = new JukeboxManager(
                files.getFile(ConfigFiles.JUKEBOX),
                files.getFile(ConfigFiles.SETTINGS));
        this.timerManager = new TimerManager();
        this.chatManager = new ChatManager(files.getFile(ConfigFiles.SETTINGS));
        this.trailParticleManager = new TrailParticleManager(files.getFile(ConfigFiles.PARTICLE));
        this.trailParticleManager.load();
        this.outfitManager = new OutfitManager(files.getFile(ConfigFiles.OUTFIT));
        this.outfitManager.load();
        this.permissionAuditService = new PermissionAuditService(
                files.getFile(ConfigFiles.SETTINGS),
                files.getFile(ConfigFiles.MESSAGES));
        this.permissionAuditService.start();
        this.networkSyncManager.start();
        this.load(false);
    }
    public Collection<? extends Player> getOnlinePlayers() {
        return new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
    }

    public void shutdown() {
        if (permissionAuditService != null) {
            permissionAuditService.shutdown();
        }
        if (timerManager != null) {
            timerManager.shutdown();
        }
        if (lotteryManager != null) {
            lotteryManager.shutdown();
        }
        if (queueManager != null) {
            queueManager.shutdown();
        }
        if (jukeboxManager != null) {
            jukeboxManager.shutdown();
        }
        if (networkSyncManager != null) {
            networkSyncManager.shutdown();
        }
        if (profileManager != null) {
            profileManager.save();
            profileManager.shutdown();
        }
    }

    
}
