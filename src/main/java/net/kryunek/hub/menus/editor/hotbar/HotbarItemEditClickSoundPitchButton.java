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
public class HotbarItemEditClickSoundPitchButton extends Button {

    private final String key;
    private final FileConfig hotbar = ModuleService.getFileModule().getFile(ConfigFiles.HOTBAR);
    private final FileConfig messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);

    @Override
    public ItemStack getButtonItem(Player player) {
        double pitch = hotbar.getConfiguration().getDouble(key + ".CLICK_SOUND.PITCH", 1.0D);
        return new ItemBuilder(Material.REPEATER)
                .name(CC.translate("&bClick Sound Pitch"))
                .lore(Arrays.asList(
                        CC.translate("&7Current: &f" + pitch),
                        "",
                        CC.translate("&eClick to edit in chat")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        EditorInputSession.start(player, EditorInputSession.Type.HOTBAR_SOUND_PITCH, key);
        player.closeInventory();
        player.sendMessage(CC.translate(messages.getString("EDITOR.HOTBAR.CLICK_SOUND.PITCH_PROMPT",
                "&eType click sound pitch for &f%item%&e (0.0 - 2.0). Type 'cancel' to abort.", true)
                .replace("%item%", key)));
        playNeutral(player);
    }
}
