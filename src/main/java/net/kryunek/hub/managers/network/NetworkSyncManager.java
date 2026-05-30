package net.kryunek.hub.managers.network;

import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.impl.ManagerModule;
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

/**
 * Optional Redis pub/sub bridge that mirrors queue, lottery, timer and chat state across hub
 * instances, with exponential-backoff auto-reconnect. No-op unless enabled in the injected config.
 */
public class NetworkSyncManager {

    private static final String CHANNEL = "celesthub:sync";
    private static final String TOPIC_QUEUE = "QUEUE_STATE";
    private static final String TOPIC_LOTTERY = "LOTTERY_STATE";
    private static final String TOPIC_TIMER = "TIMER_STATE";
    private static final String TOPIC_CHAT = "CHAT_STATE";
    private static final long RECONNECT_BACKOFF_START_MS = 1_000L;
    private static final long RECONNECT_BACKOFF_MAX_MS = 30_000L;

    private final Celest hub;
    private final ManagerModule managers;
    private final FileConfig config;
    private final String serverId;
    private volatile boolean enabled;
    private JedisPooled publisher;
    private volatile Jedis subscriber;
    private volatile JedisPubSub subscription;
    private Thread subscriberThread;

    public NetworkSyncManager(Celest hub, ManagerModule managers, FileConfig config) {
        this.hub = hub;
        this.managers = managers;
        this.config = config;
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

        this.subscriberThread = new Thread(() -> runSubscriber(redisUri), "Celest-RedisSync");
        this.subscriberThread.setDaemon(true);
        this.subscriberThread.start();
        Bukkit.getLogger().info("[Celest] Network sync enabled on Redis channel " + CHANNEL);
    }

    /**
     * Subscribes to the sync channel and blocks until disconnected. On any failure it reconnects
     * with exponential backoff (capped) for as long as the manager is enabled, so a transient Redis
     * outage no longer leaves this node permanently deaf to network updates.
     */
    private void runSubscriber(String redisUri) {
        long backoffMs = RECONNECT_BACKOFF_START_MS;
        while (enabled && !Thread.currentThread().isInterrupted()) {
            try (Jedis subscriberClient = new Jedis(URI.create(redisUri))) {
                this.subscriber = subscriberClient;
                this.subscription = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        handleMessage(message);
                    }
                };
                backoffMs = RECONNECT_BACKOFF_START_MS;
                subscriberClient.subscribe(this.subscription, CHANNEL);
            } catch (Exception ex) {
                if (enabled) {
                    Bukkit.getLogger().warning("[Celest] Redis subscriber stopped: " + ex.getMessage()
                            + " — reconnecting in " + (backoffMs / 1000L) + "s");
                }
            } finally {
                this.subscription = null;
                this.subscriber = null;
            }

            if (!enabled) {
                break;
            }
            try {
                Thread.sleep(backoffMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
            backoffMs = Math.min(backoffMs * 2, RECONNECT_BACKOFF_MAX_MS);
        }
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
        publish(TOPIC_QUEUE, yaml);
    }

    public void publishLotteryState(String yaml) {
        publish(TOPIC_LOTTERY, yaml);
    }

    public void publishTimerState(String snapshot) {
        publish(TOPIC_TIMER, snapshot);
    }

    public void publishChatState(String snapshot) {
        publish(TOPIC_CHAT, snapshot);
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
        Bukkit.getScheduler().runTask(hub, () -> {
            if (TOPIC_QUEUE.equalsIgnoreCase(topic) && managers.getQueueManager() != null) {
                managers.getQueueManager().applyRemoteSnapshot(payload);
            } else if (TOPIC_LOTTERY.equalsIgnoreCase(topic) && managers.getLotteryManager() != null) {
                managers.getLotteryManager().applyRemoteSnapshot(payload);
            } else if (TOPIC_TIMER.equalsIgnoreCase(topic) && managers.getTimerManager() != null) {
                managers.getTimerManager().applyRemoteSnapshot(payload);
            } else if (TOPIC_CHAT.equalsIgnoreCase(topic) && managers.getChatManager() != null) {
                managers.getChatManager().applyRemoteSnapshot(payload);
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
