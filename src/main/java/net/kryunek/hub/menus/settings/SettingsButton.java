package net.kryunek.hub.menus.settings;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.hook.ScoreboardHook;
import net.kryunek.hub.hook.TablistHook;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.managers.player.ProfileManager;
import net.kryunek.hub.menus.gadgets.GadgetsMenu;
import net.kryunek.hub.menus.jukebox.JukeboxMenu;
import net.kryunek.hub.menus.outfit.OutfitMenu;
import net.kryunek.hub.menus.particles.TrailParticlesMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.PlayerUtil;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SettingsButton extends Button {
    private static final String[] TIME_STATES = new String[]{"DAY", "SUNSET", "NIGHT", "MIDNIGHT"};
    private final String name;
    private final Material material;
    private final int durability;
    private final List<String> lore;
    private final String command;
    private final String type;
    private final FileConfig settings_menuConfig = ModuleService.getFileModule().getFile(ConfigFiles.SETTINGS_MENU);
    private final ProfileManager profileManager;

    @Override
    public ItemStack getButtonItem(Player player) {
        ArrayList<String> arrayList = new ArrayList<String>(this.lore);

        Profile profile = this.profileManager.getProfile(player.getUniqueId());

        String string = CC.translate(settings_menuConfig.getString("ENABLED"));
        String string2 = CC.translate(settings_menuConfig.getString("DISABLED"));
        String string3 = CC.translate(settings_menuConfig.getString("UNSELECTED"));
        String string4 = CC.translate(settings_menuConfig.getString("NO_PERMISSION"));
        String string5 = CC.translate(settings_menuConfig.getString("TOGGLE"));
        if (this.type.equalsIgnoreCase("scoreboardToggled")) {
            arrayList.add((profile.isShowScoreboard() ? string : string3) + settings_menuConfig.getString("SETTINGS.TOGGLE_SCOREBOARD.SETTING_ENABLED"));
            arrayList.add((!profile.isShowScoreboard() ? string2 : string3) + settings_menuConfig.getString("SETTINGS.TOGGLE_SCOREBOARD.SETTING_DISABLED"));
            arrayList.add("");
            arrayList.add(string5);
        } else if (this.type.equalsIgnoreCase("flyJoin")) {
            if (!player.hasPermission("celest.command.fly")) {
                arrayList.add(string4);
            } else {
                arrayList.add((profile.isFlyOnJoin() ? string : string3) + settings_menuConfig.getString("SETTINGS.FLY_JOIN.SETTING_ENABLED"));
                arrayList.add((!profile.isFlyOnJoin() ? string2 : string3) + settings_menuConfig.getString("SETTINGS.FLY_JOIN.SETTING_DISABLED"));
                arrayList.add("");
                arrayList.add(string5);
            }
        } else if (this.type.equalsIgnoreCase("tablistToggled")) {
            arrayList.add((profile.isShowTablist() ? string : string3) + settings_menuConfig.getString("SETTINGS.TOGGLE_TABLIST.SETTING_ENABLED"));
            arrayList.add((!profile.isShowTablist() ? string2 : string3) + settings_menuConfig.getString("SETTINGS.TOGGLE_TABLIST.SETTING_DISABLED"));
            arrayList.add("");
            arrayList.add(string5);
        } else if (this.type.equalsIgnoreCase("playerBuildMode")) {
            arrayList.add((profile.isBuildModeEnabled() ? string : string3) + settings_menuConfig.getString("SETTINGS.BUILD_MODE.SETTING_ENABLED"));
            arrayList.add((!profile.isBuildModeEnabled() ? string2 : string3) + settings_menuConfig.getString("SETTINGS.BUILD_MODE.SETTING_DISABLED"));
            arrayList.add("");
            arrayList.add(string5);
        } else if (this.type.equalsIgnoreCase("jukebox")) {
            arrayList.add((profile.isJukeboxEnabled() ? string : string3) + settings_menuConfig.getString("SETTINGS.JUKEBOX.SETTING_ENABLED"));
            arrayList.add((!profile.isJukeboxEnabled() ? string2 : string3) + settings_menuConfig.getString("SETTINGS.JUKEBOX.SETTING_DISABLED"));
            arrayList.add("");
            arrayList.add(string5);
        } else if (this.type.equalsIgnoreCase("particles")) {
            arrayList.add(string5);
        }
        else if (this.type.equalsIgnoreCase("outfit")) {
            arrayList.add(string5);
        } else if (this.type.equalsIgnoreCase("gadgets")) {
            arrayList.add(string5);
        } else if (this.type.equalsIgnoreCase("pvpArenaKitEditor")) {
            arrayList.add(string5);
        } else if (this.type.equalsIgnoreCase("timePreference")) {
            String current = normalizeTimePreference(profile.getTimePreference());
            arrayList.add((current.equals("DAY") ? string : string3) + settings_menuConfig.getString("SETTINGS.TIME_PREFERENCE.SETTING_DAY"));
            arrayList.add((current.equals("SUNSET") ? string : string3) + settings_menuConfig.getString("SETTINGS.TIME_PREFERENCE.SETTING_SUNSET"));
            arrayList.add((current.equals("NIGHT") ? string : string3) + settings_menuConfig.getString("SETTINGS.TIME_PREFERENCE.SETTING_NIGHT"));
            arrayList.add((current.equals("MIDNIGHT") ? string : string3) + settings_menuConfig.getString("SETTINGS.TIME_PREFERENCE.SETTING_MIDNIGHT"));
            arrayList.add("");
            arrayList.add(string5);
        }
        return new ItemBuilder(this.material).name(CC.translate(settings_menuConfig.getString("NAME").replace("%setting_name%", this.name))).amount(1).lore(arrayList).durability(this.durability).build();
    }

    @Override
    public void clicked(Player player, int n, ClickType clickType, int n2) {
        Profile profile = this.profileManager.getProfile(player.getUniqueId());
        switch (this.type) {
            case "scoreboardToggled": {

                profile.setShowScoreboard(!profile.isShowScoreboard());
                if (profile.isShowScoreboard()) {
                    if (ScoreboardHook.getScoreboard() != null) {
                        ScoreboardHook.getScoreboard().showBoard(player);
                    }
                    player.sendMessage(CC.translate(settings_menuConfig.getString("settings.scoreboard-enabled")));
                } else {
                    if (ScoreboardHook.getScoreboard() != null) {
                        ScoreboardHook.getScoreboard().hideBoard(player);
                    }
                    player.sendMessage(CC.translate(settings_menuConfig.getString("settings.scoreboard-disabled")));
                }
                SettingsButton.playSuccess(player);
                break;
            }
            case "flyJoin": {
                if (player.hasPermission("celest.command.fly")) {
                    profile.setFlyOnJoin(!profile.isFlyOnJoin());
                    if (profile.isFlyOnJoin()) {
                        player.sendMessage(CC.translate(settings_menuConfig.getString("settings.fly-enabled")));
                        PlayerUtil.applyHubFlyState(player, true, true);
                    } else {
                        player.sendMessage(CC.translate(settings_menuConfig.getString("settings.fly-disabled")));
                        PlayerUtil.applyHubFlyState(player, false, false);
                    }
                    SettingsButton.playSuccess(player);
                } else {
                    player.sendMessage(CC.translate(settings_menuConfig.getString("settings.no-permission")));
                    SettingsButton.playFail(player);
                }
                break;
            }
            case "tablistToggled": {
                profile.setShowTablist(!profile.isShowTablist());
                if (profile.isShowTablist()) {
                    player.sendMessage(CC.translate(settings_menuConfig.getString("settings.tablist-enabled")));
                } else {
                    if (TablistHook.getTablistManager() != null) {
                        TablistHook.getTablistManager().clearTablist(player);
                    }
                    player.sendMessage(CC.translate(settings_menuConfig.getString("settings.tablist-disabled")));
                }
                SettingsButton.playSuccess(player);
                break;
            }
            case "playerBuildMode": {
                if (!profile.isBuildModeEnabled()) {
                    profile.setBuildModeEnabled(true);
                    player.sendMessage(CC.translate(settings_menuConfig.getString("settings.buildmode-enabled")));
                    PlayerUtil.clear(player, true, true);
                    player.setGameMode(GameMode.CREATIVE);
                    player.setAllowFlight(true);
                    player.setFlying(false);
                    close(player);
                } else {
                    profile.setBuildModeEnabled(false);
                    player.sendMessage(CC.translate(settings_menuConfig.getString("settings.buildmode-disabled")));
                    ModuleService.getManagerModule().getHotbarManager().setHotbar(player);
                    player.setGameMode(GameMode.SURVIVAL);
                    ModuleService.getManagerModule().getOutfitManager().applySelectedOutfit(player, profile);
                    close(player);
                    if(profile.isFlyOnJoin()) {
                        PlayerUtil.applyHubFlyState(player, true, false);
                    } else {
                        PlayerUtil.applyHubFlyState(player, false, false);
                    }

                }
                SettingsButton.playSuccess(player);
                break;
            }
            case "jukebox": {
                if (ModuleService.getManagerModule().getJukeboxManager() == null || !ModuleService.getManagerModule().getJukeboxManager().isEnabled()) {
                    player.sendMessage(CC.translate("&cJukebox is disabled."));
                    playFail(player);
                    break;
                }
                if (clickType.isLeftClick()) {
                    profile.setJukeboxEnabled(!profile.isJukeboxEnabled());
                    if (profile.isJukeboxEnabled()) {
                        ModuleService.getManagerModule().getJukeboxManager().startOrResume(player);
                        player.sendMessage(CC.translate(settings_menuConfig.getString("settings.jukebox-enabled")));
                    } else {
                        ModuleService.getManagerModule().getJukeboxManager().stop(player);
                        player.sendMessage(CC.translate(settings_menuConfig.getString("settings.jukebox-disabled")));
                    }
                    playSuccess(player);
                } else {
                    new JukeboxMenu().openMenu(player);
                    playSuccess(player);
                }
                break;
            }
            case "particles": {
                new TrailParticlesMenu().openMenu(player);
                playSuccess(player);
                break;
            }
            case "outfit": {
                new OutfitMenu().openMenu(player);
                playSuccess(player);
                break;
            }
            case "gadgets": {
                new GadgetsMenu().openMenu(player);
                playSuccess(player);
                break;
            }
            case "pvpArenaKitEditor": {
                ModuleService.getManagerModule().getPvpArenaKitManager().openKitEditor(player);
                playSuccess(player);
                break;
            }
            case "timePreference": {
                String current = normalizeTimePreference(profile.getTimePreference());
                int idx = 0;
                for (int i = 0; i < TIME_STATES.length; i++) {
                    if (TIME_STATES[i].equals(current)) {
                        idx = i;
                        break;
                    }
                }
                String next = TIME_STATES[(idx + 1) % TIME_STATES.length];
                profile.setTimePreference(next);
                applyTimePreference(player, next);
                player.sendMessage(CC.translate(settings_menuConfig.getString("settings.time-preference-updated")
                        .replace("%state%", getTimeStateDisplay(next))));
                playSuccess(player);
                break;
            }

            default: {
                player.performCommand(this.command);
                SettingsButton.playSuccess(player);
            }
        }
    }

    public SettingsButton(String string, Material material, int n, List<String> list, String string2, String string3) {
        this.name = string;
        this.material = material;
        this.durability = n;
        this.lore = list;
        this.command = string2;
        this.type = string3;
        this.profileManager = ModuleService.getManagerModule().getProfileManager();

    }

    private static String normalizeTimePreference(String state) {
        if (state == null) {
            return "DAY";
        }
        String upper = state.toUpperCase();
        for (String value : TIME_STATES) {
            if (value.equals(upper)) {
                return value;
            }
        }
        return "DAY";
    }

    public static void applyTimePreference(Player player, String state) {
        String normalized = normalizeTimePreference(state);
        switch (normalized) {
            case "DAY" -> player.setPlayerTime(1000L, false);
            case "SUNSET" -> player.setPlayerTime(12000L, false);
            case "NIGHT" -> player.setPlayerTime(14000L, false);
            case "MIDNIGHT" -> player.setPlayerTime(18000L, false);
            default -> player.setPlayerTime(1000L, false);
        }
    }

    private String getTimeStateDisplay(String state) {
        return switch (normalizeTimePreference(state)) {
            case "DAY" -> "&eDay";
            case "SUNSET" -> "&6Sunset";
            case "NIGHT" -> "&9Night";
            case "MIDNIGHT" -> "&5Midnight";
            default -> "&eDay";
        };
    }
}
