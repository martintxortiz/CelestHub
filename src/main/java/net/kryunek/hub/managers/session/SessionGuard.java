package net.kryunek.hub.managers.session;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.editor.EditorInputSession;
import net.kryunek.hub.managers.gadgets.GadgetEditSession;
import net.kryunek.hub.managers.lottery.LotteryCreateSession;
import net.kryunek.hub.managers.lottery.LotteryReminderEditSession;
import net.kryunek.hub.managers.lottery.LotteryRewardSession;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.outfit.OutfitCreateSession;
import net.kryunek.hub.managers.particles.TrailParticleCreateSession;
import net.kryunek.hub.managers.queue.QueueCreateSession;
import net.kryunek.hub.managers.queue.QueueEditSession;
import net.kryunek.hub.managers.rank.RankEditSession;
import net.kryunek.hub.managers.selector.ServerSelectorEditSession;
import net.kryunek.hub.menus.timer.TimerCreateSession;
import net.kryunek.hub.utils.CC;
import org.bukkit.entity.Player;

public final class SessionGuard {

    private SessionGuard() {
    }

    public static boolean canStart(Player player, String currentSessionId) {
        if (hasOtherActiveSession(player, currentSessionId)) {
            player.sendMessage(CC.translate(ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES)
                    .getString("SESSION.ALREADY_ACTIVE", "&cYou already have an active session. Type 'cancel' in chat first.", true)));
            return false;
        }
        return true;
    }

    private static boolean hasOtherActiveSession(Player player, String currentSessionId) {
        return isOtherActive("EDITOR_INPUT", currentSessionId, EditorInputSession.isActive(player))
                || isOtherActive("GADGET_EDIT", currentSessionId, GadgetEditSession.isActive(player))
                || isOtherActive("LOTTERY_CREATE", currentSessionId, LotteryCreateSession.isActive(player))
                || isOtherActive("LOTTERY_REMINDER_EDIT", currentSessionId, LotteryReminderEditSession.isActive(player))
                || isOtherActive("LOTTERY_REWARD", currentSessionId, LotteryRewardSession.isActive(player))
                || isOtherActive("OUTFIT_CREATE", currentSessionId, OutfitCreateSession.isActive(player))
                || isOtherActive("TRAIL_CREATE", currentSessionId, TrailParticleCreateSession.isActive(player))
                || isOtherActive("QUEUE_CREATE", currentSessionId, QueueCreateSession.isActive(player))
                || isOtherActive("QUEUE_EDIT", currentSessionId, QueueEditSession.isActive(player))
                || isOtherActive("RANK_EDIT", currentSessionId, RankEditSession.isActive(player))
                || isOtherActive("SERVER_SELECTOR_EDIT", currentSessionId, ServerSelectorEditSession.isActive(player))
                || isOtherActive("TIMER_CREATE", currentSessionId, TimerCreateSession.isActive(player));
    }

    private static boolean isOtherActive(String sessionId, String currentSessionId, boolean active) {
        return active && !sessionId.equalsIgnoreCase(currentSessionId);
    }
}
