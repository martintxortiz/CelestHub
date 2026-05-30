package net.kryunek.hub.utils.bungee;

import net.kryunek.hub.support.config.ConfigFiles;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.ModuleService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class BungeeUtils implements PluginMessageListener {


    @Getter
    private static final Map<String, BungeeServer> serversByName = new HashMap<>();
    @Getter private static final List<BungeeServer> servers = new ArrayList<>();
    @Getter private static final List<String> serversName = new ArrayList<>();
    @Getter private static int globalPlayers = 0;
    @Getter private static String currentServerName = "";

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) return;
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();
        if (subChannel.equals("PlayerCount")) {
            try {
                String name = in.readUTF();
                int playerCount = in.readInt();
                if (name.equalsIgnoreCase("ALL")) {
                    globalPlayers = playerCount;
                } else if (serversName.contains(name)) {
                    BungeeServer server = serversByName.get(name);
                    if (server == null) server = new BungeeServer(name);
                    server.setPlayerCount(playerCount);
                }
            } catch (Exception e) {
                Celest.get().getLogger().log(Level.FINE, "Failed to read BungeeCord PlayerCount payload", e);
            }
        } else if (subChannel.equals("GetServer")) {
            try {
                currentServerName = in.readUTF();
            } catch (Exception e) {
                Celest.get().getLogger().log(Level.FINE, "Failed to read BungeeCord GetServer payload", e);
            }
        } else if (subChannel.equals("GetServers")) {
            String[] serverList = in.readUTF().split(", ");
            for (String serverName : serverList) {
                if (!serversName.contains(serverName)) {
                    BungeeServer server = new BungeeServer(serverName);
                    serversName.add(serverName);
                    servers.add(server);
                    serversByName.put(serverName, server);

                }
            }
        }
    }

    public static void refreshGlobalCount() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerCount");
        out.writeUTF("ALL");
        Player player = Iterables.getFirst(ModuleService.getManagerModule().getOnlinePlayers(), null);
        if (player != null) player.sendPluginMessage(Celest.get(), "BungeeCord", out.toByteArray());
    }

    public static void refreshServerList() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServers");
        Player player = Iterables.getFirst(ModuleService.getManagerModule().getOnlinePlayers(), null);
        if (player != null) player.sendPluginMessage(Celest.get(), "BungeeCord", out.toByteArray());
    }

    public static void refreshCurrentServer() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServer");
        Player player = Iterables.getFirst(ModuleService.getManagerModule().getOnlinePlayers(), null);
        if (player != null) {
            player.sendPluginMessage(Celest.get(), "BungeeCord", out.toByteArray());
        }
    }

    public static void sendToServer(Player p, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        try {
            out.writeUTF("Connect");
            out.writeUTF(server);
        } catch (Exception e) {
            Celest.get().getLogger().log(Level.WARNING, "Failed to create BungeeCord connect message for server " + server, e);
            return;
        }
        p.sendPluginMessage(Celest.get(), "BungeeCord", out.toByteArray());
    }

    public static void refreshServerCount() {
        for (BungeeServer server : servers) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("PlayerCount");
            out.writeUTF(server.getName());
            Player player = Iterables.getFirst(ModuleService.getManagerModule().getOnlinePlayers(), null);
            if (player != null) player.sendPluginMessage(Celest.get(), "BungeeCord", out.toByteArray());
        }
    }
    public static boolean getServerStatus(String server) {
        Integer ports = Integer.valueOf(ModuleService.getFileModule().getFile(ConfigFiles.CONFIG).getInt("SERVER." + server + ".PORT"));
        String ipes = ModuleService.getFileModule().getFile(ConfigFiles.CONFIG).getString("SERVER." + server + ".IP");
        try {
            SocketAddress servers = new InetSocketAddress(ipes, ports.intValue());
            Socket socket = new Socket();
            socket.connect(servers, 1000);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public static int getServerCount(String server) {
        BungeeServer data = serversByName.get(server);

        if (data == null) {
            return 0;
        }

        return data.getPlayerCount();
    }

    public static boolean isCurrentServer(String server) {
        if (server == null || currentServerName == null || currentServerName.isEmpty()) {
            return false;
        }
        return currentServerName.equalsIgnoreCase(server);
    }
}
