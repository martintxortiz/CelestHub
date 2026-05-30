package net.kryunek.hub.listeners;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.queue.Queue;
import net.kryunek.hub.managers.queue.QueueEditSession;
import net.kryunek.hub.managers.queue.QueueCreateSession;
import net.kryunek.hub.managers.queue.QueueManager;
import net.kryunek.hub.menus.queue.QueueGlobalMenu;
import net.kryunek.hub.menus.queue.list.editor.QueueEditorMenu;
import net.kryunek.hub.menus.queue.list.QueuePaginatedMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class QueueListener implements Listener {

    private final FileConfig messages;

    public QueueListener(Celest hub) {
        Bukkit.getPluginManager().registerEvents(this, hub);
        this.messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (QueueEditSession.isActive(player)) {
            handleEditChat(event, player);
            return;
        }

        if (!QueueCreateSession.isActive(player)) {
            return;
        }

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        if (message.equalsIgnoreCase("cancel")) {
            QueueCreateSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("QUEUE.CREATE.CANCELLED")));
            return;
        }

        QueueManager manager = ModuleService.getManagerModule().getQueueManager();
        if (manager.getQueue(message) != null) {
            player.sendMessage(CC.translate(messages.getString("QUEUE.CREATE.ALREADY_EXISTS")));
            return;
        }

        manager.createQueue(message);
        QueueCreateSession.stop(player);
        player.sendMessage(CC.translate(messages.getString("QUEUE.CREATE.CREATED").replace("%queue%", message)));

        Bukkit.getScheduler().runTask(Celest.get(), () -> new QueuePaginatedMenu().openMenu(player));
    }

    private void handleEditChat(AsyncChatEvent event, Player player) {
        event.setCancelled(true);

        QueueEditSession session = QueueEditSession.get(player);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        if (message.equalsIgnoreCase("cancel")) {
            QueueEditSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("QUEUE.EDITOR.CANCELLED")));
            Bukkit.getScheduler().runTask(Celest.get(), () -> {
                if (session.isGlobal()) {
                    new QueueGlobalMenu().openMenu(player);
                    return;
                }
                new QueueEditorMenu(session.getServer()).openMenu(player);
            });
            return;
        }

        int value;
        try {
            value = Integer.parseInt(message);
        } catch (NumberFormatException exception) {
            player.sendMessage(CC.translate(messages.getString("QUEUE.EDITOR.INVALID_NUMBER")));
            return;
        }

        if (value <= 0) {
            player.sendMessage(CC.translate(messages.getString("QUEUE.EDITOR.INVALID_NUMBER")));
            return;
        }

        QueueEditSession.stop(player);
        QueueManager manager = ModuleService.getManagerModule().getQueueManager();
        if (session.getType() == QueueEditSession.Type.QUEUE_DELAY) {
            manager.updateQueueDelay(value);
            player.sendMessage(CC.translate(messages.getString("QUEUE.EDITOR.QUEUE_DELAY_UPDATED").replace("%value%", String.valueOf(value))));
        } else {
            manager.updatePositionMessageDelay(value);
            player.sendMessage(CC.translate(messages.getString("QUEUE.EDITOR.MESSAGE_DELAY_UPDATED").replace("%value%", String.valueOf(value))));
        }

        Bukkit.getScheduler().runTask(Celest.get(), () -> {
            if (session.isGlobal()) {
                new QueueGlobalMenu().openMenu(player);
                return;
            }
            new QueueEditorMenu(session.getServer()).openMenu(player);
        });
    }
}
