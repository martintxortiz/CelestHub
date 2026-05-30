package net.kryunek.hub.menus.selector.manage;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.menu.Button;
import net.kryunek.hub.utils.menu.Menu;
import net.kryunek.hub.utils.menu.buttons.BackButton;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class ServerSelectorItemEditorMenu extends Menu {

    private final String key;
    private final FileConfig serverConfig = ModuleService.getFileModule().getFile(ConfigFiles.SERVER_SELECTOR);

    public ServerSelectorItemEditorMenu(String key) {
        this.key = key;
    }

    @Override
    public boolean isUpdateAfterClick() {
        return true;
    }

    @Override
    public String getTitle(Player player) {
        return CC.translate("&8Edit Server Item: " + this.key);
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        ConfigurationSection section = serverConfig.getConfiguration().getConfigurationSection("SERVER_SELECTOR.ITEMS." + this.key);
        if (section != null) {
            buttons.put(10, new ServerSelectorSlotButton(this.key));
            buttons.put(11, new ServerSelectorOpenTargetMenuButton(this.key));
            buttons.put(12, new ServerSelectorDecorativeToggleButton(this.key));
            buttons.put(13, new ServerSelectorSetIconFromHandButton(this.key));
            buttons.put(14, new ServerSelectorSetNameFromChatButton(this.key));
            buttons.put(15, new ServerSelectorSetLoreFromChatButton(this.key));
            buttons.put(16, new ServerSelectorCommandButton(this.key));
            buttons.put(17, new ServerSelectorDeleteButton(this.key));
            buttons.put(26, new ServerSelectorSizeButton());
        }

        buttons.put(22, new BackButton(new ServerSelectorEditorMenu()));
        setPlaceholder(true);
        return buttons;
    }
}
