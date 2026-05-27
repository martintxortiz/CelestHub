package net.kryunek.hub.app;

import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.ModuleService;

public final class CelestApplication {

    private final Celest plugin;
    private final ModuleService modules;
    private boolean enabled;

    public CelestApplication(Celest plugin) {
        this.plugin = plugin;
        this.modules = new ModuleService();
    }

    public void enable() {
        if (enabled) {
            return;
        }
        modules.enable(plugin);
        enabled = true;
    }

    public void disable() {
        if (!enabled) {
            return;
        }
        modules.disable(plugin);
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
