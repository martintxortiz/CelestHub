package net.kryunek.hub.managers.player;

import net.kryunek.hub.support.config.ConfigFiles;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.utils.FileConfig;
import org.bukkit.Bukkit;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MongoProfileStorage implements ProfileStorage {

    private final MongoClient client;
    private final MongoCollection<Document> collection;

    public MongoProfileStorage() {
        FileConfig config = ModuleService.getFileModule().getFile(ConfigFiles.CONFIG);
        String uri = config.getConfiguration().getString("PERSISTENCE.MONGO.URI", "mongodb://127.0.0.1:27017");
        String database = config.getConfiguration().getString("PERSISTENCE.MONGO.DATABASE", "celesthub");
        String coll = config.getConfiguration().getString("PERSISTENCE.MONGO.COLLECTION", "profiles");
        this.client = MongoClients.create(uri);
        MongoDatabase db = client.getDatabase(database);
        this.collection = db.getCollection(coll);
    }

    @Override
    public ProfileData load(UUID uuid) {
        Document document = collection.find(new Document("_id", uuid.toString())).first();
        if (document == null) {
            return null;
        }
        return fromDocument(document);
    }

    @Override
    public Map<UUID, ProfileData> loadAll() {
        Map<UUID, ProfileData> result = new HashMap<>();
        for (Document document : collection.find()) {
            String id = document.getString("_id");
            if (id == null) {
                continue;
            }
            try {
                result.put(UUID.fromString(id), fromDocument(document));
            } catch (IllegalArgumentException ex) {
                Bukkit.getLogger().warning("[Celest] Ignoring Mongo profile with invalid UUID: " + id);
            }
        }
        return result;
    }

    private ProfileData fromDocument(Document document) {
        ProfileData data = new ProfileData();
        data.setName(document.getString("name"));
        data.setVisibilityOn(document.getBoolean("visibility", true));
        data.setShowScoreboard(document.getBoolean("scoreboard", true));
        data.setShowTablist(document.getBoolean("tablist", true));
        data.setBuildModeEnabled(document.getBoolean("buildmode", false));
        data.setFlyOnJoin(document.getBoolean("flyjoin", false));
        data.setJukeboxEnabled(document.getBoolean("jukebox", true));
        data.setJukeboxVolume(document.getDouble("jukebox_volume") == null ? 1.0D : document.getDouble("jukebox_volume"));
        data.setTrail(document.getString("trail") == null ? "None" : document.getString("trail"));
        data.setOutfit(document.getString("outfit") == null ? "None" : document.getString("outfit"));
        data.setSelectedGadgetType(document.getString("gadget") == null ? "NONE" : document.getString("gadget"));
        data.setPvpArenaKitSerialized(document.getString("pvp_kit") == null ? "" : document.getString("pvp_kit"));
        data.setPvpKills(document.getInteger("pvp_kills", 0));
        data.setPvpDeaths(document.getInteger("pvp_deaths", 0));
        data.setPvpKillstreak(document.getInteger("pvp_killstreak", 0));
        data.setPvpMaxKillstreak(document.getInteger("pvp_max_killstreak", 0));
        Long firstJoin = document.getLong("first_join");
        data.setFirstJoinAt(firstJoin == null ? System.currentTimeMillis() : firstJoin);
        data.setTimePreference(document.getString("time_preference") == null ? "SERVER" : document.getString("time_preference"));
        return data;
    }

    @Override
    public void save(UUID uuid, ProfileData data) {
        Document document = new Document("_id", uuid.toString())
                .append("name", data.getName())
                .append("visibility", data.isVisibilityOn())
                .append("scoreboard", data.isShowScoreboard())
                .append("tablist", data.isShowTablist())
                .append("buildmode", data.isBuildModeEnabled())
                .append("flyjoin", data.isFlyOnJoin())
                .append("jukebox", data.isJukeboxEnabled())
                .append("jukebox_volume", data.getJukeboxVolume())
                .append("trail", data.getTrail())
                .append("outfit", data.getOutfit())
                .append("gadget", data.getSelectedGadgetType())
                .append("pvp_kit", data.getPvpArenaKitSerialized())
                .append("pvp_kills", data.getPvpKills())
                .append("pvp_deaths", data.getPvpDeaths())
                .append("pvp_killstreak", data.getPvpKillstreak())
                .append("pvp_max_killstreak", data.getPvpMaxKillstreak())
                .append("first_join", data.getFirstJoinAt())
                .append("time_preference", data.getTimePreference());
        collection.replaceOne(new Document("_id", uuid.toString()), document, new com.mongodb.client.model.ReplaceOptions().upsert(true));
    }

    @Override
    public void close() {
        client.close();
    }
}
