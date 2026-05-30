package net.kryunek.hub.menus.gadgets;

import net.kryunek.hub.support.config.ConfigFiles;
import com.google.common.collect.Maps;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.menus.settings.SettingsMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.menu.Button;
import net.kryunek.hub.utils.menu.buttons.BackButton;
import net.kryunek.hub.utils.menu.pagination.PageButton;
import net.kryunek.hub.utils.menu.pagination.PaginatedMenu;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class GadgetsMenu extends PaginatedMenu {

    private final FileConfig gadgetsMenu = ModuleService.getFileModule().getFile(ConfigFiles.GADGETS);

    @Override
    public boolean isUpdateAfterClick() {
        return true;
    }

    @Override
    public String getPrePaginatedTitle(Player player) {
        return CC.translate(gadgetsMenu.getString("GADGETS_MENU.TITLE"));
    }

    @Override
    public int getSize() {
        int configured = gadgetsMenu.getInt("GADGETS_MENU.SIZE");
        if (configured < 18 || configured > 54 || configured % 9 != 0) {
            return 54;
        }
        return configured;
    }

    @Override
    public int getMaxItemsPerPage(Player player) {
        return Math.max(9, getSize() - 9);
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = Maps.newHashMap();
        ConfigurationSection section = gadgetsMenu.getConfiguration().getConfigurationSection("GADGETS_MENU.ITEMS");
        if (section != null) {
            List<String> orderedKeys = new ArrayList<>(section.getKeys(false));
            orderedKeys.sort(Comparator
                    .comparingInt((String key) -> gadgetsMenu.getConfiguration().getInt("GADGETS_MENU.ITEMS." + key + ".SLOT", Integer.MAX_VALUE))
                    .thenComparing(String::compareToIgnoreCase));

            int index = 8;
            for (String key : orderedKeys) {
                String enabledPath = "GADGETS_MENU.ITEMS." + key + ".ENABLED";
                if (gadgetsMenu.getConfiguration().contains(enabledPath) && !gadgetsMenu.getBoolean(enabledPath)) {
                    continue;
                }
                buttons.put(index++, new GadgetButton(key));
            }
        }
        setPlaceholder(gadgetsMenu.getBoolean("GADGETS_MENU.FILLER"));
        return buttons;
    }

    @Override
    public Map<Integer, Button> getGlobalButtons(Player player) {
        Map<Integer, Button> buttons = Maps.newHashMap();
        int row = getSize() / 9 - 1;
        buttons.put(getSlot(0, row), new PageButton(-1, this));
        if (gadgetsMenu.getBoolean("GADGETS_MENU.DISABLE.ENABLED")) {
            buttons.put(getSlot(3, row), new GadgetDisableButton());
        }
        buttons.put(getSlot(4, row), new BackButton(new SettingsMenu()));
        buttons.put(getSlot(8, row), new PageButton(1, this));
        return buttons;
    }
}
