package net.kryunek.hub.menus.selector;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.AllArgsConstructor;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.queue.Queue;
import net.kryunek.hub.managers.timer.Timer;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.bungee.BungeeUtils;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
public class ServerButton extends Button {

    private String server;
    private final FileConfig settingsConfig = ModuleService.getFileModule().getFile(ConfigFiles.SERVER_SELECTOR);

    @Override
    public ItemStack getButtonItem(Player player) {
        List<String> serverLore = new ArrayList<>();
        settingsConfig.getStringList(getPath("LORE")).forEach(text -> {
            Queue queue = ModuleService.getManagerModule().getQueueManager().getQueue(server);
            boolean decorative = settingsConfig.getBoolean(getPath("DECORATIVE"));
            String queueStatus;

            if (decorative) {
                queueStatus = CC.translate("&7Decorative");
            } else if (queue != null) {
                queueStatus = queue.isPaused()
                        ? CC.translate("&cPaused")
                        : CC.translate("&aJoinable");
            } else {
                queueStatus = CC.translate("&4Not Found");
            }

            text = text.replace("%queue_status%", queueStatus);

            text = text.replace("%status%", !BungeeUtils.getServerStatus(server) ? CC.translate("&cOffline") : CC.translate("&aOnline")).replace("%players%", String.valueOf(BungeeUtils.getServerCount(server)));

            if (text.contains("%TIMER%")) {

                Timer timer = ModuleService.getManagerModule()
                        .getTimerManager()
                        .getTimers()
                        .stream()
                        .filter(t -> t.getName().equalsIgnoreCase(server))
                        .findFirst()
                        .orElse(null);

                if (timer != null) {

                    String time;

                    if (timer.isPaused()) {
                        time = CC.translate("&eTimer Paused");
                    } else {
                        time = CC.translate("&c" + timer.getFormattedTime());
                    }

                    text = text.replace("%TIMER%",
                            CC.translate("&7Opening in " + time));

                } else {
                    text = text.replace("%TIMER%", CC.translate(""));
                }
            }
            serverLore.add(text);
        });

        Material material = Material.matchMaterial(settingsConfig.getString(getPath("ITEM"), "PAPER", false));
        if (material == null) {
            material = Material.PAPER;
        }

        ItemBuilder builder = new ItemBuilder(material)
                .name(settingsConfig.getString(getPath("NAME")).replace("%queue%", String.valueOf(ModuleService.getManagerModule().getQueueManager().getQueueSize(settingsConfig.getString(getPath("SERVER"))))))
                .lore(serverLore)
                .data(settingsConfig.getInt(getPath("DATA")));

        if (material == Material.PLAYER_HEAD) {
            String headOwnerUuid = settingsConfig.getString(getPath("HEAD_OWNER_UUID"), "", false);
            String headOwner = settingsConfig.getString(getPath("HEAD_OWNER"), "", false);
            if (headOwnerUuid != null && !headOwnerUuid.isBlank()) {
                try {
                    builder.owner(UUID.fromString(headOwnerUuid));
                } catch (IllegalArgumentException ex) {
                    if (headOwner != null && !headOwner.isBlank()) {
                        builder.owner(headOwner);
                    }
                }
            } else if (headOwner != null && !headOwner.isBlank()) {
                builder.owner(headOwner);
            }
        }

        return builder.build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        String command = settingsConfig.getString(getPath("COMMAND"), "", false);
        if (command != null && !command.isBlank()) {
            String parsed = command.trim();
            if (parsed.startsWith("/")) {
                parsed = parsed.substring(1);
            }
            if (parsed.toLowerCase().startsWith("console:")) {
                String consoleCommand = parsed.substring("console:".length()).trim()
                        .replace("%player%", player.getName());
                if (!consoleCommand.isBlank()) {
                    org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), consoleCommand);
                }
            } else {
                player.performCommand(parsed.replace("%player%", player.getName()));
            }
            playSuccess(player);
            return;
        }

        if (settingsConfig.getBoolean(getPath("DECORATIVE"))) {
            playNeutral(player);
            return;
        }

        if (!BungeeUtils.getServerStatus(server)) {
            playFail(player);
            player.sendMessage(CC.translate("&cServer is Offline"));
            close(player);
        } else {
            ModuleService.getManagerModule().getQueueManager().addToQueue(player, settingsConfig.getString(getPath("SERVER")));
            playSuccess(player);
        }
    }

    private String getPath(String a) {
        return "SERVER_SELECTOR.ITEMS." + server + "." + a;
    }
}
