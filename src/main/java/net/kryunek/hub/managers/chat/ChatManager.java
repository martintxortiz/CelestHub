package net.kryunek.hub.managers.chat;

import net.kryunek.hub.support.config.ConfigSupport;
import lombok.Getter;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.FileConfig;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chat-moderation state — pause, slow-mode and prefix — backed by the injected settings config,
 * enforcing per-player slow-mode cooldowns and mirroring state across hubs via {@code NetworkSyncManager}.
 */
@Getter
public class ChatManager {

    private final FileConfig settingsConfig;
    private final Map<UUID, Long> lastMessageMap = new ConcurrentHashMap<>();
    private boolean paused;
    private int slowSeconds;
    private String prefix;

    public ChatManager(FileConfig settingsConfig) {
        this.settingsConfig = settingsConfig;
        ensureDefaults();
        load();
    }

    public void load() {
        this.paused = settingsConfig.getBoolean("CHAT.PAUSED");
        this.slowSeconds = Math.max(0, settingsConfig.getInt("CHAT.SLOW_SECONDS"));
        this.prefix = settingsConfig.getString("CHAT.PREFIX");
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        settingsConfig.getConfiguration().set("CHAT.PAUSED", paused);
        settingsConfig.save();
        notifyChatSync();
    }

    public void setSlowSeconds(int slowSeconds) {
        this.slowSeconds = Math.max(0, slowSeconds);
        settingsConfig.getConfiguration().set("CHAT.SLOW_SECONDS", this.slowSeconds);
        settingsConfig.save();
        notifyChatSync();
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        settingsConfig.getConfiguration().set("CHAT.PREFIX", prefix);
        settingsConfig.save();
        notifyChatSync();
    }

    public long getRemainingSlowSeconds(UUID uuid) {
        if (slowSeconds <= 0) {
            return 0L;
        }
        Long last = lastMessageMap.get(uuid);
        if (last == null) {
            return 0L;
        }

        long elapsed = System.currentTimeMillis() - last;
        long total = slowSeconds * 1000L;
        if (elapsed >= total) {
            return 0L;
        }

        long remainingMillis = total - elapsed;
        return (remainingMillis + 999L) / 1000L;
    }

    public void registerMessage(UUID uuid) {
        lastMessageMap.put(uuid, System.currentTimeMillis());
    }

    public void clearChatCooldowns() {
        lastMessageMap.clear();
    }

    private void ensureDefaults() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("CHAT.PREFIX", "&8[&bHub&8] &r");
        defaults.put("CHAT.PAUSED", false);
        defaults.put("CHAT.SLOW_SECONDS", 0);
        defaults.put("CHAT.CLEAR_LINES", 120);

        if (ConfigSupport.applyMissingDefaults(settingsConfig.getConfiguration(), defaults)) {
            settingsConfig.save();
        }
    }

    public void applyRemoteSnapshot(String yaml) {
        if (yaml == null || yaml.isBlank()) {
            return;
        }

        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (InvalidConfigurationException ex) {
            Bukkit.getLogger().warning("[Celest] Invalid remote chat snapshot.");
            return;
        }

        this.paused = cfg.getBoolean("chat.paused", this.paused);
        this.slowSeconds = Math.max(0, cfg.getInt("chat.slow_seconds", this.slowSeconds));
        this.prefix = cfg.getString("chat.prefix", this.prefix);

        settingsConfig.getConfiguration().set("CHAT.PAUSED", this.paused);
        settingsConfig.getConfiguration().set("CHAT.SLOW_SECONDS", this.slowSeconds);
        settingsConfig.getConfiguration().set("CHAT.PREFIX", this.prefix);
        settingsConfig.getConfiguration().set("CHAT.CLEAR_LINES", Math.max(1, cfg.getInt("chat.clear_lines", settingsConfig.getInt("CHAT.CLEAR_LINES"))));
        settingsConfig.save();
    }

    private void notifyChatSync() {
        if (ModuleService.getManagerModule() == null || ModuleService.getManagerModule().getNetworkSyncManager() == null) {
            return;
        }
        ModuleService.getManagerModule().getNetworkSyncManager().publishChatState(buildSnapshot());
    }

    private String buildSnapshot() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("chat.paused", paused);
        cfg.set("chat.slow_seconds", slowSeconds);
        cfg.set("chat.prefix", prefix);
        cfg.set("chat.clear_lines", Math.max(1, settingsConfig.getInt("CHAT.CLEAR_LINES")));
        return cfg.saveToString();
    }
}
