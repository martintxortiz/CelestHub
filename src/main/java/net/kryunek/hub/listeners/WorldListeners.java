package net.kryunek.hub.listeners;

import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.hotbar.Hotbar;
import net.kryunek.hub.managers.hotbar.HotbarManager;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.managers.player.ProfileManager;
import net.kryunek.hub.managers.pvparena.PvpArenaKitManager;
import net.kryunek.hub.managers.spawn.SpawnManager;
import net.kryunek.hub.menus.gadgets.GadgetService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.PvpArenaUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class WorldListeners implements Listener {

    private static final long TEMP_BLOCK_ANIMATION_PERIOD_TICKS = 5L;

    private final Celest hub;
    private final ProfileManager profileManager;
    private final HotbarManager hotbarManager;
    private final PvpArenaKitManager pvpArenaKitManager;
    private final SpawnManager spawnManager;
    private final FileConfig settingsConfig;
    private final Map<BlockKey, TemporaryBlockData> temporaryBlocks = new HashMap<>();

    public WorldListeners(Celest hub) {
        this.hub = hub;
        var managers = ModuleService.getManagerModule();
        Bukkit.getPluginManager().registerEvents(this, hub);
        this.settingsConfig = ModuleService.getFileModule().getFile("settings");
        this.profileManager = managers.getProfileManager();
        this.hotbarManager = managers.getHotbarManager();
        this.pvpArenaKitManager = managers.getPvpArenaKitManager();
        this.spawnManager = managers.getSpawnManager();
        Bukkit.getScheduler().runTaskTimer(hub, this::updateTemporaryBlockAnimations, 2L, TEMP_BLOCK_ANIMATION_PERIOD_TICKS);
    }

    private boolean isBuildMode(Player player) {
        Profile profile = this.profileManager.getProfile(player.getUniqueId());
        return profile != null && profile.isBuildModeEnabled();
    }

    private Player getDamagingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }

        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }

        return null;
    }

    private boolean shouldAllowPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return false;
        }

        Player attacker = getDamagingPlayer(event.getDamager());
        if (attacker == null) {
            return false;
        }

        org.bukkit.World victimWorld = victim.getWorld();
        org.bukkit.World attackerWorld = attacker.getWorld();
        if (!victimWorld.getName().equalsIgnoreCase(attackerWorld.getName())) {
            return false;
        }

        return PvpArenaUtil.isInsideArena(settingsConfig, victim.getLocation()) && PvpArenaUtil.isInsideArena(settingsConfig, attacker.getLocation());
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        if (settingsConfig.getBoolean("EVENT.ANTI_FOOD")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (settingsConfig.getBoolean("EVENT.ANTI_DAMAGE")) {
            if (event.getEntity() instanceof Player) {
                if (event instanceof EntityDamageByEntityEvent byEntityEvent && shouldAllowPvp(byEntityEvent)) {
                    return;
                }
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onDamageEntity(EntityDamageByEntityEvent event) {
        if (settingsConfig.getBoolean("EVENT.ANTI_DAMAGE")) {
            if (shouldAllowPvp(event)) {
                if (event.getEntity() instanceof Player victim) {
                    Player attacker = getDamagingPlayer(event.getDamager());
                    if (attacker != null) {
                        pvpArenaKitManager.markCombat(attacker, victim);
                    }
                }
                return;
            }
            event.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onHotbarClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (isBuildMode(player)) {
            return;
        }
        if (pvpArenaKitManager.isEditingKit(player.getUniqueId())) {
            return;
        }
        if (event.getClick() == ClickType.NUMBER_KEY) {
            event.setCancelled(true);
            return;
        }

        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            event.setCancelled(true);
            return;
        }
        if (event.getSlot() == 40) {
            event.setCancelled(true);
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        for (Hotbar hotbar : hotbarManager.getHotbars().values()) {
            if (hotbar.isHotbarItem(clickedItem)) {
                event.setCancelled(true);

                return;
            }
        }

        Profile profile = this.profileManager.getProfile(player.getUniqueId());
        if (profile != null && profile.getSelectedGadgetType() != null && !profile.getSelectedGadgetType().equalsIgnoreCase("NONE")) {
            ItemStack selectedItem = GadgetService.getItemByType(profile.getSelectedGadgetType());
            if (selectedItem != null && selectedItem.hasItemMeta() && clickedItem.hasItemMeta()
                    && selectedItem.getType() == clickedItem.getType()
                    && selectedItem.getItemMeta().getDisplayName() != null
                    && selectedItem.getItemMeta().getDisplayName().equals(clickedItem.getItemMeta().getDisplayName())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onArmorDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (isBuildMode(player)) {
            return;
        }
        if (pvpArenaKitManager.isEditingKit(player.getUniqueId())) {
            return;
        }

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 5 && rawSlot <= 8) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (isBuildMode(player)) {
            return;
        }
        if (pvpArenaKitManager.isEditingKit(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onKill(PlayerDeathEvent event) {
        event.setDeathMessage(null);
        event.setKeepInventory(true);
        event.getDrops().clear();
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        boolean victimInArena = pvpArenaKitManager.isInArenaSession(victim.getUniqueId());
        boolean killerInArena = killer != null && pvpArenaKitManager.isInArenaSession(killer.getUniqueId());

        if (killer != null && victimInArena && killerInArena) {
            Profile victimProfile = profileManager.getProfile(victim.getUniqueId());
            Profile killerProfile = profileManager.getProfile(killer.getUniqueId());
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
            pvpArenaKitManager.clearCombat(killer.getUniqueId());
        }

        pvpArenaKitManager.clearCombat(victim.getUniqueId());
        pvpArenaKitManager.resetArenaStateOnDeath(victim);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!victim.isOnline()) return;
                victim.spigot().respawn();
                Bukkit.getScheduler().runTaskLater(hub, () -> {
                    if (!victim.isOnline()) return;
                    spawnManager.toSpawn(victim, true);
                    pvpArenaKitManager.restoreHubStateNow(victim);
                }, 1L);
            }
        }.runTaskLater(hub, 1L);
    }


    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (settingsConfig.getBoolean("EVENT.ANTI_ENTITY")) {
            if (event.getEntity() instanceof EnderPearl
                    || event.getEntity() instanceof Snowball
                    || event.getEntity() instanceof Fireball
                    || event.getEntity() instanceof Firework
                    || event.getEntity() instanceof TNTPrimed
                    || event.getEntity() instanceof ArmorStand) {
                return;
            }
            event.setCancelled(true);
        }
    }


    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (isBuildMode(player)) {
            return;
        }
        if (settingsConfig.getBoolean("EVENT.ANTI_PICKUP")) {

                event.setCancelled(true);

        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (isBuildMode(player)) {
            return;
        }
        if (canUseTemporaryArenaBlocks(player)) {
            return;
        }
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getAction().equals(Action.LEFT_CLICK_BLOCK) || event.getAction().equals(Action.PHYSICAL)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (isBuildMode(player)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (isBuildMode(player)) {
            return;
        }
        if (settingsConfig.getBoolean("EVENT.ANTI_DROP")) {
                event.setCancelled(true);
            }
        }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (isBuildMode(player)) {
            return;
        }
        if (canUseTemporaryArenaBlocks(player)) {
            trackTemporaryBlock(event);
            return;
        }
        event.setCancelled(true);
        }

    @EventHandler
    public void onEntityDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Entity vehicle = event.getDismounted();
        if (vehicle instanceof EnderPearl || vehicle instanceof Snowball || vehicle instanceof Fireball || vehicle instanceof Firework) {
            player.eject();
            vehicle.remove();
            GadgetService.restorePlayerCollision(player);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isBuildMode(player)) {
            return;
        }
        if (canUseTemporaryArenaBlocks(player)) {
            BlockKey key = BlockKey.from(event.getBlock().getLocation());
            TemporaryBlockData data = temporaryBlocks.remove(key);
            if (data == null || !isStillTemporary(data)) {
                event.setCancelled(true);
                player.sendMessage(CC.translate("&cYou can only break recently placed temporary blocks."));
                return;
            }
            if (data.removeTask() != null) {
                data.removeTask().cancel();
            }
            clearBlockDamageVisual(data.world(), data.x(), data.y(), data.z());
            return;
        }
        event.setCancelled(true);
        }


    @EventHandler
    public void entityExplode(EntityExplodeEvent event) {
        if (settingsConfig.getBoolean("EVENT.ANTI_EXPLODE"))
            event.setCancelled(true);
    }

    @EventHandler
    public void onMonster(CreatureSpawnEvent event) {
        if (settingsConfig.getBoolean("EVENT.ANTI_MOBS") && event.getEntity() instanceof Mob)
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWeatherChangeEvent(WeatherChangeEvent event) {
        event.setCancelled(true);
        if (settingsConfig.getBoolean("EVENT.ALWAYS_SUNNY")) {
            event.getWorld().setWeatherDuration(0);
            event.getWorld().setTime(3000);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getLocation().getBlockY() < 0) {

            spawnManager.toSpawn(player, true);

        }
    }

    private boolean canUseTemporaryArenaBlocks(Player player) {
        if (!settingsConfig.getBoolean("PVP_ARENA.PERIMETER_BLOCKS.ALLOW")) {
            return false;
        }
        return PvpArenaUtil.isInsideArena(settingsConfig, player.getLocation());
    }

    private int getTemporaryBlockDurationSeconds() {
        if (!settingsConfig.getConfiguration().contains("PVP_ARENA.PERIMETER_BLOCKS.DISAPPEAR_SECONDS")) {
            return 8;
        }
        return Math.max(1, settingsConfig.getInt("PVP_ARENA.PERIMETER_BLOCKS.DISAPPEAR_SECONDS"));
    }

    private void trackTemporaryBlock(BlockPlaceEvent event) {
        BlockKey key = BlockKey.from(event.getBlock().getLocation());
        TemporaryBlockData previous = temporaryBlocks.remove(key);
        if (previous != null) {
            if (previous.removeTask() != null) {
                previous.removeTask().cancel();
            }
            clearBlockDamageVisual(previous.world(), previous.x(), previous.y(), previous.z());
        }

        long now = System.currentTimeMillis();
        long durationMillis = getTemporaryBlockDurationSeconds() * 1000L;
        long expiresAt = now + durationMillis;

        BukkitTask task = Bukkit.getScheduler().runTaskLater(hub, () -> {
            TemporaryBlockData active = temporaryBlocks.remove(key);
            if (active == null) {
                return;
            }
            clearBlockDamageVisual(active.world(), active.x(), active.y(), active.z());
            if (!active.world().isChunkLoaded(active.x() >> 4, active.z() >> 4)) {
                return;
            }
            if (active.world().getBlockAt(active.x(), active.y(), active.z()).getType() == active.placedType()) {
                active.world().getBlockAt(active.x(), active.y(), active.z()).setType(Material.AIR, false);
            }
        }, getTemporaryBlockDurationSeconds() * 20L);

        temporaryBlocks.put(key, new TemporaryBlockData(
                key.world(),
                key.x(),
                key.y(),
                key.z(),
                event.getBlockPlaced().getType(),
                now,
                expiresAt,
                task
        ));
    }

    private boolean isStillTemporary(TemporaryBlockData data) {
        return System.currentTimeMillis() <= data.expiresAtMillis();
    }

    private void updateTemporaryBlockAnimations() {
        long now = System.currentTimeMillis();
        for (TemporaryBlockData data : temporaryBlocks.values()) {
            long total = Math.max(1L, data.expiresAtMillis() - data.placedAtMillis());
            long elapsed = Math.max(0L, now - data.placedAtMillis());
            float progress = Math.min(1.0F, (float) elapsed / (float) total);
            sendBlockDamageVisual(data.world(), data.x(), data.y(), data.z(), progress);
        }
    }

    private void sendBlockDamageVisual(World world, int x, int y, int z, float progress) {
        org.bukkit.Location location = new org.bukkit.Location(world, x, y, z);
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= 48 * 48) {
                player.sendBlockDamage(location, progress);
            }
        }
    }

    private void clearBlockDamageVisual(World world, int x, int y, int z) {
        sendBlockDamageVisual(world, x, y, z, 0.0F);
    }

    private record BlockKey(UUID worldId, World world, int x, int y, int z) {
        static BlockKey from(org.bukkit.Location location) {
            World world = location.getWorld();
            if (world == null) {
                throw new IllegalStateException("Block location world is null");
            }
            return new BlockKey(world.getUID(), world, location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockKey blockKey)) return false;
            return x == blockKey.x && y == blockKey.y && z == blockKey.z && Objects.equals(worldId, blockKey.worldId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(worldId, x, y, z);
        }
    }

    private record TemporaryBlockData(World world, int x, int y, int z, Material placedType, long placedAtMillis, long expiresAtMillis, BukkitTask removeTask) {
    }

}


