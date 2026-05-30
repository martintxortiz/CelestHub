package net.kryunek.hub.menus.outfit.manage.list;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.outfit.OutfitCreateSession;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class OutfitCreateButton extends Button {

    private final FileConfig adminMenus = ModuleService.getFileModule().getFile(ConfigFiles.ADMIN_MENUS);
    private final FileConfig messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(adminMenus.getString("OUTFIT.CREATE_BUTTON.MATERIAL"))
                .name(CC.translate(adminMenus.getString("OUTFIT.CREATE_BUTTON.NAME")))
                .lore(adminMenus.getStringList("OUTFIT.CREATE_BUTTON.LORE"))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        player.closeInventory();
        OutfitCreateSession.stop(player);
        for (String line : messages.getStringList("OUTFIT.CREATE.PROMPT")) {
            player.sendMessage(CC.translate(line));
        }
        OutfitCreateSession.start(player, "__pending__", null);
    }
}
