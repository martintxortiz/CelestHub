package net.kryunek.hub.managers.particles;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Getter
@Setter
public class TrailParticle {
    @Setter(value = AccessLevel.NONE)
    private final String name;

    private String material;
    private int data;
    private String effect;

    private FileConfig particleConfig;

    public String getPermission() {
        return "celest.cosmetics.trail." + this.name;
    }

    public TrailParticle(String name) {
        this.name = name;
        this.particleConfig = ModuleService.getFileModule().getFile(ConfigFiles.PARTICLE);
    }

    public void playEffect(Player player) {
        Particle particle;
        try {
            particle = Particle.valueOf(this.effect);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return;
        }
        for (Player viewer : player.getWorld().getPlayers()) {
            if (viewer.equals(player) || viewer.canSee(player)) {
                viewer.spawnParticle(
                        particle,
                        player.getLocation(),
                        12,
                        0.2,
                        0.5,
                        0.2,
                        0.2
                );
            }
        }
    }

    public ItemStack getIcon(Player player, Profile profile) {
        if (profile.getTrail() != null && profile.getTrail().equals(this)) {
            return new ItemBuilder(this.material).data(this.data).name(particleConfig.getString("BUTTONS.TRAIL.EQUIPPED.ICON.NAME").replace("%trail_name%", this.name)).lore(particleConfig.getStringList("BUTTONS.TRAIL.EQUIPPED.ICON.LORE")).build();
        }
        if (player.hasPermission(this.getPermission()) || player.hasPermission("celest.cosmetics.trail.*")) {
            return new ItemBuilder(this.material).data(this.data).name(particleConfig.getString("BUTTONS.TRAIL.ALLOWED.ICON.NAME").replace("%trail_name%", this.name)).lore(particleConfig.getStringList("BUTTONS.TRAIL.ALLOWED.ICON.LORE")).build();
        }
        return new ItemBuilder(particleConfig.getString("BUTTONS.TRAIL.DENIED.ICON.MATERIAL")).data(particleConfig.getInt("BUTTONS.TRAIL.DENIED.ICON.DATA")).name(particleConfig.getString("BUTTONS.TRAIL.DENIED.ICON.NAME").replace("%trail_name%", this.name)).lore(particleConfig.getStringList("BUTTONS.TRAIL.DENIED.ICON.LORE")).build();
    }

    public static TrailParticle getTrail(String trail) {
        return ModuleService.getManagerModule().getTrailParticleManager().getTrail(trail);
    }
}
