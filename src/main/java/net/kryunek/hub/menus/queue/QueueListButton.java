package net.kryunek.hub.menus.queue;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.menus.queue.list.QueuePaginatedMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class QueueListButton extends Button {

    private final FileConfig adminMenus = ModuleService.getFileModule().getFile(ConfigFiles.ADMIN_MENUS);

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(adminMenus.getString("QUEUE.GLOBAL.BUTTONS.LIST.MATERIAL"))
                .name(CC.translate(adminMenus.getString("QUEUE.GLOBAL.BUTTONS.LIST.NAME")))
                .lore(adminMenus.getStringList("QUEUE.GLOBAL.BUTTONS.LIST.LORE"))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        new QueuePaginatedMenu().openMenu(player);
    }
}
