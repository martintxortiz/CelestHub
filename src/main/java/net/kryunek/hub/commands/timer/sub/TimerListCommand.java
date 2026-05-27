package net.kryunek.hub.commands.timer.sub;


import net.kryunek.hub.managers.timer.Timer;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.entity.Player;

import java.util.List;

public class TimerListCommand extends BaseCommand {

    @Command(name = "timer.list", inGameOnly = true)
    @Override
    public void onCommand(CommandArgs cmdArgs) {

        Player player = cmdArgs.getPlayer();

        List<Timer> timers = managers()
                .getTimerManager()
                .getTimers();

        if (timers.isEmpty()) {
            player.sendMessage(CC.translate("&cNo active timers."));
            return;
        }

        for (Timer timer : timers) {

            player.sendMessage(CC.translate(
                    "&cTimer &4" + timer.getName() +
                            " &7(" + (timer.isPaused() ? "&ePaused" : "&aRunning") + "&7): &f" +
                            timer.getFormattedTime()
            ));
        }
    }
}
