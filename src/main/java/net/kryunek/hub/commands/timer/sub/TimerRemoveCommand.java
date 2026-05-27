package net.kryunek.hub.commands.timer.sub;

import net.kryunek.hub.managers.timer.TimerManager;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.entity.Player;

public class TimerRemoveCommand extends BaseCommand {

    @Command(name = "timer.remove", permission = "celest.command.timer.remove", inGameOnly = true)
    @Override
    public void onCommand(CommandArgs cmdArgs) {

        Player player = cmdArgs.getPlayer();
        String[] args = cmdArgs.getArgs();

        if (args.length != 1) {
            player.sendMessage(CC.translate("&cUsage: /timer remove <queue>"));
            return;
        }

        String queueName = args[0];

        TimerManager timerManager = managers().getTimerManager();
        if (!timerManager.deleteTimerInternal(queueName)) {
            player.sendMessage(CC.translate("&cNo active timer for &4" + queueName));
            return;
        }

        player.sendMessage(CC.translate("&aTimer for &f" + queueName + " &aremoved."));
    }
}
