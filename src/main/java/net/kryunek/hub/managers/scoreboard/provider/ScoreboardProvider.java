package net.kryunek.hub.managers.scoreboard.provider;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.managers.player.ProfileManager;
import net.kryunek.hub.managers.queue.QueueManager;
import net.kryunek.hub.managers.scoreboard.ScoreboardAdapter;
import net.kryunek.hub.managers.scoreboard.ScoreboardAnimation;
import net.kryunek.hub.managers.timer.Timer;
import net.kryunek.hub.managers.timer.TimerManager;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.Time;
import net.kryunek.hub.utils.bungee.BungeeUtils;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ScoreboardProvider implements ScoreboardAdapter {

    private final ProfileManager profileManager;
    private final QueueManager queueManager;
    private final TimerManager timerManager;
    private final FileConfig scoreboardConfig;

    @Override
    public String getTitle(Player player) {
        return CC.translate(ScoreboardAnimation.getScoreboardTitle());

    }

    @Override
    public List<String> getLines(Player player) {

        List<String> lines = new ArrayList<>();
        Profile profile = profileManager.getProfile(player.getUniqueId());
        boolean inArena = ModuleService.getManagerModule().getPvpArenaKitManager().isInArenaSession(player.getUniqueId());

        if (inArena) {
            for (String arenaLine : scoreboardConfig.getStringList("PVP_ARENA")) {
                if (arenaLine.contains("%FOOTER%")) {
                    lines.add(ScoreboardAnimation.getScoreboardFooter());
                    continue;
                }
                lines.add(arenaLine
                        .replace("%player%", player.getDisplayName())
                        .replace("%pvp_kills%", String.valueOf(profile == null ? 0 : profile.getPvpKills()))
                        .replace("%pvp_deaths%", String.valueOf(profile == null ? 0 : profile.getPvpDeaths()))
                        .replace("%pvp_killstreak%", String.valueOf(profile == null ? 0 : profile.getPvpKillstreak()))
                        .replace("%pvp_max_killstreak%", String.valueOf(profile == null ? 0 : profile.getPvpMaxKillstreak()))
                        .replace("%combat_remaining%", String.valueOf(ModuleService.getManagerModule().getPvpArenaKitManager().getCombatRemainingSeconds(player.getUniqueId()))));
            }
            return CC.translate(lines);
        }

        for (String mainscoreboard : scoreboardConfig.getStringList("MAIN")) {

            if (mainscoreboard.contains("%QUEUE%")) {

                if (queueManager.isInQueue(player)) {
                    for (String queue : scoreboardConfig.getStringList("QUEUE")) {
                        lines.add(queue
                                .replace("%queue%", queueManager.getQueue(player).getServer())
                                .replace("%queue_pos%", String.valueOf(queueManager.getQueue(player).getPosition(player)))
                                .replace("%queue_size%", String.valueOf(queueManager.getQueue(player).getSize()))
                        );
                    }
                }

            } else if (mainscoreboard.contains("%TIMER%")) {

                List<Timer> timers = timerManager.getTimers();

                if (!timers.isEmpty()) {
                    for (Timer timer : timers) {

                        lines.add(mainscoreboard.replace(
                                "%TIMER%",
                                CC.translate(timer.getPrefix()) +
                                        CC.translate("&7 » &f") +
                                        timer.getFormattedTime()
                        ));
                    }
                }

            } else if (mainscoreboard.contains("%FOOTER%")) {

                lines.add(ScoreboardAnimation.getScoreboardFooter());
            } else {

                lines.add(mainscoreboard
                        .replace("%player%", player.getDisplayName())
                        .replace("%hour%", Time.getHour())
                        .replace("%date%", Time.getDate())
                        .replace("%global%", String.valueOf(BungeeUtils.getGlobalPlayers()))
                        .replace("%pvp_kills%", String.valueOf(profile == null ? 0 : profile.getPvpKills()))
                        .replace("%pvp_deaths%", String.valueOf(profile == null ? 0 : profile.getPvpDeaths()))
                        .replace("%pvp_killstreak%", String.valueOf(profile == null ? 0 : profile.getPvpKillstreak()))
                        .replace("%pvp_max_killstreak%", String.valueOf(profile == null ? 0 : profile.getPvpMaxKillstreak()))
                );
            }
        }

        if (profile != null && profile.isBuildModeEnabled()) {
            int insertAt = Math.min(2, lines.size());
            lines.add(insertAt, "&bBuildMode &7» &aEnabled");
        }

        return CC.translate(lines);
    }
    public ScoreboardProvider(){
        this.profileManager = ModuleService.getManagerModule().getProfileManager();
        this.queueManager = ModuleService.getManagerModule().getQueueManager();
        this.timerManager = ModuleService.getManagerModule().getTimerManager();
        this.scoreboardConfig = ModuleService.getFileModule().getFile(ConfigFiles.SCOREBOARD);
    }
}
