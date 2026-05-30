package net.kryunek.hub.menus.outfit;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.managers.player.ProfileManager;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class OutfitRemoveButton extends Button {

    private final ProfileManager profileManager;
    private final FileConfig outfitConfig;

    public OutfitRemoveButton() {
        this.profileManager = ModuleService.getManagerModule().getProfileManager();
        this.outfitConfig = ModuleService.getFileModule().getFile(ConfigFiles.OUTFIT);
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        List<String> lore = new ArrayList<>();
        this.outfitConfig.getStringList("BUTTONS.OUTFIT.REMOVE.ICON.LORE")
                .forEach(text -> lore.add(text.replace("%outfit_name%", getOutfitName(player))));

        return new ItemBuilder(this.outfitConfig.getString("BUTTONS.OUTFIT.REMOVE.ICON.MATERIAL"))
                .data(this.outfitConfig.getInt("BUTTONS.OUTFIT.REMOVE.ICON.DATA"))
                .name(this.outfitConfig.getString("BUTTONS.OUTFIT.REMOVE.ICON.NAME").replace("%outfit_name%", getOutfitName(player)))
                .lore(lore)
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType type, int i) {
        Profile profile = this.profileManager.getProfile(player.getUniqueId());
        if (profile.getOutfit() == null) {
            playFail(player);
            close(player);
            return;
        }

        profile.setOutfit(null);
        player.getInventory().setArmorContents(new ItemStack[4]);
        playSuccess(player);
        close(player);
    }

    private String getOutfitName(Player player) {
        Profile profile = this.profileManager.getProfile(player.getUniqueId());
        if (profile.getOutfit() != null) {
            return profile.getOutfit().getName();
        }
        return CC.translate("&cNone");
    }
}
