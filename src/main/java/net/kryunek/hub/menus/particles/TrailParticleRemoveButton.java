package net.kryunek.hub.menus.particles;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.particles.TrailParticle;
import net.kryunek.hub.managers.particles.TrailParticleManager;
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

public class TrailParticleRemoveButton extends Button {

    private final ProfileManager profileManager;
    private final TrailParticleManager trailManager;
    private FileConfig particleConfig;
    @Override
    public ItemStack getButtonItem(Player player) {
        List<String> trail_lore = new ArrayList<>();
        particleConfig.getStringList("BUTTONS.TRAIL.REMOVE.ICON.LORE").forEach(text -> trail_lore.add(text.replace("%trail_name%", getTrail(player))));
        return new ItemBuilder(particleConfig.getString("BUTTONS.TRAIL.REMOVE.ICON.MATERIAL")).data(particleConfig.getInt("BUTTONS.TRAIL.REMOVE.ICON.DATA")).name(particleConfig.getString("BUTTONS.TRAIL.REMOVE.ICON.NAME").replace("%trail_name%", getTrail(player))).lore(trail_lore).build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType type, int i) {
        Profile profile = this.profileManager.getProfile(player.getUniqueId());
        if (profile.getTrail() == null) {
            playFail(player);
            close(player);
            return;
        }
        profile.setTrail(null);
        Button.playSuccess(player);
        close(player);
    }


    public String getTrail(Player player) {
        Profile profile = this.profileManager.getProfile(player.getUniqueId());
        if (profile.getTrail() != null) {
            return profile.getTrail().getName();
        }
        return CC.translate("&cNone");
    }

    public TrailParticleRemoveButton() {
        this.profileManager = ModuleService.getManagerModule().getProfileManager();
        this.trailManager = ModuleService.getManagerModule().getTrailParticleManager();
        this.particleConfig = ModuleService.getFileModule().getFile(ConfigFiles.PARTICLE);

    }
}