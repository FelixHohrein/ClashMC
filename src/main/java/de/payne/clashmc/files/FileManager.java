package de.payne.clashmc.files;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;


public class FileManager {

    private final File file;
    private final FileConfiguration config;

    public FileManager(Plugin plugin, FILENAME filename) {
        this.file = new File(plugin.getDataFolder(), filename.toString().toLowerCase() + ".yml");

        // Create file if it doesn't exist
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs(); // Ensure parent dirs exist
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.config = YamlConfiguration.loadConfiguration(this.file);
    }

    public void addDefaults(Map<String, Object> defaults) {
        defaults.forEach(config::addDefault);
        config.options().copyDefaults(true);
        save();
    }

    public void save() {
        try {
            this.config.save(this.file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File getFile() {
        return this.file;
    }

    public FileConfiguration getConfig() {
        return this.config;
    }
}
