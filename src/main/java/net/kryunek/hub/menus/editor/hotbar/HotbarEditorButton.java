package net.kryunek.hub.menus.editor.hotbar;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class HotbarEditorButton extends Button {

    private final String key;
    private final FileConfig hotbar = ModuleService.getFileModule().getFile(ConfigFiles.HOTBAR);

    public HotbarEditorButton(String key) {
        this.key = key;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        String material = hotbar.getString(key + ".MATERIAL", "PAPER", false);
        int data = hotbar.getInt(key + ".DATA");
        int amount = Math.max(1, hotbar.getInt(key + ".AMOUNT"));
        boolean enabled = hotbar.getBoolean(key + ".ENABLED");
        int slot = hotbar.getInt(key + ".SLOT");
        String displayName = hotbar.getString(key + ".NAME", "&fUnknown", false);

        List<String> lore = new ArrayList<>();
        lore.add(CC.translate("&7Name: " + displayName));
        lore.add(CC.translate("&7Enabled: " + (enabled ? "&atrue" : "&cfalse")));
        lore.add(CC.translate("&7Slot: &f" + slot));
        lore.add("");
        lore.add(CC.translate("&eClick to edit item"));

        return new ItemBuilder(material)
                .data(data)
                .amount(amount)
                .name("&b" + key)
                .lore(lore)
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        playSuccess(player);
        new HotbarItemEditorMenu(key).openMenu(player);
    }
}
