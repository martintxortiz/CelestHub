package net.kryunek.hub.commands.chat.sub;

import net.kryunek.hub.managers.chat.ChatManager;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.support.message.MessageKey;
import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.Bukkit;

public class ChatSlowCommand extends BaseCommand {

    @Command(name = "chat.slow", permission = "celest.command.chat.slow", inGameOnly = false)
    @Override
    public void onCommand(CommandArgs command) {
        String[] args = command.getArgs();
        ChatManager chatManager = managers().getChatManager();

        if (args.length < 1) {
            command.getSender().sendMessage(CC.translate("&cUsage: /chat slow <seconds|off>"));
            return;
        }

        if (args[0].equalsIgnoreCase("off")) {
            chatManager.setSlowSeconds(0);
            chatManager.clearChatCooldowns();
            Bukkit.broadcastMessage(messages.get(MessageKey.CHAT_DISABLED_SLOW));
            return;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            command.getSender().sendMessage(CC.translate("&cUsage: /chat slow <seconds|off>"));
            return;
        }

        if (seconds <= 0) {
            chatManager.setSlowSeconds(0);
            chatManager.clearChatCooldowns();
            Bukkit.broadcastMessage(messages.get(MessageKey.CHAT_DISABLED_SLOW));
            return;
        }

        chatManager.setSlowSeconds(seconds);
        chatManager.clearChatCooldowns();
        Bukkit.broadcastMessage(messages.get(MessageKey.CHAT_SET_SLOW, "%time%", String.valueOf(seconds)));
    }
}
