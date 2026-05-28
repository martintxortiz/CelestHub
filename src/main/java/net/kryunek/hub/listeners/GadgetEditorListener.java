package net.kryunek.hub.listeners;

import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.gadgets.GadgetEditSession;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.menus.gadgets.manage.GadgetItemEditorMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class GadgetEditorListener implements Listener {

    private final Celest hub;
    private final FileConfig gadgets;
    private final FileConfig messages;

    public GadgetEditorListener(Celest hub) {
        this.hub = hub;
        var files = ModuleService.getFileModule();
        this.gadgets = files.getFile("gadgets");
        this.messages = files.getFile("messages");
        Bukkit.getPluginManager().registerEvents(this, hub);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!GadgetEditSession.isActive(player)) {
            return;
        }

        event.setCancelled(true);
        String text = event.getMessage().trim();
        GadgetEditSession session = GadgetEditSession.get(player);
        String key = session.getKey();
        String basePath = "GADGETS_MENU.ITEMS." + key;

        if (!gadgets.getConfiguration().contains(basePath)) {
            GadgetEditSession.stop(player);
            player.sendMessage(CC.translate("&cGadget no longer exists."));
            return;
        }

        if (text.equalsIgnoreCase("cancel")) {
            GadgetEditSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("EDITOR.CANCELLED", "&cEditor action cancelled.", true)));
            openMenu(player, key);
            return;
        }

        if (session.getType() == GadgetEditSession.Type.COOLDOWN) {
            handleCooldown(player, key, text);
            return;
        }

        handleSlot(player, key, text);
    }

    private void handleCooldown(Player player, String key, String text) {
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            player.sendMessage(CC.translate("&cType a valid cooldown in seconds (>= 0)."));
            return;
        }

        if (value < 0) {
            player.sendMessage(CC.translate("&cType a valid cooldown in seconds (>= 0)."));
            return;
        }

        String type = gadgets.getString("GADGETS_MENU.ITEMS." + key + ".TYPE", "", false);
        if (type == null || type.isBlank()) {
            GadgetEditSession.stop(player);
            player.sendMessage(CC.translate("&cGadget type not found."));
            return;
        }

        gadgets.getConfiguration().set("GADGETS." + type.toUpperCase() + ".COOLDOWN_SECONDS", value);
        gadgets.save();
        GadgetEditSession.stop(player);
        player.sendMessage(CC.translate("&aUpdated cooldown of &f" + key + " &ato &f" + value + "s&a."));
        openMenu(player, key);
    }

    private void handleSlot(Player player, String key, String text) {
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            player.sendMessage(CC.translate("&cType a valid slot between 0 and 53."));
            return;
        }

        if (value < 0 || value > 53) {
            player.sendMessage(CC.translate("&cType a valid slot between 0 and 53."));
            return;
        }

        gadgets.getConfiguration().set("GADGETS_MENU.ITEMS." + key + ".SLOT", value);
        gadgets.save();
        GadgetEditSession.stop(player);
        player.sendMessage(CC.translate("&aUpdated slot of &f" + key + " &ato &f" + value + "&a."));
        openMenu(player, key);
    }

    private void openMenu(Player player, String key) {
        Bukkit.getScheduler().runTask(hub, () -> new GadgetItemEditorMenu(key).openMenu(player));
    }
}
