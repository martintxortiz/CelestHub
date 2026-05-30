package net.kryunek.hub.managers.chat;

import net.kryunek.hub.utils.FileConfig;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatManagerTest {

    @TempDir
    private Path tempDir;

    private FileConfig newSettings() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getResource("settings.yml")).thenReturn(null);
        return new FileConfig(plugin, "settings.yml");
    }

    @Test
    void constructorSeedsChatDefaults() {
        FileConfig settings = newSettings();
        new ChatManager(settings);

        assertEquals("&8[&bHub&8] &r", settings.getConfiguration().getString("CHAT.PREFIX"));
        assertFalse(settings.getConfiguration().getBoolean("CHAT.PAUSED"));
        assertEquals(0, settings.getConfiguration().getInt("CHAT.SLOW_SECONDS"));
        assertEquals(120, settings.getConfiguration().getInt("CHAT.CLEAR_LINES"));
    }

    @Test
    void constructorDoesNotOverrideExistingValues() {
        FileConfig settings = newSettings();
        settings.getConfiguration().set("CHAT.PAUSED", true);
        settings.getConfiguration().set("CHAT.SLOW_SECONDS", 9);

        ChatManager manager = new ChatManager(settings);

        assertTrue(manager.isPaused());
        assertEquals(9, manager.getSlowSeconds());
    }

    @Test
    void slowModeCountdownReportsRemainingSeconds() {
        FileConfig settings = newSettings();
        settings.getConfiguration().set("CHAT.SLOW_SECONDS", 5);
        ChatManager manager = new ChatManager(settings);
        UUID player = UUID.randomUUID();

        // No message sent yet, so no cooldown.
        assertEquals(0L, manager.getRemainingSlowSeconds(player));

        manager.registerMessage(player);
        long remaining = manager.getRemainingSlowSeconds(player);
        assertTrue(remaining >= 1 && remaining <= 5, "expected 1..5, got " + remaining);
    }

    @Test
    void slowModeDisabledMeansNoCooldown() {
        FileConfig settings = newSettings();
        ChatManager manager = new ChatManager(settings); // SLOW_SECONDS defaults to 0
        UUID player = UUID.randomUUID();

        manager.registerMessage(player);
        assertEquals(0L, manager.getRemainingSlowSeconds(player));
    }
}
