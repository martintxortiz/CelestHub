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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;

@AllArgsConstructor
public class ServerSelectorSetIconFromHandButton extends Button {

    private final String key;
    private final FileConfig serverConfig = ModuleService.getFileModule().getFile(ConfigFiles.SERVER_SELECTOR);

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(Material.ITEM_FRAME)
                .name(CC.translate("&bSet Icon From Hand"))
                .lore(Arrays.asList(
                        CC.translate("&7Hold an item and click."),
                        CC.translate("&7Updates MATERIAL and DATA.")
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

        String basePath = "SERVER_SELECTOR.ITEMS." + key + ".";
        serverConfig.getConfiguration().set(basePath + "ITEM", hand.getType().name());
        serverConfig.getConfiguration().set(basePath + "DATA", hand.getDurability());

        if (hand.getType() == Material.PLAYER_HEAD && hand.getItemMeta() instanceof SkullMeta skullMeta && skullMeta.getOwningPlayer() != null) {
            if (skullMeta.getOwningPlayer().getName() != null) {
                serverConfig.getConfiguration().set(basePath + "HEAD_OWNER", skullMeta.getOwningPlayer().getName());
            } else {
                serverConfig.getConfiguration().set(basePath + "HEAD_OWNER", null);
            }
            serverConfig.getConfiguration().set(basePath + "HEAD_OWNER_UUID", skullMeta.getOwningPlayer().getUniqueId().toString());
        } else {
            serverConfig.getConfiguration().set(basePath + "HEAD_OWNER", null);
            serverConfig.getConfiguration().set(basePath + "HEAD_OWNER_UUID", null);
        }

        serverConfig.save();
        playSuccess(player);
        player.sendMessage(CC.translate("&aUpdated icon for &f" + key + "&a."));
        new ServerSelectorItemEditorMenu(key).openMenu(player);
    }
}
