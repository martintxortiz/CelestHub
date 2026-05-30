package net.kryunek.hub.menus.selector.manage;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.menus.editor.CelestEditorMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.menu.Button;
import net.kryunek.hub.utils.menu.Menu;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerSelectorEditorMenu extends Menu {

    private final FileConfig serverConfig = ModuleService.getFileModule().getFile(ConfigFiles.SERVER_SELECTOR);
    private static final Map<UUID, String> MOVE_MODE = new HashMap<>();

    @Override
    public String getTitle(Player player) {
        return CC.translate("&8Server Selector Editor");
    }

    @Override
    public int getSize() {
        int configured = getSelectorSize();
        if (configured >= 54) {
            return 54;
        }
        return configured + 9;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        int selectorSize = getSelectorSize();

        for (int slot = 0; slot < selectorSize; slot++) {
            buttons.put(slot, new ServerSelectorEditorEmptySlotButton(slot));
        }

        ConfigurationSection section = serverConfig.getConfiguration().getConfigurationSection("SERVER_SELECTOR.ITEMS");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                int slot = serverConfig.getInt("SERVER_SELECTOR.ITEMS." + key + ".SLOT");
                if (slot < 0 || slot >= selectorSize) {
                    continue;
                }
                buttons.put(slot, new ServerSelectorEditorEntryButton(key));
            }
        }

        if (getSize() > selectorSize) {
            int controlSlot = selectorSize + 4;
            if (controlSlot < getSize()) {
                buttons.put(controlSlot, new ServerSelectorSizeButton());
            }
        }
        return buttons;
    }

    public static boolean isMoving(Player player) {
        return MOVE_MODE.containsKey(player.getUniqueId());
    }

    public static String getMovingKey(Player player) {
        return MOVE_MODE.get(player.getUniqueId());
    }

    public static void startMove(Player player, String key) {
        MOVE_MODE.put(player.getUniqueId(), key);
    }

    public static void clearMove(Player player) {
        MOVE_MODE.remove(player.getUniqueId());
    }

    public static void moveToSlot(Player player, int targetSlot) {
        String movingKey = MOVE_MODE.get(player.getUniqueId());
        if (movingKey == null) {
            return;
        }

        FileConfig serverConfig = ModuleService.getFileModule().getFile(ConfigFiles.SERVER_SELECTOR);
        ConfigurationSection items = serverConfig.getConfiguration().getConfigurationSection("SERVER_SELECTOR.ITEMS");
        if (items == null || !items.contains(movingKey)) {
            MOVE_MODE.remove(player.getUniqueId());
            return;
        }

        String occupantKey = null;
        for (String key : items.getKeys(false)) {
            if (key.equalsIgnoreCase(movingKey)) {
                continue;
            }
            if (serverConfig.getInt("SERVER_SELECTOR.ITEMS." + key + ".SLOT") == targetSlot) {
                occupantKey = key;
                break;
            }
        }

        String movingPath = "SERVER_SELECTOR.ITEMS." + movingKey + ".SLOT";
        int previousSlot = serverConfig.getInt(movingPath);
        serverConfig.getConfiguration().set(movingPath, targetSlot);
        if (occupantKey != null) {
            serverConfig.getConfiguration().set("SERVER_SELECTOR.ITEMS." + occupantKey + ".SLOT", previousSlot);
        }
        serverConfig.save();
        MOVE_MODE.remove(player.getUniqueId());
    }

    public static String createAtSlot(Player player, int targetSlot) {
        FileConfig serverConfig = ModuleService.getFileModule().getFile(ConfigFiles.SERVER_SELECTOR);
        ConfigurationSection items = serverConfig.getConfiguration().getConfigurationSection("SERVER_SELECTOR.ITEMS");
        if (items == null) {
            items = serverConfig.getConfiguration().createSection("SERVER_SELECTOR.ITEMS");
        }

        String baseKey = "custom_" + targetSlot;
        String key = baseKey;
        int index = 1;
        while (items.contains(key)) {
            key = baseKey + "_" + index++;
        }

        String defaultServer = "survival";
        if (!ModuleService.getManagerModule().getQueueManager().getQueues().isEmpty()) {
            defaultServer = ModuleService.getManagerModule().getQueueManager().getQueues().iterator().next().getServer();
        }

        String path = "SERVER_SELECTOR.ITEMS." + key + ".";
        serverConfig.getConfiguration().set(path + "NAME", "&fNew Selector Item");
        serverConfig.getConfiguration().set(path + "ITEM", "PAPER");
        serverConfig.getConfiguration().set(path + "DATA", 0);
        serverConfig.getConfiguration().set(path + "SLOT", targetSlot);
        serverConfig.getConfiguration().set(path + "LORE", Arrays.asList(
                "&7Configure this item from editor.",
                "&7Set queue, icon, name, lore and command."
        ));
        serverConfig.getConfiguration().set(path + "SERVER", defaultServer);
        serverConfig.getConfiguration().set(path + "DECORATIVE", false);
        serverConfig.getConfiguration().set(path + "COMMAND", "");
        serverConfig.save();
        return key;
    }

    private int getSelectorSize() {
        int configured = serverConfig.getInt("SERVER_SELECTOR.SIZE");
        if (configured < 9 || configured > 54 || configured % 9 != 0) {
            return 27;
        }
        return configured;
    }

    @Override
    public void onClose(Player player) {
        super.onClose(player);
        MOVE_MODE.remove(player.getUniqueId());
        if (!isClosedByMenu()) {
            Bukkit.getScheduler().runTask(Celest.get(), () -> new CelestEditorMenu().openMenu(player));
        }
    }
}
