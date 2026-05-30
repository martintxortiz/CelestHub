package net.kryunek.hub.menus.editor.hotbar;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.AllArgsConstructor;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

@AllArgsConstructor
public class HotbarItemToggleClickSoundButton extends Button {

    private final String key;
    private final FileConfig hotbar = ModuleService.getFileModule().getFile(ConfigFiles.HOTBAR);

    @Override
    public ItemStack getButtonItem(Player player) {
        boolean enabled = hotbar.getConfiguration().getBoolean(key + ".CLICK_SOUND.ENABLED", false);
        return new ItemBuilder(enabled ? Material.NOTE_BLOCK : Material.JUKEBOX)
                .name(CC.translate("&bClick Sound"))
                .lore(Arrays.asList(
                        CC.translate("&7Enabled: " + (enabled ? "&atrue" : "&cfalse")),
                        "",
                        CC.translate("&eLeft click to toggle"),
                        CC.translate("&eRight click to preview")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        if (clickType.isRightClick()) {
            ModuleService.getManagerModule().getHotbarManager().playClickSound(player, ModuleService.getManagerModule().getHotbarManager().getHotbar(key));
            playNeutral(player);
            return;
        }

        boolean current = hotbar.getConfiguration().getBoolean(key + ".CLICK_SOUND.ENABLED", false);
        hotbar.getConfiguration().set(key + ".CLICK_SOUND.ENABLED", !current);
        hotbar.save();
        ModuleService.getManagerModule().getHotbarManager().load();
        ModuleService.getManagerModule().getHotbarManager().reload();
        playSuccess(player);
        new HotbarItemEditorMenu(key).openMenu(player);
    }
}
