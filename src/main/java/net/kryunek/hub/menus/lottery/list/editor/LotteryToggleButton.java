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
public class LotteryToggleButton extends Button {

    private final String lotteryName;

    @Override
    public ItemStack getButtonItem(Player player) {
        Lottery lottery = ModuleService.getManagerModule().getLotteryManager().getLottery(lotteryName);
        boolean active = lottery != null && lottery.isActive();
        return new ItemBuilder(active ? Material.RED_WOOL : Material.LIME_WOOL)
                .name(CC.translate(active ? "&cFinish Lottery" : "&aStart Lottery"))
                .lore(Arrays.asList(
                        CC.translate("&7Toggle this lottery state."),
                        CC.translate("&7Current: " + (active ? "&aActive" : "&eInactive"))
                ))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        Lottery lottery = ModuleService.getManagerModule().getLotteryManager().getLottery(lotteryName);
        if (lottery == null) {
            playFail(player);
            player.sendMessage(CC.translate(ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES)
                    .getString("LOTTERY.NOT_FOUND", "&cLottery not found.", true)));
            return;
        }

        if (lottery.isActive()) {
            boolean ended = ModuleService.getManagerModule().getLotteryManager().endLottery(lotteryName, player.getName());
            if (!ended) {
                playFail(player);
                return;
            }
            player.sendMessage(CC.translate(ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES)
                    .getString("LOTTERY.ENDED", "&eLottery ended: &f%lottery%", true)
                    .replace("%lottery%", lotteryName)));
            playSuccess(player);
            return;
        }

        boolean started = ModuleService.getManagerModule().getLotteryManager().startLottery(lotteryName, player);
        if (!started) {
            playFail(player);
            player.sendMessage(CC.translate(ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES)
                    .getString("LOTTERY.START_FAILED", "&cCould not start that lottery.", true)));
            return;
        }

        player.sendMessage(CC.translate(ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES)
                .getString("LOTTERY.STARTED", "&aLottery started: &f%lottery%", true)
                .replace("%lottery%", lotteryName)));
        playSuccess(player);
    }
}
