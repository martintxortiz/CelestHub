package net.kryunek.hub.menus.outfit.manage.list;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.outfit.Outfit;
import net.kryunek.hub.menus.editor.CelestEditorMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.menu.Button;
import net.kryunek.hub.utils.menu.buttons.BackButton;
import net.kryunek.hub.utils.menu.pagination.PageButton;
import net.kryunek.hub.utils.menu.pagination.PaginatedMenu;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class OutfitPaginatedMenu extends PaginatedMenu {

    private final FileConfig adminMenus = ModuleService.getFileModule().getFile(ConfigFiles.ADMIN_MENUS);

    {
        setAutoUpdate(true);
    }

    @Override
    public String getPrePaginatedTitle(Player player) {
        return CC.translate(adminMenus.getString("OUTFIT.LIST.TITLE"));
    }

    @Override
    public int getSize() {
        return 36;
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        int index = 8;
        for (Outfit outfit : ModuleService.getManagerModule().getOutfitManager().getOutfits().values()) {
            buttons.put(index++, new OutfitPaginatedButton(outfit));
        }
        return buttons;
    }

    @Override
    public Map<Integer, Button> getGlobalButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        buttons.put(getSlot(0, getSize() / 9 - 1), new PageButton(-1, this));
        buttons.put(getSlot(3, getSize() / 9 - 1), new OutfitMenuSizeButton());
        buttons.put(getSlot(4, getSize() / 9 - 1), new OutfitCreateButton());
        buttons.put(getSlot(7, getSize() / 9 - 1), new BackButton(new CelestEditorMenu()));
        buttons.put(getSlot(8, getSize() / 9 - 1), new PageButton(1, this));
        return buttons;
    }
}
