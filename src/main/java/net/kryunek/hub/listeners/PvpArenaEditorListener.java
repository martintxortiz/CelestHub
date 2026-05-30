package net.kryunek.hub.listeners;

import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.pvparena.PvpArenaSelectionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PvpArenaEditorListener implements Listener {

    private final PvpArenaSelectionManager selectionManager;

    public PvpArenaEditorListener(Celest hub) {
        Bukkit.getPluginManager().registerEvents(this, hub);
        this.selectionManager = ModuleService.getManagerModule().getPvpArenaSelectionManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!selectionManager.isSelectionMode(player)) {
            return;
        }
        String msg = PlainTextComponentSerializer.plainText().serialize(event.message()).trim().toLowerCase();
        if (!msg.equals("pos1") && !msg.equals("pos2")) {
            return;
        }
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(Celest.get(), () -> selectionManager.setPosFromPlayer(player, msg.toUpperCase()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!selectionManager.isSelectionMode(player)) {
            return;
        }
        ItemStack item = event.getItem();
        if (!selectionManager.isSelector(item)) {
            return;
        }

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            selectionManager.setPosFromPlayer(player, "POS1");
            event.setCancelled(true);
            return;
        }
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            selectionManager.setPosFromPlayer(player, "POS2");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        selectionManager.clear(event.getPlayer().getUniqueId());
    }
}
