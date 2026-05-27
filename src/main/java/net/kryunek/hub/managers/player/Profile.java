package net.kryunek.hub.managers.player;


import lombok.Getter;
import lombok.Setter;
import net.kryunek.hub.managers.outfit.Outfit;
import net.kryunek.hub.managers.particles.TrailParticle;
import net.kryunek.hub.utils.Cooldown;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
@Setter
public class Profile {

    private UUID uuid;
    private String name;
    private ProfileStatus profileStatus;
    private Cooldown visibilityCooldown = new Cooldown(0);
    private boolean visibilityOn;
    private boolean showScoreboard;
    private boolean showTablist;
    private boolean buildModeEnabled;
    private boolean flyOnJoin;
    private boolean jukeboxEnabled;
    private double jukeboxVolume;
    private TrailParticle trail;
    private Outfit outfit;
    private String selectedGadgetType;
    private String pvpArenaKitSerialized;
    private int pvpKills;
    private int pvpDeaths;
    private int pvpKillstreak;
    private int pvpMaxKillstreak;
    private long firstJoinAt;
    private String timePreference;


    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }


    void applyData(ProfileData data) {
        setName(data.getName() == null ? getName() : data.getName());
        setVisibilityOn(data.isVisibilityOn());
        setBuildModeEnabled(data.isBuildModeEnabled());
        setFlyOnJoin(data.isFlyOnJoin());
        setJukeboxEnabled(data.isJukeboxEnabled());
        setJukeboxVolume(data.getJukeboxVolume());
        setShowScoreboard(data.isShowScoreboard());
        setShowTablist(data.isShowTablist());
        String savedTrail = data.getTrail() == null ? "None" : data.getTrail();
        setTrail("None".equalsIgnoreCase(savedTrail) ? null : TrailParticle.getTrail(savedTrail));
        String savedOutfit = data.getOutfit() == null ? "None" : data.getOutfit();
        setOutfit("None".equalsIgnoreCase(savedOutfit) ? null : Outfit.getOutfit(savedOutfit));
        String savedGadget = data.getSelectedGadgetType() == null ? "NONE" : data.getSelectedGadgetType();
        if ("ENDERBUTT_VELOCITY".equalsIgnoreCase(savedGadget)) {
            savedGadget = "SNOWBALL_VELOCITY";
        }
        setSelectedGadgetType(savedGadget);
        setPvpArenaKitSerialized(data.getPvpArenaKitSerialized() == null ? "" : data.getPvpArenaKitSerialized());
        setPvpKills(data.getPvpKills());
        setPvpDeaths(data.getPvpDeaths());
        setPvpKillstreak(data.getPvpKillstreak());
        setPvpMaxKillstreak(data.getPvpMaxKillstreak());
        setFirstJoinAt(data.getFirstJoinAt());
        setTimePreference(data.getTimePreference() == null ? "SERVER" : data.getTimePreference());
    }

    ProfileData toData() {
        ProfileData data = new ProfileData();
        data.setName(getName());
        data.setShowScoreboard(isShowScoreboard());
        data.setShowTablist(isShowTablist());
        data.setBuildModeEnabled(isBuildModeEnabled());
        data.setFlyOnJoin(isFlyOnJoin());
        data.setJukeboxEnabled(isJukeboxEnabled());
        data.setJukeboxVolume(getJukeboxVolume());
        data.setVisibilityOn(isVisibilityOn());
        data.setTrail(getTrail() == null ? "None" : getTrail().getName());
        data.setOutfit(getOutfit() == null ? "None" : getOutfit().getName());
        data.setSelectedGadgetType(getSelectedGadgetType() == null ? "NONE" : getSelectedGadgetType());
        data.setPvpArenaKitSerialized(getPvpArenaKitSerialized() == null ? "" : getPvpArenaKitSerialized());
        data.setPvpKills(getPvpKills());
        data.setPvpDeaths(getPvpDeaths());
        data.setPvpKillstreak(getPvpKillstreak());
        data.setPvpMaxKillstreak(getPvpMaxKillstreak());
        data.setFirstJoinAt(getFirstJoinAt());
        data.setTimePreference(getTimePreference() == null ? "SERVER" : getTimePreference());
        return data;
    }

    public Profile(UUID uuid, String name) {
        this(uuid, name, true, 1.0D);
    }

    Profile(UUID uuid, String name, boolean jukeboxEnabled, double jukeboxVolume) {
        this.uuid = uuid;
        this.name = name;
        this.profileStatus = ProfileStatus.HUB;
        this.visibilityOn = true;
        this.showScoreboard = true;
        this.showTablist = true;
        this.flyOnJoin = false;
        this.jukeboxEnabled = jukeboxEnabled;
        this.jukeboxVolume = jukeboxVolume;
        this.buildModeEnabled = false;
        this.selectedGadgetType = "NONE";
        this.pvpArenaKitSerialized = "";
        this.pvpKills = 0;
        this.pvpDeaths = 0;
        this.pvpKillstreak = 0;
        this.pvpMaxKillstreak = 0;
        this.firstJoinAt = System.currentTimeMillis();
        this.timePreference = "SERVER";
    }
}
