package net.kryunek.hub.menus.selector.manage;

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
public class ServerSelectorDeleteButton extends Button {

    private final String key;
    private final FileConfig serverConfig = ModuleService.getFileModule().getFile(ConfigFiles.SERVER_SELECTOR);

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(Material.BARRIER)
                .name(CC.translate("&cDelete Selector Item"))
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
                new ServerSelectorItemEditorMenu(key).openMenu(player);
                return;
            }

            serverConfig.getConfiguration().set("SERVER_SELECTOR.ITEMS." + key, null);
            serverConfig.save();
            playSuccess(player);
            player.sendMessage(CC.translate("&aDeleted selector item: &f" + key));
            new ServerSelectorEditorMenu().openMenu(player);
        }, true).openMenu(player);
    }
}
