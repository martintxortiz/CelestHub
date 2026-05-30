package net.kryunek.hub.menus.lottery.list;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.lottery.LotteryCreateSession;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class LotteryCreateButton extends Button {

    private final FileConfig adminMenus = ModuleService.getFileModule().getFile(ConfigFiles.ADMIN_MENUS);
    private final FileConfig messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(adminMenus.getString("LOTTERY.CREATE_BUTTON.MATERIAL"))
                .name(CC.translate(adminMenus.getString("LOTTERY.CREATE_BUTTON.NAME")))
                .lore(adminMenus.getStringList("LOTTERY.CREATE_BUTTON.LORE"))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        player.closeInventory();
        LotteryCreateSession.start(player);
        for (String line : messages.getStringList("LOTTERY.CREATE.PROMPT")) {
            player.sendMessage(CC.translate(line));
        }
    }
}
