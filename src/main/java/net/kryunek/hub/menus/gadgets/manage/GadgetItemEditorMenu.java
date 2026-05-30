package net.kryunek.hub.menus.gadgets.manage;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.menu.Button;
import net.kryunek.hub.utils.menu.Menu;
import net.kryunek.hub.utils.menu.buttons.BackButton;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class GadgetItemEditorMenu extends Menu {

    private final String key;
    private final FileConfig gadgetsMenu = ModuleService.getFileModule().getFile(ConfigFiles.GADGETS);

    public GadgetItemEditorMenu(String key) {
        this.key = key;
    }

    @Override
    public boolean isUpdateAfterClick() {
        return true;
    }

    @Override
    public String getTitle(Player player) {
        return CC.translate("&8Edit Gadget: " + key);
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        if (gadgetsMenu.getConfiguration().contains("GADGETS_MENU.ITEMS." + key)) {
            buttons.put(11, new GadgetToggleEnabledButton(key));
            buttons.put(13, new GadgetEditCooldownButton(key));
            buttons.put(15, new GadgetEditSlotButton(key));
        }
        buttons.put(22, new BackButton(new GadgetEditorMenu()));
        setPlaceholder(true);
        return buttons;
    }
}
