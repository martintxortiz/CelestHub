package net.kryunek.hub.menus.gadgets.manage;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.AllArgsConstructor;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class GadgetEditorButton extends Button {

    private final String key;
    private final FileConfig gadgetsMenu = ModuleService.getFileModule().getFile(ConfigFiles.GADGETS);
    private final FileConfig settings = ModuleService.getFileModule().getFile(ConfigFiles.GADGETS);

    @Override
    public ItemStack getButtonItem(Player player) {
        String basePath = "GADGETS_MENU.ITEMS." + key + ".";
        Material material = Material.matchMaterial(gadgetsMenu.getString(basePath + "MATERIAL", "BLAZE_ROD", false));
        if (material == null) {
            material = Material.BLAZE_ROD;
        }

        String type = gadgetsMenu.getString(basePath + "TYPE", "UNKNOWN", false);
        boolean enabled = !gadgetsMenu.getConfiguration().contains(basePath + "ENABLED")
                || gadgetsMenu.getBoolean(basePath + "ENABLED");
        int slot = gadgetsMenu.getInt(basePath + "SLOT");
        int cooldown = resolveCooldown(type);

        List<String> lore = new ArrayList<>();
        lore.add(CC.translate("&7Type: &f" + type));
        lore.add(CC.translate("&7Enabled: " + (enabled ? "&aYes" : "&cNo")));
        lore.add(CC.translate("&7Slot: &f" + slot));
        lore.add(CC.translate("&7Cooldown: &f" + cooldown + "s"));
        lore.add("");
        lore.add(CC.translate("&eClick to edit"));

        return new ItemBuilder(material)
                .name(gadgetsMenu.getString(basePath + "NAME", "&b" + key, true))
                .lore(lore)
                .data(gadgetsMenu.getInt(basePath + "DATA"))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        playSuccess(player);
        new GadgetItemEditorMenu(key).openMenu(player);
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
