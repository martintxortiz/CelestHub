package net.kryunek.hub.commands.others;

import net.kryunek.hub.managers.jukebox.JukeboxManager;
import net.kryunek.hub.menus.jukebox.JukeboxMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.entity.Player;

public class JukeboxCommand extends BaseCommand {

    @Command(name = "jukebox", aliases = {"music", "jb"}, inGameOnly = true)
    @Override
    public void onCommand(CommandArgs cmdArgs) {
        Player player = cmdArgs.getPlayer();
        JukeboxManager manager = managers().getJukeboxManager();
        if (manager == null || !manager.isEnabled()) {
            player.sendMessage(CC.translate("&cJukebox is disabled."));
            return;
        }

        String[] args = cmdArgs.getArgs();
        if (args.length == 0) {
            new JukeboxMenu().openMenu(player);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "toggle" -> {
                boolean enabled = manager.toggle(player);
                player.sendMessage(CC.translate(enabled ? "&aJukebox enabled." : "&cJukebox disabled."));
            }
            case "pause" -> {
                if (!manager.isPauseEnabled()) {
                    player.sendMessage(CC.translate("&cPause is disabled."));
                    return;
                }
                if (!manager.pause(player)) {
                    player.sendMessage(CC.translate("&cNo track is playing."));
                    return;
                }
                player.sendMessage(CC.translate("&eTrack paused."));
            }
            case "resume" -> {
                if (!manager.isPauseEnabled()) {
                    player.sendMessage(CC.translate("&cPause is disabled."));
                    return;
                }
                if (!manager.resume(player)) {
                    player.sendMessage(CC.translate("&cNo paused track."));
                    return;
                }
                player.sendMessage(CC.translate("&aTrack resumed."));
            }
            case "next" -> {
                if (!manager.next(player)) {
                    player.sendMessage(CC.translate("&cNo tracks configured."));
                    return;
                }
                player.sendMessage(CC.translate("&aPlaying next track."));
            }
            case "volume", "vol" -> {
                if (args.length < 2) {
                    player.sendMessage(CC.translate("&7Current volume: &f" + String.format(java.util.Locale.US, "%.2f", manager.getVolume(player))));
                    player.sendMessage(CC.translate("&7Usage: &f/jukebox volume <0.2-2.0>"));
                    return;
                }
                double value;
                try {
                    value = Double.parseDouble(args[1]);
                } catch (NumberFormatException ex) {
                    player.sendMessage(CC.translate("&cInvalid volume."));
                    return;
                }
                manager.setVolume(player, value);
                player.sendMessage(CC.translate("&aVolume set to &f" + String.format(java.util.Locale.US, "%.2f", manager.getVolume(player))));
            }
            case "stop" -> {
                manager.stop(player);
                player.sendMessage(CC.translate("&eTrack stopped."));
            }
            case "play" -> {
                if (args.length < 2) {
                    player.sendMessage(CC.translate("&cUsage: /jukebox play <track>"));
                    return;
                }
                if (!manager.play(player, args[1])) {
                    player.sendMessage(CC.translate("&cTrack not found."));
                    return;
                }
                player.sendMessage(CC.translate("&aPlaying &f" + args[1] + "&a."));
            }
            default -> player.sendMessage(CC.translate("&cUnknown subcommand. Use /jukebox"));
        }
    }
}
