package net.kryunek.hub.menus.hubselector;

import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.bungee.BungeeUtils;
import net.kryunek.hub.utils.menu.Button;
import net.kryunek.hub.utils.menu.buttons.CloseButton;
import net.kryunek.hub.utils.menu.pagination.PageButton;
import net.kryunek.hub.utils.menu.pagination.PaginatedMenu;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HubSelectorPaginatedMenu extends PaginatedMenu {

    private final FileConfig hubSelectorConfig = ModuleService.getFileModule().getFile(ConfigFiles.HUB_SELECTOR);
    private final FileConfig coreConfig = ModuleService.getFileModule().getFile(ConfigFiles.CONFIG);

    {
        setAutoUpdate(true);
    }

    @Override
    public String getPrePaginatedTitle(Player player) {
        return CC.translate(hubSelectorConfig.getString("HUB_SELECTOR.TITLE"));
    }

    @Override
    public int getSize() {
        return hubSelectorConfig.getInt("HUB_SELECTOR.SIZE");
    }

    @Override
    public int getMaxItemsPerPage(Player player) {
        return hubSelectorConfig.getInt("HUB_SELECTOR.MAX_ITEMS_PER_PAGE");
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        int index = 8;

        for (String server : getAvailableServers()) {
            buttons.put(index++, new HubSelectorButton(server, BungeeUtils.isCurrentServer(server)));
        }

        return buttons;
    }

    @Override
    public Map<Integer, Button> getGlobalButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        return buttons;
    }

    private Set<String> getAvailableServers() {
        Set<String> orderedServers = new LinkedHashSet<>();
        ConfigurationSection serverSection = coreConfig.getConfiguration().getConfigurationSection("SERVER");

        List<String> configuredHubs = hubSelectorConfig.getStringList("HUB_SELECTOR.HUBS");
        List<String> configuredOrder = hubSelectorConfig.getStringList("HUB_SELECTOR.SERVER_ORDER");
        if (serverSection != null) {
            List<String> source = !configuredHubs.isEmpty() ? configuredHubs : configuredOrder;
            for (String server : source) {
                if (serverSection.contains(server)) {
                    orderedServers.add(server);
                }
            }
        }

        boolean useProxyDiscovery = !BungeeUtils.getServersName().isEmpty();
        Set<String> online = new LinkedHashSet<>();
        for (String server : orderedServers) {
            boolean isCurrent = BungeeUtils.isCurrentServer(server);

            if (useProxyDiscovery && !isCurrent && !BungeeUtils.getServersName().contains(server)) {
                continue;
            }

            if (isCurrent || BungeeUtils.getServerStatus(server)) {
                online.add(server);
            }
        }

        return online;
    }
}
