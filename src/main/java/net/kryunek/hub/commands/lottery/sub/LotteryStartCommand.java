package net.kryunek.hub.commands.lottery.sub;

import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.entity.Player;

public class LotteryStartCommand extends BaseCommand {

    @Command(name = "lottery.start", aliases = {"loteria.start", "cupon.start", "coupon.start"})
    @Override
    public void onCommand(CommandArgs cmdArgs) {
        Player player = cmdArgs.getPlayer();
        String[] args = cmdArgs.getArgs();
        if (args.length < 1) {
            player.sendMessage(CC.translate("&cUsage: /lottery start <name>"));
            return;
        }

        boolean started = managers().getLotteryManager().startLottery(args[0], player);
        if (!started) {
            send(player, "LOTTERY.START_FAILED", "&cCould not start that lottery.");
            return;
        }

        send(player, "LOTTERY.STARTED", "&aLottery started: &f%lottery%", "%lottery%", args[0]);
    }
}
