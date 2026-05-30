package net.kryunek.hub.listeners.hotbar;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.hotbar.Hotbar;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.menus.gadgets.GadgetService;
import net.kryunek.hub.menus.gadgets.GadgetsMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.PvpArenaUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class GadgetsItemListener implements Listener {
    private final FileConfig settingsConfig = ModuleService.getFileModule().getFile(ConfigFiles.SETTINGS);

    public GadgetsItemListener(Celest hub) {
        Bukkit.getPluginManager().registerEvents(this, hub);
    }

    @EventHandler
    private void onGadgets(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (event.getItem() == null) {
            return;
        }

        Profile profile = ModuleService.getManagerModule().getProfileManager().getProfile(event.getPlayer().getUniqueId());
        if (profile != null && profile.getSelectedGadgetType() != null && !profile.getSelectedGadgetType().equalsIgnoreCase("NONE")) {
            if (!GadgetService.hasPermission(event.getPlayer(), profile.getSelectedGadgetType())
                    && !PvpArenaUtil.isInsideArena(settingsConfig, event.getPlayer().getLocation())
                    && looksLikeSelectedGadgetUse(event.getItem(), profile.getSelectedGadgetType())) {
                event.setCancelled(true);
                ModuleService.getManagerModule().getHotbarManager().setHotbar(event.getPlayer());
                event.getPlayer().sendMessage(CC.translate("&cYou no longer have permission for this gadget."));
                return;
            }

            ItemStack selectedItem = GadgetService.getItemForPlayer(profile.getSelectedGadgetType(), event.getPlayer());
            if (selectedItem != null && selectedItem.hasItemMeta() && event.getItem().hasItemMeta()
                    && selectedItem.getType() == event.getItem().getType()
                    && selectedItem.getItemMeta().getDisplayName() != null
                    && selectedItem.getItemMeta().getDisplayName().equals(event.getItem().getItemMeta().getDisplayName())) {
                event.setCancelled(true);
                GadgetService.use(event.getPlayer(), profile.getSelectedGadgetType());
                return;
            }
        }

        Hotbar hotbar = ModuleService.getManagerModule().getHotbarManager().getHotbar("GADGETS");
        if (hotbar != null && hotbar.isEnabled() && hotbar.isHotbarItem(event.getItem())) {
            event.setCancelled(true);
            ModuleService.getManagerModule().getHotbarManager().playClickSound(event.getPlayer(), hotbar);
            new GadgetsMenu().openMenu(event.getPlayer());
        }
    }

    private boolean looksLikeSelectedGadgetUse(ItemStack usedItem, String gadgetType) {
        if (usedItem == null || usedItem.getType() == Material.AIR) {
            return false;
        }
        ItemStack configured = GadgetService.getItemByType(gadgetType);
        if (configured != null && configured.getType() == usedItem.getType()) {
            return true;
        }

        Material type = usedItem.getType();
        return type == Material.ENDER_PEARL || type == Material.SNOWBALL || type == Material.FIRE_CHARGE
                || type == Material.WIND_CHARGE || type == Material.FISHING_ROD;
    }
}
