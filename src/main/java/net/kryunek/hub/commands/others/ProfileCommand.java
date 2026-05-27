package net.kryunek.hub.commands.others;

import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.managers.player.ProfileData;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.menus.profile.ProfileMenu;
import net.kryunek.hub.utils.command.BaseCommand;
import net.kryunek.hub.utils.command.Command;
import net.kryunek.hub.utils.command.CommandArgs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class ProfileCommand extends BaseCommand {

    @Command(name = "profile", aliases = {"stats"}, inGameOnly = true)
    @Override
    public void onCommand(CommandArgs command) {
        Player viewer = command.getPlayer();
        if (command.length() < 1) {
            new ProfileMenu().openMenu(viewer);
            return;
        }

        String query = command.getArgs(0);
        Player online = Bukkit.getPlayerExact(query);
        if (online != null) {
            Profile profile = managers().getProfileManager().getProfile(online.getUniqueId());
            if (profile == null) {
                viewer.sendMessage(CC.translate("&cCould not load profile for that player."));
                return;
            }

            ProfileData data = new ProfileData();
            data.setName(profile.getName());
            data.setPvpKills(profile.getPvpKills());
            data.setPvpDeaths(profile.getPvpDeaths());
            data.setPvpKillstreak(profile.getPvpKillstreak());
            data.setPvpMaxKillstreak(profile.getPvpMaxKillstreak());
            new ProfileMenu(online.getUniqueId(), data, online.getName()).openMenu(viewer);
            return;
        }

        Map<UUID, ProfileData> all = managers().getProfileManager().getStorage().loadAll();
        for (Map.Entry<UUID, ProfileData> entry : all.entrySet()) {
            ProfileData data = entry.getValue();
            if (data == null || data.getName() == null) {
                continue;
            }
            if (data.getName().equalsIgnoreCase(query)) {
                new ProfileMenu(entry.getKey(), data, data.getName()).openMenu(viewer);
                return;
            }
        }

        viewer.sendMessage(CC.translate("&cPlayer not found: &f" + query));
    }
}
