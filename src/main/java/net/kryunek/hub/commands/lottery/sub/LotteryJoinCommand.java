package net.kryunek.hub.commands.lottery.sub;

import net.kryunek.hub.managers.lottery.LotteryManager;
import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.entity.Player;

public class LotteryJoinCommand extends BaseCommand {

    @Command(name = "lottery.join", aliases = {"loteria.join", "cupon.join", "coupon.join"})
    @Override
    public void onCommand(CommandArgs cmdArgs) {
        Player player = cmdArgs.getPlayer();
        String[] args = cmdArgs.getArgs();
        if (args.length < 1) {
            send(player, "LOTTERY.USAGE_JOIN", "&cUsage: /lottery join <name>");
            return;
        }

        LotteryManager manager = managers().getLotteryManager();
        LotteryManager.JoinResult result = manager.joinLottery(player, args[0]);
        switch (result) {
            case JOINED -> send(player, "LOTTERY.JOINED", "&aYou joined lottery &f%lottery%&a.", "%lottery%", args[0]);
            case ALREADY_JOINED -> send(player, "LOTTERY.ALREADY_JOINED", "&eYou are already in this lottery.");
            case NOT_ACTIVE -> send(player, "LOTTERY.NOT_ACTIVE", "&cThis lottery is not active.");
            case NOT_FOUND -> send(player, "LOTTERY.NOT_FOUND", "&cLottery not found.");
        }
    }
}
