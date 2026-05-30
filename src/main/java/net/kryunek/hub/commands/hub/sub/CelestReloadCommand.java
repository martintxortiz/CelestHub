package net.kryunek.hub.commands.hub.sub;

import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.command.CommandSender;

public class CelestReloadCommand extends BaseCommand {

    @Command(name = "celest.reload", permission = "celest.command.reload", inGameOnly = false)
    @Override
    public void onCommand(CommandArgs command) {
        CommandSender sender = command.getSender();

        files().reload();
        managers().getQueueManager().loadQueues();
        managers().getQueueManager().startSendTask();
        managers().getTrailParticleManager().load();
        managers().getOutfitManager().load();
        managers().getJukeboxManager().load();
        managers().getHotbarManager().load();
        managers().getHotbarManager().reload();
        managers().getChatManager().load();
        sender.sendMessage(CC.translate("&7&m------------------------------------------------"));
        sender.sendMessage(CC.translate("&b* &f&lHub &c*"));
        sender.sendMessage(CC.translate(""));
        sender.sendMessage(CC.translate("&fPlugin reloaded."));
        sender.sendMessage(CC.translate("&7&m------------------------------------------------"));
    }
}
