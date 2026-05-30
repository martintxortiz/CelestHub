package net.kryunek.hub.utils;

import net.kryunek.hub.Celest;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public final class TaskUtil {

    private static JavaPlugin plugin;

    private TaskUtil() {
    }

    /**
     * Resolves the owning plugin lazily and caches it. Resolving on first use (rather than in a
     * static initializer) avoids an {@code ExceptionInInitializerError} at class-load time when the
     * plugin instance is not yet registered with Bukkit.
     */
    private static JavaPlugin plugin() {
        if (plugin == null) {
            plugin = Celest.get();
        }
        return plugin;
    }

    public static void runLater(Runnable runnable, long timer) {
        Bukkit.getServer().getScheduler().runTaskLater(plugin(), runnable, timer);
    }

    /** Period values are given in seconds and converted to ticks. */
    public static void runTaskTimer(Runnable runnable, long timer, long async) {
        Bukkit.getServer().getScheduler().runTaskTimer(plugin(), runnable, 20L * timer, 20L * async);
    }

    /** Period values are given in ticks. */
    public static void runTimer(Runnable runnable, long timer, long async) {
        Bukkit.getServer().getScheduler().runTaskTimer(plugin(), runnable, timer, async);
    }

    public static BukkitTask runSyncTimer(Runnable runnable, long delay, long period) {
        return Bukkit.getServer().getScheduler().runTaskTimer(plugin(), runnable, delay, period);
    }

    public static void runTimerAsync(BukkitRunnable runnable, long timer, long async) {
        runnable.runTaskTimerAsynchronously(plugin(), timer, async);
    }

    public static void runLaterAsync(Runnable runnable, long async) {
        Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(plugin(), runnable, async);
    }

    /** Period values are given in ticks. */
    public static void runTimer(BukkitRunnable runnable, long timer, long async) {
        runnable.runTaskTimer(plugin(), timer, async);
    }

    /** Period values are given in ticks. */
    public static void runTimerAsync(Runnable runnable, long timer, long async) {
        Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(plugin(), runnable, timer, async);
    }

    public static void run(Runnable runnable) {
        Bukkit.getServer().getScheduler().runTask(plugin(), runnable);
    }

    public static void runAsync(Runnable runnable) {
        Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin(), runnable);
    }

    /** Period values are given in seconds and converted to ticks. */
    public static void runTaskTimerAsynchronously(Runnable runnable, long timer, long async) {
        Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(plugin(), runnable, 20L * timer, 20L * async);
    }

    public static void scheduleSyncDelayedTask(Runnable runnable) {
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin(), runnable);
    }
}
