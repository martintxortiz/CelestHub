package net.kryunek.hub.managers.queue;

import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.rank.IRank;
import net.kryunek.hub.managers.rank.IRankManager;
import net.kryunek.hub.utils.FileConfig;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueueTest {

    @TempDir
    private Path tempDir;

    @Test
    void getPriorityReadsRankFromInjectedRankManager() {
        UUID player = UUID.randomUUID();
        IRankManager rankManager = rankManagerReturning(player, "vip");
        FileConfig config = newQueueConfig("priority.yml");
        config.getConfiguration().set("QUEUE.PRIORITY.vip", 50);

        withServer(queueFactory -> {
            Queue queue = queueFactory.create(rankManager, config);
            assertEquals(50, queue.getPriority(player));
            // Proves the priority comes from the injected manager, not a static lookup.
            verify(rankManager).getRank();
        });
    }

    @Test
    void getPriorityFallsBackToDefaultWhenRankIsBlankOrNull() {
        UUID blank = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        IRank rank = mock(IRank.class);
        when(rank.getName(blank)).thenReturn("");
        when(rank.getName(missing)).thenReturn(null);
        IRankManager rankManager = mock(IRankManager.class);
        when(rankManager.getRank()).thenReturn(rank);

        FileConfig config = newQueueConfig("default.yml");
        config.getConfiguration().set("QUEUE.PRIORITY.default", 3);

        withServer(queueFactory -> {
            Queue queue = queueFactory.create(rankManager, config);
            assertEquals(3, queue.getPriority(blank));
            assertEquals(3, queue.getPriority(missing));
        });
    }

    @Test
    void getPriorityLowercasesRankName() {
        UUID player = UUID.randomUUID();
        IRankManager rankManager = rankManagerReturning(player, "ADMIN");
        FileConfig config = newQueueConfig("lower.yml");
        config.getConfiguration().set("QUEUE.PRIORITY.admin", 99);

        withServer(queueFactory -> {
            Queue queue = queueFactory.create(rankManager, config);
            assertEquals(99, queue.getPriority(player));
        });
    }

    @Test
    void addEntryOrdersPlayersByPriorityDescending() {
        UUID lowRank = UUID.randomUUID();
        UUID midRank = UUID.randomUUID();
        UUID highRank = UUID.randomUUID();

        IRank rank = mock(IRank.class);
        when(rank.getName(lowRank)).thenReturn("default");
        when(rank.getName(midRank)).thenReturn("vip");
        when(rank.getName(highRank)).thenReturn("admin");
        IRankManager rankManager = mock(IRankManager.class);
        when(rankManager.getRank()).thenReturn(rank);

        FileConfig config = newQueueConfig("order.yml");
        config.getConfiguration().set("QUEUE.PRIORITY.default", 1);
        config.getConfiguration().set("QUEUE.PRIORITY.vip", 5);
        config.getConfiguration().set("QUEUE.PRIORITY.admin", 10);

        withServer(queueFactory -> {
            Queue queue = queueFactory.create(rankManager, config);
            queue.addEntry(lowRank);
            queue.addEntry(highRank);
            queue.addEntry(midRank);

            assertEquals(1, queue.getPosition(highRank));
            assertEquals(2, queue.getPosition(midRank));
            assertEquals(3, queue.getPosition(lowRank));
        });
    }

    private IRankManager rankManagerReturning(UUID player, String rankName) {
        IRank rank = mock(IRank.class);
        when(rank.getName(player)).thenReturn(rankName);
        IRankManager rankManager = mock(IRankManager.class);
        when(rankManager.getRank()).thenReturn(rank);
        return rankManager;
    }

    private FileConfig newQueueConfig(String name) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getResource(name)).thenReturn(null);
        return new FileConfig(plugin, name);
    }

    /**
     * The Queue constructor schedules a repeating Bukkit task via TaskUtil, whose static initializer
     * calls Celest.get(). Both must be intercepted so a Queue can be built without a running server.
     */
    private void withServer(Consumer<QueueFactory> body) {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<Celest> celest = mockStatic(Celest.class)) {
            Server server = mock(Server.class);
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            when(server.getScheduler()).thenReturn(scheduler);
            when(scheduler.runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong()))
                    .thenReturn(mock(BukkitTask.class));
            bukkit.when(Bukkit::getServer).thenReturn(server);
            celest.when(Celest::get).thenReturn(mock(Celest.class));
            body.accept((rankManager, config) ->
                    new Queue("lobby", mock(QueueManager.class), rankManager, config));
        }
    }

    @FunctionalInterface
    private interface QueueFactory {
        Queue create(IRankManager rankManager, FileConfig config);
    }
}
