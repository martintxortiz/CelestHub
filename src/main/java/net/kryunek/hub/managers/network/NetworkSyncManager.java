package net.kryunek.hub.managers.network;

import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.FileConfig;
import org.bukkit.Bukkit;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.JedisPooled;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;

public class NetworkSyncManager {

    private static final String CHANNEL = "celesthub:sync";
    private final FileConfig config;
    private final String serverId;
    private volatile boolean enabled;
    private JedisPooled publisher;
    private volatile Jedis subscriber;
    private volatile JedisPubSub subscription;
    private Thread subscriberThread;

    public NetworkSyncManager() {
        this.config = ModuleService.getFileModule().getFile("config");
        this.serverId = UUID.randomUUID().toString();
    }

    public void start() {
        this.enabled = config.getConfiguration().getBoolean("NETWORK_SYNC.ENABLED", false);
        if (!enabled) {
            return;
        }

        String host = config.getConfiguration().getString("NETWORK_SYNC.REDIS.HOST", "127.0.0.1");
        int port = config.getConfiguration().getInt("NETWORK_SYNC.REDIS.PORT", 6379);
        String password = config.getConfiguration().getString("NETWORK_SYNC.REDIS.PASSWORD", "");
        String redisUri = buildRedisUri(host, port, password);

        this.publisher = new JedisPooled(URI.create(redisUri));

        this.subscriberThread = new Thread(() -> {
            try (Jedis subscriberClient = new Jedis(URI.create(redisUri))) {
                this.subscriber = subscriberClient;
                this.subscription = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        handleMessage(message);
                    }
                };
                subscriberClient.subscribe(this.subscription, CHANNEL);
            } catch (Exception ex) {
                if (enabled) {
                    Bukkit.getLogger().warning("[Celest] Redis subscriber stopped: " + ex.getMessage());
                }
            } finally {
                this.subscription = null;
                this.subscriber = null;
            }
        }, "Celest-RedisSync");
        this.subscriberThread.setDaemon(true);
        this.subscriberThread.start();
        Bukkit.getLogger().info("[Celest] Network sync enabled on Redis channel " + CHANNEL);
    }

    public void shutdown() {
        enabled = false;
        if (subscription != null) {
            try {
                subscription.unsubscribe();
            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.FINE, "[Celest] Failed to unsubscribe Redis listener during shutdown.", ex);
            }
            subscription = null;
        }
        if (subscriber != null) {
            try {
                subscriber.close();
            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.FINE, "[Celest] Failed to close Redis subscriber during shutdown.", ex);
            }
            subscriber = null;
        }
        if (publisher != null) {
            publisher.close();
            publisher = null;
        }
        if (subscriberThread != null) {
            subscriberThread.interrupt();
            subscriberThread = null;
        }
    }

    public void publishQueueState(String yaml) {
        publish("QUEUE_STATE", yaml);
    }

    public void publishLotteryState(String yaml) {
        publish("LOTTERY_STATE", yaml);
    }

    public void publishTimerState(String snapshot) {
        publish("TIMER_STATE", snapshot);
    }

    public void publishChatState(String snapshot) {
        publish("CHAT_STATE", snapshot);
    }

    private void publish(String topic, String payload) {
        if (!enabled || publisher == null) {
            return;
        }
        try {
            String encoded = Base64.getEncoder().encodeToString((payload == null ? "" : payload).getBytes(StandardCharsets.UTF_8));
            publisher.publish(CHANNEL, serverId + "|" + topic + "|" + encoded);
        } catch (Exception ex) {
            Bukkit.getLogger().warning("[Celest] Redis publish failed: " + ex.getMessage());
        }
    }

    private void handleMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        String[] split = message.split("\\|", 3);
        if (split.length < 2) {
            return;
        }
        if (serverId.equals(split[0])) {
            return;
        }
        String topic = split[1];
        String payload;
        try {
            payload = split.length == 3 ? new String(Base64.getDecoder().decode(split[2]), StandardCharsets.UTF_8) : "";
        } catch (IllegalArgumentException ex) {
            Bukkit.getLogger().warning("[Celest] Ignoring malformed Redis sync payload for topic " + topic + ".");
            return;
        }
        Bukkit.getScheduler().runTask(Celest.get(), () -> {
            if ("QUEUE_STATE".equalsIgnoreCase(topic) && ModuleService.getManagerModule().getQueueManager() != null) {
                ModuleService.getManagerModule().getQueueManager().applyRemoteSnapshot(payload);
            } else if ("LOTTERY_STATE".equalsIgnoreCase(topic) && ModuleService.getManagerModule().getLotteryManager() != null) {
                ModuleService.getManagerModule().getLotteryManager().applyRemoteSnapshot(payload);
            } else if ("TIMER_STATE".equalsIgnoreCase(topic) && ModuleService.getManagerModule().getTimerManager() != null) {
                ModuleService.getManagerModule().getTimerManager().applyRemoteSnapshot(payload);
            } else if ("CHAT_STATE".equalsIgnoreCase(topic) && ModuleService.getManagerModule().getChatManager() != null) {
                ModuleService.getManagerModule().getChatManager().applyRemoteSnapshot(payload);
            }
        });
    }

    private String buildRedisUri(String host, int port, String password) {
        if (password == null || password.isBlank()) {
            return "redis://" + host + ":" + port;
        }
        return "redis://:" + password + "@" + host + ":" + port;
    }
}
