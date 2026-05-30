package net.kryunek.hub.managers.particles;

import com.google.common.collect.Maps;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.utils.FileConfig;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;

/** CRUD and validation of cosmetic particle trails stored in the injected particle config. */
public class TrailParticleManager {
    private final Map<String, TrailParticle> trails;
    private final FileConfig particleConfig;

    public Map<String, TrailParticle> getTrails() {
        return this.trails;
    }

    public void load() {
        this.trails.clear();
        ConfigurationSection section = particleConfig.getConfiguration().getConfigurationSection("TRAILS");
        if (section == null) {
            return;
        }
        for (String s : section.getKeys(false)) {
            String effect = section.getString(s + ".EFFECT", "").toUpperCase();
            String material = section.getString(s + ".MATERIAL", "").toUpperCase();

            if (!isValidParticle(effect)) {
                Bukkit.getLogger().warning("[Celest] Invalid particle effect in TRAILS." + s + ".EFFECT: " + effect + ". Skipping entry.");
                continue;
            }
            if (!isValidMaterial(material)) {
                Bukkit.getLogger().warning("[Celest] Invalid trail material in TRAILS." + s + ".MATERIAL: " + material + ". Using BARRIER.");
                material = Material.BARRIER.name();
            }

            TrailParticle trail = new TrailParticle(s);
            trail.setMaterial(material);
            trail.setData(section.getInt(s + ".DATA"));
            trail.setEffect(effect);
            this.trails.put(s, trail);
        }

    }

    public TrailParticle getTrail(String name) {
        return this.trails.values().stream()
                .filter(trail -> trail.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public boolean createTrail(String name, String effect, String material, int data) {
        if (getTrail(name) != null) {
            return false;
        }

        Particle.valueOf(effect.toUpperCase());
        Material.valueOf(material.toUpperCase());

        TrailParticle trail = new TrailParticle(name);
        trail.setEffect(effect.toUpperCase());
        trail.setMaterial(material.toUpperCase());
        trail.setData(data);

        this.trails.put(name, trail);
        this.particleConfig.getConfiguration().set("TRAILS." + name + ".EFFECT", trail.getEffect());
        this.particleConfig.getConfiguration().set("TRAILS." + name + ".MATERIAL", trail.getMaterial());
        this.particleConfig.getConfiguration().set("TRAILS." + name + ".DATA", trail.getData());
        this.particleConfig.save();
        return true;
    }

    public boolean deleteTrail(String name) {
        TrailParticle trail = getTrail(name);
        if (trail == null) {
            return false;
        }

        this.trails.remove(trail.getName());
        this.particleConfig.getConfiguration().set("TRAILS." + trail.getName(), null);
        this.particleConfig.save();

        for (Profile profile : ModuleService.getManagerModule().getProfileManager().getProfiles().values()) {
            if (profile.getTrail() != null && profile.getTrail().getName().equalsIgnoreCase(trail.getName())) {
                profile.setTrail(null);
            }
        }

        return true;
    }

    public boolean updateTrailEffect(String name, String effect) {
        TrailParticle trail = getTrail(name);
        if (trail == null) {
            return false;
        }

        Particle.valueOf(effect.toUpperCase());
        trail.setEffect(effect.toUpperCase());
        this.particleConfig.getConfiguration().set("TRAILS." + trail.getName() + ".EFFECT", trail.getEffect());
        this.particleConfig.save();
        return true;
    }

    public boolean updateTrailIcon(String name, String material, int data) {
        TrailParticle trail = getTrail(name);
        if (trail == null) {
            return false;
        }

        Material.valueOf(material.toUpperCase());
        trail.setMaterial(material.toUpperCase());
        trail.setData(data);
        this.particleConfig.getConfiguration().set("TRAILS." + trail.getName() + ".MATERIAL", trail.getMaterial());
        this.particleConfig.getConfiguration().set("TRAILS." + trail.getName() + ".DATA", trail.getData());
        this.particleConfig.save();
        return true;
    }

    public TrailParticleManager(FileConfig particleConfig) {
        this.trails = Maps.newHashMap();
        this.particleConfig = particleConfig;
    }

    private boolean isValidParticle(String effect) {
        try {
            Particle.valueOf(effect);
            return true;
        } catch (IllegalArgumentException | NullPointerException ex) {
            return false;
        }
    }

    private boolean isValidMaterial(String material) {
        try {
            Material.valueOf(material);
            return true;
        } catch (IllegalArgumentException | NullPointerException ex) {
            return false;
        }
    }
}
