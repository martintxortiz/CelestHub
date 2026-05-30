package net.kryunek.hub.menus.editor.hotbar;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.menus.editor.CelestEditorMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.menu.Button;
import net.kryunek.hub.utils.menu.buttons.BackButton;
import net.kryunek.hub.utils.menu.buttons.CloseButton;
import net.kryunek.hub.utils.menu.pagination.PageButton;
import net.kryunek.hub.utils.menu.pagination.PaginatedMenu;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class HotbarEditorMenu extends PaginatedMenu {

    private final FileConfig editorMenus = ModuleService.getFileModule().getFile(ConfigFiles.EDITOR_MENUS);
    private final FileConfig hotbar = ModuleService.getFileModule().getFile(ConfigFiles.HOTBAR);

    @Override
    public String getPrePaginatedTitle(Player player) {
        return CC.translate("&8Hotbar Editor");
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public int getMaxItemsPerPage(Player player) {
        return 9;
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        if (hotbar.getConfiguration() == null) {
            return buttons;
        }

        int index = 8;
        for (String key : hotbar.getConfiguration().getKeys(false)) {
            buttons.put(index++, new HotbarEditorButton(key));
        }
        return buttons;
    }

    @Override
    public Map<Integer, Button> getGlobalButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        int row = getSize() / 9 - 1;
        buttons.put(getSlot(0, row), new PageButton(-1, this));
        buttons.put(getSlot(2, row), new HotbarCreateItemButton());
        buttons.put(getSlot(4, row), new CloseButton());
        buttons.put(getSlot(7, row), new BackButton(new CelestEditorMenu()));
        buttons.put(getSlot(8, row), new PageButton(1, this));
        return buttons;
    }
}
