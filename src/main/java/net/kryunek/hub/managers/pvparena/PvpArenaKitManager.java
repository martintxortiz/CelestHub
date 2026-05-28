package net.kryunek.hub.managers.pvparena;

import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.hotbar.HotbarManager;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.managers.player.ProfileManager;
import net.kryunek.hub.utils.BukkitUtil;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.PlayerUtil;
import org.bukkit.block.BlockFace;
import net.kryunek.hub.menus.editor.pvparena.PvpArenaEditorMenu;
import net.kryunek.hub.menus.settings.SettingsMenu;
import net.kryunek.hub.utils.menu.Menu;
import net.kryunek.hub.utils.menu.buttons.BackButton;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PvpArenaKitManager {

    private static final int[] EDITOR_CONTROL_SLOTS = new int[]{45, 46, 47, 48, 49};

    private final FileConfig settingsConfig;
    private final FileConfig settingsMenuConfig;
    private final ProfileManager profileManager;
    private final HotbarManager hotbarManager;

    private final Set<UUID> playersInArena = new HashSet<>();
    private final Set<UUID> editingPlayers = new HashSet<>();
    private final Set<UUID> editingDefaultKitPlayers = new HashSet<>();
    private final Map<UUID, Long> combatUntil = new HashMap<>();
    private final Map<UUID, UUID> lastCombatOpponent = new HashMap<>();

    public PvpArenaKitManager(ProfileManager profileManager, HotbarManager hotbarManager) {
        this.settingsConfig = ModuleService.getFileModule().getFile("settings");
        this.settingsMenuConfig = ModuleService.getFileModule().getFile("settings_menu");
        this.profileManager = profileManager;
        this.hotbarManager = hotbarManager;
    }

    public boolean isEditingKit(UUID uuid) {
        return editingPlayers.contains(uuid);
    }

    public boolean isEditorView(UUID uuid, String title) {
        return editingPlayers.contains(uuid) && isEditorTitle(title);
    }

    public boolean isDefaultEditorView(UUID uuid, String title) {
        return editingDefaultKitPlayers.contains(uuid) && getDefaultEditorTitle().equals(title);
    }

    public boolean isEditorTitle(String title) {
        return getEditorTitle().equals(title);
    }

    public void openKitEditor(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 54, getEditorTitle());
        SavedLoadout kit = getPlayerKit(player);
        fillEditorInventory(inventory, kit);
        decorateEditorInventory(inventory);
        inventory.setItem(45, new ItemBuilder(Material.CHEST).name("&eLoad Default Kit").lore(java.util.List.of("&7Load global default kit to editor")).build());
        inventory.setItem(46, new ItemBuilder(Material.LIME_WOOL).name("&aSave Kit").lore(java.util.List.of("&7Save your personal PvP kit")).build());
        inventory.setItem(47, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("&8Not Available").lore(java.util.List.of("&7Copy option is disabled here")).build());
        inventory.setItem(48, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("&8Locked").lore(java.util.List.of("&7You can't clear personal kit here")).build());
        inventory.setItem(49, new BackButton(new SettingsMenu()).getButtonItem(player));
        Menu.getOpenedMenus().remove(player.getName());
        editingPlayers.add(player.getUniqueId());
        player.openInventory(inventory);
    }

    public void openDefaultKitEditor(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 54, getDefaultEditorTitle());
        SavedLoadout kit = getDefaultKit();
        fillEditorInventory(inventory, kit);
        decorateEditorInventory(inventory);
        inventory.setItem(46, new ItemBuilder(Material.LIME_WOOL).name("&aSave Default Kit").build());
        inventory.setItem(47, new ItemBuilder(Material.HOPPER).name("&bCopy My Inventory").build());
        inventory.setItem(48, new ItemBuilder(Material.BARRIER).name("&cClear Default Kit").build());
        inventory.setItem(49, new BackButton(new PvpArenaEditorMenu()).getButtonItem(player));
        Menu.getOpenedMenus().remove(player.getName());
        editingDefaultKitPlayers.add(player.getUniqueId());
        player.openInventory(inventory);
    }

    public String getEditorTitle() {
        String raw = settingsMenuConfig.getConfiguration().getString("SETTINGS.PVP_ARENA_KIT.EDITOR_TITLE", "&8PvP Kit Editor");
        return CC.translate(raw);
    }

    public String getDefaultEditorTitle() {
        return CC.translate("&8Default PvP Kit");
    }

    public boolean handleEditorControlClick(Player player, int slot, Inventory topInventory) {
        if (slot == 45) {
            applyDefaultKitToEditor(topInventory);
            player.sendMessage(CC.translate(settingsMenuConfig.getString("settings.pvp-kit-default-loaded", "&eDefault PvP kit loaded.", true)));
            return true;
        }
        if (slot == 46) {
            saveEditorKit(player, topInventory);
            player.sendMessage(CC.translate(settingsMenuConfig.getString("settings.pvp-kit-saved", "&aPvP kit saved.", true)));
            return true;
        }
        if (slot == 48) {
            player.sendMessage(CC.translate("&cYou can't clear personal kit here."));
            return true;
        }
        if (slot == 49) {
            saveEditorKit(player, topInventory);
            new BackButton(new SettingsMenu()).clicked(player, slot, ClickType.LEFT, 0);
            return true;
        }
        return false;
    }

    public boolean handleDefaultEditorControlClick(Player player, int slot, Inventory topInventory) {
        if (slot == 46) {
            saveDefaultKit(topInventory);
            player.sendMessage(CC.translate("&aDefault PvP kit saved."));
            return true;
        }
        if (slot == 47) {
            copyInventory(player, topInventory);
            player.sendMessage(CC.translate("&aCopied your inventory into default kit editor."));
            return true;
        }
        if (slot == 48) {
            clearEditorInventory(topInventory);
            player.sendMessage(CC.translate("&cDefault PvP kit cleared."));
            return true;
        }
        if (slot == 49) {
            saveDefaultKit(topInventory);
            new BackButton(new PvpArenaEditorMenu()).clicked(player, slot, ClickType.LEFT, 0);
            return true;
        }
        return false;
    }

    public boolean isControlSlot(int slot) {
        for (int controlSlot : EDITOR_CONTROL_SLOTS) {
            if (controlSlot == slot) {
                return true;
            }
        }
        return false;
    }

    public void handleEditorClose(Player player, Inventory inventory) {
        if (!editingPlayers.remove(player.getUniqueId())) {
            return;
        }
        saveEditorKit(player, inventory);
    }

    public void handleDefaultEditorClose(Player player, Inventory inventory) {
        if (!editingDefaultKitPlayers.remove(player.getUniqueId())) {
            return;
        }
        saveDefaultKit(inventory);
    }

    public void onEnterArena(Player player) {
        UUID uuid = player.getUniqueId();
        if (playersInArena.contains(uuid)) {
            return;
        }
        Profile profile = profileManager.getProfile(uuid);
        if (profile != null && profile.isBuildModeEnabled()) {
            return;
        }
        playersInArena.add(uuid);

        player.setAllowFlight(false);
        player.setFlying(false);
        player.setWalkSpeed(0.2F);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);

        SavedLoadout kit = getPlayerKit(player);
        player.getInventory().setContents(kit.contents());
        player.getInventory().setArmorContents(kit.armor());
        player.getInventory().setItemInOffHand(kit.offhand());
        applyArenaEffects(player);
        enforceArenaVisibility(player);
        player.updateInventory();
    }

    public boolean isInArenaSession(UUID uuid) {
        return playersInArena.contains(uuid);
    }

    public void syncArenaState(Player player, boolean shouldBeInside) {
        boolean currentlyInside = playersInArena.contains(player.getUniqueId());
        if (shouldBeInside && !currentlyInside) {
            onEnterArena(player);
            return;
        }
        if (!shouldBeInside && currentlyInside) {
            onLeaveArena(player);
            return;
        }
        if (shouldBeInside && currentlyInside) {
            refreshArenaState(player);
        }
    }

    public void onLeaveArena(Player player) {
        UUID uuid = player.getUniqueId();
        playersInArena.remove(uuid);
        if (isInCombat(uuid)) {
            applyCombatLogoutStats(uuid);
            clearCombat(uuid);
            player.setHealth(0.0D);
            return;
        }
        clearArenaEffects(player);
        applyHubState(player);
        enforceArenaVisibility(player);
        player.updateInventory();
        forceFullVisibilitySync();
    }

    public void clearPlayerSession(UUID uuid) {
        playersInArena.remove(uuid);
        editingPlayers.remove(uuid);
        editingDefaultKitPlayers.remove(uuid);
        clearCombat(uuid);
    }

    public void resetArenaStateOnDeath(Player player) {
        UUID uuid = player.getUniqueId();
        playersInArena.remove(uuid);
        clearCombat(uuid);
        clearArenaEffects(player);
    }

    private void applyHubState(Player player) {
        Profile profile = profileManager.getProfile(player.getUniqueId());
        if (profile != null && profile.isBuildModeEnabled()) {
            return;
        }

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        player.setWalkSpeed((float) ModuleService.getFileModule().getFile("settings").getDouble("WALK_SPEED"));

        hotbarManager.setHotbar(player);
        if (profile != null && profile.isFlyOnJoin()) {
            PlayerUtil.applyHubFlyState(player, true, true);
        } else {
            PlayerUtil.applyHubFlyState(player, false, false);
            enableDoubleJumpIfAvailable(player, profile);
        }

        if (profile != null) {
            ModuleService.getManagerModule().getOutfitManager().applySelectedOutfit(player, profile);
        }
    }

    public void restoreHubStateNow(Player player) {
        clearArenaEffects(player);
        applyHubState(player);
        enforceArenaVisibility(player);
        player.updateInventory();
        forceFullVisibilitySync();
    }

    private void saveEditorKit(Player player, Inventory inventory) {
        SavedLoadout kit = readEditorInventory(inventory);

        Profile profile = profileManager.getProfile(player.getUniqueId());
        if (profile == null) {
            return;
        }
        profile.setPvpArenaKitSerialized(serializeLoadout(kit));
        profileManager.saveProfile(profile, false);
    }

    private void applyDefaultKitToEditor(Inventory inventory) {
        fillEditorInventory(inventory, getDefaultKit());
    }

    private SavedLoadout getPlayerKit(Player player) {
        Profile profile = profileManager.getProfile(player.getUniqueId());
        if (profile != null) {
            String serialized = profile.getPvpArenaKitSerialized();
            if (serialized != null && !serialized.isBlank()) {
                SavedLoadout parsed = deserializeLoadout(serialized);
                if (parsed != null) return parsed;
            }
        }
        return getDefaultKit();
    }

    private SavedLoadout getDefaultKit() {
        String serialized = settingsConfig.getConfiguration().getString("PVP_ARENA.DEFAULT_KIT_SERIALIZED", "");
        if (serialized != null && !serialized.isBlank()) {
            SavedLoadout parsed = deserializeLoadout(serialized);
            if (parsed != null) return parsed;
        }

        ItemStack[] contents = new ItemStack[36];
        for (int i = 0; i < 9; i++) {
            String raw = settingsConfig.getString("PVP_ARENA.KIT." + i, "", false);
            if (raw == null || raw.isBlank()) {
                continue;
            }

            String[] parts = raw.split(":");
            Material material = Material.matchMaterial(parts[0].trim());
            if (material == null || material == Material.AIR) {
                continue;
            }
            int amount = 1;
            if (parts.length > 1) {
                try {
                    amount = Math.max(1, Math.min(64, Integer.parseInt(parts[1].trim())));
                } catch (NumberFormatException ex) {
                    amount = 1;
                }
            }
            contents[i] = new ItemStack(material, amount);
        }
        return new SavedLoadout(contents, new ItemStack[4], null);
    }

    private void saveDefaultKit(Inventory inventory) {
        SavedLoadout kit = readEditorInventory(inventory);

        settingsConfig.getConfiguration().set("PVP_ARENA.DEFAULT_KIT_SERIALIZED", serializeLoadout(kit));
        ConfigurationSection section = settingsConfig.getConfiguration().getConfigurationSection("PVP_ARENA.KIT");
        if (section == null) {
            section = settingsConfig.getConfiguration().createSection("PVP_ARENA.KIT");
        }
        for (int i = 0; i < 9; i++) {
            ItemStack item = kit.contents()[i];
            if (item == null || item.getType() == Material.AIR) {
                section.set(String.valueOf(i), "");
            } else {
                section.set(String.valueOf(i), item.getType().name() + ":" + item.getAmount());
            }
        }
        settingsConfig.save();
    }

    private void copyInventory(Player player, Inventory inventory) {
        fillEditorInventory(inventory, new SavedLoadout(
                cloneArray(player.getInventory().getContents(), 36),
                cloneArray(player.getInventory().getArmorContents(), 4),
                player.getInventory().getItemInOffHand() == null ? null : player.getInventory().getItemInOffHand().clone()
        ));
    }

    private void clearEditorInventory(Inventory inventory) {
        fillEditorInventory(inventory, new SavedLoadout(new ItemStack[36], new ItemStack[4], null));
    }

    private void decorateEditorInventory(Inventory inventory) {
        ItemStack pane = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("&7").build();
        int[] slots = new int[]{41, 42, 43, 44, 50, 51, 52, 53};
        for (int slot : slots) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, pane);
            }
        }
    }

    private void fillEditorInventory(Inventory inventory, SavedLoadout loadout) {
        for (int i = 0; i < 36; i++) {
            ItemStack item = i < loadout.contents().length ? loadout.contents()[i] : null;
            inventory.setItem(i, item == null ? null : item.clone());
        }
        ItemStack[] armor = loadout.armor();
        inventory.setItem(36, armor.length > 3 && armor[3] != null ? armor[3].clone() : null); // helmet
        inventory.setItem(37, armor.length > 2 && armor[2] != null ? armor[2].clone() : null); // chest
        inventory.setItem(38, armor.length > 1 && armor[1] != null ? armor[1].clone() : null); // legs
        inventory.setItem(39, armor.length > 0 && armor[0] != null ? armor[0].clone() : null); // boots
        inventory.setItem(40, loadout.offhand() == null ? null : loadout.offhand().clone());
    }

    private SavedLoadout readEditorInventory(Inventory inventory) {
        ItemStack[] contents = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            ItemStack item = inventory.getItem(i);
            contents[i] = item == null ? null : item.clone();
        }
        ItemStack[] armor = new ItemStack[4];
        armor[3] = cloneItem(inventory.getItem(36)); // helmet
        armor[2] = cloneItem(inventory.getItem(37)); // chest
        armor[1] = cloneItem(inventory.getItem(38)); // legs
        armor[0] = cloneItem(inventory.getItem(39)); // boots
        ItemStack offhand = cloneItem(inventory.getItem(40));
        return new SavedLoadout(contents, armor, offhand);
    }

    private String serializeLoadout(SavedLoadout loadout) {
        ItemStack[] merged = new ItemStack[41];
        System.arraycopy(loadout.contents(), 0, merged, 0, Math.min(loadout.contents().length, 36));
        System.arraycopy(loadout.armor(), 0, merged, 36, Math.min(loadout.armor().length, 4));
        merged[40] = loadout.offhand();
        return BukkitUtil.serializeItemStackArray(merged);
    }

    private SavedLoadout deserializeLoadout(String raw) {
        ItemStack[] merged = BukkitUtil.deserializeItemStackArray(raw);
        if (merged.length < 41) {
            return null;
        }
        ItemStack[] contents = Arrays.copyOfRange(merged, 0, 36);
        ItemStack[] armor = Arrays.copyOfRange(merged, 36, 40);
        ItemStack offhand = merged[40];
        sanitizeDecorativeItems(contents);
        sanitizeDecorativeItems(armor);
        if (isDecorativeItem(offhand)) {
            offhand = null;
        }
        return new SavedLoadout(contents, armor, offhand);
    }

    private ItemStack[] cloneArray(ItemStack[] source, int size) {
        ItemStack[] target = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            if (source != null && i < source.length && source[i] != null) {
                target[i] = source[i].clone();
            }
        }
        return target;
    }

    private ItemStack cloneItem(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private void sanitizeDecorativeItems(ItemStack[] items) {
        if (items == null) {
            return;
        }
        for (int i = 0; i < items.length; i++) {
            if (isDecorativeItem(items[i])) {
                items[i] = null;
            }
        }
    }

    private boolean isDecorativeItem(ItemStack item) {
        if (item == null) {
            return false;
        }
        Material type = item.getType();
        if (type != Material.BLACK_STAINED_GLASS_PANE && type != Material.LIGHT_BLUE_STAINED_GLASS_PANE && type != Material.GRAY_STAINED_GLASS_PANE) {
            return false;
        }
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        String display = item.getItemMeta().getDisplayName();
        return "&7".equalsIgnoreCase(CC.translate(display))
                || "&8Locked".equalsIgnoreCase(CC.translate(display))
                || "&8Not Available".equalsIgnoreCase(CC.translate(display))
                || "§7".equals(display)
                || "§8Locked".equals(display)
                || "§8Not Available".equals(display);
    }

    public void refreshArenaState(Player player) {
        if (!playersInArena.contains(player.getUniqueId())) {
            return;
        }
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setWalkSpeed(0.2F);
        applyArenaEffects(player);
        enforceArenaVisibility(player);
    }

    private void applyArenaEffects(Player player) {
        clearArenaEffects(player);
        List<String> configured = settingsConfig.getConfiguration().getStringList("PVP_ARENA.EFFECTS");
        for (String entry : configured) {
            if (entry == null || entry.isBlank()) continue;
            String[] parts = entry.split(":");
            String effectName = parts[0].trim().toUpperCase();
            PotionEffectType type = PotionEffectType.getByName(effectName);
            if (type == null) continue;
            int amplifier = 0;
            if (parts.length > 1) {
                try {
                    amplifier = Math.max(0, Integer.parseInt(parts[1].trim()) - 1);
                } catch (NumberFormatException ex) {
                    amplifier = 0;
                }
            }
            player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, true, false, true));
        }
    }

    private void clearArenaEffects(Player player) {
        List<String> configured = settingsConfig.getConfiguration().getStringList("PVP_ARENA.EFFECTS");
        for (String entry : configured) {
            if (entry == null || entry.isBlank()) continue;
            String[] parts = entry.split(":");
            PotionEffectType type = PotionEffectType.getByName(parts[0].trim().toUpperCase());
            if (type != null) {
                player.removePotionEffect(type);
            }
        }
    }

    public void enforceArenaVisibility(Player changed) {
        boolean changedInside = playersInArena.contains(changed.getUniqueId());
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(changed)) continue;
            boolean otherInside = playersInArena.contains(other.getUniqueId());
            if (changedInside && otherInside) {
                changed.showPlayer(other);
                other.showPlayer(changed);
                continue;
            }
            if (changedInside && !otherInside) {
                changed.hidePlayer(other);
                other.showPlayer(changed);
                continue;
            }
            if (!changedInside && otherInside) {
                changed.showPlayer(other);
                other.hidePlayer(changed);
                continue;
            }

            Profile changedProfile = profileManager.getProfile(changed.getUniqueId());
            if (changedProfile != null && !changedProfile.isVisibilityOn()) {
                changed.hidePlayer(other);
            } else {
                changed.showPlayer(other);
            }

            Profile otherProfile = profileManager.getProfile(other.getUniqueId());
            if (otherProfile != null && !otherProfile.isVisibilityOn()) {
                other.hidePlayer(changed);
            } else {
                other.showPlayer(changed);
            }
        }
    }

    public void markCombat(Player a, Player b) {
        long until = System.currentTimeMillis() + 10_000L;
        combatUntil.put(a.getUniqueId(), until);
        combatUntil.put(b.getUniqueId(), until);
        lastCombatOpponent.put(a.getUniqueId(), b.getUniqueId());
        lastCombatOpponent.put(b.getUniqueId(), a.getUniqueId());
    }

    public boolean isInCombat(UUID uuid) {
        Long until = combatUntil.get(uuid);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() > until) {
            clearCombat(uuid);
            return false;
        }
        return true;
    }

    public long getCombatRemainingSeconds(UUID uuid) {
        Long until = combatUntil.get(uuid);
        if (until == null) {
            return 0L;
        }
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0L) {
            clearCombat(uuid);
            return 0L;
        }
        return Math.max(1L, (long) Math.ceil(remaining / 1000.0D));
    }

    public void clearCombat(UUID uuid) {
        combatUntil.remove(uuid);
        UUID opponent = lastCombatOpponent.remove(uuid);
        if (opponent != null && uuid.equals(lastCombatOpponent.get(opponent))) {
            lastCombatOpponent.remove(opponent);
        }
    }

    private void applyCombatLogoutStats(UUID victimId) {
        UUID killerId = lastCombatOpponent.get(victimId);
        if (killerId == null) {
            return;
        }

        Profile victimProfile = profileManager.getProfile(victimId);
        Profile killerProfile = profileManager.getProfile(killerId);

        if (victimProfile != null) {
            victimProfile.setPvpDeaths(victimProfile.getPvpDeaths() + 1);
            victimProfile.setPvpKillstreak(0);
        }

        if (killerProfile != null) {
            killerProfile.setPvpKills(killerProfile.getPvpKills() + 1);
            int nextStreak = killerProfile.getPvpKillstreak() + 1;
            killerProfile.setPvpKillstreak(nextStreak);
            if (nextStreak > killerProfile.getPvpMaxKillstreak()) {
                killerProfile.setPvpMaxKillstreak(nextStreak);
            }
        }

        clearCombat(killerId);
    }

    public void forceFullVisibilitySync() {
        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTask(Celest.get(), () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                enforceArenaVisibility(online);
            }
        });
        scheduler.runTaskLater(Celest.get(), () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                enforceArenaVisibility(online);
            }
        }, 2L);
    }

    private void enableDoubleJumpIfAvailable(Player player, Profile profile) {
        if (profile == null || profile.isFlyOnJoin() || profile.isBuildModeEnabled()) {
            return;
        }
        if (!settingsConfig.getBoolean("DOUBLE_JUMP.ENABLED")) {
            return;
        }
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }

        boolean infinite = settingsConfig.getBoolean("DOUBLE_JUMP.INFINITE");
        boolean onGround = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR;
        if (infinite || onGround) {
            player.setAllowFlight(true);
        }
    }

    private record SavedLoadout(ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
    }
}
