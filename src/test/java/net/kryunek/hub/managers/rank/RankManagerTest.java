package net.kryunek.hub.managers.rank;

import net.kryunek.hub.utils.FileConfig;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class RankManagerTest {

    @TempDir
    private Path tempDir;

    @Test
    void getQueuePriorityLowercasesRankName() {
        FileConfig queue = newConfig("queue.yml");
        queue.getConfiguration().set("QUEUE.PRIORITY.vip", 7);

        withManager(newConfig("config.yml"), queue, newConfig("tab.yml"), manager ->
                assertEquals(7, manager.getQueuePriority("VIP")));
    }

    @Test
    void setQueuePriorityClampsNegativeValuesToZero() {
        FileConfig queue = newConfig("queue.yml");

        withManager(newConfig("config.yml"), queue, newConfig("tab.yml"), manager -> {
            manager.setQueuePriority("VIP", -5);
            assertEquals(0, queue.getInt("QUEUE.PRIORITY.vip"));
        });
    }

    @Test
    void getAvailableRanksMergesQueuePriorityKeysAndTabGroups() {
        FileConfig queue = newConfig("queue.yml");
        queue.getConfiguration().set("QUEUE.PRIORITY.vip", 5);
        queue.getConfiguration().set("QUEUE.PRIORITY.mvp", 8);
        FileConfig tab = newConfig("tab.yml");
        tab.getConfiguration().set("group-sorting.groups", List.of("admin", "vip"));

        withManager(newConfig("config.yml"), queue, tab, manager -> {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                PluginManager pluginManager = mock(PluginManager.class);
                when(pluginManager.getPlugin("LuckPerms")).thenReturn(null);
                bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);

                List<String> ranks = manager.getAvailableRanks();
                assertTrue(ranks.contains("vip"), "expected queue-priority rank 'vip'");
                assertTrue(ranks.contains("mvp"), "expected queue-priority rank 'mvp'");
                assertTrue(ranks.contains("admin"), "expected tab-group rank 'admin'");
            }
        });
    }

    private FileConfig newConfig(String name) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getResource(name)).thenReturn(null);
        return new FileConfig(plugin, name);
    }

    private void withManager(FileConfig config, FileConfig queue, FileConfig tab,
                             Consumer<RankManager> body) {
        body.accept(new RankManager(config, queue, tab));
    }
}
