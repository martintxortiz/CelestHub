package net.kryunek.hub.commands.lottery.sub;

import net.kryunek.hub.managers.lottery.Lottery;
import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.stream.Collectors;

public class LotteryListCommand extends BaseCommand {

    @Command(name = "lottery.list", aliases = {"loteria.list", "cupon.list", "coupon.list"})
    @Override
    public void onCommand(CommandArgs cmdArgs) {
        Player player = cmdArgs.getPlayer();
        Collection<Lottery> lotteries = managers().getLotteryManager().getLotteries();

        if (lotteries.isEmpty()) {
            send(player, "LOTTERY.LIST_EMPTY", "&cNo lotteries found.");
            return;
        }

        String list = lotteries.stream()
                .map(lottery -> lottery.getName() + (lottery.isActive() ? " &a(active)" : " &7(inactive)"))
                .collect(Collectors.joining("&7, &f"));

        send(player, "LOTTERY.LIST", "&dLotteries&7: &f%list%", "%list%", list);
    }
}
