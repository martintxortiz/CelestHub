package net.kryunek.hub.menus.gadgets.manage;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.AllArgsConstructor;
import net.kryunek.hub.managers.gadgets.GadgetEditSession;
import net.kryunek.hub.managers.module.ModuleService;
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
public class GadgetEditCooldownButton extends Button {

    private final String key;
    private final FileConfig gadgetsMenu = ModuleService.getFileModule().getFile(ConfigFiles.GADGETS);
    private final FileConfig settings = ModuleService.getFileModule().getFile(ConfigFiles.GADGETS);

    @Override
    public ItemStack getButtonItem(Player player) {
        String type = gadgetsMenu.getString("GADGETS_MENU.ITEMS." + key + ".TYPE", "", false);
        int cooldown = resolveCooldown(type);
        return new ItemBuilder(Material.CLOCK)
                .name(CC.translate("&bCooldown"))
                .lore(Arrays.asList(
                        CC.translate("&7Current: &f" + cooldown + "s"),
                        "",
                        CC.translate("&eClick to edit in chat")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        GadgetEditSession.start(player, GadgetEditSession.Type.COOLDOWN, key);
        player.closeInventory();
        player.sendMessage(CC.translate("&eType cooldown in seconds for &f" + key + "&e. Type 'cancel' to abort."));
        playNeutral(player);
    }

    private int resolveCooldown(String type) {
        if (type == null || type.isBlank()) {
            return Math.max(0, settings.getInt("GADGETS.COOLDOWN_SECONDS"));
        }
        String path = "GADGETS." + type.toUpperCase() + ".COOLDOWN_SECONDS";
        if (settings.getConfiguration().contains(path)) {
            return Math.max(0, settings.getInt(path));
        }
        return Math.max(0, settings.getInt("GADGETS.COOLDOWN_SECONDS"));
    }
}
