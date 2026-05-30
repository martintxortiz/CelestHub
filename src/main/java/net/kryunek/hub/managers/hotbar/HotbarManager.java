package net.kryunek.hub.managers.hotbar;

import com.google.common.collect.Maps;
import lombok.Getter;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.support.config.ConfigSupport;
import net.kryunek.hub.menus.gadgets.GadgetService;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Loads the configurable hub hotbar items from the injected hotbar config and applies them to
 * players, including the gadget and lottery-join slots.
 */
public class HotbarManager {

    @Getter
    private final Map<String, Hotbar> hotbars;
    private final FileConfig hotbarConfig;

    public void load() {
        this.hotbars.clear();
        ConfigurationSection section = this.hotbarConfig.getConfiguration();
        if (section == null) {
            return;
        }

        for (String s : section.getKeys(false)) {
            if (!section.contains(s + ".MATERIAL")) {
                continue;
            }
            Hotbar hotbar = new Hotbar(s);
            hotbar.setEnabled(section.getBoolean(s + ".ENABLED"));

            Material material = ConfigSupport.getMaterial(section, s + ".MATERIAL", Material.STONE, Bukkit.getLogger());

            ItemBuilder builder = new ItemBuilder(material)
                    .data(section.getInt(s + ".DATA"))
                    .name(section.getString(s + ".NAME"))
                    .lore(section.getStringList(s + ".LORE"))
                    .amount(section.getInt(s + ".AMOUNT"));

            String headOwnerUuid = section.getString(s + ".HEAD_OWNER_UUID", "");
            String headOwner = section.getString(s + ".HEAD_OWNER", "");
            if (material == Material.PLAYER_HEAD) {
                if (headOwnerUuid != null && !headOwnerUuid.isBlank()) {
                    try {
                        builder.owner(UUID.fromString(headOwnerUuid));
                    } catch (IllegalArgumentException invalidUuid) {
                        if (headOwner != null && !headOwner.isBlank()) {
                            builder.owner(headOwner);
                        }
                    }
                } else if (headOwner != null && !headOwner.isBlank()) {
                    builder.owner(headOwner);
                }
            }

            ItemStack stack = builder.build();
            hotbar.setItem(stack);
            hotbar.setSlot(section.getInt(s + ".SLOT"));
            hotbar.setCommand(section.getString(s + ".COMMAND", ""));
            this.hotbars.put(s, hotbar);
        }
    }

    public HotbarManager(FileConfig hotbarConfig) {
        this.hotbars = Maps.newHashMap();
        this.hotbarConfig = hotbarConfig;
        ensureDefaults();
    }

    public Hotbar getHotbar(String hotbar) {
        return this.hotbars.get(hotbar);
    }

    public void setHotbar(Player player) {
        PlayerUtil.clear(player, true, true);
        for (Hotbar hotbar : this.hotbars.values()) {
            if ("GADGETS".equalsIgnoreCase(hotbar.getName())) {
                continue;
            }
            if ("LOTTERY_JOIN".equalsIgnoreCase(hotbar.getName())) {
                continue;
            }
            if (hotbar.isEnabled()) {
                player.getInventory().setItem(hotbar.getSlot(), hotbar.getItem());
            }
        }

        Hotbar gadgetsHotbar = this.hotbars.get("GADGETS");
        if (gadgetsHotbar == null || !gadgetsHotbar.isEnabled()) {
            return;
        }

        Profile profile = ModuleService.getManagerModule().getProfileManager().getProfile(player.getUniqueId());
        if (profile == null || profile.getSelectedGadgetType() == null || profile.getSelectedGadgetType().equalsIgnoreCase("NONE")) {
            return;
        }

        ItemStack selectedGadgetItem = GadgetService.getItemForPlayer(profile.getSelectedGadgetType(), player);
        if (selectedGadgetItem != null) {
            player.getInventory().setItem(gadgetsHotbar.getSlot(), selectedGadgetItem);
        }
    }

    public void reload() {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            this.setHotbar(player);
            if (ModuleService.getManagerModule().getLotteryManager() != null) {
                ModuleService.getManagerModule().getLotteryManager().handleJoin(player);
            }
        }
    }

    public void playClickSound(Player player, Hotbar hotbar) {
        if (hotbar == null) {
            return;
        }

        String base = hotbar.getName() + ".CLICK_SOUND";
        if (!hotbarConfig.getConfiguration().getBoolean(base + ".ENABLED", false)) {
            return;
        }

        float volume = (float) hotbarConfig.getConfiguration().getDouble(base + ".VOLUME", 1.0);
        float pitch = (float) hotbarConfig.getConfiguration().getDouble(base + ".PITCH", 1.0);
        Sound sound = ConfigSupport.getSound(hotbarConfig.getConfiguration(), base + ".SOUND", Sound.UI_BUTTON_CLICK, Bukkit.getLogger());
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private void ensureDefaults() {
        for (String key : hotbarConfig.getConfiguration().getKeys(false)) {
            if (!hotbarConfig.getConfiguration().contains(key + ".MATERIAL")) {
                continue;
            }
            if (!hotbarConfig.getConfiguration().contains(key + ".CLICK_SOUND")) {
                hotbarConfig.getConfiguration().set(key + ".CLICK_SOUND.ENABLED", false);
                hotbarConfig.getConfiguration().set(key + ".CLICK_SOUND.SOUND", "UI_BUTTON_CLICK");
                hotbarConfig.getConfiguration().set(key + ".CLICK_SOUND.VOLUME", 1.0);
                hotbarConfig.getConfiguration().set(key + ".CLICK_SOUND.PITCH", 1.0);
            }
        }

        if (!hotbarConfig.getConfiguration().contains("LOTTERY_JOIN")) {
            Map<String, Object> joinItem = new LinkedHashMap<>();
            joinItem.put("LOTTERY_JOIN.ENABLED", true);
            joinItem.put("LOTTERY_JOIN.GLOW", false);
            joinItem.put("LOTTERY_JOIN.NAME", "&d&lLOTTERY TICKET &7(Right Click)");
            joinItem.put("LOTTERY_JOIN.LORE", java.util.List.of(
                    "&7A lottery is active right now.",
                    "&eRight click to join instantly."
            ));
            joinItem.put("LOTTERY_JOIN.MATERIAL", "PAPER");
            joinItem.put("LOTTERY_JOIN.DATA", 0);
            joinItem.put("LOTTERY_JOIN.SLOT", 7);
            joinItem.put("LOTTERY_JOIN.AMOUNT", 1);
            joinItem.put("LOTTERY_JOIN.COMMAND", "");
            joinItem.put("LOTTERY_JOIN.CLICK_SOUND.ENABLED", false);
            joinItem.put("LOTTERY_JOIN.CLICK_SOUND.SOUND", "UI_BUTTON_CLICK");
            joinItem.put("LOTTERY_JOIN.CLICK_SOUND.VOLUME", 1.0);
            joinItem.put("LOTTERY_JOIN.CLICK_SOUND.PITCH", 1.0);
            ConfigSupport.applyMissingDefaults(hotbarConfig.getConfiguration(), joinItem);
        }
        hotbarConfig.save();
    }

}
