package net.kryunek.hub.commands.chat.sub;

import net.kryunek.hub.managers.chat.ChatManager;
import net.kryunek.hub.support.command.Permissions;
import net.kryunek.hub.support.message.MessageKey;
import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.Bukkit;

public class ChatUnmuteCommand extends BaseCommand {

    @Command(name = "chat.unmute", inGameOnly = false)
    @Override
    public void onCommand(CommandArgs command) {
        if (!commands.requireAnyPermission(command, Permissions.CHAT_UNMUTE, Permissions.CHAT_PAUSE, Permissions.CHAT_MUTE)) {
            return;
        }

        ChatManager chatManager = managers().getChatManager();
        chatManager.setPaused(false);
        Bukkit.broadcastMessage(messages.get(MessageKey.CHAT_TOGGLED_MUTE, "%state%", "&aunmuted"));
    }
}
