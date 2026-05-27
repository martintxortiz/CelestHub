package net.kryunek.hub.commands.spawn;

import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.entity.Player;

public class SpawnCommand extends BaseCommand {

    @Command(name = "spawn")
    @Override
    public void onCommand(CommandArgs cmdArgs) {
        Player player = cmdArgs.getPlayer();
        managers().getSpawnManager().toSpawn(player, true);
        send(player, "SPAWN.TELEPORT", "&aTeleported to spawn.");
    }
}
