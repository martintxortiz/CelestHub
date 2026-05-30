package net.kryunek.hub.utils.menu.buttons;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class CloseButton extends Button {

    private final FileConfig commonMenu = ModuleService.getFileModule().getFile(ConfigFiles.COMMON_MENU);

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(commonMenu.getString("CLOSE.MATERIAL"))
                .data(commonMenu.getInt("CLOSE.DATA"))
                .lore(commonMenu.getStringList("CLOSE.LORE"))
                .name(commonMenu.getString("CLOSE.NAME"))
                .build();
    }

    @Override
    public void clicked(Player player, int i, ClickType clickType, int hb) {
        playNeutral(player);
        player.closeInventory();
    }
}
