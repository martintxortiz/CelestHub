package net.kryunek.hub.menus.queue.list.editor;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.RequiredArgsConstructor;
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

@RequiredArgsConstructor
public class QueueConfigButton extends Button {

    private final String server;
    private final QueueEditSession.Type type;
    private final FileConfig adminMenus = ModuleService.getFileModule().getFile(ConfigFiles.ADMIN_MENUS);
    private final FileConfig queueConfig = ModuleService.getFileModule().getFile(ConfigFiles.QUEUE);
    private final FileConfig messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);

    @Override
    public ItemStack getButtonItem(Player player) {
        String basePath = type.getConfigPath();
        String valuePath = type == QueueEditSession.Type.QUEUE_DELAY ? "QUEUE.DELAY" : "QUEUE.POSITION_MESSAGE_DELAY";
        int seconds = Math.max(1, queueConfig.getInt(valuePath));

        return new ItemBuilder(adminMenus.getString(basePath + ".MATERIAL"))
                .name(CC.translate(adminMenus.getString(basePath + ".NAME")))
                .lore(adminMenus.getStringList(basePath + ".LORE").stream()
                        .map(line -> CC.translate(line.replace("%value%", String.valueOf(seconds))))
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        QueueEditSession.start(player, type, server);
        player.closeInventory();
        for (String line : messages.getStringList(type == QueueEditSession.Type.QUEUE_DELAY
                ? "QUEUE.EDITOR.QUEUE_DELAY_PROMPT"
                : "QUEUE.EDITOR.MESSAGE_DELAY_PROMPT")) {
            player.sendMessage(CC.translate(line.replace("%queue%", server)));
        }
        playSuccess(player);
    }
}
