package net.kryunek.hub.managers.module.impl;


import lombok.Getter;
import net.kryunek.hub.managers.chat.ChatManager;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.hotbar.HotbarManager;
import net.kryunek.hub.managers.module.Module;
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
import net.kryunek.hub.managers.rank.IRankManager;
import net.kryunek.hub.managers.spawn.SpawnManager;
import net.kryunek.hub.managers.timer.TimerManager;
import net.kryunek.hub.utils.bungee.BungeeUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collection;

@Getter
public class ManagerModule extends Module {
    private HotbarManager hotbarManager;
    private SpawnManager spawnManager;
    private ProfileManager profileManager;
    private IRankManager rankManager;
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

    public void load(boolean b) {
        this.hotbarManager.load();
        if (b) {
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
        this.rankManager = new IRankManager();
        this.rankManager.loadRank();
        this.profileManager = new ProfileManager();
        this.networkSyncManager = new NetworkSyncManager(hub, this);
        this.queueManager = new QueueManager(rankManager);
        this.lotteryManager = new LotteryManager();
        this.spawnManager = new SpawnManager();
        this.hotbarManager = new HotbarManager();
        this.pvpArenaKitManager = new PvpArenaKitManager(profileManager, hotbarManager);
        this.pvpArenaSelectionManager = new PvpArenaSelectionManager();
        this.jukeboxManager = new JukeboxManager();
        this.timerManager = new TimerManager();
        this.chatManager = new ChatManager();
        this.trailParticleManager = new TrailParticleManager();
        this.trailParticleManager.load();
        this.outfitManager = new OutfitManager();
        this.outfitManager.load();
        this.permissionAuditService = new PermissionAuditService();
        this.permissionAuditService.start();
        this.networkSyncManager.start();
        this.load(false);
    }
    public Collection<? extends Player> getOnlinePlayers() {
        Collection<Player> collection = new ArrayList<>();
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            collection.add(player);
        }
        return collection;
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
