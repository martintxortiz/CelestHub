package net.kryunek.hub.support.message;

import net.kryunek.hub.utils.FileConfig;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Resolves and sends player-facing messages from a config-backed {@link MessageSource}, applying
 * {@code &} colour codes and {@code %key%}/value placeholder substitution. A missing key falls back
 * to the {@link MessageKey} default and is logged once, so a typo never sends a blank line.
 */
public final class Messages {

    /** Supplies raw message strings by config path; typically {@code ConfigurationSection::getString}. */
    @FunctionalInterface
    public interface MessageSource {
        String getString(String path);
    }

    private final MessageSource source;
    private final Logger logger;
    private final Set<String> reportedMissingKeys = ConcurrentHashMap.newKeySet();

    public Messages(MessageSource source, Logger logger) {
        this.source = Objects.requireNonNull(source, "source");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public static Messages from(FileConfig config, Logger logger) {
        Objects.requireNonNull(config, "config");
        return from(config.getConfiguration(), logger);
    }

    public static Messages from(ConfigurationSection section, Logger logger) {
        Objects.requireNonNull(section, "section");
        return new Messages(section::getString, logger);
    }

    public String get(MessageKey key, String... placeholders) {
        Objects.requireNonNull(key, "key");
        return get(key.path(), key.fallback(), placeholders);
    }

    public String get(String path, String fallback, String... placeholders) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(fallback, "fallback");

        String value = source.getString(path);
        if (value == null) {
            reportMissing(path);
            value = fallback;
        }

        return colorize(applyPlaceholders(value, placeholders));
    }

    public void send(CommandSender sender, MessageKey key, String... placeholders) {
        Objects.requireNonNull(sender, "sender");
        sender.sendMessage(get(key, placeholders));
    }

    public void send(CommandSender sender, String path, String fallback, String... placeholders) {
        Objects.requireNonNull(sender, "sender");
        sender.sendMessage(get(path, fallback, placeholders));
    }

    private String applyPlaceholders(String value, String... placeholders) {
        if (placeholders == null || placeholders.length == 0) {
            return value;
        }
        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders must be key/value pairs.");
        }

        String result = value;
        for (int i = 0; i < placeholders.length; i += 2) {
            String key = Objects.requireNonNull(placeholders[i], "placeholder key");
            String replacement = placeholders[i + 1] == null ? "" : placeholders[i + 1];
            result = result.replace(key, replacement);
        }
        return result;
    }

    private String colorize(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private void reportMissing(String path) {
        if (reportedMissingKeys.add(path)) {
            logger.warning("[Celest] Missing message key '" + path + "'. Using fallback text.");
        }
    }
}
