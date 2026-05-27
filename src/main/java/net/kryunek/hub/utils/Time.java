package net.kryunek.hub.utils;


import net.kryunek.hub.managers.module.ModuleService;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Time {
    public static DecimalFormat getDecimalFormat() {
        return new DecimalFormat("0.0");
    }

    public static class IntegerTime {
        public static int convertMillisecondsToSeconds(Long paramLong) {
            return (int)(paramLong.longValue() / 1000L);
        }

        public static String setHMSFormat(Integer paramInteger) {
            int remainder = paramInteger.intValue() * 1000;
            int seconds = remainder / 1000 % 60;
            int minutes = remainder / 60000 % 60;
            int hours = remainder / 3600000 % 24;
            return ((hours > 0) ? String.format("%02d:", Integer.valueOf(hours)) : "") + String.format("%02d:%02d", Integer.valueOf(minutes), Integer.valueOf(seconds));
        }

        public String setMSFormat(Integer paramInteger) {
            int minutes = (int)(paramInteger.intValue() / 60.0D);
            int seconds = paramInteger.intValue() % 60;
            return String.format("%d:%02d", Integer.valueOf(minutes), Integer.valueOf(seconds));
        }
    }

    public static class LongTime {
        public static long convertSecondsToMilliseconds(Integer paramInteger) {
            return paramInteger.intValue() * 1000L;
        }

    }
    public static String getDate() {
        SimpleDateFormat timeDate = new SimpleDateFormat(ModuleService.getFileModule().getFile("config")
                .getString("TIME.DATE", "yyyy-MM-dd", false));
        timeDate.setTimeZone(TimeZone.getTimeZone(ModuleService.getFileModule().getFile("config").getString("TIME.ZONE")));
        return timeDate.format(new Date());
    }

    public static String getHour() {
        SimpleDateFormat timeHour = new SimpleDateFormat(ModuleService.getFileModule().getFile("config")
                .getString("TIME.HOUR", "HH:mm", false));
        timeHour.setTimeZone(TimeZone.getTimeZone(ModuleService.getFileModule().getFile("config").getString("TIME.ZONE")));
        return timeHour.format(new Date());
    }
}
