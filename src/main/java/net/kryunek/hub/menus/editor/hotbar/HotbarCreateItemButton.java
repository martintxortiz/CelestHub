package net.kryunek.hub.menus.editor.hotbar;

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

public class HotbarCreateItemButton extends Button {

    private final FileConfig hotbar = ModuleService.getFileModule().getFile(ConfigFiles.HOTBAR);

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(Material.ANVIL)
                .name(CC.translate("&aCreate Hotbar Item"))
                .lore(Arrays.asList(
                        CC.translate("&7Create a new hotbar entry."),
                        CC.translate("&7It will use first free slot (0-8)."),
                        "",
                        CC.translate("&eClick to create")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        String key = nextKey();
        int itemSlot = firstFreeSlot();

        hotbar.getConfiguration().set(key + ".ENABLED", true);
        hotbar.getConfiguration().set(key + ".GLOW", false);
        hotbar.getConfiguration().set(key + ".NAME", "&fNew Hotbar Item");
        hotbar.getConfiguration().set(key + ".LORE", Arrays.asList(
                "&7Edit this item from hotbar editor."
        ));
        hotbar.getConfiguration().set(key + ".MATERIAL", "PAPER");
        hotbar.getConfiguration().set(key + ".DATA", 0);
        hotbar.getConfiguration().set(key + ".SLOT", itemSlot);
        hotbar.getConfiguration().set(key + ".AMOUNT", 1);
        hotbar.getConfiguration().set(key + ".COMMAND", "");
        hotbar.save();

        ModuleService.getManagerModule().getHotbarManager().load();
        ModuleService.getManagerModule().getHotbarManager().reload();

        playSuccess(player);
        player.sendMessage(CC.translate("&aCreated hotbar item: &f" + key + "&a in slot &f" + itemSlot + "&a."));
        new HotbarItemEditorMenu(key).openMenu(player);
    }

    private String nextKey() {
        ConfigurationSection section = hotbar.getConfiguration();
        if (section == null) {
            return "CUSTOM_1";
        }

        int index = 1;
        String key = "CUSTOM_" + index;
        while (section.contains(key)) {
            index++;
            key = "CUSTOM_" + index;
        }
        return key;
    }

    private int firstFreeSlot() {
        ConfigurationSection section = hotbar.getConfiguration();
        if (section == null) {
            return 0;
        }

        for (int slot = 0; slot <= 8; slot++) {
            boolean occupied = false;
            for (String key : section.getKeys(false)) {
                if (section.contains(key + ".SLOT") && hotbar.getInt(key + ".SLOT") == slot) {
                    occupied = true;
                    break;
                }
            }
            if (!occupied) {
                return slot;
            }
        }
        return 0;
    }
}
