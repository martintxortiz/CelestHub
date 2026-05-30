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
public class HotbarItemEditHeadOwnerUuidButton extends Button {

    private final String key;
    private final FileConfig hotbar = ModuleService.getFileModule().getFile(ConfigFiles.HOTBAR);
    private final FileConfig messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);

    @Override
    public ItemStack getButtonItem(Player player) {
        String uuid = hotbar.getString(key + ".HEAD_OWNER_UUID", "", false);
        if (uuid == null || uuid.isBlank()) {
            uuid = "&7(none)";
        }
        return new ItemBuilder(Material.PAPER)
                .name(CC.translate("&bHead Owner UUID"))
                .lore(Arrays.asList(
                        CC.translate("&7Current: &f" + uuid),
                        CC.translate("&7Used first when both are set."),
                        "",
                        CC.translate("&eClick to edit in chat"),
                        CC.translate("&7Type 'clear' to remove")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        EditorInputSession.start(player, EditorInputSession.Type.HOTBAR_HEAD_OWNER_UUID, key);
        player.closeInventory();
        player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.HEAD_OWNER_UUID_PROMPT",
                "&eType head owner UUID for &f%item%&e. Type 'clear' to remove. Type 'cancel' to abort.", true)
                .replace("%item%", key)));
        playNeutral(player);
    }
}
