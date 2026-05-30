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
public class HotbarItemEditNameButton extends Button {

    private final String key;
    private final FileConfig messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(Material.NAME_TAG)
                .name(CC.translate("&bSet Name From Chat"))
                .lore(Arrays.asList(
                        CC.translate("&7Type the new name in chat."),
                        CC.translate("&7Use ',' to split into lines."),
                        "",
                        CC.translate("&eClick to edit")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        EditorInputSession.start(player, EditorInputSession.Type.HOTBAR_NAME, key);
        player.closeInventory();
        for (String line : messages.getStringListOrDefault("EDITOR.HOTBAR.NAME_PROMPT", Arrays.asList(
                "&eType the new name for &f%item%&e.",
                "&7Use ',' to split into lines.",
                "&7Type 'cancel' to abort."
        ))) {
            player.sendMessage(CC.translate(line.replace("%item%", key)));
        }
        playNeutral(player);
    }
}
