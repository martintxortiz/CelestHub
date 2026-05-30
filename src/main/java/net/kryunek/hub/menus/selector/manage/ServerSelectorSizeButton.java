package net.kryunek.hub.menus.selector.manage;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.selector.ServerSelectorEditSession;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class ServerSelectorSizeButton extends Button {

    private final FileConfig serverConfig = ModuleService.getFileModule().getFile(ConfigFiles.SERVER_SELECTOR);

    @Override
    public ItemStack getButtonItem(Player player) {
        int current = normalizedSize(serverConfig.getInt("SERVER_SELECTOR.SIZE"));
        return new ItemBuilder(Material.CHEST)
                .name(CC.translate("&bSelector Menu Size"))
                .lore(Arrays.asList(
                        CC.translate("&7Current size: &f" + current),
                        "",
                        CC.translate("&eLeft click: +9"),
                        CC.translate("&eRight click: -9"),
                        CC.translate("&eShift click: set exact size")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        int current = normalizedSize(serverConfig.getInt("SERVER_SELECTOR.SIZE"));

        if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
            ServerSelectorEditSession.start(player, ServerSelectorEditSession.Type.SIZE, null);
            player.closeInventory();
            player.sendMessage(CC.translate("&eType selector size: 9, 18, 27, 36, 45, 54. Type 'cancel' to abort."));
            playNeutral(player);
            return;
        }

        int delta = clickType == ClickType.RIGHT ? -9 : 9;
        int updated = Math.max(9, Math.min(54, current + delta));
        if (updated == current) {
            playFail(player);
            return;
        }

        serverConfig.getConfiguration().set("SERVER_SELECTOR.SIZE", updated);
        serverConfig.save();
        playSuccess(player);
        player.sendMessage(CC.translate("&aUpdated selector size to &f" + updated + "&a."));
        new ServerSelectorEditorMenu().openMenu(player);
    }

    private int normalizedSize(int configured) {
        if (configured < 9 || configured > 54 || configured % 9 != 0) {
            return 27;
        }
        return configured;
    }
}
