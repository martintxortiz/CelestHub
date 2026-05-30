package net.kryunek.hub.menus.editor;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.menu.Button;
import net.kryunek.hub.utils.menu.Menu;
import net.kryunek.hub.utils.menu.buttons.CloseButton;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class CelestEditorMenu extends Menu {

    private static final int LAYOUT_VERSION = 3;
    private final FileConfig editorMenuConfig = ModuleService.getFileModule().getFile(ConfigFiles.CELEST_EDITOR);

    @Override
    public String getTitle(Player player) {
        return CC.translate(editorMenuConfig.getString("EDITOR_MENU.TITLE"));
    }

    @Override
    public int getSize() {
        return editorMenuConfig.getInt("EDITOR_MENU.SIZE");
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        ensureEditorDefaults();
        Map<Integer, Button> buttons = new HashMap<>();
        buttons.put(editorMenuConfig.getInt("EDITOR_MENU.BUTTONS.QUEUE.SLOT"), new CelestEditorOpenButton("QUEUE"));
        buttons.put(editorMenuConfig.getInt("EDITOR_MENU.BUTTONS.PARTICLES.SLOT"), new CelestEditorOpenButton("PARTICLES"));
        buttons.put(editorMenuConfig.getInt("EDITOR_MENU.BUTTONS.OUTFIT.SLOT"), new CelestEditorOpenButton("OUTFIT"));
        buttons.put(editorMenuConfig.getInt("EDITOR_MENU.BUTTONS.SERVER_SELECTOR.SLOT"), new CelestEditorOpenButton("SERVER_SELECTOR"));
        buttons.put(editorMenuConfig.getInt("EDITOR_MENU.BUTTONS.HOTBAR.SLOT"), new CelestEditorOpenButton("HOTBAR"));
        buttons.put(editorMenuConfig.getInt("EDITOR_MENU.BUTTONS.TIMER.SLOT"), new CelestEditorOpenButton("TIMER"));
        buttons.put(editorMenuConfig.getInt("EDITOR_MENU.BUTTONS.CHAT.SLOT"), new CelestEditorOpenButton("CHAT"));
        buttons.put(editorMenuConfig.getInt("EDITOR_MENU.BUTTONS.LOTTERY.SLOT"), new CelestEditorOpenButton("LOTTERY"));
        if (editorMenuConfig.getConfiguration().contains("EDITOR_MENU.BUTTONS.PVP_ARENA")) {
            buttons.put(editorMenuConfig.getInt("EDITOR_MENU.BUTTONS.PVP_ARENA.SLOT"), new CelestEditorOpenButton("PVP_ARENA"));
        }

        if (editorMenuConfig.getConfiguration().contains("EDITOR_MENU.BUTTONS.GADGETS")) {
            buttons.put(editorMenuConfig.getInt("EDITOR_MENU.BUTTONS.GADGETS.SLOT"), new CelestEditorOpenButton("GADGETS"));
        }
        if (editorMenuConfig.getConfiguration().contains("EDITOR_MENU.BUTTONS.RANKS")) {
            buttons.put(editorMenuConfig.getInt("EDITOR_MENU.BUTTONS.RANKS.SLOT"), new CelestEditorOpenButton("RANKS"));
        }

        buttons.put(editorMenuConfig.getInt("EDITOR_MENU.BUTTONS.CLOSE.SLOT"), new CloseButton());
        setPlaceholder(editorMenuConfig.getBoolean("EDITOR_MENU.FILLER"));
        return buttons;
    }

    private void ensureEditorDefaults() {
        boolean changed = false;

        if (editorMenuConfig.getInt("EDITOR_MENU.LAYOUT_VERSION") < LAYOUT_VERSION) {
            editorMenuConfig.getConfiguration().set("EDITOR_MENU.SIZE", 54);
            editorMenuConfig.getConfiguration().set("EDITOR_MENU.BUTTONS.QUEUE.SLOT", 10);
            editorMenuConfig.getConfiguration().set("EDITOR_MENU.BUTTONS.PARTICLES.SLOT", 11);
            editorMenuConfig.getConfiguration().set("EDITOR_MENU.BUTTONS.OUTFIT.SLOT", 12);
            editorMenuConfig.getConfiguration().set("EDITOR_MENU.BUTTONS.SERVER_SELECTOR.SLOT", 13);
            editorMenuConfig.getConfiguration().set("EDITOR_MENU.BUTTONS.HOTBAR.SLOT", 14);
            editorMenuConfig.getConfiguration().set("EDITOR_MENU.BUTTONS.GADGETS.SLOT", 15);
            editorMenuConfig.getConfiguration().set("EDITOR_MENU.BUTTONS.RANKS.SLOT", 16);
            editorMenuConfig.getConfiguration().set("EDITOR_MENU.BUTTONS.LOTTERY.SLOT", 21);
            editorMenuConfig.getConfiguration().set("EDITOR_MENU.BUTTONS.TIMER.SLOT", 22);
            editorMenuConfig.getConfiguration().set("EDITOR_MENU.BUTTONS.CHAT.SLOT", 23);
            editorMenuConfig.getConfiguration().set("EDITOR_MENU.BUTTONS.PVP_ARENA.SLOT", 31);
            editorMenuConfig.getConfiguration().set("EDITOR_MENU.BUTTONS.CLOSE.SLOT", 49);
            editorMenuConfig.getConfiguration().set("EDITOR_MENU.LAYOUT_VERSION", LAYOUT_VERSION);
            changed = true;
        }

        if (!editorMenuConfig.getConfiguration().contains("EDITOR_MENU.BUTTONS.PVP_ARENA")) {
            String pvp = "EDITOR_MENU.BUTTONS.PVP_ARENA.";
            editorMenuConfig.getConfiguration().set(pvp + "SLOT", 31);
            editorMenuConfig.getConfiguration().set(pvp + "MATERIAL", "DIAMOND_SWORD");
            editorMenuConfig.getConfiguration().set(pvp + "DATA", 0);
            editorMenuConfig.getConfiguration().set(pvp + "NAME", "&cPvP Arena Editor");
            editorMenuConfig.getConfiguration().set(pvp + "LORE", java.util.List.of(
                    "&7Configure PvP arena area",
                    "&7Edit default arena kit",
                    "",
                    "&eClick to open"
            ));
            changed = true;
        }

        if (!editorMenuConfig.getConfiguration().contains("EDITOR_MENU.BUTTONS.GADGETS")) {
            String gadgets = "EDITOR_MENU.BUTTONS.GADGETS.";
            editorMenuConfig.getConfiguration().set(gadgets + "SLOT", 15);
            editorMenuConfig.getConfiguration().set(gadgets + "MATERIAL", "BLAZE_ROD");
            editorMenuConfig.getConfiguration().set(gadgets + "DATA", 0);
            editorMenuConfig.getConfiguration().set(gadgets + "NAME", "&bGadgets Editor");
            editorMenuConfig.getConfiguration().set(gadgets + "LORE", java.util.List.of(
                    "&7Enable/disable gadgets",
                    "&7Edit cooldown and slot",
                    "",
                    "&eClick to open"
            ));
            changed = true;
        }

        if (!editorMenuConfig.getConfiguration().contains("EDITOR_MENU.BUTTONS.RANKS")) {
            String ranks = "EDITOR_MENU.BUTTONS.RANKS.";
            editorMenuConfig.getConfiguration().set(ranks + "SLOT", 16);
            editorMenuConfig.getConfiguration().set(ranks + "MATERIAL", "NAME_TAG");
            editorMenuConfig.getConfiguration().set(ranks + "DATA", 0);
            editorMenuConfig.getConfiguration().set(ranks + "NAME", "&bRanks Editor");
            editorMenuConfig.getConfiguration().set(ranks + "LORE", java.util.List.of(
                    "&7Select rank provider",
                    "&7Edit queue/tab priorities",
                    "",
                    "&eClick to open"
            ));
            changed = true;
        }

        if (changed) {
            editorMenuConfig.save();
        }
    }
}
