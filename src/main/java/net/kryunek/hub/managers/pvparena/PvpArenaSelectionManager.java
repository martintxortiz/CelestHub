package net.kryunek.hub.managers.pvparena;

import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Wand-driven POS1/POS2 selection mode for defining the PvP arena region in the injected settings config. */
public class PvpArenaSelectionManager {

    private static final String WAND_NAME = CC.translate("&ePvP Arena Selector");

    private final FileConfig settings;
    private final Set<UUID> selectionMode = new HashSet<>();

    public PvpArenaSelectionManager(FileConfig settings) {
        this.settings = settings;
    }

    public boolean isSelectionMode(Player player) {
        return selectionMode.contains(player.getUniqueId());
    }

    public void toggleSelectionMode(Player player) {
        UUID uuid = player.getUniqueId();
        if (selectionMode.contains(uuid)) {
            selectionMode.remove(uuid);
            player.sendMessage(CC.translate("&cPvP position mode disabled."));
            return;
        }

        selectionMode.add(uuid);
        player.getInventory().addItem(new ItemBuilder(Material.GOLDEN_AXE).name(WAND_NAME).lore(
                java.util.List.of(
                        CC.translate("&7Left click: set &fPOS1"),
                        CC.translate("&7Right click: set &fPOS2"),
                        CC.translate("&7Chat: type &fpos1 &7or &fpos2")
                )
        ).build());
        player.sendMessage(CC.translate("&aPvP position mode enabled. Use selector or type pos1/pos2 in chat."));
    }

    public boolean isSelector(ItemStack item) {
        return item != null
                && item.getType() == Material.GOLDEN_AXE
                && item.hasItemMeta()
                && item.getItemMeta().hasDisplayName()
                && WAND_NAME.equals(item.getItemMeta().getDisplayName());
    }

    public void setPosFromPlayer(Player player, String key) {
        settings.getConfiguration().set("PVP_ARENA.WORLD", player.getWorld().getName());
        settings.getConfiguration().set("PVP_ARENA." + key + ".X", player.getLocation().getX());
        settings.getConfiguration().set("PVP_ARENA." + key + ".Y", player.getLocation().getY());
        settings.getConfiguration().set("PVP_ARENA." + key + ".Z", player.getLocation().getZ());
        settings.save();
        player.sendMessage(CC.translate("&a" + key + " updated at your current location."));
    }

    public void clear(UUID uuid) {
        selectionMode.remove(uuid);
    }
}
