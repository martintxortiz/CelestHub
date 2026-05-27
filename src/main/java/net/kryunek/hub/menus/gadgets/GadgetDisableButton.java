package net.kryunek.hub.menus.gadgets;

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

public class GadgetDisableButton extends Button {

    private final FileConfig gadgetsMenu = ModuleService.getFileModule().getFile("gadgets");

    @Override
    public ItemStack getButtonItem(Player player) {
        Profile profile = ModuleService.getManagerModule().getProfileManager().getProfile(player.getUniqueId());
        String activeType = profile == null ? "NONE" : profile.getSelectedGadgetType();
        String activeName = GadgetService.getDisplayNameByType(activeType);

        String path = "GADGETS_MENU.DISABLE.";
        String materialName = gadgetsMenu.getString(path + "MATERIAL", "BARRIER", false);
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.BARRIER;
        }

        List<String> lore = new ArrayList<>();
        for (String line : gadgetsMenu.getStringList(path + "LORE")) {
            lore.add(line.replace("%active_gadget%", activeName));
        }

        return new ItemBuilder(material)
                .name(gadgetsMenu.getString(path + "NAME", "&cDisable Gadget", true))
                .lore(lore)
                .data(gadgetsMenu.getInt(path + "DATA"))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        Profile profile = ModuleService.getManagerModule().getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            playFail(player);
            return;
        }

        String activeType = profile.getSelectedGadgetType();
        if (activeType == null || activeType.equalsIgnoreCase("NONE")) {
            playFail(player);
            close(player);
            return;
        }

        GadgetService.deactivatePersistentEffects(player);
        profile.setSelectedGadgetType("NONE");
        ModuleService.getManagerModule().getProfileManager().saveProfile(profile, true);
        ModuleService.getManagerModule().getHotbarManager().setHotbar(player);
        player.sendMessage(CC.translate(gadgetsMenu.getString("messages.disabled", "&cGadget disabled.", true)));
        playSuccess(player);
        close(player);
    }
}
