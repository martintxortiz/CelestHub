package net.kryunek.hub.menus.lottery;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.menus.lottery.list.LotteryPaginatedMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class LotteryListButton extends Button {

    private final FileConfig adminMenus = ModuleService.getFileModule().getFile(ConfigFiles.ADMIN_MENUS);

    @Override
    public ItemStack getButtonItem(Player player) {
        String path = "LOTTERY.GLOBAL.BUTTONS.LIST.";
        return new ItemBuilder(adminMenus.getString(path + "MATERIAL"))
                .name(CC.translate(adminMenus.getString(path + "NAME")))
                .lore(adminMenus.getStringList(path + "LORE"))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        playSuccess(player);
        new LotteryPaginatedMenu().openMenu(player);
    }
}
