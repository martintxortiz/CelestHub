package net.kryunek.hub.managers.outfit;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Getter
@Setter
public class Outfit {

    @Setter(AccessLevel.NONE)
    private final String name;

    private int red;
    private int green;
    private int blue;
    private boolean enchanted;

    private final FileConfig outfitConfig;

    public Outfit(String name) {
        this.name = name;
        this.outfitConfig = ModuleService.getFileModule().getFile(ConfigFiles.OUTFIT);
    }

    public String getPermission() {
        return "celest.cosmetics.outfit." + this.name;
    }

    public Color getColor() {
        return Color.fromRGB(this.red, this.green, this.blue);
    }

    public void apply(Player player) {
        player.getInventory().setHelmet(new ItemBuilder(Material.LEATHER_HELMET).armorColor(getColor()).enchant(this.enchanted).build());
        player.getInventory().setChestplate(new ItemBuilder(Material.LEATHER_CHESTPLATE).armorColor(getColor()).enchant(this.enchanted).build());
        player.getInventory().setLeggings(new ItemBuilder(Material.LEATHER_LEGGINGS).armorColor(getColor()).enchant(this.enchanted).build());
        player.getInventory().setBoots(new ItemBuilder(Material.LEATHER_BOOTS).armorColor(getColor()).enchant(this.enchanted).build());
    }

    public ItemStack getIcon(Player player, Profile profile) {
        ItemBuilder builder = new ItemBuilder(Material.LEATHER_CHESTPLATE).armorColor(getColor()).enchant(this.enchanted);

        if (profile.getOutfit() != null && profile.getOutfit().equals(this)) {
            return builder
                    .name(outfitConfig.getString("BUTTONS.OUTFIT.EQUIPPED.ICON.NAME").replace("%outfit_name%", this.name))
                    .lore(outfitConfig.getStringList("BUTTONS.OUTFIT.EQUIPPED.ICON.LORE"))
                    .build();
        }

        if (player.hasPermission(getPermission()) || player.hasPermission("celest.cosmetics.outfit.*")) {
            return builder
                    .name(outfitConfig.getString("BUTTONS.OUTFIT.ALLOWED.ICON.NAME").replace("%outfit_name%", this.name))
                    .lore(outfitConfig.getStringList("BUTTONS.OUTFIT.ALLOWED.ICON.LORE"))
                    .build();
        }

        return new ItemBuilder(outfitConfig.getString("BUTTONS.OUTFIT.DENIED.ICON.MATERIAL"))
                .data(outfitConfig.getInt("BUTTONS.OUTFIT.DENIED.ICON.DATA"))
                .name(outfitConfig.getString("BUTTONS.OUTFIT.DENIED.ICON.NAME").replace("%outfit_name%", this.name))
                .lore(outfitConfig.getStringList("BUTTONS.OUTFIT.DENIED.ICON.LORE"))
                .build();
    }

    public static Outfit getOutfit(String outfit) {
        return ModuleService.getManagerModule().getOutfitManager().getOutfit(outfit);
    }
}
