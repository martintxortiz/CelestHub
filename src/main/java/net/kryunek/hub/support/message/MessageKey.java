package net.kryunek.hub.support.message;

/** Typed catalogue of reusable message keys, each pairing a config path with a built-in fallback. */
public enum MessageKey {
    COMMAND_NO_PERMISSION("COMMAND.NO_PERMISSION", "&cNo permission."),
    COMMAND_IN_GAME_ONLY("COMMAND.IN_GAME_ONLY", "&cThis command can only be executed in game."),
    PROFILE_NOT_LOADED("PROFILE.NOT_LOADED", "&cFailed to load your profile, please join again."),
    CHAT_TOGGLED_MUTE("CHAT.MESSAGES.TOGGLED_MUTE", "&eChat is now %state%&e."),
    CHAT_SET_SLOW("CHAT.MESSAGES.SET_SLOW", "&eChat slow mode set to &f%time%s&e."),
    CHAT_DISABLED_SLOW("CHAT.MESSAGES.DISABLED_SLOW", "&eChat slow mode disabled."),
    CHAT_CLEARED("CHAT.MESSAGES.CLEARED", "&eChat was cleared by &f%player%&e.");

    private final String path;
    private final String fallback;

    MessageKey(String path, String fallback) {
        this.path = path;
        this.fallback = fallback;
    }

    public String path() {
        return path;
    }

    public String fallback() {
        return fallback;
    }
}
