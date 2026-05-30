package net.kryunek.hub.managers.scoreboard;

import net.kryunek.hub.support.config.ConfigFiles;

import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.TaskUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ScoreboardAnimation {

    public static String title, footer;

    public static void init() {
        List<String> titles = ModuleService.getFileModule().getFile(ConfigFiles.SCOREBOARD).getStringList("TITLE");
        AtomicInteger p = new AtomicInteger();
        TaskUtil.runTimerAsync(() -> {
            if (p.get() == titles.size()) p.set(0);
            title = titles.get(p.getAndIncrement());
        }, 0L, (long) (ModuleService.getFileModule().getFile(ConfigFiles.SCOREBOARD).getDouble("TITLE-TASK") * 20L));

        List<String> footers = ModuleService.getFileModule().getFile(ConfigFiles.SCOREBOARD).getStringList("FOOTER");
        AtomicInteger b = new AtomicInteger();
        TaskUtil.runTimerAsync(() -> {
            if (b.get() == footers.size()) b.set(0);
            footer = footers.get(b.getAndIncrement());
        }, 0L, (long) (ModuleService.getFileModule().getFile(ConfigFiles.SCOREBOARD).getDouble("FOOTER-TASK") * 20L));
    }


    public static String getScoreboardTitle() {
        return title;
    }

    public static String getScoreboardFooter() {
        return footer;
    }

}