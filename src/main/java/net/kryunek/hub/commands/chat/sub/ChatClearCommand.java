package net.kryunek.hub.commands.chat.sub;

import net.kryunek.hub.managers.chat.ChatManager;
import net.kryunek.hub.support.message.MessageKey;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ChatClearCommand extends BaseCommand {

    private final FileConfig settings = file("settings");

    @Command(name = "chat.clear", permission = "celest.command.chat.clear", inGameOnly = false)
    @Override
    public void onCommand(CommandArgs command) {
        ChatManager chatManager = managers().getChatManager();
        int lines = Math.max(1, settings.getInt("CHAT.CLEAR_LINES"));

        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < lines; i++) {
                player.sendMessage("");
            }
            messages.send(player, MessageKey.CHAT_CLEARED, "%player%", command.getSender().getName());
        }

        chatManager.clearChatCooldowns();
    }
}
