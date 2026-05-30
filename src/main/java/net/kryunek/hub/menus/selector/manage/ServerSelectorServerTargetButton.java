package net.kryunek.hub.menus.selector.manage;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.AllArgsConstructor;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.queue.Queue;
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
public class ServerSelectorServerTargetButton extends Button {

    private final String selectorKey;
    private final String targetServer;
    private final boolean returnToItemEditor;
    private final FileConfig serverConfig = ModuleService.getFileModule().getFile(ConfigFiles.SERVER_SELECTOR);

    @Override
    public ItemStack getButtonItem(Player player) {
        Queue queue = ModuleService.getManagerModule().getQueueManager().getQueue(targetServer);
        boolean exists = queue != null;
        return new ItemBuilder(exists ? Material.LIME_WOOL : Material.YELLOW_WOOL)
                .name(CC.translate("&b" + targetServer))
                .lore(Arrays.asList(
                        CC.translate("&7Queue exists: " + (exists ? "&aYes" : "&cNo")),
                        "",
                        CC.translate("&eClick to set as target")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        serverConfig.getConfiguration().set("SERVER_SELECTOR.ITEMS." + selectorKey + ".SERVER", targetServer);
        serverConfig.save();
        playSuccess(player);
        player.sendMessage(CC.translate("&aUpdated &f" + selectorKey + "&a target to &f" + targetServer));
        if (returnToItemEditor) {
            new ServerSelectorItemEditorMenu(selectorKey).openMenu(player);
            return;
        }
        new ServerSelectorEditorMenu().openMenu(player);
    }
}
