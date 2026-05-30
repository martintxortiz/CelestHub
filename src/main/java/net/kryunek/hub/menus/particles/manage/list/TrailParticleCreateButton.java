package net.kryunek.hub.menus.particles.manage.list;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.particles.TrailParticleCreateSession;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class TrailParticleCreateButton extends Button {

    private final FileConfig adminMenus = ModuleService.getFileModule().getFile(ConfigFiles.ADMIN_MENUS);
    private final FileConfig messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(adminMenus.getString("TRAIL.CREATE_BUTTON.MATERIAL"))
                .name(CC.translate(adminMenus.getString("TRAIL.CREATE_BUTTON.NAME")))
                .lore(adminMenus.getStringList("TRAIL.CREATE_BUTTON.LORE"))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        player.closeInventory();
        TrailParticleCreateSession.stop(player);
        TrailParticleCreateSession.start(player, "__pending__");
        for (String line : messages.getStringList("TRAIL.CREATE.PROMPT")) {
            player.sendMessage(CC.translate(line));
        }
    }
}
