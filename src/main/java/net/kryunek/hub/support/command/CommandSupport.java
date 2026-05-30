package net.kryunek.hub.support.command;

import net.kryunek.hub.support.message.MessageKey;
import net.kryunek.hub.support.message.Messages;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * Common command guard rails: permission and in-game checks that send the standard denial message
 * (via {@link Messages}) and return a boolean the caller branches on.
 */
public final class CommandSupport {

    private final Messages messages;

    public CommandSupport(Messages messages) {
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    public boolean requirePermission(CommandArgs command, String permission) {
        return requirePermission(command.getSender(), permission);
    }

    public boolean requirePermission(CommandSender sender, String permission) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(permission, "permission");

        if (sender.hasPermission(permission)) {
            return true;
        }

        messages.send(sender, MessageKey.COMMAND_NO_PERMISSION);
        return false;
    }

    public boolean requireAnyPermission(CommandArgs command, String... permissions) {
        return requireAnyPermission(command.getSender(), permissions);
    }

    public boolean requireAnyPermission(CommandSender sender, String... permissions) {
        Objects.requireNonNull(sender, "sender");
        if (permissions == null || permissions.length == 0) {
            throw new IllegalArgumentException("At least one permission is required.");
        }

        for (String permission : permissions) {
            if (permission != null && sender.hasPermission(permission)) {
                return true;
            }
        }

        messages.send(sender, MessageKey.COMMAND_NO_PERMISSION);
        return false;
    }

    public Player requirePlayer(CommandArgs command) {
        return requirePlayer(command.getSender());
    }

    public Player requirePlayer(CommandSender sender) {
        Objects.requireNonNull(sender, "sender");
        if (sender instanceof Player player) {
            return player;
        }

        messages.send(sender, MessageKey.COMMAND_IN_GAME_ONLY);
        return null;
    }
}
