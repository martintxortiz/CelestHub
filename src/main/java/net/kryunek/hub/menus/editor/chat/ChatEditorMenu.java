package net.kryunek.hub.menus.editor.chat;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.menus.editor.CelestEditorMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.menu.Button;
import net.kryunek.hub.utils.menu.Menu;
import net.kryunek.hub.utils.menu.buttons.BackButton;
import net.kryunek.hub.utils.menu.buttons.CloseButton;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class ChatEditorMenu extends Menu {

    private final FileConfig editorMenus = ModuleService.getFileModule().getFile(ConfigFiles.EDITOR_MENUS);

    @Override
    public String getTitle(Player player) {
        return CC.translate(editorMenus.getString("CHAT_EDITOR.TITLE"));
    }

    @Override
    public int getSize() {
        return editorMenus.getInt("CHAT_EDITOR.SIZE");
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        buttons.put(editorMenus.getConfiguration().getInt("CHAT_EDITOR.BUTTONS.PAUSE.SLOT", 0), new ChatPauseToggleButton());
        buttons.put(editorMenus.getConfiguration().getInt("CHAT_EDITOR.BUTTONS.SLOW.SLOT", 1), new ChatSlowEditButton());
        buttons.put(editorMenus.getConfiguration().getInt("CHAT_EDITOR.BUTTONS.CLEAR_LINES.SLOT", 2), new ChatClearLinesEditButton());
        buttons.put(editorMenus.getConfiguration().getInt("CHAT_EDITOR.BUTTONS.PREFIX.SLOT", 3), new ChatPrefixEditButton());
        buttons.put(editorMenus.getConfiguration().getInt("CHAT_EDITOR.BUTTONS.CLEAR_NOW.SLOT", 4), new ChatClearNowButton());
        buttons.put(editorMenus.getConfiguration().getInt("CHAT_EDITOR.BUTTONS.BACK.SLOT", 9), new BackButton(new CelestEditorMenu()));
        setPlaceholder(editorMenus.getBoolean("CHAT_EDITOR.FILLER"));
        return buttons;
    }
}
