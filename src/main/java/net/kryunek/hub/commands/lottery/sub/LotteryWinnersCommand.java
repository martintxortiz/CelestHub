package net.kryunek.hub.commands.lottery.sub;

import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.entity.Player;

public class LotteryWinnersCommand extends BaseCommand {

    @Command(name = "lottery.winners", aliases = {"loteria.winners", "cupon.winners", "coupon.winners"})
    @Override
    public void onCommand(CommandArgs cmdArgs) {
        Player player = cmdArgs.getPlayer();
        String[] args = cmdArgs.getArgs();
        if (args.length < 2) {
            player.sendMessage(CC.translate("&cUsage: /lottery winners <name> <count>"));
            return;
        }

        int winners;
        try {
            winners = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            send(player, "LOTTERY.INVALID_NUMBER", "&cType a valid positive number.");
            return;
        }

        if (winners < 1) {
            send(player, "LOTTERY.INVALID_NUMBER", "&cType a valid positive number.");
            return;
        }

        boolean updated = managers().getLotteryManager().updateWinnersCount(args[0], winners);
        if (!updated) {
            send(player, "LOTTERY.NOT_FOUND", "&cLottery not found.");
            return;
        }

        player.sendMessage(CC.translate("&aWinners count for &f" + args[0] + "&a updated to &f" + winners + "&a."));
    }
}
