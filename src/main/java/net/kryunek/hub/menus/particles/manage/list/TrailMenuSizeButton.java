package net.kryunek.hub.menus.particles.manage.list;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class TrailMenuSizeButton extends Button {

    private final FileConfig particleConfig = ModuleService.getFileModule().getFile(ConfigFiles.PARTICLE);

    @Override
    public ItemStack getButtonItem(Player player) {
        int current = normalizeSize(particleConfig.getInt("SIZE"));
        return new ItemBuilder(Material.CHEST)
                .name(CC.translate("&bTrails Menu Size"))
                .lore(Arrays.asList(
                        CC.translate("&7Current size: &f" + current),
                        "",
                        CC.translate("&eLeft click: +9"),
                        CC.translate("&eRight click: -9")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        int current = normalizeSize(particleConfig.getInt("SIZE"));
        int updated = clickType == ClickType.RIGHT ? current - 9 : current + 9;
        updated = Math.max(18, Math.min(54, updated));
        particleConfig.getConfiguration().set("SIZE", updated);
        particleConfig.save();
        playSuccess(player);
        new TrailParticlePaginatedMenu().openMenu(player);
    }

    private int normalizeSize(int configured) {
        if (configured < 18 || configured > 54 || configured % 9 != 0) {
            return 18;
        }
        return configured;
    }
}
