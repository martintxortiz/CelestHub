package net.kryunek.hub.support.config;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigSupportTest {

    private final Logger logger = Logger.getLogger("ConfigSupportTest");

    @Test
    void getStringUsesFallbackForMissingPath() {
        YamlConfiguration config = new YamlConfiguration();

        assertEquals("fallback", ConfigSupport.getString(config, "missing.path", "fallback"));
    }

    @Test
    void requireStringFailsForMissingPath() {
        YamlConfiguration config = new YamlConfiguration();

        assertThrows(IllegalStateException.class, () -> ConfigSupport.requireString(config, "missing.path"));
    }

    @Test
    void getMaterialParsesValidMaterial() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("item.material", "DIAMOND");

        assertEquals(Material.DIAMOND, ConfigSupport.getMaterial(config, "item.material", Material.STONE, logger));
    }

    @Test
    void getMaterialUsesFallbackForInvalidMaterial() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("item.material", "NOT_A_MATERIAL");

        assertEquals(Material.STONE, ConfigSupport.getMaterial(config, "item.material", Material.STONE, logger));
    }

    @Test
    void getParticleUsesFallbackForInvalidParticle() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("jump.particle", "NOT_A_PARTICLE");

        assertEquals(Particle.CLOUD, ConfigSupport.getParticle(config, "jump.particle", Particle.CLOUD, logger));
    }

    @Test
    void getPositiveIntUsesFallbackForZeroOrNegativeValues() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("zero", 0);
        config.set("negative", -5);

        assertEquals(10, ConfigSupport.getPositiveInt(config, "zero", 10));
        assertEquals(10, ConfigSupport.getPositiveInt(config, "negative", 10));
    }
}
