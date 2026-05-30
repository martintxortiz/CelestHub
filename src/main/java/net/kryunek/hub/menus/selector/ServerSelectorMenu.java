package net.kryunek.hub.menus.selector;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.menu.Button;
import net.kryunek.hub.utils.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public class ServerSelectorMenu extends Menu {

    private final FileConfig serverConfig = ModuleService.getFileModule().getFile(ConfigFiles.SERVER_SELECTOR);

    @Override
    public String getTitle(Player player) {
        return CC.translate(serverConfig.getString("SERVER_SELECTOR.TITLE"));
    }

    @Override
    public int getSize() {
        return serverConfig.getInt("SERVER_SELECTOR.SIZE");
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        ConfigurationSection section = serverConfig.getConfiguration().getConfigurationSection("SERVER_SELECTOR.ITEMS");
        if (section == null) {
            return buttons;
        }
        for(String s : section.getKeys(false)) {
            buttons.put(serverConfig.getInt("SERVER_SELECTOR.ITEMS." + s + ".SLOT"), new ServerButton(s));
        }
        setAutoUpdate(true);
        return buttons;
    }


    @Override
    public boolean isAutoUpdate() {
        return true;
    }
}
