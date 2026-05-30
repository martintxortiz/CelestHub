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
public class HotbarItemEditClickSoundButton extends Button {

    private final String key;
    private final FileConfig hotbar = ModuleService.getFileModule().getFile(ConfigFiles.HOTBAR);
    private final FileConfig messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);

    @Override
    public ItemStack getButtonItem(Player player) {
        String sound = hotbar.getConfiguration().getString(key + ".CLICK_SOUND.SOUND", "UI_BUTTON_CLICK");
        return new ItemBuilder(Material.MUSIC_DISC_11)
                .name(CC.translate("&bClick Sound Type"))
                .lore(Arrays.asList(
                        CC.translate("&7Current: &f" + sound),
                        "",
                        CC.translate("&eClick to edit in chat")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        EditorInputSession.start(player, EditorInputSession.Type.HOTBAR_SOUND, key);
        player.closeInventory();
        player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.CLICK_SOUND.SOUND_PROMPT",
                "&eType click sound enum for &f%item%&e. Type 'cancel' to abort.", true)
                .replace("%item%", key)));
        playNeutral(player);
    }
}
