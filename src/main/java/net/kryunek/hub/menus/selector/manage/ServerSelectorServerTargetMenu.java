package net.kryunek.hub.menus.selector.manage;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.queue.Queue;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.menu.Button;
import net.kryunek.hub.utils.menu.Menu;
import net.kryunek.hub.utils.menu.buttons.BackButton;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ServerSelectorServerTargetMenu extends Menu {

    private final String selectorKey;
    private final boolean returnToItemEditor;
    private final FileConfig serverConfig = ModuleService.getFileModule().getFile(ConfigFiles.SERVER_SELECTOR);

    public ServerSelectorServerTargetMenu(String selectorKey) {
        this(selectorKey, false);
    }

    public ServerSelectorServerTargetMenu(String selectorKey, boolean returnToItemEditor) {
        this.selectorKey = selectorKey;
        this.returnToItemEditor = returnToItemEditor;
    }

    @Override
    public String getTitle(Player player) {
        return CC.translate("&8Select Target: " + selectorKey);
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        Set<String> servers = new LinkedHashSet<>();

        for (Queue queue : ModuleService.getManagerModule().getQueueManager().getQueues()) {
            servers.add(queue.getServer());
        }

        ConfigurationSection selectorItems = serverConfig.getConfiguration().getConfigurationSection("SERVER_SELECTOR.ITEMS");
        if (selectorItems != null) {
            for (String key : selectorItems.getKeys(false)) {
                String configured = serverConfig.getString("SERVER_SELECTOR.ITEMS." + key + ".SERVER", "", false);
                if (configured != null && !configured.isEmpty()) {
                    servers.add(configured);
                }
            }
        }

        int slot = 10;
        for (String server : servers) {
            if (slot == 17) {
                break;
            }
            buttons.put(slot, new ServerSelectorServerTargetButton(selectorKey, server, returnToItemEditor));
            slot++;
        }

        buttons.put(22, new BackButton(returnToItemEditor
                ? new ServerSelectorItemEditorMenu(selectorKey)
                : new ServerSelectorEditorMenu()));
        setPlaceholder(true);
        return buttons;
    }
}
