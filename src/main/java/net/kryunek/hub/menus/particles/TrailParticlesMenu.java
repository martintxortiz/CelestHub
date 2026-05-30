package net.kryunek.hub.menus.particles;

import net.kryunek.hub.support.config.ConfigFiles;
import com.google.common.collect.Maps;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.particles.TrailParticle;
import net.kryunek.hub.managers.particles.TrailParticleManager;
import net.kryunek.hub.menus.settings.SettingsMenu;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import net.kryunek.hub.utils.menu.Button;
import net.kryunek.hub.utils.menu.buttons.BackButton;
import net.kryunek.hub.utils.menu.pagination.PageButton;
import net.kryunek.hub.utils.menu.pagination.PaginatedMenu;
import org.bukkit.entity.Player;

import java.util.Map;

public class TrailParticlesMenu extends PaginatedMenu {
    private final TrailParticleManager trailManager;
    private FileConfig particleConfig;

    @Override
    public boolean isPlaceholder() {
        return false;
    }

    @Override
    public boolean isUpdateAfterClick() {
        return true;
    }

    @Override
    public Map<Integer, Button> getGlobalButtons(Player player) {
        Map<Integer, Button> buttons = Maps.newHashMap();
        int row = getSize() / 9 - 1;
        buttons.put(getSlot(0, row), new PageButton(-1, this));
        buttons.put(getSlot(3, row), new TrailParticleRemoveButton());
        buttons.put(getSlot(4, row), new BackButton(new SettingsMenu()));
        buttons.put(getSlot(8, row), new PageButton(1, this));
        return buttons;
    }

    @Override
    public String getPrePaginatedTitle(Player player) {
        return CC.translate(particleConfig.getString("TITLE"));
    }

    @Override
    public int getMaxItemsPerPage(Player player) {
        return Math.max(9, normalizeSize(particleConfig.getInt("SIZE")) - 9);
    }

    @Override
    public int getSize() {
        return normalizeSize(particleConfig.getInt("SIZE"));
    }


    public TrailParticlesMenu() {
        this.trailManager = ModuleService.getManagerModule().getTrailParticleManager();
        this.particleConfig = ModuleService.getFileModule().getFile(ConfigFiles.PARTICLE);
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = Maps.newHashMap();
        int i = 8;
        for (TrailParticle trail : this.trailManager.getTrails().values()) {
            buttons.put(i, new TrailParticleButton(trail));
            i++;
        }
        setPlaceholder(particleConfig.getBoolean("FILLER"));
        return buttons;
    }

    private int normalizeSize(int configured) {
        if (configured < 18 || configured > 54 || configured % 9 != 0) {
            return 18;
        }
        return configured;
    }
}
