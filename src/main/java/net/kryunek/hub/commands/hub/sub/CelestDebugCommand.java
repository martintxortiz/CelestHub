package net.kryunek.hub.commands.hub.sub;

import net.kryunek.hub.managers.editor.EditorInputSession;
import net.kryunek.hub.managers.outfit.OutfitCreateSession;
import net.kryunek.hub.managers.particles.TrailParticleCreateSession;
import net.kryunek.hub.managers.queue.QueueCreateSession;
import net.kryunek.hub.managers.queue.QueueEditSession;
import net.kryunek.hub.managers.selector.ServerSelectorEditSession;
import net.kryunek.hub.menus.timer.TimerCreateSession;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.text.DecimalFormat;

public class CelestDebugCommand extends BaseCommand {

    private static final DecimalFormat DECIMAL = new DecimalFormat("0.00");

    @Command(name = "celest.debug", permission = "celest.command.debug", inGameOnly = false)
    @Override
    public void onCommand(CommandArgs command) {
        CommandSender sender = command.getSender();

        long usedMemoryMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024L * 1024L);
        long totalMemoryMb = Runtime.getRuntime().totalMemory() / (1024L * 1024L);
        long maxMemoryMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L);

        double[] tps = Bukkit.getServer().getTPS();
        String tpsLine = DECIMAL.format(Math.min(20.0D, tps[0])) + " / "
                + DECIMAL.format(Math.min(20.0D, tps[1])) + " / "
                + DECIMAL.format(Math.min(20.0D, tps[2]));

        sender.sendMessage(CC.translate("&7&m------------------------------------------------"));
        sender.sendMessage(CC.translate("&b&lCelest Debug"));
        sender.sendMessage(CC.translate(""));
        sender.sendMessage(CC.translate("&bPlayers: &f" + Bukkit.getOnlinePlayers().size()));
        sender.sendMessage(CC.translate("&bTPS (1m/5m/15m): &f" + tpsLine));
        sender.sendMessage(CC.translate("&bMemory (used/total/max): &f" + usedMemoryMb + "MB / " + totalMemoryMb + "MB / " + maxMemoryMb + "MB"));
        sender.sendMessage(CC.translate("&bHotbars loaded: &f" + managers().getHotbarManager().getHotbars().size()));
        sender.sendMessage(CC.translate("&bQueues loaded: &f" + managers().getQueueManager().getQueues().size()));
        sender.sendMessage(CC.translate("&bTimers active: &f" + managers().getTimerManager().getTimers().size()));
        sender.sendMessage(CC.translate("&bOutfits loaded: &f" + managers().getOutfitManager().getOutfits().size()));
        sender.sendMessage(CC.translate("&bTrails loaded: &f" + managers().getTrailParticleManager().getTrails().size()));
        sender.sendMessage(CC.translate("&bChat muted: &f" + managers().getChatManager().isPaused()));
        sender.sendMessage(CC.translate("&bChat slow: &f" + managers().getChatManager().getSlowSeconds() + "s"));
        sender.sendMessage(CC.translate(""));
        sender.sendMessage(CC.translate("&bSessions active:"));
        sender.sendMessage(CC.translate("&7- Editor input: &f" + EditorInputSession.activeCount()));
        sender.sendMessage(CC.translate("&7- Selector edit: &f" + ServerSelectorEditSession.activeCount()));
        sender.sendMessage(CC.translate("&7- Queue create: &f" + QueueCreateSession.activeCount()));
        sender.sendMessage(CC.translate("&7- Queue edit: &f" + QueueEditSession.activeCount()));
        sender.sendMessage(CC.translate("&7- Outfit create: &f" + OutfitCreateSession.activeCount()));
        sender.sendMessage(CC.translate("&7- Trail create: &f" + TrailParticleCreateSession.activeCount()));
        sender.sendMessage(CC.translate("&7- Timer create: &f" + TimerCreateSession.activeCount()));
        sender.sendMessage(CC.translate("&7&m------------------------------------------------"));
    }
}
