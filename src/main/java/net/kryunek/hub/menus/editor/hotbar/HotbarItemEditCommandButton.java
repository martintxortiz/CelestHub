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
public class HotbarItemEditCommandButton extends Button {

    private final String key;
    private final FileConfig hotbar = ModuleService.getFileModule().getFile(ConfigFiles.HOTBAR);
    private final FileConfig messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);

    @Override
    public ItemStack getButtonItem(Player player) {
        String command = hotbar.getString(key + ".COMMAND", "", false);
        if (command == null || command.isBlank()) {
            command = "&7(none)";
        }
        return new ItemBuilder(Material.COMMAND_BLOCK)
                .name(CC.translate("&bClick Command"))
                .lore(Arrays.asList(
                        CC.translate("&7Current: &f" + command),
                        "",
                        CC.translate("&eClick to edit in chat"),
                        CC.translate("&7Type 'clear' to disable")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        EditorInputSession.start(player, EditorInputSession.Type.HOTBAR_COMMAND, key);
        player.closeInventory();
        player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.COMMAND_PROMPT",
                "&eType command for &f%item%&e. Type 'clear' to disable. Type 'cancel' to abort.", true)
                .replace("%item%", key)));
        playNeutral(player);
    }
}
