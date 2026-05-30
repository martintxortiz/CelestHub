package net.kryunek.hub.menus.editor.chat;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.chat.ChatManager;
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

public class ChatPauseToggleButton extends Button {

    private final FileConfig messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);

    @Override
    public ItemStack getButtonItem(Player player) {
        ChatManager chatManager = ModuleService.getManagerModule().getChatManager();
        boolean paused = chatManager.isPaused();
        return new ItemBuilder(paused ? Material.REDSTONE_BLOCK : Material.EMERALD_BLOCK)
                .name(paused ? "&cChat Muted" : "&aChat Unmuted")
                .lore(Arrays.asList(
                        CC.translate("&7Current: " + (paused ? "&cMuted" : "&aUnmuted")),
                        "",
                        CC.translate("&eClick to toggle mute")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        ChatManager chatManager = ModuleService.getManagerModule().getChatManager();
        boolean newState = !chatManager.isPaused();
        chatManager.setPaused(newState);
        String state = newState ? CC.translate("&cmuted") : CC.translate("&aunmuted");
        Bukkit.broadcastMessage(CC.translate(messages.getString("CHAT.MESSAGES.TOGGLED_MUTE", "&eChat is now %state%&e.", true)
                .replace("%state%", state)));
        playSuccess(player);
        new ChatEditorMenu().openMenu(player);
    }
}
