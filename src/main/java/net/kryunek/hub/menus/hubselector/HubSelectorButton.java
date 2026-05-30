package net.kryunek.hub.menus.hubselector;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.AllArgsConstructor;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.bungee.BungeeUtils;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class HubSelectorButton extends Button {

    private final String server;
    private final boolean currentServer;
    private final FileConfig hubSelectorConfig = ModuleService.getFileModule().getFile(ConfigFiles.HUB_SELECTOR);

    @Override
    public ItemStack getButtonItem(Player player) {
        boolean online = currentServer || BungeeUtils.getServerStatus(server);
        int players = BungeeUtils.getServerCount(server);

        String status = online ? CC.translate("&aOnline") : CC.translate("&cOffline");
        String materialName = currentServer
                ? hubSelectorConfig.getString("HUB_SELECTOR.CURRENT.ITEM")
                : hubSelectorConfig.getString("HUB_SELECTOR.DEFAULT.ITEM");
        String displayName = currentServer
                ? hubSelectorConfig.getString("HUB_SELECTOR.CURRENT.NAME")
                : hubSelectorConfig.getString("HUB_SELECTOR.DEFAULT.NAME");
        int data = currentServer
                ? hubSelectorConfig.getInt("HUB_SELECTOR.CURRENT.DATA")
                : hubSelectorConfig.getInt("HUB_SELECTOR.DEFAULT.DATA");

        List<String> lore = new ArrayList<>();
        List<String> sourceLore = currentServer
                ? hubSelectorConfig.getStringList("HUB_SELECTOR.CURRENT.LORE")
                : hubSelectorConfig.getStringList("HUB_SELECTOR.DEFAULT.LORE");

        for (String line : sourceLore) {
            lore.add(line
                    .replace("%server%", server)
                    .replace("%players%", String.valueOf(players))
                    .replace("%status%", status));
        }

        return new ItemBuilder(materialName)
                .name(displayName
                        .replace("%server%", server)
                        .replace("%players%", String.valueOf(players))
                        .replace("%status%", status))
                .lore(lore)
                .data(data)
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        if (currentServer) {
            playFail(player);
            player.sendMessage(CC.translate(hubSelectorConfig.getString("HUB_SELECTOR.MESSAGES.ALREADY_HERE")));
            close(player);
            return;
        }

        if (!BungeeUtils.getServerStatus(server)) {
            playFail(player);
            player.sendMessage(CC.translate(hubSelectorConfig.getString("HUB_SELECTOR.MESSAGES.OFFLINE")));
            close(player);
            return;
        }

        BungeeUtils.sendToServer(player, server);
        playSuccess(player);
        player.sendMessage(CC.translate(hubSelectorConfig.getString("HUB_SELECTOR.MESSAGES.CONNECTING")
                .replace("%server%", server)));
        close(player);
    }
}
