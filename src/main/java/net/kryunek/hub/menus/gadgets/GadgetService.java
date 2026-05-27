package net.kryunek.hub.menus.gadgets;

import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.hotbar.Hotbar;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class GadgetService {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final String GADGET_ENTITY_TAG = "celest_gadget_entity";
    private static final Map<UUID, BukkitTask> RAINBOW_TASKS = new HashMap<>();
    private static final Map<UUID, String> RAINBOW_LAST_BLOCK = new HashMap<>();
    private static final Map<UUID, BukkitTask> ICE_TRAIL_TASKS = new HashMap<>();
    private static final Map<UUID, String> ICE_TRAIL_LAST_BLOCK = new HashMap<>();
    private static final Map<UUID, Location> PORTAL_POINTS = new HashMap<>();
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, BukkitTask> COLLISION_RESET_TASKS = new HashMap<>();
    private static final Material[] RAINBOW_GLASS = new Material[]{
            Material.RED_STAINED_GLASS,
            Material.ORANGE_STAINED_GLASS,
            Material.YELLOW_STAINED_GLASS,
            Material.LIME_STAINED_GLASS,
            Material.LIGHT_BLUE_STAINED_GLASS,
            Material.BLUE_STAINED_GLASS,
            Material.PURPLE_STAINED_GLASS,
            Material.PINK_STAINED_GLASS
    };

    private GadgetService() {
    }

    private static FileConfig gadgetsMenu() {
        return ModuleService.getFileModule().getFile("gadgets");
    }

    private static FileConfig settings() {
        return ModuleService.getFileModule().getFile("gadgets");
    }

    public static ItemStack getItemByType(String type) {
        String normalizedType = normalizeType(type);
        if (normalizedType == null || normalizedType.equalsIgnoreCase("NONE")) {
            return null;
        }
        if (!isEnabled(normalizedType)) {
            return buildNoPermissionItem();
        }

        String key = getKeyByType(normalizedType);
        if (key == null) {
            return null;
        }

        String path = "GADGETS_MENU.ITEMS." + key + ".";
        String materialName = gadgetsMenu().getString(path + "MATERIAL", "BLAZE_ROD", false);
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.BLAZE_ROD;
        }

        return new ItemBuilder(material)
                .name(gadgetsMenu().getString(path + "NAME", "&dGadget", true))
                .lore(gadgetsMenu().getStringList(path + "LORE"))
                .data(gadgetsMenu().getInt(path + "DATA"))
                .build();
    }

    public static ItemStack getItemForPlayer(String type, Player player) {
        String normalizedType = normalizeType(type);
        if (!isEnabled(normalizedType)) {
            return buildNoPermissionItem();
        }
        if (hasPermission(player, normalizedType)) {
            if ("PORTAL_JUMP".equalsIgnoreCase(normalizedType) && PORTAL_POINTS.containsKey(player.getUniqueId())) {
                String key = getKeyByType(normalizedType);
                if (key != null) {
                    String basePath = "GADGETS_MENU.ITEMS." + key + ".";
                    String materialName = gadgetsMenu().getString(basePath + "PORTAL_SET_MATERIAL",
                            gadgetsMenu().getString(basePath + "MATERIAL", "END_PORTAL_FRAME", false), false);
                    Material material = Material.matchMaterial(materialName);
                    if (material == null) {
                        material = Material.END_PORTAL_FRAME;
                    }

                    return new ItemBuilder(material)
                            .name(gadgetsMenu().getString(basePath + "PORTAL_SET_NAME",
                                    "&dPortal Jump &a[SET]", true))
                            .lore(gadgetsMenu().getStringList(basePath + "PORTAL_SET_LORE"))
                            .data(gadgetsMenu().getInt(basePath + "DATA"))
                            .build();
                }
            }
            return getItemByType(normalizedType);
        }

        return buildNoPermissionItem();
    }

    public static ItemStack buildNoPermissionItem() {
        return buildNoPermissionItem("&fGadget");
    }

    public static ItemStack buildNoPermissionItem(String gadgetName) {
        String basePath = "GADGETS_MENU.NO_PERMISSION_ITEM.";
        String materialName = gadgetsMenu().getString(basePath + "MATERIAL", "RED_WOOL", false);
        Material material = Material.matchMaterial(materialName);
        int data = gadgetsMenu().getInt(basePath + "DATA");
        if (material == null && "RED_WOOL".equalsIgnoreCase(materialName)) {
            material = Material.matchMaterial("WOOL");
            data = 14;
        }
        if (material == null) {
            material = Material.BARRIER;
        }

        String name = gadgetsMenu().getString(basePath + "NAME", "&cGadget locked", true)
                .replace("%gadget%", gadgetName == null ? "&fGadget" : gadgetName)
                .replace("%gadget_name%", gadgetName == null ? "Gadget" : CC.translate(gadgetName));
        List<String> lore = gadgetsMenu().getStringList(basePath + "LORE");
        lore.replaceAll(line -> line.replace("%gadget_name%", gadgetName == null ? "Gadget" : CC.translate(gadgetName)));

        return new ItemBuilder(material)
                .name(name)
                .lore(lore)
                .data(data)
                .build();
    }

    public static boolean use(Player player, String type) {
        String normalizedType = normalizeType(type);
        if (normalizedType == null || normalizedType.equalsIgnoreCase("NONE")) {
            return false;
        }
        if (!isEnabled(normalizedType)) {
            player.sendMessage(CC.translate("&cThis gadget is currently disabled."));
            return false;
        }
        if (!hasPermission(player, normalizedType)) {
            sendActionBar(player, "ACTIONBAR.GADGET_NO_PERMISSION", Map.of());
            return false;
        }

        if (normalizedType.equalsIgnoreCase("RAINBOW_TRAIL")) {
            useRainbowTrail(player);
            return true;
        }
        if (normalizedType.equalsIgnoreCase("ICE_TRAIL")) {
            useIceTrail(player);
            return true;
        }

        long left = getCooldownSecondsLeft(player.getUniqueId(), normalizedType);
        if (left > 0) {
            sendActionBar(player, "ACTIONBAR.GADGET_COOLDOWN", Map.of("%time%", String.valueOf(left)));
            return false;
        }

        boolean used;
        switch (normalizedType.toUpperCase()) {
            case "SNOWBALL_VELOCITY":
                useEnderButtVelocity(player);
                used = true;
                break;
            case "ENDERBUTT_RIDEABLE":
                useEnderButtRideable(player);
                used = true;
                break;
            case "GRAPPLING_HOOK":
                useGrapplingHook(player);
                used = true;
                break;
            case "LEAP_BOOST":
                useLeapBoost(player);
                used = true;
                break;
            case "FIREWORK_DASH":
                useFireworkDash(player);
                used = true;
                break;
            case "DASH_PAD":
                useDashPad(player);
                used = true;
                break;
            case "BLINK":
                used = useBlink(player);
                break;
            case "BOOMERANG":
                useBoomerang(player);
                used = true;
                break;
            case "JETPACK_BURST":
                useJetpackBurst(player);
                used = true;
                break;
            case "CLONE_DECOY":
                useCloneDecoy(player);
                used = true;
                break;
            case "COLOR_BOMB":
                useColorBomb(player);
                used = true;
                break;
            case "PORTAL_JUMP":
                usePortalJump(player);
                used = true;
                break;
            case "CONFETTI_CANNON":
                useConfettiCannon(player);
                used = true;
                break;
            default:
                used = false;
                break;
        }

        if (!used) {
            return false;
        }

        long cooldown = getCooldownSeconds(normalizedType);
        if (cooldown > 0) {
            COOLDOWNS.computeIfAbsent(player.getUniqueId(), uuid -> new HashMap<>())
                    .put(normalizedType, System.currentTimeMillis() + (cooldown * 1000L));
        }
        sendActionBar(player, "ACTIONBAR.GADGET_USED", Map.of("%gadget%", getDisplayNameByType(normalizedType)));
        return true;
    }

    private static String getKeyByType(String type) {
        if (type == null) {
            return null;
        }

        ConfigurationSection section = gadgetsMenu().getConfiguration().getConfigurationSection("GADGETS_MENU.ITEMS");
        if (section == null) {
            return null;
        }

        for (String key : section.getKeys(false)) {
            String configType = gadgetsMenu().getString("GADGETS_MENU.ITEMS." + key + ".TYPE", "", false);
            if (type.equalsIgnoreCase(normalizeType(configType))) {
                return key;
            }
        }
        return null;
    }

    private static void useEnderButtVelocity(Player player) {
        String path = getSnowballVelocityPath();
        double boostY = settings().getDouble(path + ".BOOST");
        double multiplier = settings().getDouble(path + ".MULTIPLIER");

        // Visual projectile so this mode does not look like a plain velocity jump.
        Snowball snowball = player.launchProjectile(Snowball.class);
        hideEntityFromHiddenViewers(player, snowball);
        Bukkit.getScheduler().runTaskLater(Celest.get(), snowball::remove, 1L);

        Vector velocity = player.getLocation().getDirection().normalize().multiply(multiplier);
        // Keep this mode forward-oriented, with limited vertical boost.
        double limitedY = Math.max(0.15D, Math.min(0.55D, boostY));
        velocity.setY(limitedY);
        player.setVelocity(velocity);
    }

    private static void useEnderButtRideable(Player player) {
        if (player.isSneaking()) {
            return;
        }

        if (player.getVehicle() != null) {
            player.getVehicle().remove();
            player.eject();
        }
        if (player.isInsideVehicle() && player.getVehicle() != null) {
            player.getVehicle().remove();
        }

        double boostY = settings().getDouble("GADGETS.ENDERBUTT_RIDEABLE.BOOST");
        double multiplier = settings().getDouble("GADGETS.ENDERBUTT_RIDEABLE.MULTIPLIER");
        String projectileType = settings().getString("GADGETS.ENDERBUTT_RIDEABLE.PROJECTILE", "ENDER_PEARL", false);
        Material inHand = player.getInventory().getItemInMainHand() == null ? Material.AIR : player.getInventory().getItemInMainHand().getType();
        Projectile projectile;
        if (inHand == Material.SNOWBALL || "SNOWBALL".equalsIgnoreCase(projectileType)) {
            projectile = player.launchProjectile(Snowball.class);
        } else if (inHand == Material.FIRE_CHARGE || "FIREBALL".equalsIgnoreCase(projectileType) || "FIRE_CHARGE".equalsIgnoreCase(projectileType)) {
            projectile = player.launchProjectile(Fireball.class);
        } else {
            projectile = player.launchProjectile(EnderPearl.class);
        }
        hideEntityFromHiddenViewers(player, projectile);

        Vector direction = player.getLocation().getDirection().normalize();
        double finalMultiplier = Math.max(0.1D, multiplier + boostY);
        projectile.setVelocity(direction.multiply(finalMultiplier));
        projectile.addPassenger(player);
        disablePlayerCollisionTemporary(player);
    }

    private static void useGrapplingHook(Player player) {
        int maxDistance = settings().getInt("GADGETS.GRAPPLING_HOOK.MAX_DISTANCE");
        double strength = settings().getDouble("GADGETS.GRAPPLING_HOOK.STRENGTH");
        double yBoost = settings().getDouble("GADGETS.GRAPPLING_HOOK.Y_BOOST");

        FishHook hook = player.launchProjectile(FishHook.class);
        hideEntityFromHiddenViewers(player, hook);
        Bukkit.getScheduler().runTaskLater(Celest.get(), hook::remove, 8L);

        if (player.getTargetBlockExact(maxDistance) == null) {
            player.sendMessage(CC.translate("&cNo valid block in range."));
            return;
        }

        Vector target = player.getTargetBlockExact(maxDistance).getLocation().toVector().add(new Vector(0.5, 1.0, 0.5));
        Vector current = player.getLocation().toVector();
        Vector velocity = target.subtract(current).normalize().multiply(strength).setY(yBoost);
        player.setVelocity(velocity);
    }

    private static void useLeapBoost(Player player) {
        double multiplier = settings().getDouble("GADGETS.LEAP_BOOST.MULTIPLIER");
        double yBoost = settings().getDouble("GADGETS.LEAP_BOOST.Y_BOOST");
        player.setVelocity(player.getLocation().getDirection().normalize().multiply(multiplier).setY(yBoost));
    }

    private static void useRainbowTrail(Player player) {
        int revertSeconds = Math.max(1, settings().getInt("GADGETS.RAINBOW_TRAIL.REVERT_SECONDS"));
        UUID uuid = player.getUniqueId();

        if (isRainbowTrailActive(player)) {
            stopRainbowTrail(player, true);
            sendActionBar(player, "ACTIONBAR.GADGET_DISABLED", Map.of("%gadget%", getDisplayNameByType("RAINBOW_TRAIL")));
            return;
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(Celest.get(), () -> {
            if (!player.isOnline()) {
                stopRainbowTrail(player, false);
                return;
            }

            Location blockLocation = player.getLocation().clone().subtract(0, 1, 0).getBlock().getLocation();
            Material originalType = blockLocation.getBlock().getType();
            if (isAir(originalType)) {
                return;
            }

            String key = blockKey(blockLocation);
            String previous = RAINBOW_LAST_BLOCK.get(uuid);
            if (key.equals(previous)) {
                return;
            }

            RAINBOW_LAST_BLOCK.put(uuid, key);
            Material material = RAINBOW_GLASS[ThreadLocalRandom.current().nextInt(RAINBOW_GLASS.length)];
            sendFakeBlockToVisibleViewers(player, blockLocation, material);
            Bukkit.getScheduler().runTaskLater(Celest.get(), () -> sendRealBlockToVisibleViewers(player, blockLocation), Math.max(1L, revertSeconds) * 20L);
        }, 0L, 2L);

        RAINBOW_TASKS.put(uuid, task);
        sendActionBar(player, "ACTIONBAR.GADGET_USED", Map.of("%gadget%", getDisplayNameByType("RAINBOW_TRAIL")));
    }

    private static void useFireworkDash(Player player) {
        double multiplier = settings().getDouble("GADGETS.FIREWORK_DASH.MULTIPLIER");
        double yBoost = settings().getDouble("GADGETS.FIREWORK_DASH.Y_BOOST");
        player.setVelocity(player.getLocation().getDirection().normalize().multiply(multiplier).setY(yBoost));

        Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
        hideEntityFromHiddenViewers(player, firework);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.setPower(Math.max(0, settings().getInt("GADGETS.FIREWORK_DASH.POWER")));
        firework.setFireworkMeta(meta);
        Bukkit.getScheduler().runTaskLater(Celest.get(), firework::detonate, 2L);
    }

    private static void useDashPad(Player player) {
        double multiplier = settings().getDouble("GADGETS.DASH_PAD.MULTIPLIER");
        double yBoost = settings().getDouble("GADGETS.DASH_PAD.Y_BOOST");
        player.setVelocity(player.getLocation().getDirection().normalize().multiply(multiplier).setY(yBoost));
        spawnParticleToVisibleViewers(player, Particle.CLOUD, player.getLocation().add(0, 0.1, 0), 28, 0.5, 0.15, 0.5, 0.03);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0F, 1.35F);
    }

    private static boolean useBlink(Player player) {
        int maxDistance = Math.max(2, settings().getInt("GADGETS.BLINK.MAX_DISTANCE"));
        if (player.getTargetBlockExact(maxDistance) == null) {
            sendActionBar(player, "ACTIONBAR.GADGET_INVALID_TARGET", Map.of());
            return false;
        }

        Location destination = player.getTargetBlockExact(maxDistance).getLocation().add(0.5, 1.0, 0.5);
        if (destination.getBlock().getType().isSolid()) {
            destination = destination.add(0, 1, 0);
        }
        if (destination.getBlock().getType().isSolid()) {
            sendActionBar(player, "ACTIONBAR.GADGET_INVALID_TARGET", Map.of());
            return false;
        }

        spawnParticleToVisibleViewers(player, Particle.PORTAL, player.getLocation().add(0, 1.0, 0), 35, 0.3, 0.6, 0.3, 0.02);
        player.teleport(destination);
        spawnParticleToVisibleViewers(player, Particle.PORTAL, destination.add(0, 0.5, 0), 35, 0.3, 0.6, 0.3, 0.02);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.2F);
        return true;
    }

    private static void useBoomerang(Player player) {
        double speed = settings().getDouble("GADGETS.BOOMERANG.SPEED");
        double returnSpeed = settings().getDouble("GADGETS.BOOMERANG.RETURN_SPEED");
        int returnTicks = Math.max(4, settings().getInt("GADGETS.BOOMERANG.RETURN_AFTER_TICKS"));
        int removeTicks = Math.max(returnTicks + 2, settings().getInt("GADGETS.BOOMERANG.REMOVE_AFTER_TICKS"));

        Snowball boomerang = player.launchProjectile(Snowball.class);
        hideEntityFromHiddenViewers(player, boomerang);
        boomerang.setVelocity(player.getLocation().getDirection().normalize().multiply(speed));
        boomerang.addScoreboardTag(GADGET_ENTITY_TAG);

        Bukkit.getScheduler().runTaskLater(Celest.get(), () -> {
            if (!boomerang.isValid() || boomerang.isDead() || !player.isOnline()) {
                return;
            }
            Vector back = player.getEyeLocation().toVector().subtract(boomerang.getLocation().toVector()).normalize().multiply(returnSpeed);
            boomerang.setVelocity(back);
            spawnParticleToVisibleViewers(player, Particle.CRIT, boomerang.getLocation(), 10, 0.2, 0.2, 0.2, 0.02);
        }, returnTicks);

        Bukkit.getScheduler().runTaskLater(Celest.get(), boomerang::remove, removeTicks);
    }

    private static void useIceTrail(Player player) {
        int revertSeconds = Math.max(1, settings().getInt("GADGETS.ICE_TRAIL.REVERT_SECONDS"));
        UUID uuid = player.getUniqueId();

        if (isIceTrailActive(player)) {
            stopIceTrail(player, true);
            sendActionBar(player, "ACTIONBAR.GADGET_DISABLED", Map.of("%gadget%", getDisplayNameByType("ICE_TRAIL")));
            return;
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(Celest.get(), () -> {
            if (!player.isOnline()) {
                stopIceTrail(player, false);
                return;
            }

            Location blockLocation = player.getLocation().clone().subtract(0, 1, 0).getBlock().getLocation();
            Material originalType = blockLocation.getBlock().getType();
            if (isAir(originalType)) {
                return;
            }

            String key = blockKey(blockLocation);
            String previous = ICE_TRAIL_LAST_BLOCK.get(uuid);
            if (key.equals(previous)) {
                return;
            }

            ICE_TRAIL_LAST_BLOCK.put(uuid, key);
            sendFakeBlockToVisibleViewers(player, blockLocation, Material.PACKED_ICE);
            Bukkit.getScheduler().runTaskLater(Celest.get(), () -> sendRealBlockToVisibleViewers(player, blockLocation), Math.max(1L, revertSeconds) * 20L);
        }, 0L, 2L);

        ICE_TRAIL_TASKS.put(uuid, task);
        sendActionBar(player, "ACTIONBAR.GADGET_USED", Map.of("%gadget%", getDisplayNameByType("ICE_TRAIL")));
    }

    private static void useJetpackBurst(Player player) {
        double verticalBoost = settings().getDouble("GADGETS.JETPACK_BURST.VERTICAL_BOOST");
        int slowFallingSeconds = Math.max(1, settings().getInt("GADGETS.JETPACK_BURST.SLOW_FALLING_SECONDS"));
        player.setVelocity(new Vector(0, verticalBoost, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, slowFallingSeconds * 20, 0, false, false, true));
        spawnParticleToVisibleViewers(player, Particle.FLAME, player.getLocation().add(0, 0.2, 0), 30, 0.3, 0.1, 0.3, 0.02);
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8F, 1.3F);
    }

    private static void useCloneDecoy(Player player) {
        int durationSeconds = Math.max(1, settings().getInt("GADGETS.CLONE_DECOY.DURATION_SECONDS"));
        Location spawn = player.getLocation().clone();

        ArmorStand decoy = player.getWorld().spawn(spawn, ArmorStand.class, stand -> {
            stand.addScoreboardTag(GADGET_ENTITY_TAG);
            stand.setVisible(true);
            stand.setCustomName(player.getName());
            stand.setCustomNameVisible(true);
            stand.setArms(true);
            stand.setBasePlate(false);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setCollidable(false);
            stand.setSilent(true);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(player);
                head.setItemMeta(meta);
            }
            stand.getEquipment().setHelmet(head);
        });

        hideEntityFromHiddenViewers(player, decoy);
        Bukkit.getScheduler().runTaskLater(Celest.get(), decoy::remove, durationSeconds * 20L);
        spawnParticleToVisibleViewers(player, Particle.POOF, spawn.add(0, 1, 0), 25, 0.3, 0.4, 0.3, 0.02);
    }

    private static void useColorBomb(Player player) {
        int bursts = Math.max(1, settings().getInt("GADGETS.COLOR_BOMB.BURSTS"));
        int particlesPerBurst = Math.max(12, settings().getInt("GADGETS.COLOR_BOMB.PARTICLES_PER_BURST"));
        Location center = player.getLocation().add(0, 1.0, 0);

        for (int i = 0; i < bursts; i++) {
            Particle.DustOptions color = randomDustColor();
            spawnParticleToVisibleViewers(player, Particle.DUST, center, particlesPerBurst, 0.7, 0.7, 0.7, 0.02, color);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8F, 1.2F);
    }

    private static void usePortalJump(Player player) {
        UUID uuid = player.getUniqueId();
        Location saved = PORTAL_POINTS.get(uuid);
        Location current = player.getLocation().clone();

        if (saved == null) {
            PORTAL_POINTS.put(uuid, current);
            spawnParticleToVisibleViewers(player, Particle.PORTAL, current.add(0, 1, 0), 25, 0.3, 0.6, 0.3, 0.01);
            updateSelectedGadgetItem(player);
            sendActionBar(player, "ACTIONBAR.GADGET_PORTAL_SET", Map.of());
            return;
        }

        PORTAL_POINTS.remove(uuid);
        spawnParticleToVisibleViewers(player, Particle.PORTAL, player.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.01);
        player.teleport(saved);
        spawnParticleToVisibleViewers(player, Particle.PORTAL, saved.add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.01);
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.8F, 1.1F);
        updateSelectedGadgetItem(player);
    }

    private static void useConfettiCannon(Player player) {
        int tntCount = Math.max(1, settings().getInt("GADGETS.CONFETTI_CANNON.TNT_COUNT"));
        double speed = settings().getDouble("GADGETS.CONFETTI_CANNON.SPEED");
        Vector direction = player.getLocation().getDirection().normalize();

        for (int i = 0; i < tntCount; i++) {
            TNTPrimed tnt = player.getWorld().spawn(player.getEyeLocation(), TNTPrimed.class);
            tnt.setFuseTicks(Math.max(18, settings().getInt("GADGETS.CONFETTI_CANNON.FUSE_TICKS")));
            tnt.setYield(0F);
            tnt.setIsIncendiary(false);
            tnt.addScoreboardTag(GADGET_ENTITY_TAG);
            hideEntityFromHiddenViewers(player, tnt);

            Vector spread = direction.clone().add(new Vector(
                    ThreadLocalRandom.current().nextDouble(-0.16, 0.16),
                    ThreadLocalRandom.current().nextDouble(-0.06, 0.22),
                    ThreadLocalRandom.current().nextDouble(-0.16, 0.16)
            )).normalize().multiply(speed);
            tnt.setVelocity(spread);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.9F, 1.3F);
    }

    private static Particle.DustOptions randomDustColor() {
        org.bukkit.Color color = org.bukkit.Color.fromRGB(
                ThreadLocalRandom.current().nextInt(256),
                ThreadLocalRandom.current().nextInt(256),
                ThreadLocalRandom.current().nextInt(256)
        );
        return new Particle.DustOptions(color, 1.2F);
    }

    private static void sendFakeBlockToVisibleViewers(Player owner, Location location, Material material) {
        for (Player viewer : owner.getWorld().getPlayers()) {
            if (!canViewerSeeOwner(viewer, owner)) {
                continue;
            }
            viewer.sendBlockChange(location, material.createBlockData());
        }
    }

    private static void sendRealBlockToVisibleViewers(Player owner, Location location) {
        for (Player viewer : owner.getWorld().getPlayers()) {
            if (!canViewerSeeOwner(viewer, owner)) {
                continue;
            }
            viewer.sendBlockChange(location, location.getBlock().getBlockData());
        }
    }

    private static String blockKey(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    public static void deactivatePersistentEffects(Player player) {
        stopRainbowTrail(player, true);
        stopIceTrail(player, true);
        PORTAL_POINTS.remove(player.getUniqueId());
        restorePlayerCollision(player);
        updateSelectedGadgetItem(player);
    }

    public static void restorePlayerCollision(Player player) {
        BukkitTask task = COLLISION_RESET_TASKS.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        player.setCollidable(true);
    }

    private static void disablePlayerCollisionTemporary(Player player) {
        BukkitTask previous = COLLISION_RESET_TASKS.remove(player.getUniqueId());
        if (previous != null) {
            previous.cancel();
        }
        player.setCollidable(false);
        BukkitTask resetTask = Bukkit.getScheduler().runTaskLater(Celest.get(), () -> {
            if (player.isOnline()) {
                player.setCollidable(true);
            }
            COLLISION_RESET_TASKS.remove(player.getUniqueId());
        }, 60L);
        COLLISION_RESET_TASKS.put(player.getUniqueId(), resetTask);
    }

    public static boolean isRainbowTrailActive(Player player) {
        return RAINBOW_TASKS.containsKey(player.getUniqueId());
    }

    public static boolean isGadgetEntity(org.bukkit.entity.Entity entity) {
        return entity != null && entity.getScoreboardTags().contains(GADGET_ENTITY_TAG);
    }

    private static boolean isIceTrailActive(Player player) {
        return ICE_TRAIL_TASKS.containsKey(player.getUniqueId());
    }

    private static void stopRainbowTrail(Player player, boolean restoreLastBlock) {
        UUID uuid = player.getUniqueId();
        BukkitTask running = RAINBOW_TASKS.remove(uuid);
        if (running != null) {
            running.cancel();
        }

        String key = RAINBOW_LAST_BLOCK.remove(uuid);
        if (!restoreLastBlock || key == null) {
            return;
        }

        String[] split = key.split(":");
        if (split.length != 4) {
            return;
        }

        if (Bukkit.getWorld(split[0]) == null) {
            return;
        }

        try {
            int x = Integer.parseInt(split[1]);
            int y = Integer.parseInt(split[2]);
            int z = Integer.parseInt(split[3]);
            sendRealBlockToVisibleViewers(player, new Location(Bukkit.getWorld(split[0]), x, y, z));
        } catch (NumberFormatException ex) {
            return;
        }
    }

    private static void stopIceTrail(Player player, boolean restoreLastBlock) {
        UUID uuid = player.getUniqueId();
        BukkitTask running = ICE_TRAIL_TASKS.remove(uuid);
        if (running != null) {
            running.cancel();
        }

        String key = ICE_TRAIL_LAST_BLOCK.remove(uuid);
        if (!restoreLastBlock || key == null) {
            return;
        }
        restoreBlockFromKey(player, key);
    }

    private static void updateSelectedGadgetItem(Player player) {
        Profile profile = ModuleService.getManagerModule().getProfileManager().getProfile(player.getUniqueId());
        if (profile == null || profile.getSelectedGadgetType() == null || profile.getSelectedGadgetType().equalsIgnoreCase("NONE")) {
            return;
        }

        Hotbar gadgetHotbar = ModuleService.getManagerModule().getHotbarManager().getHotbar("GADGETS");
        if (gadgetHotbar == null || !gadgetHotbar.isEnabled()) {
            return;
        }

        ItemStack item = getItemForPlayer(profile.getSelectedGadgetType(), player);
        if (item != null) {
            player.getInventory().setItem(gadgetHotbar.getSlot(), item);
        }
    }

    private static void restoreBlockFromKey(Player player, String key) {
        String[] split = key.split(":");
        if (split.length != 4) {
            return;
        }

        if (Bukkit.getWorld(split[0]) == null) {
            return;
        }

        try {
            int x = Integer.parseInt(split[1]);
            int y = Integer.parseInt(split[2]);
            int z = Integer.parseInt(split[3]);
            sendRealBlockToVisibleViewers(player, new Location(Bukkit.getWorld(split[0]), x, y, z));
        } catch (NumberFormatException ex) {
            return;
        }
    }

    private static boolean canViewerSeeOwner(Player viewer, Player owner) {
        return viewer.equals(owner) || viewer.canSee(owner);
    }

    private static void hideEntityFromHiddenViewers(Player owner, Entity entity) {
        for (Player viewer : owner.getWorld().getPlayers()) {
            if (canViewerSeeOwner(viewer, owner)) {
                continue;
            }
            viewer.hideEntity(Celest.get(), entity);
        }
    }

    private static void spawnParticleToVisibleViewers(Player owner, Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        for (Player viewer : owner.getWorld().getPlayers()) {
            if (!canViewerSeeOwner(viewer, owner)) {
                continue;
            }
            viewer.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
        }
    }

    private static <T> void spawnParticleToVisibleViewers(Player owner, Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra, T data) {
        for (Player viewer : owner.getWorld().getPlayers()) {
            if (!canViewerSeeOwner(viewer, owner)) {
                continue;
            }
            viewer.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, data);
        }
    }

    private static boolean isAir(Material material) {
        return material == Material.AIR
                || material.name().equals("CAVE_AIR")
                || material.name().equals("VOID_AIR");
    }

    public static String getDisplayNameByType(String type) {
        String key = getKeyByType(normalizeType(type));
        if (key == null) {
            return CC.translate("&cNone");
        }
        return gadgetsMenu().getString("GADGETS_MENU.ITEMS." + key + ".NAME", key, true);
    }

    public static boolean isSameType(String first, String second) {
        String normalizedFirst = normalizeType(first);
        String normalizedSecond = normalizeType(second);
        if (normalizedFirst == null || normalizedSecond == null) {
            return false;
        }
        return normalizedFirst.equalsIgnoreCase(normalizedSecond);
    }

    public static boolean hasPermission(Player player, String type) {
        String key = getKeyByType(normalizeType(type));
        if (key == null) {
            return true;
        }

        String permission = gadgetsMenu().getString("GADGETS_MENU.ITEMS." + key + ".PERMISSION", "", false);
        return permission == null
                || permission.isEmpty()
                || player.hasPermission(permission)
                || player.hasPermission("celest.gadget.*");
    }

    public static boolean isEnabled(String type) {
        String key = getKeyByType(normalizeType(type));
        if (key == null) {
            return true;
        }
        String path = "GADGETS_MENU.ITEMS." + key + ".ENABLED";
        return !gadgetsMenu().getConfiguration().contains(path) || gadgetsMenu().getBoolean(path);
    }

    private static long getCooldownSecondsLeft(UUID uuid, String type) {
        Map<String, Long> byType = COOLDOWNS.get(uuid);
        if (byType == null) {
            return 0L;
        }

        Long expireAt = byType.get(type);
        if (expireAt == null) {
            return 0L;
        }

        long leftMillis = expireAt - System.currentTimeMillis();
        if (leftMillis <= 0L) {
            byType.remove(type);
            if (byType.isEmpty()) {
                COOLDOWNS.remove(uuid);
            }
            return 0L;
        }

        return (long) Math.ceil(leftMillis / 1000.0D);
    }

    private static long getCooldownSeconds(String type) {
        String normalized = normalizeType(type);
        if (normalized == null) {
            return 0L;
        }

        String perTypePath = "GADGETS." + normalized + ".COOLDOWN_SECONDS";
        if (settings().getConfiguration().contains(perTypePath)) {
            return Math.max(0L, settings().getInt(perTypePath));
        }

        return Math.max(0L, settings().getInt("GADGETS.COOLDOWN_SECONDS"));
    }

    private static String normalizeType(String type) {
        if (type == null) {
            return null;
        }
        if (type.equalsIgnoreCase("ENDERBUTT_VELOCITY")) {
            return "SNOWBALL_VELOCITY";
        }
        return type.toUpperCase();
    }

    private static String getSnowballVelocityPath() {
        if (settings().getConfiguration().contains("GADGETS.SNOWBALL_VELOCITY")) {
            return "GADGETS.SNOWBALL_VELOCITY";
        }
        return "GADGETS.ENDERBUTT_VELOCITY";
    }

    private static void sendActionBar(Player player, String path, Map<String, String> placeholders) {
        FileConfig messages = ModuleService.getFileModule().getFile("messages");
        String prefix = messages.getString("ACTIONBAR.PREFIX", "", true);
        String message = messages.getString(path, "", true);
        if (message == null || message.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }

        player.sendActionBar(SERIALIZER.deserialize(CC.translate((prefix == null ? "" : prefix) + message)));
    }
}
