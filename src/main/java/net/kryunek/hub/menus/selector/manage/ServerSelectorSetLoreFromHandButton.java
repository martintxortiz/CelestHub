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
import java.util.List;

@AllArgsConstructor
public class ServerSelectorSetLoreFromHandButton extends Button {

    private final String key;
    private final FileConfig serverConfig = ModuleService.getFileModule().getFile(ConfigFiles.SERVER_SELECTOR);

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(Material.BOOK)
                .name(CC.translate("&bSet Lore From Hand"))
                .lore(Arrays.asList(
                        CC.translate("&7Copies lore from held item."),
                        CC.translate("&7If no lore, clears selector lore."),
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

        List<String> lore = (hand.hasItemMeta() && hand.getItemMeta().hasLore())
                ? hand.getItemMeta().getLore()
                : List.of();

        serverConfig.getConfiguration().set("SERVER_SELECTOR.ITEMS." + key + ".LORE", lore);
        serverConfig.save();
        playSuccess(player);
        player.sendMessage(CC.translate("&aUpdated lore for &f" + key + "&a."));
        new ServerSelectorItemEditorMenu(key).openMenu(player);
    }
}
