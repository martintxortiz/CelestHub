package net.kryunek.hub.support.config;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;

public final class ConfigSupport {

    private ConfigSupport() {
    }

    public static String getString(ConfigurationSection section, String path, String fallback) {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(fallback, "fallback");
        String value = section.getString(path);
        return value == null ? fallback : value;
    }

    public static String requireString(ConfigurationSection section, String path) {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(path, "path");
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required config string: " + path);
        }
        return value;
    }

    public static Material getMaterial(ConfigurationSection section, String path, Material fallback, Logger logger) {
        Objects.requireNonNull(fallback, "fallback");
        String raw = getString(section, path, fallback.name());
        Material material = Material.matchMaterial(raw);
        if (material != null) {
            return material;
        }

        warn(logger, "Invalid material at '" + path + "': '" + raw + "'. Using " + fallback.name() + ".");
        return fallback;
    }

    public static Sound getSound(ConfigurationSection section, String path, Sound fallback, Logger logger) {
        Objects.requireNonNull(fallback, "fallback");
        String raw = getString(section, path, fallback.name());
        try {
            return Sound.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            warn(logger, "Invalid sound at '" + path + "': '" + raw + "'. Using " + fallback.name() + ".");
            return fallback;
        }
    }

    public static Particle getParticle(ConfigurationSection section, String path, Particle fallback, Logger logger) {
        Objects.requireNonNull(fallback, "fallback");
        String raw = getString(section, path, fallback.name());
        try {
            return Particle.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            warn(logger, "Invalid particle at '" + path + "': '" + raw + "'. Using " + fallback.name() + ".");
            return fallback;
        }
    }

    public static int getPositiveInt(ConfigurationSection section, String path, int fallback) {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(path, "path");
        int value = section.getInt(path, fallback);
        return value > 0 ? value : fallback;
    }

    private static void warn(Logger logger, String message) {
        if (logger != null) {
            logger.warning("[Celest] " + message);
        }
    }
}
