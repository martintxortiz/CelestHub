package net.kryunek.hub.commands.queue.sub;


import net.kryunek.hub.managers.queue.Queue;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.command.CommandSender;

public class QueuePauseCommand extends BaseCommand {

    private FileConfig queueConfig;

    @Command(name = "queue.pause", permission = "celest.command.queue.pause", inGameOnly = false)
    @Override
    public void onCommand(CommandArgs cmdArgs) {
        CommandSender sender = cmdArgs.getSender();
        String[] args = cmdArgs.getArgs();
        if (args.length < 1) {
            sender.sendMessage(CC.translate("&cUsage: /queue pause <queue>"));
            return;
        }
        Queue queue = managers().getQueueManager().getQueue(args[0]);
        if (queue == null) {
            sender.sendMessage(CC.translate(queueConfig.getString("QUEUE_NOT_FOUND").replace("%queue%", args[0])));
            return;
        }
        if (queue.isPaused()) {
            sender.sendMessage(CC.translate(queueConfig.getString("QUEUE_UNPAUSED").replace("%queue%", queue.getServer())));
        }
        else {
            sender.sendMessage(CC.translate(queueConfig.getString("QUEUE_PAUSED").replace("%queue%", queue.getServer())));
        }
        queue.setPaused(!queue.isPaused());
    }
    public QueuePauseCommand() {
        this.queueConfig = file("queue");
    }
}
