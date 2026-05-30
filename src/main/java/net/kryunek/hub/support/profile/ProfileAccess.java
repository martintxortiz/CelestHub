package net.kryunek.hub.support.profile;

import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.managers.player.ProfileManager;
import net.kryunek.hub.support.message.MessageKey;
import net.kryunek.hub.support.message.Messages;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * Convenience accessor for a player's {@link Profile}. {@link #require(Player)} sends the standard
 * "profile not loaded" message when absent, centralising the null-profile guard that commands repeat.
 */
public final class ProfileAccess {

    private final ProfileManager profileManager;
    private final Messages messages;

    public ProfileAccess(ProfileManager profileManager, Messages messages) {
        this.profileManager = Objects.requireNonNull(profileManager, "profileManager");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    public Profile get(Player player) {
        Objects.requireNonNull(player, "player");
        return profileManager.getProfile(player.getUniqueId());
    }

    public Profile require(Player player) {
        Profile profile = get(player);
        if (profile == null) {
            messages.send(player, MessageKey.PROFILE_NOT_LOADED);
        }
        return profile;
    }
}
