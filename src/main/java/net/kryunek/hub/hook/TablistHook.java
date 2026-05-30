package net.kryunek.hub.hook;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.tablist.TablistManager;

@UtilityClass
public class TablistHook {

    @Getter
    private TablistManager tablistManager;
    private Celest plugin;

    public synchronized void init(Celest hub) {
        plugin = hub;
        if (!ModuleService.getFileModule().getFile(ConfigFiles.TAB).getBoolean("enabled")) {
            return;
        }

        tablistManager = new TablistManager(hub);
        tablistManager.start();
    }

    public synchronized void reload() {
        if (plugin == null) {
            return;
        }
        if (tablistManager != null) {
            tablistManager.stop();
        }
        if (!ModuleService.getFileModule().getFile(ConfigFiles.TAB).getBoolean("enabled")) {
            tablistManager = null;
            return;
        }
        tablistManager = new TablistManager(plugin);
        tablistManager.start();
    }

    public synchronized void shutdown() {
        if (tablistManager != null) {
            tablistManager.stop();
            tablistManager = null;
        }
        plugin = null;
    }
}
