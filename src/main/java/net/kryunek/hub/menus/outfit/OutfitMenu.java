package net.kryunek.hub.menus.outfit;

import net.kryunek.hub.support.config.ConfigFiles;
import com.google.common.collect.Maps;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.outfit.Outfit;
import net.kryunek.hub.managers.outfit.OutfitManager;
import net.kryunek.hub.menus.settings.SettingsMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.menu.Button;
import net.kryunek.hub.utils.menu.buttons.BackButton;
import net.kryunek.hub.utils.menu.pagination.PageButton;
import net.kryunek.hub.utils.menu.pagination.PaginatedMenu;
import org.bukkit.entity.Player;

import java.util.Map;

public class OutfitMenu extends PaginatedMenu {

    private final OutfitManager outfitManager;
    private final FileConfig outfitConfig;

    public OutfitMenu() {
        this.outfitManager = ModuleService.getManagerModule().getOutfitManager();
        this.outfitConfig = ModuleService.getFileModule().getFile(ConfigFiles.OUTFIT);
    }

    @Override
    public boolean isUpdateAfterClick() {
        return true;
    }

    @Override
    public Map<Integer, Button> getGlobalButtons(Player player) {
        Map<Integer, Button> buttons = Maps.newHashMap();
        int row = getSize() / 9 - 1;
        buttons.put(getSlot(0, row), new PageButton(-1, this));
        buttons.put(getSlot(3, row), new OutfitRemoveButton());
        buttons.put(getSlot(4, row), new BackButton(new SettingsMenu()));
        buttons.put(getSlot(8, row), new PageButton(1, this));
        return buttons;
    }

    @Override
    public String getPrePaginatedTitle(Player player) {
        return CC.translate(outfitConfig.getString("TITLE"));
    }

    @Override
    public int getMaxItemsPerPage(Player player) {
        return Math.max(9, normalizeSize(outfitConfig.getInt("SIZE")) - 9);
    }

    @Override
    public int getSize() {
        return normalizeSize(outfitConfig.getInt("SIZE"));
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = Maps.newHashMap();
        int index = 8;
        for (Outfit outfit : this.outfitManager.getOutfits().values()) {
            buttons.put(index++, new OutfitButton(outfit));
        }
        setPlaceholder(outfitConfig.getBoolean("FILLER"));
        return buttons;
    }

    private int normalizeSize(int configured) {
        if (configured < 18 || configured > 54 || configured % 9 != 0) {
            return 18;
        }
        return configured;
    }
}
