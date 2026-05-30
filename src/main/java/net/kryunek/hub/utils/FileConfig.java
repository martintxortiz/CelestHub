package net.kryunek.hub.utils;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;



@Getter
public class FileConfig {

    private final File file;
    private FileConfiguration configuration;

    public FileConfig(JavaPlugin plugin, String fileName) {
        this.file = new File(plugin.getDataFolder(), fileName);

        if (!this.file.exists()) {
            this.file.getParentFile().mkdirs();
            if (plugin.getResource(fileName) == null) {
                try {
                    this.file.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to create new file " + fileName);
                }
            } else {
                plugin.saveResource(fileName, false);
            }
        }

        this.configuration = YamlConfiguration.loadConfiguration(this.file);
    }

    public double getDouble(String path) {
        if (configuration.contains(path)) {
            return configuration.getDouble(path);
        }
        return 0;
    }

    public int getInt(String path) {
        if (configuration.contains(path)) {
            return configuration.getInt(path);
        }
        return 0;
    }

    public boolean getBoolean(String path) {
        if (configuration.contains(path)) {
            return configuration.getBoolean(path);
        }
        return false;
    }

    public long getLong(String path){
        if (configuration.contains(path)){
            return configuration.getLong(path);
        }
        return 0L;
    }

    public String getString(String path) {
        return getString(path, "", true);
    }

    public String getString(String path, String callback, boolean colorize) {
        if (configuration.contains(path)) {
            String value = configuration.getString(path);
            if (value == null) {
                return callback;
            }
            if (colorize) {
                return ChatColor.translateAlternateColorCodes('&', value);
            } else {
                return value;
            }
        }
        return callback;
    }

    public List<String> getReversedStringList(String path) {
        List<String> list = getStringList(path);
        List<String> toReturn = new ArrayList<>(list);
        Collections.reverse(toReturn);
        return toReturn;
    }

    /**
     * Returns the colorized string list at {@code path}, or an <em>empty mutable list</em>
     * when the path is missing. Never returns {@code null} and never returns a sentinel
     * placeholder, so callers can iterate the result directly.
     */
    public List<String> getStringList(String path) {
        ArrayList<String> strings = new ArrayList<>();
        if (configuration.contains(path)) {
            for (String string : configuration.getStringList(path)) {
                strings.add(ChatColor.translateAlternateColorCodes('&', string));
            }
        }
        return strings;
    }

    public List<String> getStringListOrDefault(String path, List<String> toReturn) {
        if (configuration.contains(path)) {
            ArrayList<String> strings = new ArrayList<>();
            for (String string : configuration.getStringList(path)) {
                strings.add(ChatColor.translateAlternateColorCodes('&', string));
            }
            return strings;
        }
        return toReturn;
    }

    public void save() {
        try {
            this.configuration.save(this.file);
        }
        catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not save config file " + this.file, e);
        }
    }

    public void reload() {
        this.configuration = YamlConfiguration.loadConfiguration(file);
    }
}
