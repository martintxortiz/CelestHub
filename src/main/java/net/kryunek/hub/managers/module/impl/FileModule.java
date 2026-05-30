package net.kryunek.hub.managers.module.impl;

import com.google.common.collect.Maps;
import lombok.Getter;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.Module;
import net.kryunek.hub.support.config.ConfigFiles;
import net.kryunek.hub.utils.FileConfig;

import java.util.Map;

@Getter
public class FileModule extends Module {
    private final Map<String, FileConfig> files;

    public FileConfig getFile(String file) {
        return this.files.get(file);
    }
    
    @Override
    public int getPriority() {
        return 1;
    }
    
    public void reload() {
        for (FileConfig config : this.files.values()) {
            config.reload();
        }
    }
    
    @Override
    public void onEnable(Celest hub) {
        this.files.put(ConfigFiles.CONFIG, new FileConfig(hub, "core/config.yml"));
        this.files.put(ConfigFiles.PLAYERS, new FileConfig(hub, "core/players.yml"));
        this.files.put(ConfigFiles.MESSAGES, new FileConfig(hub, "core/messages.yml"));
        this.files.put(ConfigFiles.HOTBAR, new FileConfig(hub, "features/hotbar.yml"));
        this.files.put(ConfigFiles.SETTINGS, new FileConfig(hub, "features/settings.yml"));
        this.files.put(ConfigFiles.SCOREBOARD, new FileConfig(hub, "features/scoreboard.yml"));
        this.files.put(ConfigFiles.QUEUE, new FileConfig(hub, "features/queue.yml"));
        this.files.put(ConfigFiles.LOTTERY, new FileConfig(hub, "features/lottery.yml"));
        this.files.put(ConfigFiles.GADGETS, new FileConfig(hub, "features/gadgets.yml"));
        this.files.put(ConfigFiles.PARTICLE, new FileConfig(hub, "features/particle.yml"));
        this.files.put(ConfigFiles.OUTFIT, new FileConfig(hub, "features/outfit.yml"));
        this.files.put(ConfigFiles.TAB, new FileConfig(hub, "features/tab.yml"));
        this.files.put(ConfigFiles.JUKEBOX, new FileConfig(hub, "features/jukebox.yml"));
        this.files.put(ConfigFiles.COMMON_MENU, new FileConfig(hub, "menus/common.yml"));
        this.files.put(ConfigFiles.ADMIN_MENUS, new FileConfig(hub, "menus/admin_menus.yml"));
        this.files.put(ConfigFiles.SERVER_SELECTOR, new FileConfig(hub, "menus/server_selector.yml"));
        this.files.put(ConfigFiles.HUB_SELECTOR, new FileConfig(hub, "menus/hub_selector.yml"));
        this.files.put(ConfigFiles.EDITOR_MENUS, new FileConfig(hub, "menus/editor_menus.yml"));
        this.files.put(ConfigFiles.CELEST_EDITOR, new FileConfig(hub, "menus/celest_editor.yml"));
        this.files.put(ConfigFiles.SETTINGS_MENU, new FileConfig(hub, "menus/settings_menu.yml"));
    }
    
    public FileModule() {
        this.files = Maps.newHashMap();
    }
}
