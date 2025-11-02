package de.payne.clashmc.utils;

import org.bukkit.plugin.Plugin;

public class LogUtil {
	
    public static void logInfo(Plugin plugin, String message) {
        plugin.getLogger().info("[INFO] " + message);
    }

    public static void logError(Plugin plugin, String message) {
        plugin.getLogger().severe("[ERROR] " + message);
    }

    public static void logDebug(Plugin plugin, String message) {
        // Optional: nur im Dev-Modus loggen
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
}
