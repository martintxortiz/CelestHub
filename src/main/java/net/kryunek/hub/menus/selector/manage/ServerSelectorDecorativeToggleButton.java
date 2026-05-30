package net.kryunek.hub.menus.selector.manage;

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
public class ServerSelectorDecorativeToggleButton extends Button {

    private final String key;
    private final FileConfig serverConfig = ModuleService.getFileModule().getFile(ConfigFiles.SERVER_SELECTOR);

    @Override
    public ItemStack getButtonItem(Player player) {
        boolean decorative = serverConfig.getBoolean("SERVER_SELECTOR.ITEMS." + key + ".DECORATIVE");
        return new ItemBuilder(decorative ? Material.GRAY_DYE : Material.LIME_DYE)
                .name(CC.translate("&bDecorative Item"))
                .lore(Arrays.asList(
                        CC.translate("&7Current: " + (decorative ? "&aEnabled" : "&cDisabled")),
                        CC.translate("&7When enabled, it won't join queue."),
                        "",
                        CC.translate("&eClick to toggle")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        String path = "SERVER_SELECTOR.ITEMS." + key + ".DECORATIVE";
        boolean decorative = serverConfig.getBoolean(path);
        serverConfig.getConfiguration().set(path, !decorative);
        serverConfig.save();
        playSuccess(player);
        new ServerSelectorItemEditorMenu(key).openMenu(player);
    }
}
