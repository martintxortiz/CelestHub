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
public class HotbarItemEditHeadOwnerButton extends Button {

    private final String key;
    private final FileConfig hotbar = ModuleService.getFileModule().getFile(ConfigFiles.HOTBAR);
    private final FileConfig messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);

    @Override
    public ItemStack getButtonItem(Player player) {
        String owner = hotbar.getString(key + ".HEAD_OWNER", "", false);
        if (owner == null || owner.isBlank()) {
            owner = "&7(none)";
        }
        return new ItemBuilder(Material.PLAYER_HEAD)
                .name(CC.translate("&bHead Owner (Name)"))
                .lore(Arrays.asList(
                        CC.translate("&7Current: &f" + owner),
                        CC.translate("&7Sets MATERIAL to PLAYER_HEAD."),
                        "",
                        CC.translate("&eClick to edit in chat"),
                        CC.translate("&7Type 'clear' to remove")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        EditorInputSession.start(player, EditorInputSession.Type.HOTBAR_HEAD_OWNER, key);
        player.closeInventory();
        player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.HEAD_OWNER_PROMPT",
                "&eType head owner name for &f%item%&e. Type 'clear' to remove. Type 'cancel' to abort.", true)
                .replace("%item%", key)));
        playNeutral(player);
    }
}
