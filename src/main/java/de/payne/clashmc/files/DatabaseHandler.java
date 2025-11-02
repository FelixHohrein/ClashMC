package de.payne.clashmc.files;

import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public class DatabaseHandler extends FileManager {
	
	
	private final FileConfiguration configuration;
	
	
	public DatabaseHandler(Plugin plugin) {
		super(plugin, FILENAME.DATABASE);
		this.configuration = super.getConfig();
		this.setDefaults();
	}
	
    private void setDefaults() {
        Map<String, Object> defaults = Map.of(
            "database.host", "host",
            "database.port", 3306,
            "database.database", "database",
            "database.username", "username",
            "database.password", "password",
            "database.connectionAllowed", false
        );
        addDefaults(defaults);
    }
	
	public final String getHost() {
		return this.configuration.getString("database.host");
	}
	
	public final int getPort() {
		return this.configuration.getInt("database.port");
	}	
	
	public final String getDatabase() {
		return this.configuration.getString("database.database");
	}
	
	public final String getUsername() {
		return this.configuration.getString("database.username");
	}
	
	public final String getPassword() {
		return this.configuration.getString("database.password");
	}
	
	public final boolean isConnectionAllowed() {
		return this.configuration.getBoolean("database.connectionAllowed");
	}
}
