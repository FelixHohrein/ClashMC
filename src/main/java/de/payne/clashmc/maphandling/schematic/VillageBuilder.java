package de.payne.clashmc.maphandling.schematic;



import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EditSession.ReorderMode;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockTypes;

public class VillageBuilder {

    private final Plugin plugin;
    private final SchematicManager schematicManager;
    private final VillageAllocator allocator;

    public VillageBuilder(Plugin plugin, SchematicManager schematicManager, VillageAllocator allocator) {
        this.plugin = plugin;
        this.schematicManager = schematicManager;
        this.allocator = allocator;
    }
    
    public void upgradeVillage(Player player, int nextLevel) {
        int currentLevel = nextLevel - 1;

        deleteVillageWithSchematic(player, currentLevel); // Entfernt altes Dorf
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            buildVillage(player, nextLevel); // Baut neues
        }, 5);
    }

    public void buildVillage(Player king, int level) {
        Location spawn = this.allocator.allocateNextAvailableArea(king.getUniqueId());
        
        try {
            this.schematicManager.pasteSchematic(plugin, spawn, level);
            king.sendMessage("§aDein Dorf der Stufe " + level + " wurde platziert.");
        } catch (Exception e) {
            king.sendMessage("§cFehler beim Platzieren: " + e.getMessage());
            e.printStackTrace();
        }
        
    }
    
    public void deleteVillageWithSchematic(Player king, int currentLevel) {
        Location spawn = allocator.getVillageSpawn(king.getUniqueId());
        if (spawn == null) {
            king.sendMessage("§cDu besitzt kein Dorf zum Löschen.");
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Clipboard clipboard = schematicManager.loadSchematic(currentLevel);
                if (clipboard == null) {
                    king.sendMessage("§cSchematic konnte nicht geladen werden.");
                    return;
                }

                com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(spawn.getWorld());
                BlockVector3 origin = BlockVector3.at(spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ());

                try (EditSession editSession = WorldEdit.getInstance()
                        .newEditSessionBuilder()
                        .world(weWorld)
                        .build()) {
                	editSession.setReorderMode(ReorderMode.MULTI_STAGE);
                    // Alle Schematic-Blöcke auf AIR setzen
                	clipboard.getRegion().forEach(pos -> {
                	    BlockVector3 worldPos = origin.add(pos.subtract(clipboard.getOrigin()));
                	    try {
                	        editSession.setBlock(worldPos, BlockTypes.AIR.getDefaultState());
                	    } catch (WorldEditException e) {
                	        Bukkit.getLogger().warning("Fehler beim Löschen eines Blocks bei " + worldPos + ": " + e.getMessage());
                	        e.printStackTrace();
                	    }
                	});

                    king.sendMessage("§aDein Dorf wurde erfolgreich gelöscht.");
                }

            } catch (Exception e) {
                king.sendMessage("§cFehler beim Löschen des Dorfes.");
                e.printStackTrace();
            }
            
            this.clearWorldEntities(Bukkit.getWorld("Clash"));
        });
    }
    
    private void clearWorldEntities(World world) {
        for (Entity entity : world.getEntities()) {
            // Entferne alle Drops (Items auf dem Boden) und lebende Mobs/Tiere
            if (entity instanceof Item || entity instanceof LivingEntity && !(entity instanceof Player)) {
                entity.remove();
            }
        }
    }
    
    //TEST METHODE UM CHUNKBLOCK ZUWEISUNG ZU TESTEN
    
//    public void visualizeAllVillages(Material blockType) {
//        Map<UUID, Location> villages = allocator.getAllVillageSpawns();
//
//        Bukkit.getLogger().info("[ClashMC] Anzahl Villages: " + villages.size()); // <- HIER
//
//        for (Location center : villages.values()) {
//        	Bukkit.getLogger().info("[ClashMC] Visualisiere Dorf bei " + center + " (" + blockType + ")");
//            visualizeVillageArea(center, blockType);
//        }
//    }
//    
//    private void visualizeVillageArea(Location center, Material blockType) {
//        World world = center.getWorld();
//
//        int radius = (allocator.getChunksPerArea() * 16) / 2;
//        int y = center.getBlockY();
//
//        int minX = center.getBlockX() - radius;
//        int maxX = center.getBlockX() + radius - 1;
//        int minZ = center.getBlockZ() - radius;
//        int maxZ = center.getBlockZ() + radius - 1;
//
//        // Fülle den Bodenbereich mit Blöcken (optional: nur Rahmen zeichnen)
//        for (int x = minX; x <= maxX; x++) {
//            for (int z = minZ; z <= maxZ; z++) {
//                // Nur Kante setzen für Rahmen:
//                if (x == minX || x == maxX || z == minZ || z == maxZ) {
//                    world.getBlockAt(x, y, z).setType(blockType);
//                }
//            }
//        }
//    }
    
}
