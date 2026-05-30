package net.kryunek.hub.utils.menu.buttons;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.AllArgsConstructor;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import net.kryunek.hub.utils.menu.pagination.PaginatedMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

@AllArgsConstructor
public class PageInfoButton extends Button {

    private final PaginatedMenu paginatedMenu;
    private final FileConfig commonMenu = ModuleService.getFileModule().getFile(ConfigFiles.COMMON_MENU);

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(commonMenu.getString("PAGE_INFO.MATERIAL"))
                .name(commonMenu.getString("PAGE_INFO.NAME"))
                .lore(commonMenu.getString("PAGE_INFO.LORE")
                        .replace("%current%", String.valueOf(paginatedMenu.getPage()))
                        .replace("%total%", String.valueOf(paginatedMenu.getPages(player))))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        Button.playSuccess(player);
        close(player);
    }
}
