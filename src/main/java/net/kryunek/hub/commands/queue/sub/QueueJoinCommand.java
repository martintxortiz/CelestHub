package net.kryunek.hub.commands.queue.sub;

import net.kryunek.hub.managers.queue.Queue;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.entity.Player;

public class QueueJoinCommand extends BaseCommand {

    private FileConfig queueConfig;
    @Command(name = "queue.join")
    @Override
    public void onCommand(CommandArgs cmdArgs) {
        Player player = cmdArgs.getPlayer();
        String[] args = cmdArgs.getArgs();
        if (args.length < 1) {
            player.sendMessage(CC.translate("&cUsage: /queue join <queue>"));
            return;
        }
        Queue queue = managers().getQueueManager().getQueue(args[0]);
        if (queue == null) {
            player.sendMessage(CC.translate(queueConfig.getString("QUEUE_NOT_FOUND").replace("%queue%", args[0])));
            return;
        }
        managers().getQueueManager().addToQueue(player, args[0]);
    }

    public QueueJoinCommand() {
        this.queueConfig = file("queue");
    }
}
