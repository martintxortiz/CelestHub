package net.kryunek.hub.listeners;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.lottery.LotteryCreateSession;
import net.kryunek.hub.managers.lottery.LotteryManager;
import net.kryunek.hub.managers.lottery.LotteryReminderEditSession;
import net.kryunek.hub.managers.lottery.LotteryRewardSession;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.menus.lottery.list.LotteryPaginatedMenu;
import net.kryunek.hub.menus.lottery.list.editor.LotteryEditorMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class LotteryListener implements Listener {

    private final Celest hub;
    private final LotteryManager lotteryManager;
    private final FileConfig messages;

    public LotteryListener(Celest hub) {
        this.hub = hub;
        this.lotteryManager = ModuleService.getManagerModule().getLotteryManager();
        this.messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);
        Bukkit.getPluginManager().registerEvents(this, hub);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cleanupPlayer(event.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        cleanupPlayer(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        lotteryManager.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() == null) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!lotteryManager.isJoinItem(event.getItem())) {
            return;
        }

        event.setCancelled(true);
        var active = lotteryManager.getSingleActiveLottery();
        if (active == null) {
            lotteryManager.handleJoin(event.getPlayer());
            if (lotteryManager.getFirstActiveLottery() != null) {
                event.getPlayer().sendMessage(CC.translate("&cThere are multiple active lotteries, use /lottery join <name>."));
                return;
            }
            event.getPlayer().sendMessage(CC.translate(messages.getString("LOTTERY.NOT_ACTIVE", "&cThis lottery is not active.", true)));
            return;
        }

        switch (lotteryManager.joinLottery(event.getPlayer(), active.getName())) {
            case JOINED -> event.getPlayer().sendMessage(CC.translate(messages.getString("LOTTERY.JOINED", "&aYou joined lottery &f%lottery%&a.", true)
                    .replace("%lottery%", active.getName())));
            case ALREADY_JOINED -> event.getPlayer().sendMessage(CC.translate(messages.getString("LOTTERY.ALREADY_JOINED", "&eYou are already in this lottery.", true)));
            case NOT_ACTIVE -> event.getPlayer().sendMessage(CC.translate(messages.getString("LOTTERY.NOT_ACTIVE", "&cThis lottery is not active.", true)));
            case NOT_FOUND -> event.getPlayer().sendMessage(CC.translate(messages.getString("LOTTERY.NOT_FOUND", "&cLottery not found.", true)));
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (LotteryCreateSession.isActive(player)) {
            event.setCancelled(true);
            handleCreateSession(player, PlainTextComponentSerializer.plainText().serialize(event.message()).trim());
            return;
        }

        if (LotteryReminderEditSession.isActive(player)) {
            event.setCancelled(true);
            handleReminderSession(player, PlainTextComponentSerializer.plainText().serialize(event.message()).trim());
            return;
        }

        if (!LotteryRewardSession.isActive(player)) {
            return;
        }

        event.setCancelled(true);
        handleRewardSession(player, PlainTextComponentSerializer.plainText().serialize(event.message()).trim());
    }

    private void cleanupPlayer(Player player) {
        LotteryCreateSession.stop(player);
        LotteryReminderEditSession.stop(player);
        LotteryRewardSession.stop(player);
    }

    private void handleCreateSession(Player player, String text) {
        if (text.equalsIgnoreCase("cancel")) {
            LotteryCreateSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("LOTTERY.CREATE.CANCELLED", "&cLottery creation cancelled.", true)));
            return;
        }

        String[] split = text.split("\\s+");
        if (split.length != 2) {
            player.sendMessage(CC.translate(messages.getString("LOTTERY.CREATE.USAGE", "&cUse: <name> <seconds>", true)));
            return;
        }

        int duration;
        try {
            duration = Integer.parseInt(split[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(CC.translate(messages.getString("LOTTERY.INVALID_NUMBER", "&cInvalid number.", true)));
            return;
        }

        if (duration <= 0) {
            player.sendMessage(CC.translate(messages.getString("LOTTERY.INVALID_NUMBER", "&cInvalid number.", true)));
            return;
        }

        boolean created = lotteryManager.createLottery(split[0], duration);
        if (!created) {
            player.sendMessage(CC.translate(messages.getString("LOTTERY.ALREADY_EXISTS", "&cA lottery with that name already exists.", true)));
            return;
        }

        LotteryCreateSession.stop(player);
        player.sendMessage(CC.translate(messages.getString("LOTTERY.CREATED", "&aLottery created: &f%lottery% &7(%seconds%s)", true)
                .replace("%lottery%", split[0])
                .replace("%seconds%", String.valueOf(duration))));
        Bukkit.getScheduler().runTask(hub, () -> new LotteryEditorMenu(split[0]).openMenu(player));
    }

    private void handleRewardSession(Player player, String text) {
        LotteryRewardSession session = LotteryRewardSession.get(player);
        if (session == null) {
            return;
        }

        if (text.equalsIgnoreCase("cancel")) {
            LotteryRewardSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("LOTTERY.REWARD.CANCELLED", "&cReward edition cancelled.", true)));
            Bukkit.getScheduler().runTask(hub, () -> new LotteryEditorMenu(session.getLotteryName()).openMenu(player));
            return;
        }

        if (text.isBlank()) {
            player.sendMessage(CC.translate(messages.getString("LOTTERY.REWARD.INVALID", "&cType a valid command.", true)));
            return;
        }

        boolean updated = lotteryManager.addReward(session.getLotteryName(), text);
        if (!updated) {
            LotteryRewardSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("LOTTERY.NOT_FOUND", "&cLottery not found.", true)));
            return;
        }

        LotteryRewardSession.stop(player);
        player.sendMessage(CC.translate(messages.getString("LOTTERY.REWARD_ADDED", "&aReward added to &f%lottery%&a.", true)
                .replace("%lottery%", session.getLotteryName())));
        Bukkit.getScheduler().runTask(hub, () -> new LotteryEditorMenu(session.getLotteryName()).openMenu(player));
    }

    private void handleReminderSession(Player player, String text) {
        if (text.equalsIgnoreCase("cancel")) {
            LotteryReminderEditSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("EDITOR.CANCELLED", "&cEditor action cancelled.", true)));
            Bukkit.getScheduler().runTask(hub, () -> new LotteryPaginatedMenu().openMenu(player));
            return;
        }

        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            player.sendMessage(CC.translate("&cType a valid number (>=5)."));
            return;
        }

        if (value < 5) {
            player.sendMessage(CC.translate("&cType a valid number (>=5)."));
            return;
        }

        lotteryManager.updateReminderIntervalSeconds(value);
        LotteryReminderEditSession.stop(player);
        player.sendMessage(CC.translate("&aUpdated lottery reminder interval to &f" + value + "s&a."));
        Bukkit.getScheduler().runTask(hub, () -> new LotteryPaginatedMenu().openMenu(player));
    }
}
