package net.kryunek.hub.listeners;

import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.chat.ChatManager;
import net.kryunek.hub.managers.editor.EditorInputSession;
import net.kryunek.hub.managers.hotbar.HotbarManager;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.pvparena.PvpArenaKitManager;
import net.kryunek.hub.menus.editor.chat.ChatEditorMenu;
import net.kryunek.hub.menus.editor.hotbar.HotbarEditorMenu;
import net.kryunek.hub.menus.editor.hotbar.HotbarItemEditorMenu;
import net.kryunek.hub.menus.editor.pvparena.PvpArenaEditorMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import org.bukkit.Bukkit;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class EditorListener implements Listener {

    private final Celest hub;
    private final ChatManager chatManager;
    private final HotbarManager hotbarManager;
    private final PvpArenaKitManager pvpArenaKitManager;
    private final FileConfig settings;
    private final FileConfig messages;
    private final FileConfig hotbar;

    public EditorListener(Celest hub) {
        this.hub = hub;
        var files = ModuleService.getFileModule();
        var managers = ModuleService.getManagerModule();
        this.settings = files.getFile("settings");
        this.messages = files.getFile("messages");
        this.hotbar = files.getFile("hotbar");
        this.chatManager = managers.getChatManager();
        this.hotbarManager = managers.getHotbarManager();
        this.pvpArenaKitManager = managers.getPvpArenaKitManager();
        Bukkit.getPluginManager().registerEvents(this, hub);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!EditorInputSession.isActive(player)) {
            return;
        }

        event.setCancelled(true);
        String text = event.getMessage().trim();
        EditorInputSession session = EditorInputSession.get(player);

        if (text.equalsIgnoreCase("cancel")) {
            EditorInputSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("EDITOR.CANCELLED", "&cEditor action cancelled.", true)));
            openBackMenu(player, session);
            return;
        }

        switch (session.getType()) {
            case CHAT_SLOW_SECONDS -> handleChatSlow(player, text, session);
            case CHAT_CLEAR_LINES -> handleChatClearLines(player, text, session);
            case CHAT_PREFIX -> handleChatPrefix(player, text, session);
            case HOTBAR_SLOT -> handleHotbarSlot(player, text, session);
            case HOTBAR_NAME -> handleHotbarName(player, text, session);
            case HOTBAR_LORE -> handleHotbarLore(player, text, session);
            case HOTBAR_COMMAND -> handleHotbarCommand(player, text, session);
            case HOTBAR_HEAD_OWNER -> handleHotbarHeadOwner(player, text, session);
            case HOTBAR_HEAD_OWNER_UUID -> handleHotbarHeadOwnerUuid(player, text, session);
            case HOTBAR_SOUND -> handleHotbarClickSound(player, text, session);
            case HOTBAR_SOUND_VOLUME -> handleHotbarClickSoundVolume(player, text, session);
            case HOTBAR_SOUND_PITCH -> handleHotbarClickSoundPitch(player, text, session);
            case PVP_ARENA_EFFECT -> handlePvpArenaEffect(player, text, session);
        }
    }

    private void handleChatSlow(Player player, String text, EditorInputSession session) {
        if (text.equalsIgnoreCase("off")) {
            chatManager.setSlowSeconds(0);
            chatManager.clearChatCooldowns();
            EditorInputSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("CHAT.MESSAGES.DISABLED_SLOW", "&eChat slow mode disabled.", true)));
            openBackMenu(player, session);
            return;
        }

        int value = parsePositiveInt(player, text);
        if (value < 0) {
            return;
        }

        chatManager.setSlowSeconds(value);
        chatManager.clearChatCooldowns();
        EditorInputSession.stop(player);
        player.sendMessage(CC.translate(messages.getString("CHAT.MESSAGES.SET_SLOW", "&eChat slow mode set to &f%time%s&e.", true)
                .replace("%time%", String.valueOf(value))));
        openBackMenu(player, session);
    }

    private void handleChatClearLines(Player player, String text, EditorInputSession session) {
        int value = parsePositiveInt(player, text);
        if (value < 0) {
            return;
        }

        settings.getConfiguration().set("CHAT.CLEAR_LINES", value);
        settings.save();
        EditorInputSession.stop(player);
        player.sendMessage(CC.translate(messages.getString("EDITOR.CHAT.CLEAR_LINES_UPDATED", "&aClear lines updated to &f%value%&a.", true)
                .replace("%value%", String.valueOf(value))));
        openBackMenu(player, session);
    }

    private void handleChatPrefix(Player player, String text, EditorInputSession session) {
        chatManager.setPrefix(text);
        EditorInputSession.stop(player);
        player.sendMessage(CC.translate(messages.getString("EDITOR.CHAT.PREFIX_UPDATED", "&aChat prefix updated: %prefix%", true)
                .replace("%prefix%", CC.translate(text))));
        openBackMenu(player, session);
    }

    private void handleHotbarSlot(Player player, String text, EditorInputSession session) {
        int value = parseSlotInt(player, text);
        if (value < 0) {
            return;
        }

        String item = session.getContext();
        if (item == null || !hotbar.getConfiguration().contains(item)) {
            EditorInputSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.ITEM_NOT_FOUND", "&cHotbar item not found.", true)));
            openBackMenu(player, session);
            return;
        }

        hotbar.getConfiguration().set(item + ".SLOT", value);
        hotbar.save();
        reloadHotbarConfig();

        EditorInputSession.stop(player);
        player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.SLOT_UPDATED", "&aUpdated slot of &f%item% &ato &f%slot%&a.", true)
                .replace("%item%", item)
                .replace("%slot%", String.valueOf(value))));
        openBackMenu(player, session);
    }

    private void handleHotbarName(Player player, String text, EditorInputSession session) {
        String item = session.getContext();
        if (item == null || !hotbar.getConfiguration().contains(item)) {
            EditorInputSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.ITEM_NOT_FOUND", "&cHotbar item not found.", true)));
            openBackMenu(player, session);
            return;
        }

        List<String> parts = splitByComma(text);
        if (parts.isEmpty()) {
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.INVALID_NAME", "&cType a valid name.", true)));
            return;
        }

        hotbar.getConfiguration().set(item + ".NAME", String.join("\n", parts));
        hotbar.save();
        reloadHotbarConfig();

        EditorInputSession.stop(player);
        player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.NAME_UPDATED", "&aUpdated name of &f%item%&a.", true)
                .replace("%item%", item)));
        openBackMenu(player, session);
    }

    private void handleHotbarLore(Player player, String text, EditorInputSession session) {
        String item = session.getContext();
        if (item == null || !hotbar.getConfiguration().contains(item)) {
            EditorInputSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.ITEM_NOT_FOUND", "&cHotbar item not found.", true)));
            openBackMenu(player, session);
            return;
        }

        if (text.equalsIgnoreCase("clear")) {
            hotbar.getConfiguration().set(item + ".LORE", List.of());
            hotbar.save();
            reloadHotbarConfig();

            EditorInputSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.LORE_UPDATED", "&aUpdated lore of &f%item%&a.", true)
                    .replace("%item%", item)));
            openBackMenu(player, session);
            return;
        }

        List<String> lines = splitByComma(text);
        if (lines.isEmpty()) {
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.INVALID_LORE", "&cType lore text or 'clear'.", true)));
            return;
        }

        hotbar.getConfiguration().set(item + ".LORE", lines);
        hotbar.save();
        reloadHotbarConfig();

        EditorInputSession.stop(player);
        player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.LORE_UPDATED", "&aUpdated lore of &f%item%&a.", true)
                .replace("%item%", item)));
        openBackMenu(player, session);
    }

    private void handleHotbarCommand(Player player, String text, EditorInputSession session) {
        String item = session.getContext();
        if (item == null || !hotbar.getConfiguration().contains(item)) {
            EditorInputSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.ITEM_NOT_FOUND", "&cHotbar item not found.", true)));
            openBackMenu(player, session);
            return;
        }

        String value = text;
        if (text.equalsIgnoreCase("clear")) {
            value = "";
        }

        hotbar.getConfiguration().set(item + ".COMMAND", value);
        hotbar.save();
        reloadHotbarConfig();

        EditorInputSession.stop(player);
        player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.COMMAND_UPDATED", "&aUpdated command of &f%item%&a.", true)
                .replace("%item%", item)));
        openBackMenu(player, session);
    }

    private void handleHotbarHeadOwner(Player player, String text, EditorInputSession session) {
        String item = session.getContext();
        if (item == null || !hotbar.getConfiguration().contains(item)) {
            EditorInputSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.ITEM_NOT_FOUND", "&cHotbar item not found.", true)));
            openBackMenu(player, session);
            return;
        }

        if (text.equalsIgnoreCase("clear")) {
            hotbar.getConfiguration().set(item + ".HEAD_OWNER", null);
            hotbar.getConfiguration().set(item + ".HEAD_OWNER_UUID", null);
        } else {
            hotbar.getConfiguration().set(item + ".MATERIAL", "PLAYER_HEAD");
            hotbar.getConfiguration().set(item + ".HEAD_OWNER", text);
        }

        hotbar.save();
        reloadHotbarConfig();

        EditorInputSession.stop(player);
        player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.HEAD_OWNER_UPDATED", "&aUpdated head owner of &f%item%&a.", true)
                .replace("%item%", item)));
        openBackMenu(player, session);
    }

    private void handleHotbarHeadOwnerUuid(Player player, String text, EditorInputSession session) {
        String item = session.getContext();
        if (item == null || !hotbar.getConfiguration().contains(item)) {
            EditorInputSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.ITEM_NOT_FOUND", "&cHotbar item not found.", true)));
            openBackMenu(player, session);
            return;
        }

        if (text.equalsIgnoreCase("clear")) {
            hotbar.getConfiguration().set(item + ".HEAD_OWNER_UUID", null);
        } else {
            try {
                UUID uuid = UUID.fromString(text);
                hotbar.getConfiguration().set(item + ".MATERIAL", "PLAYER_HEAD");
                hotbar.getConfiguration().set(item + ".HEAD_OWNER_UUID", uuid.toString());
            } catch (IllegalArgumentException e) {
                player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.INVALID_UUID", "&cType a valid UUID.", true)));
                return;
            }
        }

        hotbar.save();
        reloadHotbarConfig();

        EditorInputSession.stop(player);
        player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.HEAD_OWNER_UUID_UPDATED", "&aUpdated head owner UUID of &f%item%&a.", true)
                .replace("%item%", item)));
        openBackMenu(player, session);
    }

    private void handleHotbarClickSound(Player player, String text, EditorInputSession session) {
        String item = session.getContext();
        if (item == null || !hotbar.getConfiguration().contains(item)) {
            EditorInputSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.ITEM_NOT_FOUND", "&cHotbar item not found.", true)));
            openBackMenu(player, session);
            return;
        }

        try {
            Sound sound = Sound.valueOf(text.toUpperCase());
            hotbar.getConfiguration().set(item + ".CLICK_SOUND.SOUND", sound.name());
        } catch (IllegalArgumentException e) {
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.INVALID_SOUND", "&cInvalid sound enum.", true)));
            return;
        }

        hotbar.save();
        reloadHotbarConfig();

        EditorInputSession.stop(player);
        player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.CLICK_SOUND.SOUND_UPDATED", "&aUpdated click sound of &f%item%&a.", true)
                .replace("%item%", item)));
        openBackMenu(player, session);
    }

    private void handleHotbarClickSoundVolume(Player player, String text, EditorInputSession session) {
        String item = session.getContext();
        if (item == null || !hotbar.getConfiguration().contains(item)) {
            EditorInputSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.ITEM_NOT_FOUND", "&cHotbar item not found.", true)));
            openBackMenu(player, session);
            return;
        }

        double volume;
        try {
            volume = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.INVALID_VOLUME", "&cType a valid decimal number.", true)));
            return;
        }

        if (volume < 0.0D || volume > 10.0D) {
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.INVALID_VOLUME_RANGE", "&cVolume must be between 0.0 and 10.0.", true)));
            return;
        }

        hotbar.getConfiguration().set(item + ".CLICK_SOUND.VOLUME", volume);
        hotbar.save();
        reloadHotbarConfig();

        EditorInputSession.stop(player);
        player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.CLICK_SOUND.VOLUME_UPDATED", "&aUpdated click sound volume of &f%item%&a.", true)
                .replace("%item%", item)));
        openBackMenu(player, session);
    }

    private void handleHotbarClickSoundPitch(Player player, String text, EditorInputSession session) {
        String item = session.getContext();
        if (item == null || !hotbar.getConfiguration().contains(item)) {
            EditorInputSession.stop(player);
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.ITEM_NOT_FOUND", "&cHotbar item not found.", true)));
            openBackMenu(player, session);
            return;
        }

        double pitch;
        try {
            pitch = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.INVALID_PITCH", "&cType a valid decimal number.", true)));
            return;
        }

        if (pitch < 0.0D || pitch > 2.0D) {
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.INVALID_PITCH_RANGE", "&cPitch must be between 0.0 and 2.0.", true)));
            return;
        }

        hotbar.getConfiguration().set(item + ".CLICK_SOUND.PITCH", pitch);
        hotbar.save();
        reloadHotbarConfig();

        EditorInputSession.stop(player);
        player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.CLICK_SOUND.PITCH_UPDATED", "&aUpdated click sound pitch of &f%item%&a.", true)
                .replace("%item%", item)));
        openBackMenu(player, session);
    }

    private int parsePositiveInt(Player player, String text) {
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate(messages.getString("EDITOR.INVALID_NUMBER", "&cType a valid positive number.", true)));
            return -1;
        }

        if (value <= 0) {
            player.sendMessage(CC.translate(messages.getString("EDITOR.INVALID_NUMBER", "&cType a valid positive number.", true)));
            return -1;
        }
        return value;
    }

    private int parseSlotInt(Player player, String text) {
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.INVALID_SLOT", "&cSlot must be between 0 and 8.", true)));
            return -1;
        }

        if (value < 0 || value > 8) {
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.INVALID_SLOT", "&cSlot must be between 0 and 8.", true)));
            return -1;
        }
        return value;
    }

    private List<String> splitByComma(String text) {
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.toList());
    }

    private void handlePvpArenaEffect(Player player, String text, EditorInputSession session) {
        if (text.equalsIgnoreCase("clear")) {
            settings.getConfiguration().set("PVP_ARENA.EFFECTS", List.of());
            settings.save();
            EditorInputSession.stop(player);
            player.sendMessage(CC.translate("&aCleared all PvP arena effects."));
            openBackMenu(player, session);
            return;
        }

        String[] parts = text.split(":");
        if (parts.length == 0) {
            player.sendMessage(CC.translate("&cUse format EFFECT or EFFECT:LEVEL. Example: SPEED:2"));
            return;
        }

        String effectName = parts[0].trim().toUpperCase();
        PotionEffectType type = PotionEffectType.getByName(effectName);
        if (type == null) {
            player.sendMessage(CC.translate("&cInvalid effect. Use Bukkit effect enum name."));
            return;
        }

        int level = 1;
        if (parts.length > 1) {
            try {
                level = Math.max(1, Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException e) {
                player.sendMessage(CC.translate("&cInvalid level. Example: SPEED:2"));
                return;
            }
        }

        String normalized = type.getName() + ":" + level;
        List<String> effects = new java.util.ArrayList<>(settings.getConfiguration().getStringList("PVP_ARENA.EFFECTS"));
        boolean removed = effects.removeIf(s -> s.equalsIgnoreCase(normalized) || s.toUpperCase().startsWith(type.getName().toUpperCase() + ":"));
        if (!removed) {
            effects.add(normalized);
            player.sendMessage(CC.translate("&aAdded PvP effect: &f" + normalized));
        } else {
            player.sendMessage(CC.translate("&eRemoved PvP effect: &f" + type.getName()));
        }
        settings.getConfiguration().set("PVP_ARENA.EFFECTS", effects);
        settings.save();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (pvpArenaKitManager.isInArenaSession(online.getUniqueId())) {
                pvpArenaKitManager.refreshArenaState(online);
            }
        }
        EditorInputSession.stop(player);
        openBackMenu(player, session);
    }

    private void openBackMenu(Player player, EditorInputSession session) {
        Bukkit.getScheduler().runTask(hub, () -> {
            if (session.getType() == EditorInputSession.Type.HOTBAR_SLOT
                    || session.getType() == EditorInputSession.Type.HOTBAR_NAME
                    || session.getType() == EditorInputSession.Type.HOTBAR_LORE
                    || session.getType() == EditorInputSession.Type.HOTBAR_COMMAND
                    || session.getType() == EditorInputSession.Type.HOTBAR_HEAD_OWNER
                    || session.getType() == EditorInputSession.Type.HOTBAR_HEAD_OWNER_UUID
                    || session.getType() == EditorInputSession.Type.HOTBAR_SOUND
                    || session.getType() == EditorInputSession.Type.HOTBAR_SOUND_VOLUME
                    || session.getType() == EditorInputSession.Type.HOTBAR_SOUND_PITCH) {
                if (session.getContext() != null && hotbar.getConfiguration().contains(session.getContext())) {
                    new HotbarItemEditorMenu(session.getContext()).openMenu(player);
                    return;
                }
                new HotbarEditorMenu().openMenu(player);
                return;
            }
            if (session.getType() == EditorInputSession.Type.PVP_ARENA_EFFECT) {
                new PvpArenaEditorMenu().openMenu(player);
                return;
            }
            new ChatEditorMenu().openMenu(player);
        });
    }

    private void reloadHotbarConfig() {
        hotbarManager.load();
        hotbarManager.reload();
    }
}
