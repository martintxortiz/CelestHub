package net.kryunek.hub.menus.editor.chat;

import net.kryunek.hub.support.config.ConfigFiles;
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

public class ChatPrefixEditButton extends Button {

    private final FileConfig settings = ModuleService.getFileModule().getFile(ConfigFiles.SETTINGS);
    private final FileConfig messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);

    @Override
    public ItemStack getButtonItem(Player player) {
        String prefix = settings.getString("CHAT.PREFIX");
        return new ItemBuilder(Material.NAME_TAG)
                .name("&bChat Prefix")
                .lore(Arrays.asList(
                        CC.translate("&7Current: " + (prefix == null ? "&f" : prefix)),
                        "",
                        CC.translate("&eClick to edit in chat")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        EditorInputSession.start(player, EditorInputSession.Type.CHAT_PREFIX);
        player.closeInventory();
        player.sendMessage(CC.translate(messages.getString("EDITOR.CHAT.PREFIX_PROMPT", "&eType the new global chat prefix.", true)));
        playNeutral(player);
    }
}
