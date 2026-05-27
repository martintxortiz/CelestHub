package net.kryunek.hub.utils.command;

import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.module.impl.FileModule;
import net.kryunek.hub.managers.module.impl.ManagerModule;
import net.kryunek.hub.support.command.CommandSupport;
import net.kryunek.hub.support.message.Messages;
import net.kryunek.hub.utils.FileConfig;
import org.bukkit.command.CommandSender;

public abstract class BaseCommand {

    protected final Messages messages;
    protected final CommandSupport commands;

    public BaseCommand() {
        this.messages = Messages.from(file("messages"), Celest.get().getLogger());
        this.commands = new CommandSupport(messages);
        CommandManager.getInstance().registerCommands(this, null);
    }

    public abstract void onCommand(CommandArgs command);

    protected ManagerModule managers() {
        return ModuleService.getManagerModule();
    }

    protected FileModule files() {
        return ModuleService.getFileModule();
    }

    protected FileConfig file(String name) {
        return files().getFile(name);
    }

    protected String message(String path, String fallback, String... placeholders) {
        return messages.get(path, fallback, placeholders);
    }

    protected void send(CommandSender sender, String path, String fallback, String... placeholders) {
        messages.send(sender, path, fallback, placeholders);
    }
}
