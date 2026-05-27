package net.kryunek.hub.utils;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileConfigTest {

    @TempDir
    private Path tempDir;

    @Test
    void getStringReturnsFallbackWhenPathIsMissing() {
        FileConfig config = newConfig("missing.yml");

        assertEquals("", config.getString("missing.path"));
        assertEquals("fallback", config.getString("missing.path", "fallback", true));
    }

    @Test
    void getStringColorizesStringValues() {
        FileConfig config = newConfig("color.yml");
        config.getConfiguration().set("message", "&aHello");

        assertEquals(ChatColor.GREEN + "Hello", config.getString("message"));
    }

    private FileConfig newConfig(String name) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getResource(name)).thenReturn(null);
        return new FileConfig(plugin, name);
    }
}
