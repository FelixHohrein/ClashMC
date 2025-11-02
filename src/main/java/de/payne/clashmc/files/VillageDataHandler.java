package de.payne.clashmc.files;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public class VillageDataHandler extends FileManager {

	
    public VillageDataHandler(Plugin plugin) {
        super(plugin, FILENAME.VILLAGE);
    }

    public Location getVillageSpawn(UUID kingUUID) {
        String path = "villages." + kingUUID.toString() + ".spawn";
        if (!super.getConfig().contains(path)) return null;

        String worldName = super.getConfig().getString(path + ".world");
        double x = super.getConfig().getDouble(path + ".x");
        double y = super.getConfig().getDouble(path + ".y");
        double z = super.getConfig().getDouble(path + ".z");
        float yaw = (float) super.getConfig().getDouble(path + ".yaw");
        float pitch = (float) super.getConfig().getDouble(path + ".pitch");

        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        return new Location(world, x, y, z, yaw, pitch);
    }

    public void setVillageSpawn(UUID kingUUID, Location location) {
        String path = "villages." + kingUUID.toString() + ".spawn";
        super.getConfig().set(path + ".world", location.getWorld().getName());
        super.getConfig().set(path + ".x", location.getX());
        super.getConfig().set(path + ".y", location.getY());
        super.getConfig().set(path + ".z", location.getZ());
        super.getConfig().set(path + ".yaw", location.getYaw());
        super.getConfig().set(path + ".pitch", location.getPitch());
        save();
    }
    
    public Map<UUID, Location> getAllVillageSpawns() {
        Map<UUID, Location> result = new HashMap<>();
        ConfigurationSection section = super.getConfig().getConfigurationSection("villages");
        if (section == null) return result;

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection spawnSection = section.getConfigurationSection(key + ".spawn");
                if (spawnSection != null) {
                    World world = Bukkit.getWorld(spawnSection.getString("world"));
                    double x = spawnSection.getDouble("x");
                    double y = spawnSection.getDouble("y");
                    double z = spawnSection.getDouble("z");
                    float yaw = (float) spawnSection.getDouble("yaw");
                    float pitch = (float) spawnSection.getDouble("pitch");

                    if (world != null) {
                        Location loc = new Location(world, x, y, z, yaw, pitch);
                        result.put(uuid, loc);
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // ungültige UUID
            }
        }

        return result;
    }
}
