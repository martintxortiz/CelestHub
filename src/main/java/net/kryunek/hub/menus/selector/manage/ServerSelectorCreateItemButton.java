package net.kryunek.hub.menus.selector.manage;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class ServerSelectorCreateItemButton extends Button {

    private final FileConfig serverConfig = ModuleService.getFileModule().getFile(ConfigFiles.SERVER_SELECTOR);

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(Material.ANVIL)
                .name(CC.translate("&aCreate Selector Item"))
                .lore(Arrays.asList(
                        CC.translate("&7Creates a new selector entry"),
                        CC.translate("&7in the first free slot."),
                        "",
                        CC.translate("&eClick to create")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        int size = serverConfig.getInt("SERVER_SELECTOR.SIZE");
        if (size < 9 || size > 54 || size % 9 != 0) {
            size = 27;
        }
        int freeSlot = findFirstFreeSlot(size);
        if (freeSlot < 0) {
            playFail(player);
            player.sendMessage(CC.translate("&cNo free slots left in selector editor."));
            return;
        }

        String key = ServerSelectorEditorMenu.createAtSlot(player, freeSlot);
        playSuccess(player);
        player.sendMessage(CC.translate("&aCreated selector item: &f" + key + " &7(slot " + freeSlot + ")"));
        new ServerSelectorItemEditorMenu(key).openMenu(player);
    }

    private int findFirstFreeSlot(int size) {
        ConfigurationSection items = serverConfig.getConfiguration().getConfigurationSection("SERVER_SELECTOR.ITEMS");
        for (int slot = 0; slot < size; slot++) {
            boolean occupied = false;
            if (items != null) {
                for (String key : items.getKeys(false)) {
                    if (serverConfig.getInt("SERVER_SELECTOR.ITEMS." + key + ".SLOT") == slot) {
                        occupied = true;
                        break;
                    }
                }
            }

            if (!occupied) {
                return slot;
            }
        }
        return -1;
    }
}
