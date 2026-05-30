package net.kryunek.hub.managers.outfit;

import com.google.common.collect.Maps;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.utils.FileConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/** CRUD and application of leather-armour cosmetic outfits stored in the injected outfit config. */
public class OutfitManager {

    private final Map<String, Outfit> outfits;
    private final FileConfig outfitConfig;

    public OutfitManager(FileConfig outfitConfig) {
        this.outfits = Maps.newHashMap();
        this.outfitConfig = outfitConfig;
    }

    public Map<String, Outfit> getOutfits() {
        return this.outfits;
    }

    public void load() {
        this.outfits.clear();
        ConfigurationSection section = this.outfitConfig.getConfiguration().getConfigurationSection("OUTFITS");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            Outfit outfit = new Outfit(key);
            outfit.setRed(section.getInt(key + ".RED"));
            outfit.setGreen(section.getInt(key + ".GREEN"));
            outfit.setBlue(section.getInt(key + ".BLUE"));
            outfit.setEnchanted(section.getBoolean(key + ".ENCHANTED"));
            this.outfits.put(key, outfit);
        }
    }

    public Outfit getOutfit(String name) {
        return this.outfits.values().stream()
                .filter(outfit -> outfit.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public boolean createOutfit(String name, int red, int green, int blue, boolean enchanted) {
        if (getOutfit(name) != null) {
            return false;
        }

        Outfit outfit = new Outfit(name);
        outfit.setRed(red);
        outfit.setGreen(green);
        outfit.setBlue(blue);
        outfit.setEnchanted(enchanted);
        this.outfits.put(name, outfit);

        this.outfitConfig.getConfiguration().set("OUTFITS." + name + ".RED", red);
        this.outfitConfig.getConfiguration().set("OUTFITS." + name + ".GREEN", green);
        this.outfitConfig.getConfiguration().set("OUTFITS." + name + ".BLUE", blue);
        this.outfitConfig.getConfiguration().set("OUTFITS." + name + ".ENCHANTED", enchanted);
        this.outfitConfig.save();
        return true;
    }

    public boolean deleteOutfit(String name) {
        Outfit outfit = getOutfit(name);
        if (outfit == null) {
            return false;
        }

        this.outfits.remove(outfit.getName());
        this.outfitConfig.getConfiguration().set("OUTFITS." + outfit.getName(), null);
        this.outfitConfig.save();

        for (Profile profile : ModuleService.getManagerModule().getProfileManager().getProfiles().values()) {
            if (profile.getOutfit() != null && profile.getOutfit().getName().equalsIgnoreCase(outfit.getName())) {
                profile.setOutfit(null);
                Player player = profile.getPlayer();
                if (player != null && !profile.isBuildModeEnabled()) {
                    player.getInventory().setArmorContents(new ItemStack[4]);
                }
            }
        }

        return true;
    }

    public boolean updateOutfit(String name, int red, int green, int blue, boolean enchanted) {
        Outfit outfit = getOutfit(name);
        if (outfit == null) {
            return false;
        }

        outfit.setRed(red);
        outfit.setGreen(green);
        outfit.setBlue(blue);
        outfit.setEnchanted(enchanted);

        this.outfitConfig.getConfiguration().set("OUTFITS." + outfit.getName() + ".RED", red);
        this.outfitConfig.getConfiguration().set("OUTFITS." + outfit.getName() + ".GREEN", green);
        this.outfitConfig.getConfiguration().set("OUTFITS." + outfit.getName() + ".BLUE", blue);
        this.outfitConfig.getConfiguration().set("OUTFITS." + outfit.getName() + ".ENCHANTED", enchanted);
        this.outfitConfig.save();

        for (Profile profile : ModuleService.getManagerModule().getProfileManager().getProfiles().values()) {
            if (profile.getOutfit() != null && profile.getOutfit().getName().equalsIgnoreCase(outfit.getName())) {
                Player player = profile.getPlayer();
                if (player != null && !profile.isBuildModeEnabled()) {
                    outfit.apply(player);
                }
            }
        }

        return true;
    }

    public void applySelectedOutfit(Player player, Profile profile) {
        if (player == null || profile == null || profile.isBuildModeEnabled()) {
            return;
        }

        if (profile.getOutfit() == null) {
            player.getInventory().setArmorContents(new ItemStack[4]);
            return;
        }

        profile.getOutfit().apply(player);
    }
}
