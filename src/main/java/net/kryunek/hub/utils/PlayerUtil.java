package net.kryunek.hub.utils;

import net.kryunek.hub.Celest;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UtilityClass
public class PlayerUtil {

    private static final Map<Player, Long> LAST_MOVE = new HashMap<>();


    public static boolean timeElapsed(long from, long required) {
        return System.currentTimeMillis() - from > required;
    }



    @EventHandler(priority = EventPriority.MONITOR)
    public void setMoving(PlayerMoveEvent event) {
        Location from = event.getFrom(), to = event.getTo();

        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }

        LAST_MOVE.put(event.getPlayer(), System.currentTimeMillis());
    }



    public void clear(Player player, boolean armor, boolean inventory) {
        if (armor) player.getInventory().setArmorContents(new ItemStack[4]);
        if (inventory) player.getInventory().clear();
    }
    public void denyMovement(final Player player) {
        player.setWalkSpeed(0.0F);
        player.setFlySpeed(0.0F);
        player.setFoodLevel(1);
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 200));
    }

    public void allowMovement(final Player player) {
        player.setWalkSpeed(0.2F);
        player.setFlySpeed(0.1F);
        player.setFoodLevel(20);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
    }

    public void applyHubFlyState(Player player, boolean enabled, boolean boost) {
        if (enabled) {
            player.setAllowFlight(true);
            player.setFlying(true);
            if (boost) {
                applyFlyBoost(player);
            }

            Bukkit.getScheduler().runTaskLater(Celest.get(), () -> {
                if (!player.isOnline()) {
                    return;
                }
                player.setAllowFlight(true);
                player.setFlying(true);
                if (boost) {
                    applyFlyBoost(player);
                }
            }, 1L);
            return;
        }

        player.setFlying(false);
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setAllowFlight(false);
        }
    }

    private void applyFlyBoost(Player player) {
        Vector velocity = player.getVelocity().clone();
        velocity.setY(Math.max(velocity.getY(), 0.45D));
        player.setVelocity(velocity);
    }



    public int getSlotsPlayers() {
        int multiplier = 1;
        List<Player> playerList = new ArrayList<>();
        playerList = Bukkit.getOnlinePlayers().stream().collect(Collectors.toList());
        int i = 0;
        for (Player onlinePlayer : playerList) {
            ++i;
        }
        if (i > 9*multiplier ){
            ++multiplier;
        }

        return i*multiplier;
    }
}
