package net.kryunek.hub.commands.timer.sub;

import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class TimerCreateCommand extends BaseCommand {

    @Command(name = "timer.create", permission = "celest.command.timer.create", inGameOnly = true)
    @Override
    public void onCommand(CommandArgs cmdArgs) {

        Player player = cmdArgs.getPlayer();
        String[] args = cmdArgs.getArgs();

        if (args.length < 3) {
            player.sendMessage(CC.translate("&cUsage: /timer create <queue> <seconds> <prefix>"));
            return;
        }

        String queueName = args[0];
        long seconds;

        try {
            seconds = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate("&cInvalid duration."));
            return;
        }

        // 🔥 prefix con colores
        String prefix = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        managers()
                .getTimerManager()
                .createTimer(player, queueName, seconds, prefix);
    }
}
