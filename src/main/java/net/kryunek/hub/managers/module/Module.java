package net.kryunek.hub.managers.module;

import net.kryunek.hub.Celest;

public abstract class Module {
    public abstract int getPriority();

    public abstract void onEnable(Celest hub);
}
