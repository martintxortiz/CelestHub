package net.kryunek.hub.support.config;

/**
 * Canonical identifiers for every config file registered by
 * {@code net.kryunek.hub.managers.module.impl.FileModule}.
 *
 * <p>These keys are passed to {@code FileModule#getFile(String)} all over the code base. Using a
 * typed constant instead of an inline string literal means a mistyped name becomes a compile error
 * instead of a silent {@code null} file at runtime, and gives a single place to see every config
 * file the plugin owns.
 *
 * <p>The constant <em>value</em> must always match the key the file is registered under in
 * {@code FileModule#onEnable}.
 */
public final class ConfigFiles {

    private ConfigFiles() {
    }

    // core/
    public static final String CONFIG = "config";
    public static final String PLAYERS = "players";
    public static final String MESSAGES = "messages";

    // features/
    public static final String HOTBAR = "hotbar";
    public static final String SETTINGS = "settings";
    public static final String SCOREBOARD = "scoreboard";
    public static final String QUEUE = "queue";
    public static final String LOTTERY = "lottery";
    public static final String GADGETS = "gadgets";
    public static final String PARTICLE = "particle";
    public static final String OUTFIT = "outfit";
    public static final String TAB = "tab";
    public static final String JUKEBOX = "jukebox";

    // menus/
    public static final String COMMON_MENU = "common_menu";
    public static final String ADMIN_MENUS = "admin_menus";
    public static final String SERVER_SELECTOR = "server_selector";
    public static final String HUB_SELECTOR = "hub_selector";
    public static final String EDITOR_MENUS = "editor_menus";
    public static final String CELEST_EDITOR = "celest_editor";
    public static final String SETTINGS_MENU = "settings_menu";
}
