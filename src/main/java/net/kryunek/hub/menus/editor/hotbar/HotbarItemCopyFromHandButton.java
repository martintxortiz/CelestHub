package net.kryunek.hub.menus.editor.hotbar;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.AllArgsConstructor;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public class HotbarItemCopyFromHandButton extends Button {

    private final String key;
    private final FileConfig hotbar = ModuleService.getFileModule().getFile(ConfigFiles.HOTBAR);
    private final FileConfig messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(Material.CHEST)
                .name(CC.translate("&bSet Icon From Hand"))
                .lore(Arrays.asList(
                        CC.translate("&7Copies icon, amount, name and lore"),
                        CC.translate("&7from held item."),
                        "",
                        CC.translate("&eClick to apply")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            playFail(player);
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.HAND_EMPTY", "&cHold an item in your hand first.", true)));
            return;
        }

        ItemMeta meta = hand.getItemMeta();
        String name = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : "&f" + hand.getType().name();
        List<String> lore = meta != null && meta.hasLore() ? meta.getLore() : new ArrayList<>();
        int data = (meta instanceof Damageable damageable) ? damageable.getDamage() : 0;

        hotbar.getConfiguration().set(key + ".MATERIAL", hand.getType().name());
        hotbar.getConfiguration().set(key + ".DATA", data);
        hotbar.getConfiguration().set(key + ".AMOUNT", hand.getAmount());
        hotbar.getConfiguration().set(key + ".NAME", name);
        hotbar.getConfiguration().set(key + ".LORE", lore);
        if (meta instanceof SkullMeta skullMeta && skullMeta.getOwningPlayer() != null && skullMeta.getOwningPlayer().getName() != null) {
            hotbar.getConfiguration().set(key + ".HEAD_OWNER", skullMeta.getOwningPlayer().getName());
            if (skullMeta.getOwningPlayer().getUniqueId() != null) {
                hotbar.getConfiguration().set(key + ".HEAD_OWNER_UUID", skullMeta.getOwningPlayer().getUniqueId().toString());
            } else {
                hotbar.getConfiguration().set(key + ".HEAD_OWNER_UUID", null);
            }
        } else {
            hotbar.getConfiguration().set(key + ".HEAD_OWNER", null);
            hotbar.getConfiguration().set(key + ".HEAD_OWNER_UUID", null);
        }
        hotbar.save();
        ModuleService.getManagerModule().getHotbarManager().load();
        ModuleService.getManagerModule().getHotbarManager().reload();

        playSuccess(player);
        new HotbarItemEditorMenu(key).openMenu(player);
    }
}
