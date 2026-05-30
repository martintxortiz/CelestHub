package net.kryunek.hub.menus.selector.manage;

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

import java.util.Arrays;
import java.util.UUID;

@AllArgsConstructor
public class ServerSelectorEditorEntryButton extends Button {

    private final String key;
    private final FileConfig serverConfig = ModuleService.getFileModule().getFile(ConfigFiles.SERVER_SELECTOR);

    @Override
    public ItemStack getButtonItem(Player player) {
        String basePath = "SERVER_SELECTOR.ITEMS." + key + ".";
        Material material = Material.matchMaterial(serverConfig.getString(basePath + "ITEM", "PAPER", false));
        if (material == null) {
            material = Material.PAPER;
        }

        ItemBuilder builder = new ItemBuilder(material)
                .name(CC.translate("&b" + key))
                .lore(Arrays.asList(
                        CC.translate("&7Display name: &f" + serverConfig.getString(basePath + "NAME", "&fUnknown", false)),
                        CC.translate("&7Server: &f" + serverConfig.getString(basePath + "SERVER", "unknown", false)),
                        CC.translate("&7Decorative: " + (serverConfig.getBoolean(basePath + "DECORATIVE") ? "&aYes" : "&cNo")),
                        CC.translate("&7Slot: &f" + serverConfig.getInt(basePath + "SLOT")),
                        "",
                        CC.translate("&eLeft click: edit item"),
                        CC.translate("&eRight click: move this item")
                ))
                .data(serverConfig.getInt(basePath + "DATA"));

        if (material == Material.PLAYER_HEAD) {
            String headOwnerUuid = serverConfig.getString(basePath + "HEAD_OWNER_UUID", "", false);
            String headOwner = serverConfig.getString(basePath + "HEAD_OWNER", "", false);
            if (headOwnerUuid != null && !headOwnerUuid.isBlank()) {
                try {
                    builder.owner(UUID.fromString(headOwnerUuid));
                } catch (IllegalArgumentException ex) {
                    if (headOwner != null && !headOwner.isBlank()) {
                        builder.owner(headOwner);
                    }
                }
            } else if (headOwner != null && !headOwner.isBlank()) {
                builder.owner(headOwner);
            }
        }

        return builder.build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        if (ServerSelectorEditorMenu.isMoving(player)) {
            ServerSelectorEditorMenu.moveToSlot(player, slot);
            playSuccess(player);
            new ServerSelectorEditorMenu().openMenu(player);
            return;
        }

        if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
            ServerSelectorEditorMenu.startMove(player, this.key);
            playSuccess(player);
            player.sendMessage(CC.translate("&aMove mode enabled for &f" + this.key + "&a. Click a slot to place it."));
            new ServerSelectorEditorMenu().openMenu(player);
            return;
        }

        playSuccess(player);
        new ServerSelectorItemEditorMenu(this.key).openMenu(player);
    }
}
