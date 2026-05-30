package net.kryunek.hub.managers.editor;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.session.SessionGuard;
import net.kryunek.hub.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EditorInputSession {

    private static final long TIMEOUT_TICKS = 20L * 60L;
    private static final Map<UUID, EditorInputSession> SESSIONS = new HashMap<>();

    @Getter
    private final Type type;
    @Getter
    private final String context;
    @Getter
    private final long createdAt;

    private EditorInputSession(Type type, String context, long createdAt) {
        this.type = type;
        this.context = context;
        this.createdAt = createdAt;
    }

    public static void start(Player player, Type type) {
        start(player, type, null);
    }

    public static void start(Player player, Type type, String context) {
        if (!SessionGuard.canStart(player, "EDITOR_INPUT")) {
            return;
        }
        UUID uuid = player.getUniqueId();
        long startedAt = System.currentTimeMillis();
        SESSIONS.put(uuid, new EditorInputSession(type, context, startedAt));
        scheduleTimeout(uuid, startedAt);
    }

    public static EditorInputSession get(Player player) {
        return SESSIONS.get(player.getUniqueId());
    }

    public static boolean isActive(Player player) {
        return SESSIONS.containsKey(player.getUniqueId());
    }

    public static void stop(Player player) {
        SESSIONS.remove(player.getUniqueId());
    }

    public static int activeCount() {
        return SESSIONS.size();
    }

    private static void scheduleTimeout(UUID uuid, long startedAt) {
        Bukkit.getScheduler().runTaskLater(Celest.get(), () -> {
            EditorInputSession session = SESSIONS.get(uuid);
            if (session == null || session.getCreatedAt() != startedAt) {
                return;
            }

            SESSIONS.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(CC.translate(ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES)
                        .getString("SESSION.EXPIRED", "&cEditor session expired after 60 seconds.", true)));
            }
        }, TIMEOUT_TICKS);
    }

    @Getter
    @RequiredArgsConstructor
    public enum Type {
        CHAT_SLOW_SECONDS("EDITOR.CHAT.SLOW_PROMPT"),
        CHAT_CLEAR_LINES("EDITOR.CHAT.CLEAR_LINES_PROMPT"),
        CHAT_PREFIX("EDITOR.CHAT.PREFIX_PROMPT"),
        HOTBAR_SLOT("EDITOR.HOTBAR.SLOT_PROMPT"),
        HOTBAR_NAME("EDITOR.HOTBAR.NAME_PROMPT"),
        HOTBAR_LORE("EDITOR.HOTBAR.LORE_PROMPT"),
        HOTBAR_COMMAND("EDITOR.HOTBAR.COMMAND_PROMPT"),
        HOTBAR_HEAD_OWNER("EDITOR.HOTBAR.HEAD_OWNER_PROMPT"),
        HOTBAR_HEAD_OWNER_UUID("EDITOR.HOTBAR.HEAD_OWNER_UUID_PROMPT"),
        HOTBAR_SOUND("EDITOR.HOTBAR.CLICK_SOUND.SOUND_PROMPT"),
        HOTBAR_SOUND_VOLUME("EDITOR.HOTBAR.CLICK_SOUND.VOLUME_PROMPT"),
        HOTBAR_SOUND_PITCH("EDITOR.HOTBAR.CLICK_SOUND.PITCH_PROMPT"),
        PVP_ARENA_EFFECT("EDITOR.PVP_ARENA.EFFECT_PROMPT");

        private final String promptPath;
    }
}
