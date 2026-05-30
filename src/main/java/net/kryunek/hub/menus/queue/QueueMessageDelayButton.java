package net.kryunek.hub.menus.queue;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.queue.QueueEditSession;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.stream.Collectors;

public class QueueMessageDelayButton extends Button {

    private final FileConfig adminMenus = ModuleService.getFileModule().getFile(ConfigFiles.ADMIN_MENUS);
    private final FileConfig queueConfig = ModuleService.getFileModule().getFile(ConfigFiles.QUEUE);
    private final FileConfig messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);

    @Override
    public ItemStack getButtonItem(Player player) {
        int seconds = Math.max(1, queueConfig.getInt("QUEUE.POSITION_MESSAGE_DELAY"));
        return new ItemBuilder(adminMenus.getString("QUEUE.GLOBAL.BUTTONS.MESSAGE_DELAY.MATERIAL"))
                .name(CC.translate(adminMenus.getString("QUEUE.GLOBAL.BUTTONS.MESSAGE_DELAY.NAME")))
                .lore(adminMenus.getStringList("QUEUE.GLOBAL.BUTTONS.MESSAGE_DELAY.LORE").stream()
                        .map(line -> CC.translate(line.replace("%value%", String.valueOf(seconds))))
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        QueueEditSession.startGlobal(player, QueueEditSession.Type.MESSAGE_DELAY);
        player.closeInventory();
        for (String line : messages.getStringList("QUEUE.EDITOR.GLOBAL_MESSAGE_DELAY_PROMPT")) {
            player.sendMessage(CC.translate(line));
        }
        playSuccess(player);
    }
}
