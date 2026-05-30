package net.kryunek.hub.menus.editor.hotbar;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.AllArgsConstructor;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import net.kryunek.hub.utils.menu.ConfirmMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

@AllArgsConstructor
public class HotbarDeleteItemButton extends Button {

    private final String key;
    private final FileConfig hotbar = ModuleService.getFileModule().getFile(ConfigFiles.HOTBAR);

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(Material.BARRIER)
                .name(CC.translate("&cDelete Hotbar Item"))
                .lore(Arrays.asList(
                        CC.translate("&7Delete item: &f" + key),
                        "",
                        CC.translate("&cThis action is immediate")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        new ConfirmMenu(CC.translate("&8Confirm Delete"), confirmed -> {
            if (!confirmed) {
                new HotbarItemEditorMenu(key).openMenu(player);
                return;
            }

            hotbar.getConfiguration().set(key, null);
            hotbar.save();
            ModuleService.getManagerModule().getHotbarManager().load();
            ModuleService.getManagerModule().getHotbarManager().reload();
            playSuccess(player);
            player.sendMessage(CC.translate("&aDeleted hotbar item: &f" + key));
            new HotbarEditorMenu().openMenu(player);
        }, true).openMenu(player);
    }
}
