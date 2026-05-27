package net.kryunek.hub.utils;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class ItemBuilder {

    private final ItemStack itemStack;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material, 1);
    }

    public ItemBuilder(String material) {
        this.itemStack = new ItemStack(Material.valueOf(material), 1);
    }


    public ItemBuilder(int material) {
        this.itemStack = new ItemStack(Material.valueOf(String.valueOf(material)), 1);
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
    }

    public ItemBuilder(Material material, int damage) {
        this.itemStack = new ItemStack(material, 1, (short) damage);
    }

    public ItemBuilder(Material material, int amount, int damage) {
        this.itemStack = new ItemStack(material, amount, (short) damage);
    }
    public static ItemStack createSkull(String player, String playername, List<String> skullLore) {
        return new ItemBuilder(Material.PLAYER_HEAD).owner(player).name(playername).lore(skullLore).build();
    }

    public ItemBuilder name(String name) {
        if (name != null) {
            name = CC.translate(name);
            ItemMeta meta = this.itemStack.getItemMeta();
            meta.setDisplayName(name);
            this.itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder lore(List<String> lore) {
        if (lore != null) {
            ItemMeta meta = this.itemStack.getItemMeta();
            meta.setLore(CC.translate(lore));
            this.itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder lore(String... lore) {
        if (lore != null) {
            ItemMeta meta = this.itemStack.getItemMeta();
            meta.setLore(CC.translate(Arrays.asList(lore)));
            this.itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder amount(int amount) {
        this.itemStack.setAmount(amount);
        return this;
    }

    public ItemBuilder enchant(boolean enchanted) {
        if (enchanted) {
            ItemMeta meta = this.itemStack.getItemMeta();
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            this.itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder enchant(boolean enchanted, int level) {
        if (enchanted) {
            ItemMeta meta = this.itemStack.getItemMeta();
            meta.addEnchant(Enchantment.UNBREAKING, level, true);
            this.itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder enchant(boolean enchanted, Enchantment enchant, int level) {
        if (enchanted) {
            ItemMeta meta = this.itemStack.getItemMeta();
            meta.addEnchant(enchant, level, true);
            this.itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder data(int dur) {
        this.itemStack.setDurability((short) dur);
        return this;
    }

    public ItemBuilder owner(String owner) {
        if (this.itemStack.getType() != Material.PLAYER_HEAD) {
            throw new IllegalArgumentException("setOwner() only applicable for Skull Item");
        }

        if (owner != null) {
            try {
                UUID uuid = UUID.fromString(owner);
                return owner(uuid);
            } catch (IllegalArgumentException notUuid) {
                // Treat non-UUID values as player names below.
            }
        }

        SkullMeta meta = (SkullMeta) this.itemStack.getItemMeta();
        Player online = Bukkit.getPlayerExact(owner);
        if (online != null) {
            meta.setOwningPlayer(online);
        } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(owner);
            meta.setOwningPlayer(offlinePlayer);
        }
        this.itemStack.setItemMeta(meta);

        return this;
    }

    public ItemBuilder owner(UUID uuid) {
        return owner(uuid, null);
    }

    public ItemBuilder owner(UUID uuid, String fallbackName) {
        if (this.itemStack.getType() != Material.PLAYER_HEAD) {
            throw new IllegalArgumentException("owner() only applicable for Player Heads");
        }

        SkullMeta meta = (SkullMeta) this.itemStack.getItemMeta();
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            meta.setOwningPlayer(online);
        } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            meta.setOwningPlayer(offlinePlayer);
            try {
                String name = fallbackName;
                if (name == null || name.isBlank()) {
                    name = offlinePlayer.getName();
                }
                PlayerProfile profile = Bukkit.createPlayerProfile(uuid, name);
                meta.setOwnerProfile(profile);
            } catch (Throwable ex) {
                Bukkit.getLogger().log(Level.FINE, "Failed to apply owner profile to player head.", ex);
            }
        }

        this.itemStack.setItemMeta(meta);
        return this;
    }
    public ItemBuilder durability(int durability) {
        itemStack.setDurability((short) durability);
        return this;
    }

    public ItemBuilder armorColor(Color color) {
        try {
            LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) this.itemStack.getItemMeta();
            leatherArmorMeta.setColor(color);
            this.itemStack.setItemMeta(leatherArmorMeta);
        } catch (Exception exception) {
            Bukkit.getConsoleSender().sendMessage("Error set armor color.");
        }
        return this;
    }




    public ItemStack build() {
        return this.itemStack;
    }
}
