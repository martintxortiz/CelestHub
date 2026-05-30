package net.kryunek.hub.menus.editor.pvparena;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.support.config.ConfigSupport;
import lombok.RequiredArgsConstructor;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class PvpArenaEditorButton extends Button {

    private final Action action;
    private final FileConfig settings = ModuleService.getFileModule().getFile(ConfigFiles.SETTINGS);
    private final FileConfig menus = ModuleService.getFileModule().getFile(ConfigFiles.EDITOR_MENUS);

    @Override
    public ItemStack getButtonItem(Player player) {
        String basePath = "PVP_ARENA_EDITOR.BUTTONS." + action.name() + ".";
        Material material = ConfigSupport.getMaterial(menus.getConfiguration(), basePath + "MATERIAL", Material.STONE, Bukkit.getLogger());
        String name = menus.getConfiguration().getString(basePath + "NAME", "&7Button");
        List<String> lore = new ArrayList<>(menus.getConfiguration().getStringList(basePath + "LORE"));

        if (action == Action.TOGGLE) {
            lore.add("");
            lore.add(CC.translate("&7Current: " + (settings.getBoolean("PVP_ARENA.ENABLED") ? "&aENABLED" : "&cDISABLED")));
        } else if (action == Action.SET_WORLD) {
            lore.add("");
            lore.add(CC.translate("&7Current: &f" + settings.getConfiguration().getString("PVP_ARENA.WORLD", "world")));
        } else if (action == Action.SET_POS1 || action == Action.SET_POS2) {
            String key = action == Action.SET_POS1 ? "POS1" : "POS2";
            lore.add("");
            lore.add(CC.translate("&7Current: &f"
                    + settings.getDouble("PVP_ARENA." + key + ".X") + ", "
                    + settings.getDouble("PVP_ARENA." + key + ".Y") + ", "
                    + settings.getDouble("PVP_ARENA." + key + ".Z")));
        } else if (action == Action.POS_MODE) {
            boolean active = ModuleService.getManagerModule().getPvpArenaSelectionManager().isSelectionMode(player);
            lore.add("");
            lore.add(CC.translate("&7Current: " + (active ? "&aACTIVE" : "&cINACTIVE")));
        } else if (action == Action.EFFECTS) {
            int count = settings.getConfiguration().getStringList("PVP_ARENA.EFFECTS").size();
            lore.add("");
            lore.add(CC.translate("&7Current effects: &f" + count));
        } else if (action == Action.PERIMETER_BLOCKS_TOGGLE) {
            lore.add("");
            lore.add(CC.translate("&7Current: " + (settings.getConfiguration().getBoolean("PVP_ARENA.PERIMETER_BLOCKS.ALLOW", false) ? "&aENABLED" : "&cDISABLED")));
        } else if (action == Action.PERIMETER_BLOCKS_DURATION) {
            int seconds = Math.max(1, settings.getConfiguration().getInt("PVP_ARENA.PERIMETER_BLOCKS.DISAPPEAR_SECONDS", 8));
            lore.add("");
            lore.add(CC.translate("&7Current: &f" + seconds + "s"));
            lore.add(CC.translate("&aLeft click: +1s"));
            lore.add(CC.translate("&cRight click: -1s"));
        }

        return new ItemBuilder(material).name(CC.translate(name)).lore(CC.translate(lore)).build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        switch (action) {
            case TOGGLE -> {
                boolean next = !settings.getBoolean("PVP_ARENA.ENABLED");
                settings.getConfiguration().set("PVP_ARENA.ENABLED", next);
                settings.save();
                if (!next) {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        ModuleService.getManagerModule().getPvpArenaKitManager().syncArenaState(online, false);
                    }
                }
                player.sendMessage(CC.translate(next ? "&aPvP arena enabled." : "&cPvP arena disabled."));
                playSuccess(player);
            }
            case SET_WORLD -> {
                settings.getConfiguration().set("PVP_ARENA.WORLD", player.getWorld().getName());
                settings.save();
                player.sendMessage(CC.translate("&aArena world set to &f" + player.getWorld().getName() + "&a."));
                playSuccess(player);
            }
            case SET_POS1 -> {
                setPos(player.getLocation(), "POS1");
                player.sendMessage(CC.translate("&aPOS1 updated."));
                playSuccess(player);
            }
            case SET_POS2 -> {
                setPos(player.getLocation(), "POS2");
                player.sendMessage(CC.translate("&aPOS2 updated."));
                playSuccess(player);
            }
            case POS_MODE -> {
                ModuleService.getManagerModule().getPvpArenaSelectionManager().toggleSelectionMode(player);
                playSuccess(player);
            }
            case EDIT_DEFAULT_KIT -> {
                ModuleService.getManagerModule().getPvpArenaKitManager().openDefaultKitEditor(player);
                playSuccess(player);
            }
            case EFFECTS -> {
                new PvpArenaEffectsMenu().openMenu(player);
                playSuccess(player);
            }
            case PERIMETER_BLOCKS_TOGGLE -> {
                boolean next = !settings.getConfiguration().getBoolean("PVP_ARENA.PERIMETER_BLOCKS.ALLOW", false);
                settings.getConfiguration().set("PVP_ARENA.PERIMETER_BLOCKS.ALLOW", next);
                settings.save();
                player.sendMessage(CC.translate(next ? "&aPvP arena temporary blocks enabled." : "&cPvP arena temporary blocks disabled."));
                playSuccess(player);
            }
            case PERIMETER_BLOCKS_DURATION -> {
                int current = Math.max(1, settings.getConfiguration().getInt("PVP_ARENA.PERIMETER_BLOCKS.DISAPPEAR_SECONDS", 8));
                int next = clickType.isRightClick() ? current - 1 : current + 1;
                next = Math.max(1, Math.min(120, next));
                settings.getConfiguration().set("PVP_ARENA.PERIMETER_BLOCKS.DISAPPEAR_SECONDS", next);
                settings.save();
                player.sendMessage(CC.translate("&aTemporary blocks disappear time set to &f" + next + "s&a."));
                playSuccess(player);
            }
        }
    }

    private void setPos(org.bukkit.Location location, String key) {
        settings.getConfiguration().set("PVP_ARENA." + key + ".X", location.getX());
        settings.getConfiguration().set("PVP_ARENA." + key + ".Y", location.getY());
        settings.getConfiguration().set("PVP_ARENA." + key + ".Z", location.getZ());
        settings.save();
    }

    public enum Action {
        TOGGLE,
        SET_WORLD,
        SET_POS1,
        SET_POS2,
        POS_MODE,
        EDIT_DEFAULT_KIT,
        EFFECTS,
        PERIMETER_BLOCKS_TOGGLE,
        PERIMETER_BLOCKS_DURATION
    }
}
