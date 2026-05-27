package net.kryunek.hub.listeners;

import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.selector.ServerSelectorEditSession;
import net.kryunek.hub.menus.selector.manage.ServerSelectorEditorMenu;
import net.kryunek.hub.menus.selector.manage.ServerSelectorItemEditorMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ServerSelectorEditorListener implements Listener {

    private final FileConfig messages;
    private final FileConfig serverConfig;

    public ServerSelectorEditorListener(Celest hub) {
        Bukkit.getPluginManager().registerEvents(this, hub);
        this.messages = ModuleService.getFileModule().getFile("messages");
        this.serverConfig = ModuleService.getFileModule().getFile("server_selector");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!ServerSelectorEditSession.isActive(player)) {
            return;
        }

        event.setCancelled(true);
        String text = event.getMessage().trim();
        ServerSelectorEditSession session = ServerSelectorEditSession.get(player);
        if (text.equalsIgnoreCase("cancel")) {
            ServerSelectorEditSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("EDITOR.CANCELLED", "&cEditor action cancelled.", true)));
            if (session.getType() == ServerSelectorEditSession.Type.SIZE) {
                Bukkit.getScheduler().runTask(Celest.get(), () -> new ServerSelectorEditorMenu().openMenu(player));
            } else {
                openMenu(player, session.getKey());
            }
            return;
        }

        if (session.getType() == ServerSelectorEditSession.Type.SIZE) {
            handleSize(player, text);
            return;
        }

        String key = session.getKey();
        String basePath = "SERVER_SELECTOR.ITEMS." + key;

        if (!serverConfig.getConfiguration().contains(basePath)) {
            ServerSelectorEditSession.stop(player);
            player.sendMessage(CC.translate("&cSelector item no longer exists."));
            return;
        }

        switch (session.getType()) {
            case NAME -> handleName(player, key, text);
            case SLOT -> handleSlot(player, key, text);
            case COMMAND -> handleCommand(player, key, text);
            default -> handleLore(player, key, text);
        }
    }

    private void handleName(Player player, String key, String text) {
        List<String> parts = splitByComma(text);
        if (parts.isEmpty()) {
            player.sendMessage(CC.translate("&cType a valid name."));
            return;
        }

        String value = String.join("\n", parts);
        serverConfig.getConfiguration().set("SERVER_SELECTOR.ITEMS." + key + ".NAME", value);
        serverConfig.save();
        ServerSelectorEditSession.stop(player);
        player.sendMessage(CC.translate("&aUpdated name for &f" + key + "&a."));
        openMenu(player, key);
    }

    private void handleLore(Player player, String key, String text) {
        if (text.equalsIgnoreCase("clear")) {
            serverConfig.getConfiguration().set("SERVER_SELECTOR.ITEMS." + key + ".LORE", List.of());
            serverConfig.save();
            ServerSelectorEditSession.stop(player);
            player.sendMessage(CC.translate("&aCleared lore for &f" + key + "&a."));
            openMenu(player, key);
            return;
        }

        List<String> lines = splitByComma(text);
        if (lines.isEmpty()) {
            player.sendMessage(CC.translate("&cType lore text or 'clear'."));
            return;
        }

        serverConfig.getConfiguration().set("SERVER_SELECTOR.ITEMS." + key + ".LORE", lines);
        serverConfig.save();
        ServerSelectorEditSession.stop(player);
        player.sendMessage(CC.translate("&aUpdated lore for &f" + key + "&a."));
        openMenu(player, key);
    }

    private void handleSlot(Player player, String key, String text) {
        int maxSlot = Math.max(8, normalizeSize(serverConfig.getInt("SERVER_SELECTOR.SIZE")) - 1);
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            player.sendMessage(CC.translate("&cType a valid slot between 0 and " + maxSlot + "."));
            return;
        }

        if (value < 0 || value > maxSlot) {
            player.sendMessage(CC.translate("&cType a valid slot between 0 and " + maxSlot + "."));
            return;
        }

        serverConfig.getConfiguration().set("SERVER_SELECTOR.ITEMS." + key + ".SLOT", value);
        serverConfig.save();
        ServerSelectorEditSession.stop(player);
        player.sendMessage(CC.translate("&aUpdated slot for &f" + key + " &ato &f" + value));
        openMenu(player, key);
    }

    private void handleCommand(Player player, String key, String text) {
        String value = text.equalsIgnoreCase("clear") ? "" : text;
        serverConfig.getConfiguration().set("SERVER_SELECTOR.ITEMS." + key + ".COMMAND", value);
        serverConfig.save();
        ServerSelectorEditSession.stop(player);
        player.sendMessage(CC.translate("&aUpdated click command for &f" + key + "&a."));
        openMenu(player, key);
    }

    private void handleSize(Player player, String text) {
        int requested;
        try {
            requested = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            player.sendMessage(CC.translate("&cType a valid size: 9, 18, 27, 36, 45, 54."));
            return;
        }

        int normalized = normalizeSize(requested);
        if (normalized != requested) {
            player.sendMessage(CC.translate("&cSize must be 9, 18, 27, 36, 45 or 54."));
            return;
        }

        serverConfig.getConfiguration().set("SERVER_SELECTOR.SIZE", normalized);
        serverConfig.save();
        ServerSelectorEditSession.stop(player);
        player.sendMessage(CC.translate("&aUpdated selector size to &f" + normalized + "&a."));
        Bukkit.getScheduler().runTask(Celest.get(), () -> new ServerSelectorEditorMenu().openMenu(player));
    }

    private int normalizeSize(int configured) {
        if (configured < 9) {
            return 9;
        }
        if (configured > 54) {
            return 54;
        }
        if (configured % 9 != 0) {
            return -1;
        }
        return configured;
    }

    private List<String> splitByComma(String input) {
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.toList());
    }

    private void openMenu(Player player, String key) {
        Bukkit.getScheduler().runTask(Celest.get(), () -> new ServerSelectorItemEditorMenu(key).openMenu(player));
    }
}
