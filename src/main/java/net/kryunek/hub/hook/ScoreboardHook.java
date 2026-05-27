package net.kryunek.hub.hook;


import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.scoreboard.Scoreboard;
import net.kryunek.hub.managers.scoreboard.ScoreboardStyle;
import net.kryunek.hub.managers.scoreboard.provider.ScoreboardProvider;

@UtilityClass
public class ScoreboardHook {

    @Getter
    private Scoreboard scoreboard;

    public void init(Celest hub) {
        if (ModuleService.getFileModule().getFile("scoreboard").getBoolean("ENABLED")) {
            scoreboard = new Scoreboard(hub, new ScoreboardProvider());
            scoreboard.setTicks(ModuleService.getFileModule().getFile("scoreboard").getInt("UPDATE-TASK"));
            scoreboard.setAssembleStyle(ScoreboardStyle.MODERN);

        }
    }

    public void shutdown() {
        if (scoreboard != null) {
            scoreboard.cleanup();
            scoreboard = null;
        }
    }
}
