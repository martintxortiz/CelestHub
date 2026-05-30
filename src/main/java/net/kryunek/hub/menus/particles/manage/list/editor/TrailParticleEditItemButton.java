package net.kryunek.hub.menus.particles.manage.list.editor;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.particles.TrailParticle;
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

import java.util.Arrays;

public class TrailParticleEditItemButton extends Button {

    private final String trailName;
    private final FileConfig messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);

    public TrailParticleEditItemButton(String trailName) {
        this.trailName = trailName;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(Material.CHEST)
                .name(CC.translate("&bEdit Trail Item"))
                .lore(Arrays.asList(
                        CC.translate("&7Change icon item of &f" + this.trailName),
                        CC.translate("&7using the item in your hand."),
                        "",
                        CC.translate("&eClick to apply from hand")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        TrailParticle trail = ModuleService.getManagerModule().getTrailParticleManager().getTrail(this.trailName);
        if (trail == null) {
            playFail(player);
            player.sendMessage(CC.translate("&cTrail particle not found."));
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            playFail(player);
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.HAND_EMPTY", "&cHold an item in your hand first.", true)));
            return;
        }

        ItemMeta meta = hand.getItemMeta();
        int data = (meta instanceof Damageable damageable) ? damageable.getDamage() : 0;
        boolean updated = ModuleService.getManagerModule().getTrailParticleManager()
                .updateTrailIcon(this.trailName, hand.getType().name(), data);

        if (!updated) {
            playFail(player);
            player.sendMessage(CC.translate("&cCould not update trail item."));
            return;
        }

        playSuccess(player);
        player.sendMessage(CC.translate("&aTrail item updated: &f" + this.trailName + " &7-> &f" + hand.getType().name()));
        new TrailParticleEditorMenu(this.trailName).openMenu(player);
    }
}
