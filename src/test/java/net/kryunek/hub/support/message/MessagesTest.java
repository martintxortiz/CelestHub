package net.kryunek.hub.support.message;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessagesTest {

    private final Logger logger = Logger.getLogger("MessagesTest");

    @Test
    void getsConfiguredMessageWithColorAndPlaceholders() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("CHAT.MESSAGES.SET_SLOW", "&eSlow: &f%time%s");

        Messages messages = Messages.from(config, logger);

        assertEquals(ChatColor.YELLOW + "Slow: " + ChatColor.WHITE + "5s",
                messages.get(MessageKey.CHAT_SET_SLOW, "%time%", "5"));
    }

    @Test
    void usesFallbackWhenMessageKeyIsMissing() {
        Messages messages = Messages.from(new YamlConfiguration(), logger);

        assertEquals(ChatColor.RED + "No permission.", messages.get(MessageKey.COMMAND_NO_PERMISSION));
    }

    @Test
    void rejectsOddPlaceholderArguments() {
        Messages messages = Messages.from(new YamlConfiguration(), logger);

        assertThrows(IllegalArgumentException.class,
                () -> messages.get(MessageKey.COMMAND_NO_PERMISSION, "%missing%"));
    }

    @Test
    void treatsNullPlaceholderValuesAsEmptyText() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("CHAT.MESSAGES.CLEARED", "Cleared by %player%");
        Messages messages = Messages.from(config, logger);

        assertEquals("Cleared by ", messages.get(MessageKey.CHAT_CLEARED, "%player%", null));
    }
}
