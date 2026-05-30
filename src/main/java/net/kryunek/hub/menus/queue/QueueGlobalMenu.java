package net.kryunek.hub.menus.queue;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.menus.editor.CelestEditorMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.menu.Button;
import net.kryunek.hub.utils.menu.Menu;
import net.kryunek.hub.utils.menu.buttons.BackButton;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class QueueGlobalMenu extends Menu {

    private final FileConfig adminMenus = ModuleService.getFileModule().getFile(ConfigFiles.ADMIN_MENUS);

    @Override
    public String getTitle(Player player) {
        return CC.translate(adminMenus.getString("QUEUE.GLOBAL.TITLE"));
    }

    @Override
    public int getSize() {
        return adminMenus.getInt("QUEUE.GLOBAL.SIZE");
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        buttons.put(adminMenus.getInt("QUEUE.GLOBAL.BUTTONS.LIST.SLOT"), new QueueListButton());
        buttons.put(adminMenus.getInt("QUEUE.GLOBAL.BUTTONS.DELAY.SLOT"), new QueueDelayButton());
        buttons.put(adminMenus.getInt("QUEUE.GLOBAL.BUTTONS.MESSAGE_DELAY.SLOT"), new QueueMessageDelayButton());
        buttons.put(8, new BackButton(new CelestEditorMenu()));
        setPlaceholder(adminMenus.getBoolean("QUEUE.GLOBAL.FILLER"));
        return buttons;
    }
}
