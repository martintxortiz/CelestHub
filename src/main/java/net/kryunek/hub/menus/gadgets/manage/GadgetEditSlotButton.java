package net.kryunek.hub.menus.gadgets.manage;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.AllArgsConstructor;
import net.kryunek.hub.managers.gadgets.GadgetEditSession;
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
public class GadgetEditSlotButton extends Button {

    private final String key;
    private final FileConfig gadgetsMenu = ModuleService.getFileModule().getFile(ConfigFiles.GADGETS);

    @Override
    public ItemStack getButtonItem(Player player) {
        int current = gadgetsMenu.getInt("GADGETS_MENU.ITEMS." + key + ".SLOT");
        return new ItemBuilder(Material.HOPPER)
                .name(CC.translate("&bSlot"))
                .lore(Arrays.asList(
                        CC.translate("&7Current: &f" + current),
                        "",
                        CC.translate("&eLeft click: +1"),
                        CC.translate("&eRight click: -1"),
                        CC.translate("&eShift click: set exact")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        String path = "GADGETS_MENU.ITEMS." + key + ".SLOT";
        int current = gadgetsMenu.getInt(path);
        int max = 53;

        if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
            GadgetEditSession.start(player, GadgetEditSession.Type.SLOT, key);
            player.closeInventory();
            player.sendMessage(CC.translate("&eType slot (0-53) for &f" + key + "&e. Type 'cancel' to abort."));
            playNeutral(player);
            return;
        }

        int delta = clickType == ClickType.RIGHT ? -1 : 1;
        int updated = Math.max(0, Math.min(max, current + delta));
        if (updated == current) {
            playFail(player);
            return;
        }

        gadgetsMenu.getConfiguration().set(path, updated);
        gadgetsMenu.save();
        playSuccess(player);
        new GadgetItemEditorMenu(key).openMenu(player);
    }
}
