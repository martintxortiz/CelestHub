package net.kryunek.hub.menus.editor.pvparena;

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

public class PvpArenaEditorMenu extends Menu {

    private final FileConfig editorMenus = ModuleService.getFileModule().getFile(ConfigFiles.EDITOR_MENUS);

    @Override
    public boolean isUpdateAfterClick() {
        return true;
    }

    @Override
    public String getTitle(Player player) {
        return CC.translate(editorMenus.getString("PVP_ARENA_EDITOR.TITLE"));
    }

    @Override
    public int getSize() {
        return editorMenus.getInt("PVP_ARENA_EDITOR.SIZE");
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        buttons.put(editorMenus.getConfiguration().getInt("PVP_ARENA_EDITOR.BUTTONS.TOGGLE.SLOT", 10), new PvpArenaEditorButton(PvpArenaEditorButton.Action.TOGGLE));
        buttons.put(editorMenus.getConfiguration().getInt("PVP_ARENA_EDITOR.BUTTONS.SET_WORLD.SLOT", 12), new PvpArenaEditorButton(PvpArenaEditorButton.Action.SET_WORLD));
        buttons.put(editorMenus.getConfiguration().getInt("PVP_ARENA_EDITOR.BUTTONS.POS_MODE.SLOT", 13), new PvpArenaEditorButton(PvpArenaEditorButton.Action.POS_MODE));
        buttons.put(editorMenus.getConfiguration().getInt("PVP_ARENA_EDITOR.BUTTONS.EDIT_KIT.SLOT", 16), new PvpArenaEditorButton(PvpArenaEditorButton.Action.EDIT_DEFAULT_KIT));
        buttons.put(editorMenus.getConfiguration().getInt("PVP_ARENA_EDITOR.BUTTONS.EFFECTS.SLOT", 14), new PvpArenaEditorButton(PvpArenaEditorButton.Action.EFFECTS));
        buttons.put(editorMenus.getConfiguration().getInt("PVP_ARENA_EDITOR.BUTTONS.PERIMETER_BLOCKS_TOGGLE.SLOT", 15), new PvpArenaEditorButton(PvpArenaEditorButton.Action.PERIMETER_BLOCKS_TOGGLE));
        buttons.put(editorMenus.getConfiguration().getInt("PVP_ARENA_EDITOR.BUTTONS.PERIMETER_BLOCKS_DURATION.SLOT", 11), new PvpArenaEditorButton(PvpArenaEditorButton.Action.PERIMETER_BLOCKS_DURATION));
        buttons.put(editorMenus.getConfiguration().getInt("PVP_ARENA_EDITOR.BUTTONS.BACK.SLOT", 26), new BackButton(new CelestEditorMenu()));
        setPlaceholder(editorMenus.getBoolean("PVP_ARENA_EDITOR.FILLER"));
        return buttons;
    }
}
