package net.kryunek.hub.support.command;

import net.kryunek.hub.support.message.Messages;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommandSupportTest {

    private final Messages messages = Messages.from(new YamlConfiguration(), Logger.getLogger("CommandSupportTest"));
    private final CommandSupport commands = new CommandSupport(messages);

    @Test
    void requirePermissionAllowsSenderWithPermission() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission(Permissions.CHAT_MUTE)).thenReturn(true);

        assertTrue(commands.requirePermission(sender, Permissions.CHAT_MUTE));
        verify(sender, never()).sendMessage(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void requirePermissionRejectsSenderWithoutPermission() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission(Permissions.CHAT_MUTE)).thenReturn(false);

        assertFalse(commands.requirePermission(sender, Permissions.CHAT_MUTE));
        verify(sender).sendMessage(ChatColor.RED + "No permission.");
    }

    @Test
    void requireAnyPermissionAllowsAnyMatchingPermission() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission(Permissions.CHAT_MUTE)).thenReturn(false);
        when(sender.hasPermission(Permissions.CHAT_PAUSE)).thenReturn(true);

        assertTrue(commands.requireAnyPermission(sender, Permissions.CHAT_MUTE, Permissions.CHAT_PAUSE));
        verify(sender, never()).sendMessage(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void requireAnyPermissionRejectsEmptyPermissionList() {
        CommandSender sender = mock(CommandSender.class);

        assertThrows(IllegalArgumentException.class, () -> commands.requireAnyPermission(sender));
    }

    @Test
    void requirePlayerReturnsPlayerSenders() {
        Player player = mock(Player.class);

        assertSame(player, commands.requirePlayer(player));
    }

    @Test
    void requirePlayerRejectsConsoleSenders() {
        CommandSender sender = mock(CommandSender.class);

        assertNull(commands.requirePlayer(sender));
        verify(sender).sendMessage(ChatColor.RED + "This command can only be executed in game.");
    }
}
