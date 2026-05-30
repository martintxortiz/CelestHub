package net.kryunek.hub.listeners;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.pvparena.PvpArenaKitManager;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.PvpArenaUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PvpArenaListener implements Listener {

    private final FileConfig settingsConfig;
    private final PvpArenaKitManager pvpArenaKitManager;

    public PvpArenaListener(Celest hub) {
        Bukkit.getPluginManager().registerEvents(this, hub);
        this.settingsConfig = ModuleService.getFileModule().getFile(ConfigFiles.SETTINGS);
        this.pvpArenaKitManager = ModuleService.getManagerModule().getPvpArenaKitManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!PvpArenaUtil.isArenaEnabled(settingsConfig)) {
            return;
        }
        if (event.getTo() == null) {
            return;
        }
        Player player = event.getPlayer();
        boolean inside = PvpArenaUtil.isInsideArena(settingsConfig, event.getTo());
        pvpArenaKitManager.syncArenaState(player, inside);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!PvpArenaUtil.isArenaEnabled(settingsConfig) || event.getTo() == null) {
            return;
        }
        Player player = event.getPlayer();
        boolean inside = PvpArenaUtil.isInsideArena(settingsConfig, event.getTo());
        pvpArenaKitManager.syncArenaState(player, inside);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!PvpArenaUtil.isArenaEnabled(settingsConfig)) {
            return;
        }
        Player player = event.getPlayer();
        boolean inside = PvpArenaUtil.isInsideArena(settingsConfig, player.getLocation());
        pvpArenaKitManager.syncArenaState(player, inside);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEditorClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (pvpArenaKitManager.isDefaultEditorView(player.getUniqueId(), event.getView().getTitle())) {
            int rawSlot = event.getRawSlot();
            int topSize = event.getView().getTopInventory().getSize();
            if (event.getClick() == ClickType.NUMBER_KEY || event.isShiftClick() || rawSlot >= topSize) {
                event.setCancelled(true);
                return;
            }
            if (rawSlot < topSize) {
                if (pvpArenaKitManager.isControlSlot(rawSlot)) {
                    event.setCancelled(true);
                    pvpArenaKitManager.handleDefaultEditorControlClick(player, rawSlot, event.getView().getTopInventory());
                    return;
                }
                if (rawSlot > 40) {
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }
        if (!pvpArenaKitManager.isEditorView(player.getUniqueId(), event.getView().getTitle())) {
            return;
        }

        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getClick() == ClickType.NUMBER_KEY || event.isShiftClick() || rawSlot >= topSize) {
            event.setCancelled(true);
            return;
        }
        if (rawSlot < topSize) {
            if (pvpArenaKitManager.isControlSlot(rawSlot)) {
                event.setCancelled(true);
                pvpArenaKitManager.handleEditorControlClick(player, rawSlot, event.getView().getTopInventory());
                return;
            }
            if (rawSlot > 40) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEditorDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        boolean isPersonal = pvpArenaKitManager.isEditorView(player.getUniqueId(), event.getView().getTitle());
        boolean isDefault = pvpArenaKitManager.isDefaultEditorView(player.getUniqueId(), event.getView().getTitle());
        if (!isPersonal && !isDefault) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= topSize || rawSlot > 40) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onEditorClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (pvpArenaKitManager.isDefaultEditorView(player.getUniqueId(), event.getView().getTitle())) {
            pvpArenaKitManager.handleDefaultEditorClose(player, event.getView().getTopInventory());
            return;
        }
        if (!pvpArenaKitManager.isEditorView(player.getUniqueId(), event.getView().getTitle())) {
            return;
        }
        pvpArenaKitManager.handleEditorClose(player, event.getView().getTopInventory());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pvpArenaKitManager.clearPlayerSession(event.getPlayer().getUniqueId());
    }
}
