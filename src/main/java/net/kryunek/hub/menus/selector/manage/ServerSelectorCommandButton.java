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
public class ServerSelectorCommandButton extends Button {

    private final String key;
    private final FileConfig serverConfig = ModuleService.getFileModule().getFile(ConfigFiles.SERVER_SELECTOR);

    @Override
    public ItemStack getButtonItem(Player player) {
        String command = serverConfig.getString("SERVER_SELECTOR.ITEMS." + key + ".COMMAND", "", false);
        if (command == null || command.isBlank()) {
            command = "&7(none)";
        }

        return new ItemBuilder(Material.COMMAND_BLOCK)
                .name(CC.translate("&bClick Command"))
                .lore(Arrays.asList(
                        CC.translate("&7Current: &f" + command),
                        "",
                        CC.translate("&eClick to edit command"),
                        CC.translate("&7Use 'clear' to disable")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        ServerSelectorEditSession.start(player, ServerSelectorEditSession.Type.COMMAND, key);
        player.closeInventory();
        player.sendMessage(CC.translate("&eType command for &f" + key + "&e. Use 'clear' to disable. Type 'cancel' to abort."));
        playNeutral(player);
    }
}
