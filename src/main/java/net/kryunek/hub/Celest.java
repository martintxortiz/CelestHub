package net.kryunek.hub;

import lombok.Getter;
import lombok.Setter;
import net.kryunek.hub.app.CelestApplication;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
@Setter
public final class Celest extends JavaPlugin {

    private boolean isServerLoaded;
    private CelestApplication application;

    @Override
    public void onEnable() {
        this.application = new CelestApplication(this);
        this.application.enable();
    }

    @Override
    public void onDisable() {
        if (this.application != null) {
            this.application.disable();
            this.application = null;
        }
    }

    public static Celest get() {
        return getPlugin(Celest.class);
    }

}
