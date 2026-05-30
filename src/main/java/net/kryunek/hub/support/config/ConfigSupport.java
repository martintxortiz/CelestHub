package net.kryunek.hub.support.config;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Null-safe readers for Bukkit configuration values. Every accessor returns a supplied fallback
 * (logging a warning where appropriate) instead of throwing, so a mistyped material, sound or
 * particle in a config file degrades gracefully rather than crashing the feature that reads it.
 * Also seeds absent config defaults via {@link #applyMissingDefaults}.
 *
 * <p>Stateless and thread-safe; all members are static.
 */
public final class ConfigSupport {

    private ConfigSupport() {
    }

    /** Returns the string at {@code path}, or {@code fallback} when it is absent. */
    public static String getString(ConfigurationSection section, String path, String fallback) {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(fallback, "fallback");
        String value = section.getString(path);
        return value == null ? fallback : value;
    }

    /** Returns the string at {@code path}, or throws {@link IllegalStateException} if it is missing or blank. */
    public static String requireString(ConfigurationSection section, String path) {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(path, "path");
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required config string: " + path);
        }
        return value;
    }

    /** Resolves the material named at {@code path}, falling back (with a warning) when missing or unknown. */
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

    /**
     * Resolves a raw material name without a backing config path. Returns {@code fallback}
     * (logging a warning) instead of throwing when the name is null or unrecognised, so a
     * mistyped material can never crash item construction.
     */
    public static Material getMaterial(String raw, Material fallback, Logger logger) {
        Objects.requireNonNull(fallback, "fallback");
        if (raw != null) {
            Material material = Material.matchMaterial(raw);
            if (material != null) {
                return material;
            }
        }

        warn(logger, "Invalid material '" + raw + "'. Using " + fallback.name() + ".");
        return fallback;
    }

    /** Resolves the sound named at {@code path}, falling back (with a warning) when missing or unknown. */
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

    /** Resolves the particle named at {@code path}, falling back (with a warning) when missing or unknown. */
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

    /** Returns the int at {@code path} when greater than zero, otherwise {@code fallback}. */
    public static int getPositiveInt(ConfigurationSection section, String path, int fallback) {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(path, "path");
        int value = section.getInt(path, fallback);
        return value > 0 ? value : fallback;
    }

    /**
     * Writes each default whose key is absent from {@code section}, leaving existing values
     * untouched. Returns {@code true} if at least one key was added, letting the caller decide
     * whether the owning file needs saving. This replaces the repeated
     * {@code if (!contains(k)) { set(k, v); }} blocks managers used to seed their config.
     */
    public static boolean applyMissingDefaults(ConfigurationSection section, Map<String, Object> defaults) {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(defaults, "defaults");
        boolean changed = false;
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            if (!section.contains(entry.getKey())) {
                section.set(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        return changed;
    }

    private static void warn(Logger logger, String message) {
        if (logger != null) {
            logger.warning("[Celest] " + message);
        }
    }
}
