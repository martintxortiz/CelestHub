package net.kryunek.hub.menus.lottery.list.editor;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.AllArgsConstructor;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.menus.lottery.list.LotteryPaginatedMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import net.kryunek.hub.utils.menu.ConfirmMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

@AllArgsConstructor
public class LotteryDeleteButton extends Button {

    private final String lotteryName;

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(Material.REDSTONE_TORCH)
                .name(CC.translate("&4Delete Lottery"))
                .lore(Arrays.asList(CC.translate("&7Permanently delete this lottery")))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        new ConfirmMenu(CC.translate("&8Confirm Delete"), confirmed -> {
            if (!confirmed) {
                new LotteryEditorMenu(lotteryName).openMenu(player);
                return;
            }

            boolean removed = ModuleService.getManagerModule().getLotteryManager().deleteLottery(lotteryName);
            if (!removed) {
                playFail(player);
                player.sendMessage(CC.translate(ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES)
                        .getString("LOTTERY.NOT_FOUND", "&cLottery not found.", true)));
                new LotteryPaginatedMenu().openMenu(player);
                return;
            }

            playSuccess(player);
            player.sendMessage(CC.translate(ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES)
                    .getString("LOTTERY.DELETED", "&cLottery deleted: &f%lottery%", true)
                    .replace("%lottery%", lotteryName)));
            new LotteryPaginatedMenu().openMenu(player);
        }, true).openMenu(player);
    }
}
