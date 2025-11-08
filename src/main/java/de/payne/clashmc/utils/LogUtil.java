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
        // Immer loggen (auch wenn debug=false ist, für Replay-System wichtig)
        plugin.getLogger().info("[DEBUG] " + message);
    }
}
