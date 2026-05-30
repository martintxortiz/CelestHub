package net.kryunek.hub.menus.editor;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.AllArgsConstructor;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.menus.editor.chat.ChatEditorMenu;
import net.kryunek.hub.menus.editor.hotbar.HotbarEditorMenu;
import net.kryunek.hub.menus.editor.pvparena.PvpArenaEditorMenu;
import net.kryunek.hub.menus.gadgets.manage.GadgetEditorMenu;
import net.kryunek.hub.menus.lottery.list.LotteryPaginatedMenu;
import net.kryunek.hub.menus.outfit.manage.list.OutfitPaginatedMenu;
import net.kryunek.hub.menus.particles.manage.list.TrailParticlePaginatedMenu;
import net.kryunek.hub.menus.queue.QueueGlobalMenu;
import net.kryunek.hub.menus.rank.RankEditorMenu;
import net.kryunek.hub.menus.selector.manage.ServerSelectorEditorMenu;
import net.kryunek.hub.menus.timer.TimerPaginatedMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.ItemBuilder;
import net.kryunek.hub.utils.menu.Button;
import net.kryunek.hub.utils.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

@AllArgsConstructor
public class CelestEditorOpenButton extends Button {

    private final String key;
    private final FileConfig editorMenuConfig = ModuleService.getFileModule().getFile(ConfigFiles.CELEST_EDITOR);

    @Override
    public ItemStack getButtonItem(Player player) {
        String basePath = "EDITOR_MENU.BUTTONS." + key + ".";
        return new ItemBuilder(editorMenuConfig.getString(basePath + "MATERIAL"))
                .name(editorMenuConfig.getString(basePath + "NAME"))
                .lore(editorMenuConfig.getStringList(basePath + "LORE"))
                .data(editorMenuConfig.getInt(basePath + "DATA"))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        Menu target = switch (key.toUpperCase()) {
            case "QUEUE" -> new QueueGlobalMenu();
            case "PARTICLES" -> new TrailParticlePaginatedMenu();
            case "OUTFIT" -> new OutfitPaginatedMenu();
            case "HOTBAR" -> new HotbarEditorMenu();
            case "TIMER" -> new TimerPaginatedMenu();
            case "LOTTERY" -> new LotteryPaginatedMenu();
            case "GADGETS" -> new GadgetEditorMenu();
            case "RANKS" -> new RankEditorMenu();
            case "CHAT" -> new ChatEditorMenu();
            case "SERVER_SELECTOR" -> new ServerSelectorEditorMenu();
            case "PVP_ARENA" -> new PvpArenaEditorMenu();
            default -> null;
        };

        if (target == null) {
            playFail(player);
            player.sendMessage(CC.translate("&cEditor destination not found."));
            return;
        }

        playSuccess(player);
        target.openMenu(player);
    }
}
