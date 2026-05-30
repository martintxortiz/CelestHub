package net.kryunek.hub.menus.editor.chat;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class ChatClearNowButton extends Button {

    private final FileConfig settings = ModuleService.getFileModule().getFile(ConfigFiles.SETTINGS);
    private final FileConfig messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(Material.BARRIER)
                .name("&cClear Chat Now")
                .lore(Arrays.asList(
                        CC.translate("&7Send blank lines to all players"),
                        "",
                        CC.translate("&eClick to clear")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        int lines = Math.max(1, settings.getInt("CHAT.CLEAR_LINES"));
        for (Player online : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < lines; i++) {
                online.sendMessage("");
            }
            online.sendMessage(CC.translate(messages.getString("CHAT.MESSAGES.CLEARED", "&eChat was cleared by &f%player%&e.", true)
                    .replace("%player%", player.getName())));
        }
        playSuccess(player);
        new ChatEditorMenu().openMenu(player);
    }
}
