package net.kryunek.hub.commands.chat.sub;

import net.kryunek.hub.managers.chat.ChatManager;
import net.kryunek.hub.support.command.Permissions;
import net.kryunek.hub.support.message.MessageKey;
import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.Bukkit;

public class ChatMuteCommand extends BaseCommand {

    @Command(name = "chat.mute", inGameOnly = false)
    @Override
    public void onCommand(CommandArgs command) {
        if (!commands.requireAnyPermission(command, Permissions.CHAT_MUTE, Permissions.CHAT_PAUSE)) {
            return;
        }

        ChatManager chatManager = managers().getChatManager();
        chatManager.setPaused(true);
        Bukkit.broadcastMessage(messages.get(MessageKey.CHAT_TOGGLED_MUTE, "%state%", "&cmuted"));
    }
}
