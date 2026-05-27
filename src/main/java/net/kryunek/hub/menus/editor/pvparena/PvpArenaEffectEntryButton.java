package net.kryunek.hub.menus.editor.pvparena;

import lombok.RequiredArgsConstructor;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.Material;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class PvpArenaEffectEntryButton extends Button {

    private final PotionEffectType type;
    private final FileConfig settings = ModuleService.getFileModule().getFile("settings");

    @Override
    public ItemStack getButtonItem(Player player) {
        int level = getCurrentLevel();
        String status = level > 0 ? "&aLevel " + level : "&cDisabled";

        ItemStack item = new ItemBuilder(Material.POTION)
                .name(CC.translate("&b" + type.getName()))
                .lore(List.of(
                        CC.translate("&7Current: " + status),
                        "",
                        CC.translate("&eLeft click: +1 level"),
                        CC.translate("&eRight click: -1 level"),
                        CC.translate("&eMiddle click: disable")
                ))
                .build();

        PotionMeta meta = (PotionMeta) item.getItemMeta();
        if (meta != null) {
            int amplifier = Math.max(0, level > 0 ? level - 1 : 0);
            meta.addCustomEffect(new PotionEffect(type, 20 * 30, amplifier), true);
            Color color = type.getColor();
            if (color != null) {
                meta.setColor(color);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        int level = getCurrentLevel();
        if (clickType == ClickType.LEFT) {
            level = Math.min(5, level + 1);
        } else if (clickType == ClickType.RIGHT) {
            level = Math.max(0, level - 1);
        } else if (clickType == ClickType.MIDDLE) {
            level = 0;
        } else {
            return;
        }

        List<String> effects = new ArrayList<>(settings.getConfiguration().getStringList("PVP_ARENA.EFFECTS"));
        effects.removeIf(s -> s != null && s.toUpperCase().startsWith(type.getName().toUpperCase() + ":"));
        if (level > 0) {
            effects.add(type.getName().toUpperCase() + ":" + level);
        }
        settings.getConfiguration().set("PVP_ARENA.EFFECTS", effects);
        settings.save();

        for (Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (ModuleService.getManagerModule().getPvpArenaKitManager().isInArenaSession(online.getUniqueId())) {
                ModuleService.getManagerModule().getPvpArenaKitManager().refreshArenaState(online);
            }
        }
        playSuccess(player);
    }

    private int getCurrentLevel() {
        for (String entry : settings.getConfiguration().getStringList("PVP_ARENA.EFFECTS")) {
            if (entry == null) continue;
            String[] parts = entry.split(":");
            if (parts.length < 2) continue;
            if (!parts[0].equalsIgnoreCase(type.getName())) continue;
            try {
                return Math.max(0, Integer.parseInt(parts[1]));
            } catch (NumberFormatException ex) {
                return 0;
            }
        }
        return 0;
    }
}
