package net.kryunek.hub.commands.others;

import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.PlayerUtil;
import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.entity.Player;

public class FlyCommand extends BaseCommand {

    private final FileConfig settingsMenu = file("settings_menu");

    @Command(name = "fly", aliases = {"togglefly"}, permission = "celest.command.fly", inGameOnly = true)
    @Override
    public void onCommand(CommandArgs command) {
        Player player = command.getPlayer();
        Profile profile = managers().getProfileManager().getProfile(player.getUniqueId());
        profile.setFlyOnJoin(!profile.isFlyOnJoin());

        if (profile.isFlyOnJoin()) {
            PlayerUtil.applyHubFlyState(player, true, true);
            player.sendMessage(settingsMenu.getString("settings.fly-enabled"));
        } else {
            PlayerUtil.applyHubFlyState(player, false, false);
            player.sendMessage(settingsMenu.getString("settings.fly-disabled"));
        }
    }
}
