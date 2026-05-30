package net.kryunek.hub.utils.menu.pagination;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.AllArgsConstructor;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

@AllArgsConstructor
public class PageButton extends Button {

    private final int mod;
    private final PaginatedMenu menu;
    private final FileConfig commonMenu = ModuleService.getFileModule().getFile(ConfigFiles.COMMON_MENU);

    @Override
    public ItemStack getButtonItem(Player player) {
        boolean canMove = this.hasNext(player);
        if (!canMove) {
            String materialName = this.mod > 0
                    ? commonMenu.getString("PAGE_BUTTON.LAST_MATERIAL", "GRAY_DYE", false)
                    : commonMenu.getString("PAGE_BUTTON.FIRST_MATERIAL", "GRAY_DYE", false);
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                material = Material.GRAY_DYE;
            }

            String name = this.mod > 0
                    ? commonMenu.getString("PAGE_BUTTON.LAST_NAME")
                    : commonMenu.getString("PAGE_BUTTON.FIRST_NAME");

            int data = this.mod > 0
                    ? commonMenu.getInt("PAGE_BUTTON.LAST_DATA")
                    : commonMenu.getInt("PAGE_BUTTON.FIRST_DATA");

            return new ItemBuilder(material).data(data).name(name).build();
        }

        String materialName = this.mod > 0
                ? commonMenu.getString("PAGE_BUTTON.NEXT_MATERIAL", "LIME_DYE", false)
                : commonMenu.getString("PAGE_BUTTON.PREVIOUS_MATERIAL", "RED_DYE", false);
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = this.mod > 0 ? Material.LIME_DYE : Material.RED_DYE;
        }

        int data = this.mod > 0
                ? commonMenu.getInt("PAGE_BUTTON.NEXT_DATA")
                : commonMenu.getInt("PAGE_BUTTON.PREVIOUS_DATA");

        String name = this.mod > 0
                ? commonMenu.getString("PAGE_BUTTON.NEXT_NAME")
                : commonMenu.getString("PAGE_BUTTON.PREVIOUS_NAME");

        return new ItemBuilder(material).data(data).name(name).build();
    }

    @Override
    public void clicked(Player player, int i, ClickType clickType, int hb) {
        if (hasNext(player)) {
            this.menu.modPage(player, this.mod);
            Button.playNeutral(player);
        } else {
            Button.playFail(player);
        }
    }

    private boolean hasNext(Player player) {
        int pg = this.menu.getPage() + this.mod;
        return pg > 0 && this.menu.getPages(player) >= pg;
    }
}
