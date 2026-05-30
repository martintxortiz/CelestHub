package net.kryunek.hub.menus.gadgets.manage;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.menus.editor.CelestEditorMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.menu.Button;
import net.kryunek.hub.utils.menu.buttons.BackButton;
import net.kryunek.hub.utils.menu.pagination.PageButton;
import net.kryunek.hub.utils.menu.pagination.PaginatedMenu;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class GadgetEditorMenu extends PaginatedMenu {

    private final FileConfig gadgetsMenu = ModuleService.getFileModule().getFile(ConfigFiles.GADGETS);

    {
        setAutoUpdate(true);
    }

    @Override
    public String getPrePaginatedTitle(Player player) {
        return CC.translate("&8Gadget Editor");
    }

    @Override
    public int getSize() {
        return 36;
    }

    @Override
    public int getMaxItemsPerPage(Player player) {
        return 18;
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        ConfigurationSection section = gadgetsMenu.getConfiguration().getConfigurationSection("GADGETS_MENU.ITEMS");
        if (section == null) {
            return buttons;
        }

        int index = 8;
        for (String key : section.getKeys(false)) {
            buttons.put(index++, new GadgetEditorButton(key));
        }
        return buttons;
    }

    @Override
    public Map<Integer, Button> getGlobalButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        int row = getSize() / 9 - 1;
        buttons.put(getSlot(0, row), new PageButton(-1, this));
        buttons.put(getSlot(7, row), new BackButton(new CelestEditorMenu()));
        buttons.put(getSlot(8, row), new PageButton(1, this));
        return buttons;
    }
}
