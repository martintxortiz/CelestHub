package net.kryunek.hub.commands.queue.sub;

import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Set;

public class QueueListCommand extends BaseCommand {

    private final FileConfig queueConfig;

    @Command(name = "queue.list")
    @Override
    public void onCommand(CommandArgs cmdArgs) {
        Player player = cmdArgs.getPlayer();
        ConfigurationSection section = queueConfig.getConfiguration().getConfigurationSection("QUEUE.SERVERS");

        if (section == null) {
            send(player, "QUEUE.LIST_EMPTY", "&cNo queues are configured.");
            return;
        }

        Set<String> queues = section.getKeys(false);
        send(player, "QUEUE.LIST_AVAILABLE", "&aAvailable queues: &f%queues%", "%queues%", String.join(", ", queues));
    }

    public QueueListCommand() {
        this.queueConfig = file("queue");
    }
}
