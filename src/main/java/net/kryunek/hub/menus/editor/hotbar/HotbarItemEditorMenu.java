package net.kryunek.hub.menus.editor.hotbar;

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

public class HotbarItemEditorMenu extends Menu {

    private final String key;
    private final FileConfig hotbar = ModuleService.getFileModule().getFile(ConfigFiles.HOTBAR);

    public HotbarItemEditorMenu(String key) {
        this.key = key;
    }

    @Override
    public boolean isUpdateAfterClick() {
        return true;
    }

    @Override
    public String getTitle(Player player) {
        return CC.translate("&8Edit Hotbar Item: " + key);
    }

    @Override
    public int getSize() {
        return 36;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        if (hotbar.getConfiguration().contains(key)) {
            buttons.put(10, new HotbarItemEditSlotButton(key));
            buttons.put(11, new HotbarItemToggleEnabledButton(key));
            buttons.put(12, new HotbarItemEditCommandButton(key));
            buttons.put(13, new HotbarItemCopyFromHandButton(key));
            buttons.put(14, new HotbarItemEditNameButton(key));
            buttons.put(15, new HotbarItemEditLoreButton(key));
            buttons.put(16, new HotbarDeleteItemButton(key));
            buttons.put(19, new HotbarItemEditHeadOwnerButton(key));
            buttons.put(20, new HotbarItemEditHeadOwnerUuidButton(key));
            buttons.put(21, new HotbarItemToggleClickSoundButton(key));
            buttons.put(23, new HotbarItemEditClickSoundButton(key));
            buttons.put(24, new HotbarItemEditClickSoundVolumeButton(key));
            buttons.put(25, new HotbarItemEditClickSoundPitchButton(key));
        }

        buttons.put(31, new BackButton(new HotbarEditorMenu()));
        setPlaceholder(true);
        return buttons;
    }
}
