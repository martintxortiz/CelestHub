package net.kryunek.hub.menus.gadgets.manage;

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
public class GadgetToggleEnabledButton extends Button {

    private final String key;
    private final FileConfig gadgetsMenu = ModuleService.getFileModule().getFile(ConfigFiles.GADGETS);

    @Override
    public ItemStack getButtonItem(Player player) {
        String path = "GADGETS_MENU.ITEMS." + key + ".ENABLED";
        boolean enabled = !gadgetsMenu.getConfiguration().contains(path) || gadgetsMenu.getBoolean(path);
        return new ItemBuilder(enabled ? Material.LIME_DYE : Material.GRAY_DYE)
                .name(CC.translate("&bEnabled"))
                .lore(Arrays.asList(
                        CC.translate("&7Current: " + (enabled ? "&aYes" : "&cNo")),
                        "",
                        CC.translate("&eClick to toggle")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        String path = "GADGETS_MENU.ITEMS." + key + ".ENABLED";
        boolean enabled = !gadgetsMenu.getConfiguration().contains(path) || gadgetsMenu.getBoolean(path);
        gadgetsMenu.getConfiguration().set(path, !enabled);
        gadgetsMenu.save();
        playSuccess(player);
        new GadgetItemEditorMenu(key).openMenu(player);
    }
}
