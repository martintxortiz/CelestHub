package net.kryunek.hub.managers.module;

import net.kryunek.hub.Celest;
import net.kryunek.hub.hook.ScoreboardHook;
import net.kryunek.hub.hook.TablistHook;
import net.kryunek.hub.managers.module.impl.CommandModule;
import net.kryunek.hub.managers.module.impl.FileModule;
import net.kryunek.hub.managers.module.impl.ListenerModule;
import net.kryunek.hub.managers.module.impl.ManagerModule;
import net.kryunek.hub.managers.module.impl.VisualsModule;
import net.kryunek.hub.utils.TaskUtil;
import org.bukkit.event.HandlerList;

import java.util.Comparator;
import java.util.List;

public class ModuleService {
    private static ModuleService current;

    private final ManagerModule managerModule;
    private final FileModule fileModule;
    private final CommandModule commandModule;
    private final ListenerModule listenerModule;
    private final VisualsModule visualModule;
    private final List<Module> modules;

    public ModuleService() {
        this.fileModule = new FileModule();
        this.managerModule = new ManagerModule();
        this.listenerModule = new ListenerModule();
        this.commandModule = new CommandModule();
        this.visualModule = new VisualsModule();
        this.modules = List.of(fileModule, managerModule, listenerModule, commandModule, visualModule).stream()
                .sorted(Comparator.comparingInt(Module::getPriority))
                .toList();
    }

    public void disable(Celest hub) {
        ScoreboardHook.shutdown();
        TablistHook.shutdown();
        managerModule.shutdown();
        hub.getServer().getScheduler().cancelTasks(hub);
        hub.getServer().getMessenger().unregisterIncomingPluginChannel(hub);
        hub.getServer().getMessenger().unregisterOutgoingPluginChannel(hub);
        HandlerList.unregisterAll(hub);
        if (current == this) {
            current = null;
        }
    }

    public void enable(Celest hub) {
        current = this;
        for (Module module : modules) {
            module.onEnable(hub);
        }

        TaskUtil.runLater(() -> hub.setServerLoaded(true), 100L);
    }

    public static FileModule getFileModule() {
        return getCurrent().fileModule;
    }

    public static ListenerModule getListenerModule() {
        return getCurrent().listenerModule;
    }

    public static VisualsModule getVisualModule() {
        return getCurrent().visualModule;
    }

    public static ManagerModule getManagerModule() {
        return getCurrent().managerModule;
    }

    public static CommandModule getCommandModule() {
        return getCurrent().commandModule;
    }

    private static ModuleService getCurrent() {
        if (current == null) {
            throw new IllegalStateException("ModuleService is not enabled.");
        }
        return current;
    }
}
