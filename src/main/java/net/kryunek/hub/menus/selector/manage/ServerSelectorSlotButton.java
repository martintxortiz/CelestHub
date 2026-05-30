package net.kryunek.hub.menus.selector.manage;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.AllArgsConstructor;
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

@AllArgsConstructor
public class ServerSelectorSlotButton extends Button {

    private final String key;
    private final FileConfig serverConfig = ModuleService.getFileModule().getFile(ConfigFiles.SERVER_SELECTOR);

    @Override
    public ItemStack getButtonItem(Player player) {
        int current = serverConfig.getInt("SERVER_SELECTOR.ITEMS." + key + ".SLOT");
        int maxSlot = Math.max(8, normalizedSize() - 1);
        return new ItemBuilder(Material.HOPPER)
                .name(CC.translate("&bItem Slot"))
                .lore(Arrays.asList(
                        CC.translate("&7Current slot: &f" + current),
                        CC.translate("&7Allowed range: &f0-" + maxSlot),
                        "",
                        CC.translate("&eLeft click: +1 slot"),
                        CC.translate("&eRight click: -1 slot"),
                        CC.translate("&eShift click: set exact slot")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        String path = "SERVER_SELECTOR.ITEMS." + key + ".SLOT";
        int current = serverConfig.getInt(path);
        int maxSlot = Math.max(8, normalizedSize() - 1);

        if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
            ServerSelectorEditSession.start(player, ServerSelectorEditSession.Type.SLOT, key);
            player.closeInventory();
            player.sendMessage(CC.translate("&eType slot for &f" + key + "&e between 0 and " + maxSlot + ". Type 'cancel' to abort."));
            playNeutral(player);
            return;
        }

        int delta = clickType == ClickType.RIGHT ? -1 : 1;
        int updated = Math.max(0, Math.min(maxSlot, current + delta));
        if (updated == current) {
            playFail(player);
            return;
        }

        serverConfig.getConfiguration().set(path, updated);
        serverConfig.save();
        playSuccess(player);
        new ServerSelectorItemEditorMenu(key).openMenu(player);
    }

    private int normalizedSize() {
        int size = serverConfig.getInt("SERVER_SELECTOR.SIZE");
        if (size < 9 || size > 54 || size % 9 != 0) {
            return 27;
        }
        return size;
    }
}
