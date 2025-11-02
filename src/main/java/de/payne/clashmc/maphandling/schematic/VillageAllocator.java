package de.payne.clashmc.maphandling.schematic;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

import de.payne.clashmc.files.VillageDataHandler;
import lombok.Getter;

public class VillageAllocator {

    private final VillageDataHandler dataHandler;
    private final World world;
    @Getter
    private final int maxX; // max Bereiche in X-Richtung
    @Getter
    private final int chunksPerArea;
    @Getter
    private final int chunkGap;

    public VillageAllocator(VillageDataHandler dataHandler, World world) {
        this.dataHandler = dataHandler;
        this.world = world;
        this.maxX = 100;
        this.chunksPerArea = 5;
        this.chunkGap = 1;
    }

    public Map<UUID, Location> getAllVillageSpawns() {
        return dataHandler.getAllVillageSpawns();
    }
    
    public Location allocateNextAvailableArea(UUID playerUUID) {
        // Falls bereits gesetzt, gib zurück
        Location existing = dataHandler.getVillageSpawn(playerUUID);
        if (existing != null) return existing;

        int index = dataHandler.getConfig().getInt("nextAreaIndex", 0);

        int totalChunks = chunksPerArea + chunkGap;

        int gridX = index % maxX;
        int gridZ = index / maxX;

        int chunkX = gridX * totalChunks;
        int chunkZ = gridZ * totalChunks;

        double x = chunkX * 16.0;
        double z = chunkZ * 16.0;
        double y = 64; //world.getHighestBlockYAt((int)x, (int)z);

        // NEU: Verwende direkt den Chunk-Start als Spawnpunkt
        Location spawn = new Location(world, x, y, z);

        // Speichern & Zähler erhöhen
        dataHandler.setVillageSpawn(playerUUID, spawn);
        dataHandler.getConfig().set("nextAreaIndex", index + 1);
        dataHandler.save();

        return spawn;
    }
    
    /**
     * Gibt die Mitte der Village-Chuckfläche zurück, wobei die Y-Höhe anhand des höchsten Blocks an dieser Position bestimmt wird.
     */
    public Location getVillageCenterTeleportOrSpawnLocation(UUID playerUUID) {
        Location origin = dataHandler.getVillageSpawn(playerUUID); // Linke obere Ecke der Fläche
        if (origin == null) return null;

        double x = origin.getX();
        double z = origin.getZ();

        // Berechne Mittelpunkt in X und Z
        double centerX = x + (chunksPerArea * 16) / 2.0;
        double centerZ = z + (chunksPerArea * 16) / 2.0;
        int y = 66;

        return new Location(world, centerX, y, centerZ);
    }
    
    public Location getVillageSpawn(UUID playerUUID) {
    	return this.dataHandler.getVillageSpawn(playerUUID);
    }
}
