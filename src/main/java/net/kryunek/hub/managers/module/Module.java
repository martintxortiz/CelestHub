package net.kryunek.hub.managers.module;

import net.kryunek.hub.Celest;

/**
 * A bootstrap unit enabled by {@link ModuleService}. Modules are enabled in ascending
 * {@link #getPriority()} order — FileModule(1) → ManagerModule(2) → CommandModule(3) →
 * ListenerModule(4) → VisualsModule(5) — so a later module may rely on earlier ones being ready.
 */
public abstract class Module {
    public abstract int getPriority();

    public abstract void onEnable(Celest hub);
}
