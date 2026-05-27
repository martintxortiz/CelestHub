package net.kryunek.hub.listeners;

import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.managers.player.ProfileManager;
import net.kryunek.hub.support.message.MessageKey;
import net.kryunek.hub.support.message.Messages;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ProfileListener implements Listener {

    private final ProfileManager profileManager;
    private final Messages messages;

    public ProfileListener(org.bukkit.plugin.Plugin plugin, ProfileManager profileManager, Messages messages) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.profileManager = profileManager;
        this.messages = messages;
    }



    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerSaveProfile(PlayerQuitEvent event) {
        this.profileManager.saveAndRemove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerLoginEvent(PlayerLoginEvent event) {
        Profile profile = this.profileManager.getProfile(event.getPlayer().getUniqueId());
        if (profile == null) {
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            event.setKickMessage(messages.get(MessageKey.PROFILE_NOT_LOADED));
            return;
        }
        profile.setName(event.getPlayer().getName());
        this.profileManager.saveProfile(profile, true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        Profile profile = this.profileManager.createProfile(event.getUniqueId(), event.getName());
        this.profileManager.loadProfile(profile);

    }
}
