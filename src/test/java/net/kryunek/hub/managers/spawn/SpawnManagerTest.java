package net.kryunek.hub.managers.spawn;

import net.kryunek.hub.utils.FileConfig;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpawnManagerTest {

    @TempDir
    private Path tempDir;

    private FileConfig newSettings() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getResource("settings.yml")).thenReturn(null);
        return new FileConfig(plugin, "settings.yml");
    }

    @Test
    void constructorWithoutSavedLocationLeavesLocationNull() {
        SpawnManager manager = new SpawnManager(newSettings());
        assertNull(manager.getLocation());
    }

    @Test
    void constructorIgnoresMalformedLocationString() {
        FileConfig settings = newSettings();
        // Fewer than 6 comma-separated parts is rejected before any world lookup.
        settings.getConfiguration().set("SPAWN_LOCATION", "only, three, parts");

        SpawnManager manager = new SpawnManager(settings);
        assertNull(manager.getLocation());
    }
}
