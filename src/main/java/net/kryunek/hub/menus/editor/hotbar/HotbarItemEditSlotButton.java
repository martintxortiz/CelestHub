package net.kryunek.hub.menus.editor.hotbar;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.AllArgsConstructor;
import net.kryunek.hub.managers.editor.EditorInputSession;
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

@AllArgsConstructor
public class HotbarItemEditSlotButton extends Button {

    private final String key;
    private final FileConfig hotbar = ModuleService.getFileModule().getFile(ConfigFiles.HOTBAR);
    private final FileConfig messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);

    @Override
    public ItemStack getButtonItem(Player player) {
        int currentSlot = hotbar.getInt(key + ".SLOT");
        return new ItemBuilder(Material.HOPPER)
                .name(CC.translate("&bItem Slot"))
                .lore(Arrays.asList(
                        CC.translate("&7Current: &f" + currentSlot),
                        "",
                        CC.translate("&eLeft click: +1 slot"),
                        CC.translate("&eRight click: -1 slot"),
                        CC.translate("&eShift click: set exact slot")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        String path = key + ".SLOT";
        if (!hotbar.getConfiguration().contains(path)) {
            playFail(player);
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.ITEM_NOT_FOUND", "&cHotbar item not found.", true)));
            return;
        }

        if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
            EditorInputSession.start(player, EditorInputSession.Type.HOTBAR_SLOT, key);
            player.closeInventory();
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.SLOT_PROMPT", "&eType the slot number (0-8).", true).replace("%item%", key)));
            playNeutral(player);
            return;
        }

        int delta = (clickType == ClickType.RIGHT) ? -1 : 1;
        int current = hotbar.getInt(path);
        int next = Math.max(0, Math.min(8, current + delta));
        if (next == current) {
            playFail(player);
            player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.INVALID_SLOT", "&cSlot must be between 0 and 8.", true)));
            return;
        }

        hotbar.getConfiguration().set(path, next);
        hotbar.save();
        ModuleService.getManagerModule().getHotbarManager().load();
        ModuleService.getManagerModule().getHotbarManager().reload();
        playSuccess(player);
        new HotbarItemEditorMenu(key).openMenu(player);
    }
}
