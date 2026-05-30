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
public class HotbarItemToggleEnabledButton extends Button {

    private final String key;
    private final FileConfig hotbar = ModuleService.getFileModule().getFile(ConfigFiles.HOTBAR);

    @Override
    public ItemStack getButtonItem(Player player) {
        boolean enabled = hotbar.getBoolean(key + ".ENABLED");
        return new ItemBuilder(enabled ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
                .name(CC.translate(enabled ? "&aEnabled" : "&cDisabled"))
                .lore(Arrays.asList(
                        CC.translate("&7Current: " + (enabled ? "&atrue" : "&cfalse")),
                        "",
                        CC.translate("&eClick to toggle")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        boolean current = hotbar.getBoolean(key + ".ENABLED");
        hotbar.getConfiguration().set(key + ".ENABLED", !current);
        hotbar.save();
        ModuleService.getManagerModule().getHotbarManager().load();
        ModuleService.getManagerModule().getHotbarManager().reload();
        playSuccess(player);
        new HotbarItemEditorMenu(key).openMenu(player);
    }
}
