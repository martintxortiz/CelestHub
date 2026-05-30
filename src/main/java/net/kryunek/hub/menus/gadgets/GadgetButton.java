package net.kryunek.hub.menus.gadgets;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.player.Profile;
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

public class GadgetButton extends Button {

    private final String key;
    private final FileConfig gadgetsMenu = ModuleService.getFileModule().getFile(ConfigFiles.GADGETS);

    public GadgetButton(String key) {
        this.key = key;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        String path = "GADGETS_MENU.ITEMS." + key + ".";
        String type = gadgetsMenu.getString(path + "TYPE", "", false);
        if (!GadgetService.isEnabled(type)) {
            return new ItemBuilder(Material.GRAY_DYE)
                    .name(CC.translate("&cDisabled Gadget"))
                    .lore(List.of(CC.translate("&7This gadget is disabled by admin.")))
                    .build();
        }
        if (!GadgetService.hasPermission(player, type)) {
            return GadgetService.buildNoPermissionItem(gadgetsMenu.getString(path + "NAME", key, true));
        }

        Profile profile = ModuleService.getManagerModule().getProfileManager().getProfile(player.getUniqueId());
        boolean selected = profile != null && GadgetService.isSameType(type, profile.getSelectedGadgetType());

        List<String> lore = new ArrayList<>(gadgetsMenu.getStringList(path + "LORE"));
        lore.add("");
        lore.add(CC.translate(selected
                ? gadgetsMenu.getString("GADGETS_MENU.SELECTED_LORE", "&aSelected", true)
                : gadgetsMenu.getString("GADGETS_MENU.CLICK_TO_SELECT_LORE", "&eClick to select", true)));

        return new ItemBuilder(gadgetsMenu.getString(path + "MATERIAL"))
                .name(gadgetsMenu.getString(path + "NAME"))
                .lore(lore)
                .data(gadgetsMenu.getInt(path + "DATA"))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        String type = gadgetsMenu.getString("GADGETS_MENU.ITEMS." + key + ".TYPE", "", false);
        Profile profile = ModuleService.getManagerModule().getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            playFail(player);
            return;
        }

        if (type.isEmpty()) {
            playFail(player);
            player.sendMessage(CC.translate("&cUnknown gadget type."));
            return;
        }
        if (!GadgetService.isEnabled(type)) {
            playFail(player);
            player.sendMessage(CC.translate("&cThis gadget is currently disabled."));
            return;
        }
        if (!GadgetService.hasPermission(player, type)) {
            playFail(player);
            player.sendMessage(CC.translate(gadgetsMenu.getString("messages.no-permission", "&cYou do not have permission for this gadget.", true)));
            close(player);
            return;
        }

        GadgetService.deactivatePersistentEffects(player);
        profile.setSelectedGadgetType(type.toUpperCase());
        ModuleService.getManagerModule().getProfileManager().saveProfile(profile, true);
        ModuleService.getManagerModule().getHotbarManager().setHotbar(player);
        playSuccess(player);
        player.sendMessage(CC.translate(gadgetsMenu.getString("messages.selected", "&aSelected gadget: &f%gadget%", true)
                .replace("%gadget%", gadgetsMenu.getString("GADGETS_MENU.ITEMS." + key + ".NAME"))));
        close(player);
    }
}
