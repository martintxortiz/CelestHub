package net.kryunek.hub.menus.lottery.list.editor;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.lottery.LotteryRewardSession;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LotteryAddRewardButton extends Button {

    private final String lotteryName;
    private final FileConfig messages = ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES);

    public LotteryAddRewardButton(String lotteryName) {
        this.lotteryName = lotteryName;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        List<String> lore = new ArrayList<>(messages.getStringList("LOTTERY.REWARD.PROMPT").stream()
                .map(line -> CC.translate(line.replace("%lottery%", lotteryName)))
                .collect(Collectors.toList()));

        return new ItemBuilder(Material.COMMAND_BLOCK)
                .name(CC.translate("&bAdd Reward"))
                .lore(lore)
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        if (ModuleService.getManagerModule().getLotteryManager().getLottery(lotteryName) == null) {
            playFail(player);
            player.sendMessage(CC.translate(messages.getString("LOTTERY.NOT_FOUND", "&cLottery not found.", true)));
            return;
        }

        player.closeInventory();
        LotteryRewardSession.start(player, lotteryName);
        for (String line : messages.getStringList("LOTTERY.REWARD.PROMPT")) {
            player.sendMessage(CC.translate(line.replace("%lottery%", lotteryName)));
        }
        playSuccess(player);
    }
}
