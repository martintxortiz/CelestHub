package net.kryunek.hub.managers.timer;

import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.network.NetworkSyncManager;
import net.kryunek.hub.managers.queue.Queue;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Countdown timers that auto-unpause their target queue on expiry, mirrored across hubs via network sync. */
public class TimerManager {

    private final List<Timer> timers;
    private final BukkitTask updateTask;

    public TimerManager() {
        this.timers = new ArrayList<>();
        this.updateTask = TaskUtil.runSyncTimer(this::updateTimers, 20L, 20L);
    }

    public List<Timer> getTimers() {
        return timers;
    }

    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
    }

    public void createTimer(Player player, String name, long durationInSeconds, String prefix) {
        for (Timer t : timers) {
            if (t.getName().equalsIgnoreCase(name)) {
                player.sendMessage(CC.translate("&cTimer already exists."));
                return;
            }
        }

        Queue queue = ModuleService.getManagerModule().getQueueManager().getQueue(name);
        if (queue == null) {
            player.sendMessage(CC.translate("&cQueue &4" + name + " &cdoes not exist."));
            return;
        }

        timers.add(new Timer(name, durationInSeconds, prefix));
        publishSnapshot();
        player.sendMessage(CC.translate("&aTimer &f" + name + " &acreated."));
    }

    public void deleteTimer(Player player, String name) {
        if (deleteTimerInternal(name)) {
            player.sendMessage(CC.translate("&cTimer &4" + name + "&c removed."));
            return;
        }
        player.sendMessage(CC.translate("&cTimer not found."));
    }

    public boolean deleteTimerInternal(String name) {
        Iterator<Timer> iterator = timers.iterator();
        while (iterator.hasNext()) {
            Timer timer = iterator.next();
            if (timer.getName().equalsIgnoreCase(name)) {
                iterator.remove();
                publishSnapshot();
                return true;
            }
        }
        return false;
    }

    public boolean togglePauseTimer(String name) {
        Timer timer = timers.stream()
                .filter(t -> t.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        if (timer == null) {
            return false;
        }

        timer.togglePause();
        publishSnapshot();
        return true;
    }

    public void applyRemoteSnapshot(String yaml) {
        if (yaml == null || yaml.isBlank()) {
            return;
        }

        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (InvalidConfigurationException e) {
            Bukkit.getLogger().warning("[Celest] Invalid remote timer snapshot.");
            return;
        }

        List<Timer> rebuilt = new ArrayList<>();
        var section = cfg.getConfigurationSection("timers");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String base = "timers." + key + ".";
                String queueName = cfg.getString(base + "name");
                if (queueName == null || queueName.isBlank()) {
                    continue;
                }
                long endTime = cfg.getLong(base + "endTime", System.currentTimeMillis());
                boolean paused = cfg.getBoolean(base + "paused", false);
                long remaining = cfg.getLong(base + "remainingTime", 0L);
                String prefix = cfg.getString(base + "prefix", queueName);
                rebuilt.add(new Timer(queueName, endTime, paused, remaining, prefix));
            }
        }

        timers.clear();
        timers.addAll(rebuilt);
    }

    private void publishSnapshot() {
        if (ModuleService.getManagerModule() == null) {
            return;
        }
        NetworkSyncManager network = ModuleService.getManagerModule().getNetworkSyncManager();
        if (network == null) {
            return;
        }

        YamlConfiguration cfg = new YamlConfiguration();
        for (int i = 0; i < timers.size(); i++) {
            Timer timer = timers.get(i);
            String base = "timers." + i + ".";
            cfg.set(base + "name", timer.getName());
            cfg.set(base + "endTime", timer.getEndTime());
            cfg.set(base + "paused", timer.isPaused());
            cfg.set(base + "remainingTime", timer.isPaused() ? timer.getRemainingTimeInMillis() : 0L);
            cfg.set(base + "prefix", timer.getPrefix());
        }
        network.publishTimerState(cfg.saveToString());
    }

    private void updateTimers() {
        Iterator<Timer> iterator = timers.iterator();
        boolean changed = false;

        while (iterator.hasNext()) {
            Timer timer = iterator.next();
            if (!timer.isExpired()) {
                continue;
            }

            iterator.remove();
            changed = true;

            Bukkit.broadcastMessage(CC.translate(timer.getPrefix() + " &7has started!"));

            Queue queue = ModuleService.getManagerModule().getQueueManager().getQueue(timer.getName());
            if (queue != null) {
                queue.setPaused(false);
            }
        }

        if (changed) {
            publishSnapshot();
        }
    }
}
