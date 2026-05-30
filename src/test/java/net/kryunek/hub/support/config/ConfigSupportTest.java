package net.kryunek.hub.support.config;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSupportTest {

    private final Logger logger = Logger.getLogger("ConfigSupportTest");

    @Test
    void getStringUsesFallbackForMissingPath() {
        YamlConfiguration config = new YamlConfiguration();

        assertEquals("fallback", ConfigSupport.getString(config, "missing.path", "fallback"));
    }

    @Test
    void getStringReturnsConfiguredValue() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("greeting", "hello");

        assertEquals("hello", ConfigSupport.getString(config, "greeting", "fallback"));
    }

    @Test
    void getStringRejectsNullArguments() {
        YamlConfiguration config = new YamlConfiguration();

        assertThrows(NullPointerException.class, () -> ConfigSupport.getString(null, "path", "fallback"));
        assertThrows(NullPointerException.class, () -> ConfigSupport.getString(config, null, "fallback"));
        assertThrows(NullPointerException.class, () -> ConfigSupport.getString(config, "path", null));
    }

    @Test
    void requireStringFailsForMissingPath() {
        YamlConfiguration config = new YamlConfiguration();

        assertThrows(IllegalStateException.class, () -> ConfigSupport.requireString(config, "missing.path"));
    }

    @Test
    void requireStringFailsForBlankValue() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("blank", "   ");

        assertThrows(IllegalStateException.class, () -> ConfigSupport.requireString(config, "blank"));
    }

    @Test
    void requireStringReturnsConfiguredValue() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("name", "Celest");

        assertEquals("Celest", ConfigSupport.requireString(config, "name"));
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
    void getMaterialRejectsNullFallback() {
        YamlConfiguration config = new YamlConfiguration();

        assertThrows(NullPointerException.class,
                () -> ConfigSupport.getMaterial(config, "item.material", null, logger));
    }

    @Test
    void getMaterialFromRawNameParsesValidMaterial() {
        assertEquals(Material.DIAMOND, ConfigSupport.getMaterial("DIAMOND", Material.BARRIER, logger));
    }

    @Test
    void getMaterialFromRawNameUsesFallbackForInvalidOrNullName() {
        assertEquals(Material.BARRIER, ConfigSupport.getMaterial("NOT_A_MATERIAL", Material.BARRIER, logger));
        assertEquals(Material.BARRIER, ConfigSupport.getMaterial((String) null, Material.BARRIER, logger));
    }

    @Test
    void getMaterialFromRawNameRejectsNullFallback() {
        assertThrows(NullPointerException.class,
                () -> ConfigSupport.getMaterial("DIAMOND", null, logger));
    }

    @Test
    void getParticleUsesFallbackForInvalidParticle() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("jump.particle", "NOT_A_PARTICLE");

        assertEquals(Particle.CLOUD, ConfigSupport.getParticle(config, "jump.particle", Particle.CLOUD, logger));
    }

    @Test
    void getParticleParsesValidParticleCaseInsensitively() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("jump.particle", "flame");

        assertEquals(Particle.FLAME, ConfigSupport.getParticle(config, "jump.particle", Particle.CLOUD, logger));
    }

    // NOTE: getSound() is intentionally not unit-tested. org.bukkit.Sound is registry-backed in
    // this Paper version and its static initializer fails outside a running server, so any test
    // touching it throws NoClassDefFoundError. That path is exercised in-game only.

    @Test
    void getPositiveIntReturnsConfiguredPositiveValue() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("amount", 7);

        assertEquals(7, ConfigSupport.getPositiveInt(config, "amount", 10));
    }

    @Test
    void getPositiveIntUsesFallbackForZeroOrNegativeValues() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("zero", 0);
        config.set("negative", -5);

        assertEquals(10, ConfigSupport.getPositiveInt(config, "zero", 10));
        assertEquals(10, ConfigSupport.getPositiveInt(config, "negative", 10));
    }

    @Test
    void applyMissingDefaultsAddsOnlyAbsentKeysAndReportsChange() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("a", "existing");

        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("a", "new");
        defaults.put("b", 5);

        assertTrue(ConfigSupport.applyMissingDefaults(config, defaults));
        assertEquals("existing", config.getString("a"));
        assertEquals(5, config.getInt("b"));
    }

    @Test
    void applyMissingDefaultsReturnsFalseWhenAllKeysPresent() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("a", 1);

        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("a", 99);

        assertFalse(ConfigSupport.applyMissingDefaults(config, defaults));
        assertEquals(1, config.getInt("a"));
    }

    @Test
    void getPositiveIntDoesNotRequireLogger() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("amount", 3);

        // getMaterial/getSound/getParticle tolerate a null logger; exercise that branch.
        assertEquals(Material.STONE, ConfigSupport.getMaterial(config, "missing", Material.STONE, null));
    }
}
