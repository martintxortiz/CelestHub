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
public class ServerSelectorSetNameFromHandButton extends Button {

    private final String key;
    private final FileConfig serverConfig = ModuleService.getFileModule().getFile(ConfigFiles.SERVER_SELECTOR);

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(Material.NAME_TAG)
                .name(CC.translate("&bSet Name From Hand"))
                .lore(Arrays.asList(
                        CC.translate("&7Uses held item's display name."),
                        CC.translate("&7If no display name, uses item material."),
                        "",
                        CC.translate("&eClick to apply")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            playFail(player);
            player.sendMessage(CC.translate("&cHold an item in your hand."));
            return;
        }

        String name;
        if (hand.hasItemMeta() && hand.getItemMeta().getDisplayName() != null) {
            name = hand.getItemMeta().getDisplayName();
        } else {
            name = "&f" + hand.getType().name();
        }

        serverConfig.getConfiguration().set("SERVER_SELECTOR.ITEMS." + key + ".NAME", name);
        serverConfig.save();
        playSuccess(player);
        player.sendMessage(CC.translate("&aUpdated name for &f" + key + "&a."));
        new ServerSelectorItemEditorMenu(key).openMenu(player);
    }
}
