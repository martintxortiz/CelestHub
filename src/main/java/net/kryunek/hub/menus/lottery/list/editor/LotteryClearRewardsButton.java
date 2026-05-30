package net.kryunek.hub.menus.lottery.list.editor;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.AllArgsConstructor;
import net.kryunek.hub.managers.lottery.Lottery;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

@AllArgsConstructor
public class LotteryClearRewardsButton extends Button {

    private final String lotteryName;

    @Override
    public ItemStack getButtonItem(Player player) {
        Lottery lottery = ModuleService.getManagerModule().getLotteryManager().getLottery(lotteryName);
        int rewards = lottery == null ? 0 : lottery.getRewards().size();
        return new ItemBuilder(Material.BARRIER)
                .name(CC.translate("&cClear Rewards"))
                .lore(Arrays.asList(
                        CC.translate("&7Current rewards: &f" + rewards),
                        "",
                        CC.translate("&eClick to clear all")
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        boolean ok = ModuleService.getManagerModule().getLotteryManager().clearRewards(lotteryName);
        if (!ok) {
            playFail(player);
            player.sendMessage(CC.translate(ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES)
                    .getString("LOTTERY.NOT_FOUND", "&cLottery not found.", true)));
            return;
        }
        playSuccess(player);
        player.sendMessage(CC.translate(ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES)
                .getString("LOTTERY.REWARDS_CLEARED", "&eRewards cleared for &f%lottery%", true)
                .replace("%lottery%", lotteryName)));
    }
}
