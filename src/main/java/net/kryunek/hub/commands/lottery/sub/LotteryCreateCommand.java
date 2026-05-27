package net.kryunek.hub.commands.lottery.sub;

import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.entity.Player;

public class LotteryCreateCommand extends BaseCommand {

    @Command(name = "lottery.create", aliases = {"loteria.create", "cupon.create", "coupon.create"})
    @Override
    public void onCommand(CommandArgs cmdArgs) {
        Player player = cmdArgs.getPlayer();
        String[] args = cmdArgs.getArgs();
        if (args.length < 2) {
            player.sendMessage(CC.translate("&cUsage: /lottery create <name> <seconds>"));
            return;
        }

        int duration;
        try {
            duration = Integer.parseInt(args[1]);
        } catch (NumberFormatException exception) {
            send(player, "LOTTERY.INVALID_NUMBER", "&cInvalid number.");
            return;
        }

        if (duration <= 0) {
            send(player, "LOTTERY.INVALID_NUMBER", "&cInvalid number.");
            return;
        }

        boolean created = managers().getLotteryManager().createLottery(args[0], duration);
        if (!created) {
            send(player, "LOTTERY.ALREADY_EXISTS", "&cA lottery with that name already exists.");
            return;
        }

        send(player, "LOTTERY.CREATED", "&aLottery created: &f%lottery% &7(%seconds%s)",
                "%lottery%", args[0], "%seconds%", String.valueOf(duration));
    }
}
