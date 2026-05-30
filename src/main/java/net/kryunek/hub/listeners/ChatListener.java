package net.kryunek.hub.listeners;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.chat.ChatManager;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;

public class ChatListener implements Listener {

    private final ChatManager chatManager;
    private final FileConfig messages;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    public ChatListener(Celest hub) {
        Bukkit.getPluginManager().registerEvents(this, hub);
        this.chatManager = ModuleService.getManagerModule().getChatManager();
        this.messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (chatManager.isPaused()
                && !player.hasPermission("celest.chat.bypass.pause")
                && !player.hasPermission("celest.chat.bypass.mute")) {
            event.setCancelled(true);
            player.sendMessage(CC.translate(messages.getString("CHAT.MESSAGES.MUTED", "&cChat is currently muted.", true)));
            return;
        }

        if (chatManager.getSlowSeconds() > 0 && !player.hasPermission("celest.chat.bypass.slow")) {
            long remaining = chatManager.getRemainingSlowSeconds(player.getUniqueId());
            if (remaining > 0) {
                event.setCancelled(true);
                player.sendMessage(CC.translate(messages.getString("CHAT.MESSAGES.SLOWMODE", "&cSlow mode is enabled. Wait &f%time%s&c.", true)
                        .replace("%time%", String.valueOf(remaining))));
                return;
            }
            chatManager.registerMessage(player.getUniqueId());
        }

        // AsyncChatEvent has no setFormat; reproduce "<displayName>&7: &f<message>" via a renderer.
        event.renderer((source, sourceDisplayName, message, viewer) ->
                sourceDisplayName
                        .append(LEGACY.deserialize(CC.translate("&7: &f")))
                        .append(message));
    }
}
