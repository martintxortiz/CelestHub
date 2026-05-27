package net.kryunek.hub.hook;

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

    public void init(Celest hub) {
        plugin = hub;
        if (!ModuleService.getFileModule().getFile("tab").getBoolean("enabled")) {
            return;
        }

        tablistManager = new TablistManager(hub);
        tablistManager.iniciar();
    }

    public void reload() {
        if (plugin == null) {
            return;
        }
        if (tablistManager != null) {
            tablistManager.detener();
        }
        if (!ModuleService.getFileModule().getFile("tab").getBoolean("enabled")) {
            tablistManager = null;
            return;
        }
        tablistManager = new TablistManager(plugin);
        tablistManager.iniciar();
    }

    public void shutdown() {
        if (tablistManager != null) {
            tablistManager.detener();
            tablistManager = null;
        }
        plugin = null;
    }
}
