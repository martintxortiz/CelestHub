package net.kryunek.hub.support.profile;

import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.managers.player.ProfileManager;
import net.kryunek.hub.support.message.Messages;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileAccessTest {

    private final Messages messages = Messages.from(new YamlConfiguration(), Logger.getLogger("ProfileAccessTest"));

    @Test
    void returnsProfileForPlayer() {
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        ProfileManager profileManager = mock(ProfileManager.class);
        Profile profile = mock(Profile.class);
        ProfileAccess profileAccess = new ProfileAccess(profileManager, messages);

        when(player.getUniqueId()).thenReturn(uuid);
        when(profileManager.getProfile(uuid)).thenReturn(profile);

        assertSame(profile, profileAccess.require(player));
    }

    @Test
    void getReturnsProfileWithoutMessagingOnMiss() {
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        ProfileManager profileManager = mock(ProfileManager.class);
        ProfileAccess profileAccess = new ProfileAccess(profileManager, messages);

        when(player.getUniqueId()).thenReturn(uuid);
        when(profileManager.getProfile(uuid)).thenReturn(null);

        // get() is the silent lookup: a missing profile must NOT trigger a player message.
        assertNull(profileAccess.get(player));
        verify(player, never()).sendMessage(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsNullArguments() {
        ProfileManager profileManager = mock(ProfileManager.class);
        ProfileAccess profileAccess = new ProfileAccess(profileManager, messages);

        assertThrows(NullPointerException.class, () -> profileAccess.get(null));
        assertThrows(NullPointerException.class, () -> new ProfileAccess(null, messages));
        assertThrows(NullPointerException.class, () -> new ProfileAccess(profileManager, null));
    }

    @Test
    void sendsFailureMessageWhenProfileIsMissing() {
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        ProfileManager profileManager = mock(ProfileManager.class);
        ProfileAccess profileAccess = new ProfileAccess(profileManager, messages);

        when(player.getUniqueId()).thenReturn(uuid);
        when(profileManager.getProfile(uuid)).thenReturn(null);

        assertNull(profileAccess.require(player));
        verify(player).sendMessage(ChatColor.RED + "Failed to load your profile, please join again.");
    }
}
