package net.kryunek.hub.listeners;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.Celest;
import net.kryunek.hub.hook.TablistHook;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.rank.RankEditSession;
import net.kryunek.hub.menus.rank.RankEntryEditorMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class RankEditorListener implements Listener {

    private final FileConfig messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);

    public RankEditorListener(Celest hub) {
        Bukkit.getPluginManager().registerEvents(this, hub);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!RankEditSession.isActive(player)) {
            return;
        }

        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        RankEditSession session = RankEditSession.get(player);

        if (text.equalsIgnoreCase("cancel")) {
            RankEditSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("EDITOR.CANCELLED", "&cEditor action cancelled.", true)));
            openMenu(player, session.getRankName());
            return;
        }

        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            player.sendMessage(CC.translate("&cType a valid number >= 0."));
            return;
        }

        if (value < 0) {
            player.sendMessage(CC.translate("&cType a valid number >= 0."));
            return;
        }

        if (session.getType() == RankEditSession.Type.QUEUE_PRIORITY) {
            ModuleService.getManagerModule().getRankManager().setQueuePriority(session.getRankName(), value);
            player.sendMessage(CC.translate("&aUpdated queue priority of &f" + session.getRankName() + " &ato &f" + value + "&a."));
        } else {
            ModuleService.getManagerModule().getRankManager().setTabPriority(session.getRankName(), value);
            TablistHook.reload();
            player.sendMessage(CC.translate("&aUpdated tab priority of &f" + session.getRankName() + " &ato &f" + value + "&a."));
        }

        RankEditSession.stop(player);
        openMenu(player, session.getRankName());
    }

    private void openMenu(Player player, String rankName) {
        Bukkit.getScheduler().runTask(Celest.get(), () -> new RankEntryEditorMenu(rankName).openMenu(player));
    }
}
