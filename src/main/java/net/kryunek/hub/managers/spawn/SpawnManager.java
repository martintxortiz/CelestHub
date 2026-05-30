package net.kryunek.hub.managers.spawn;

import net.kryunek.hub.support.config.ConfigFiles;
import lombok.Getter;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.BukkitUtil;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Tracks and persists the hub spawn location (in the injected settings config) and teleports players to it. */
@Getter
public class SpawnManager {
    private Location location;
    private final FileConfig settingsConfig;

    public void setLocation(Location location) {
        this.location = location;
        this.settingsConfig.getConfiguration().set("SPAWN_LOCATION", BukkitUtil.serializeLocation(location));
        this.settingsConfig.save();
    }

    public void toSpawn(Player player, boolean notifyIfMissing) {
        if (this.location == null) {
            player.teleport(player.getWorld().getSpawnLocation());
            if (notifyIfMissing) {
                player.sendMessage(CC.translate(ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES).getString("SPAWN.NOT_SET")));
            }
            return;
        }

        player.teleport(this.location);
    }

    public SpawnManager(FileConfig settingsConfig) {
        this.settingsConfig = settingsConfig;
        this.location = BukkitUtil.deserializeLocation(settingsConfig.getString("SPAWN_LOCATION"));
    }
}
