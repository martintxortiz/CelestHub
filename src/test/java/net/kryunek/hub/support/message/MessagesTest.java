package net.kryunek.hub.support.message;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

    @Test
    void getViaRawPathUsesFallbackWhenMissing() {
        Messages messages = Messages.from(new YamlConfiguration(), logger);

        assertEquals(ChatColor.GREEN + "Hi Steve",
                messages.get("custom.path", "&aHi %name%", "%name%", "Steve"));
    }

    @Test
    void sendDeliversResolvedMessageToSender() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("CHAT.MESSAGES.SET_SLOW", "&eSlow: &f%time%s");
        Messages messages = Messages.from(config, logger);
        CommandSender sender = mock(CommandSender.class);

        messages.send(sender, MessageKey.CHAT_SET_SLOW, "%time%", "3");

        verify(sender).sendMessage(ChatColor.YELLOW + "Slow: " + ChatColor.WHITE + "3s");
    }

    @Test
    void sendViaRawPathDeliversResolvedMessage() {
        Messages messages = Messages.from(new YamlConfiguration(), logger);
        CommandSender sender = mock(CommandSender.class);

        messages.send(sender, "custom.path", "&aReady");

        verify(sender).sendMessage(ChatColor.GREEN + "Ready");
    }

    @Test
    void constructorRejectsNullSourceAndLogger() {
        assertThrows(NullPointerException.class, () -> new Messages(null, logger));
        assertThrows(NullPointerException.class, () -> new Messages(path -> null, null));
    }

    @Test
    void missingKeyIsLoggedOnlyOnce() {
        Logger isolated = Logger.getLogger("MessagesTest.dedup." + System.nanoTime());
        isolated.setUseParentHandlers(false);
        AtomicInteger warnings = new AtomicInteger();
        isolated.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel() == Level.WARNING) {
                    warnings.incrementAndGet();
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });

        Messages messages = Messages.from(new YamlConfiguration(), isolated);
        messages.get(MessageKey.PROFILE_NOT_LOADED);
        messages.get(MessageKey.PROFILE_NOT_LOADED);

        assertEquals(1, warnings.get());
    }
}
