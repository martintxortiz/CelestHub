package net.kryunek.hub.managers.player;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class ProfileManagerTest {

    @Test
    void saveAndRemoveSavesProfileAndEvictsItFromManagerCache() throws Exception {
        UUID uuid = UUID.randomUUID();
        Profile profile = new Profile(uuid, "Player");
        ProfileStorage storage = mock(ProfileStorage.class);
        ProfileManager manager = new ProfileManager(storage, false);

        mutableProfiles(manager).put(uuid, profile);

        manager.saveAndRemove(uuid);

        verify(storage).save(eq(uuid), any(ProfileData.class));
        assertFalse(manager.getProfiles().containsKey(uuid));
    }

    @Test
    void profileSnapshotIsStableWhenCacheChanges() throws Exception {
        UUID uuid = UUID.randomUUID();
        Profile profile = mock(Profile.class);
        ProfileManager manager = new ProfileManager(mock(ProfileStorage.class), false);
        Map<UUID, Profile> profiles = mutableProfiles(manager);

        profiles.put(uuid, profile);
        Collection<Profile> snapshot = manager.getProfileSnapshot();
        profiles.clear();

        assertTrue(snapshot.contains(profile));
        assertFalse(manager.getProfiles().containsKey(uuid));
    }

    @Test
    void exposedProfilesMapCannotBeMutatedByCallers() {
        ProfileManager manager = new ProfileManager(mock(ProfileStorage.class), false);

        assertThrows(UnsupportedOperationException.class,
                () -> manager.getProfiles().put(UUID.randomUUID(), mock(Profile.class)));
    }

    @Test
    void loadProfileAppliesStoredDataToExistingProfile() {
        UUID uuid = UUID.randomUUID();
        ProfileStorage storage = mock(ProfileStorage.class);
        ProfileData data = new ProfileData();
        data.setName("StoredName");
        data.setShowScoreboard(false);
        data.setShowTablist(false);
        data.setSelectedGadgetType("SNOWBALL_VELOCITY");
        data.setPvpKills(7);
        when(storage.load(uuid)).thenReturn(data);

        ProfileManager manager = new ProfileManager(storage, false);
        Profile profile = new Profile(uuid, "OriginalName");

        manager.loadProfile(profile);

        assertEquals("StoredName", profile.getName());
        assertFalse(profile.isShowScoreboard());
        assertFalse(profile.isShowTablist());
        assertEquals("SNOWBALL_VELOCITY", profile.getSelectedGadgetType());
        assertEquals(7, profile.getPvpKills());
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, Profile> mutableProfiles(ProfileManager manager) throws Exception {
        Field field = ProfileManager.class.getDeclaredField("profiles");
        field.setAccessible(true);
        return (Map<UUID, Profile>) field.get(manager);
    }
}
