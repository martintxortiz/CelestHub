package net.kryunek.hub.menus.jukebox;

import net.kryunek.hub.support.config.ConfigFiles;
import com.google.common.collect.Maps;
import net.kryunek.hub.managers.jukebox.JukeboxManager;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.menus.settings.SettingsMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import net.kryunek.hub.utils.menu.buttons.BackButton;
import net.kryunek.hub.utils.menu.pagination.PageButton;
import net.kryunek.hub.utils.menu.pagination.PaginatedMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class JukeboxMenu extends PaginatedMenu {

    private final JukeboxManager jukeboxManager;

    public JukeboxMenu() {
        this.jukeboxManager = ModuleService.getManagerModule().getJukeboxManager();
    }

    @Override
    public boolean isPlaceholder() {
        return false;
    }

    @Override
    public boolean isUpdateAfterClick() {
        return true;
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public int getMaxItemsPerPage(Player player) {
        return 18;
    }

    @Override
    public String getPrePaginatedTitle(Player player) {
        return CC.translate("&8Jukebox");
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = Maps.newHashMap();
        int i = 8;
        for (String trackName : jukeboxManager.getTrackNames()) {
            buttons.put(i++, new JukeboxTrackButton(trackName));
        }
        return buttons;
    }

    @Override
    public Map<Integer, Button> getGlobalButtons(Player player) {
        Map<Integer, Button> buttons = Maps.newHashMap();
        int row = getSize() / 9 - 1;
        buttons.put(getSlot(0, row), new PageButton(-1, this));
        buttons.put(getSlot(1, row), new VolumeButton());
        buttons.put(getSlot(2, row), new ToggleButton());
        buttons.put(getSlot(3, row), new PauseResumeButton());
        buttons.put(getSlot(4, row), new BackButton(new SettingsMenu()));
        buttons.put(getSlot(5, row), new NextButton());
        buttons.put(getSlot(6, row), new StopButton());
        buttons.put(getSlot(8, row), new PageButton(1, this));
        return buttons;
    }

    private class JukeboxTrackButton extends Button {
        private final String trackName;

        private JukeboxTrackButton(String trackName) {
            this.trackName = trackName;
        }

        @Override
        public ItemStack getButtonItem(Player player) {
            String now = jukeboxManager.getNowPlaying(player.getUniqueId());
            boolean playingThis = now != null && now.equalsIgnoreCase(trackName);
            return new ItemBuilder(playingThis ? Material.MUSIC_DISC_PIGSTEP : Material.MUSIC_DISC_13)
                    .name(CC.translate("&d" + trackName))
                    .lore(List.of(
                            CC.translate("&7Status: " + (playingThis ? "&aNow Playing" : "&7Ready")),
                            "",
                            CC.translate("&eClick to play this track")
                    ))
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (!jukeboxManager.play(player, trackName)) {
                playFail(player);
                player.sendMessage(CC.translate("&cCould not play that track."));
                return;
            }
            playSuccess(player);
            player.sendMessage(CC.translate("&aNow playing &f" + trackName + "&a."));
        }
    }

    private class StopButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.REDSTONE_BLOCK)
                    .name("&cStop Playback")
                    .lore(List.of("&7Stop current song and queue"))
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            jukeboxManager.stop(player);
            playNeutral(player);
            player.sendMessage(CC.translate("&eTrack stopped."));
        }
    }

    private class PauseResumeButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            boolean hasCurrent = jukeboxManager.getNowPlaying(player.getUniqueId()) != null;
            return new ItemBuilder(hasCurrent ? Material.CLOCK : Material.AMETHYST_SHARD)
                    .name(hasCurrent ? "&6Pause Track" : "&aResume Track")
                    .lore(List.of("&7Click to pause/resume music"))
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (!jukeboxManager.isPauseEnabled()) {
                playFail(player);
                player.sendMessage(CC.translate("&cPause is disabled."));
                return;
            }
            if (jukeboxManager.getNowPlaying(player.getUniqueId()) != null) {
                if (jukeboxManager.pause(player)) {
                    playNeutral(player);
                    player.sendMessage(CC.translate("&eTrack paused."));
                    return;
                }
            } else if (jukeboxManager.resume(player)) {
                playSuccess(player);
                player.sendMessage(CC.translate("&aTrack resumed."));
                return;
            }
            playFail(player);
            player.sendMessage(CC.translate("&cNothing to pause/resume."));
        }
    }

    private class NextButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.MUSIC_DISC_5)
                    .name("&aNext Track")
                    .lore(List.of("&7Skip to the next song"))
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (!jukeboxManager.next(player)) {
                playFail(player);
                player.sendMessage(CC.translate("&cNo tracks configured."));
                return;
            }
            playSuccess(player);
            player.sendMessage(CC.translate("&aPlaying next track."));
        }
    }

    private class ToggleButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            Profile profile = ModuleService.getManagerModule().getProfileManager().getProfile(player.getUniqueId());
            boolean enabled = profile != null && profile.isJukeboxEnabled();
            return new ItemBuilder(enabled ? Material.JUKEBOX : Material.NOTE_BLOCK)
                    .name(enabled ? "&aJukebox Enabled" : "&cJukebox Disabled")
                    .lore(List.of("&7Click to toggle jukebox"))
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            boolean enabled = jukeboxManager.toggle(player);
            if (enabled) {
                playSuccess(player);
                player.sendMessage(CC.translate("&aJukebox enabled."));
            } else {
                playNeutral(player);
                player.sendMessage(CC.translate("&cJukebox disabled."));
            }
        }
    }

    private class VolumeButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.NOTE_BLOCK)
                    .name("&aVolume")
                    .lore(List.of(
                            "&7Current: &f" + String.format(java.util.Locale.US, "%.2f", jukeboxManager.getVolume(player)),
                            "",
                            "&eLeft click: +volume",
                            "&eRight click: -volume"
                    ))
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            double step = ModuleService.getFileModule().getFile(ConfigFiles.JUKEBOX).getConfiguration().getDouble("JUKEBOX.CONTROLS.VOLUME_STEP", 0.1D);
            if (clickType.isRightClick()) {
                jukeboxManager.setVolume(player, jukeboxManager.getVolume(player) - step);
                playNeutral(player);
            } else {
                jukeboxManager.setVolume(player, jukeboxManager.getVolume(player) + step);
                playSuccess(player);
            }
            player.sendMessage(CC.translate("&aVolume: &f" + String.format(java.util.Locale.US, "%.2f", jukeboxManager.getVolume(player))));
        }
    }
}
