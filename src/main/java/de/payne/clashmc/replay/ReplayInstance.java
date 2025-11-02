package de.payne.clashmc.replay;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import de.payne.clashmc.ClashMC;
import de.payne.clashmc.maphandling.schematic.SchematicManager;
import de.payne.clashmc.utils.LogUtil;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Verwaltet eine aktive Replay-Session für einen Spieler.
 */
public class ReplayInstance {

    private final ClashMC plugin;
    @Getter
    private final Player viewer;
    @Getter
    private final ReplayData replayData;
    @Getter
    private final Location baseLocation;
    
    private GameMode originalGameMode;
    private Location originalLocation;
    @Getter
    private ReplayPlayer replayPlayer;
    
    public ReplayInstance(ClashMC plugin, Player viewer, ReplayData replayData) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.replayData = replayData;
        this.baseLocation = plugin.getReplayWorldManager().claimReplaySlot();
        
        if (baseLocation == null) {
            throw new RuntimeException("Keine freien Replay-Slots verfügbar!");
        }
    }
    
    /**
     * Startet die Replay-Wiedergabe.
     */
    public void start() {
        if (!viewer.isOnline()) {
            cleanup();
            return;
        }
        
        // Speichere Original-State
        originalGameMode = viewer.getGameMode();
        originalLocation = viewer.getLocation().clone();
        
        // Lade Verteidiger-Level aus Database
        plugin.getDatabaseManager().villages().getVillageLevelAsync(replayData.getDefenderId())
            .thenAccept(defenderLevel -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        // Spawne Verteidiger-Dorf
                        spawnDefenderVillage(defenderLevel);
                        
                        // Teleportiere Viewer in Vogelperspektive
                        teleportViewerToSkyView();
                        
                        // Setze Spectator-Mode
                        viewer.setGameMode(GameMode.SPECTATOR);
                        viewer.setAllowFlight(true);
                        viewer.setFlying(true);
                        
                        // Gebe Replay-Controls
                        giveReplayControls();
                        
                        // Starte Replay-Player
                        replayPlayer = new ReplayPlayer(plugin, this, replayData);
                        
                        // Registriere bei Controls-Listener
                        de.payne.clashmc.listeners.player.ReplayControlsListener.registerReplay(viewer, ReplayInstance.this);
                        
                        replayPlayer.start();
                        
                        viewer.sendMessage("§a§lReplay gestartet!");
                        viewer.sendMessage("§7Nutze die Items im Inventar zur Steuerung");
                        
                    } catch (Exception e) {
                        LogUtil.logError(plugin, "[Replay] Fehler beim Starten: " + e.getMessage());
                        e.printStackTrace();
                        cleanup();
                        viewer.sendMessage("§cFehler beim Laden des Replays.");
                    }
                });
            })
            .exceptionally(throwable -> {
                LogUtil.logError(plugin, "[Replay] Fehler beim Laden des Defender-Levels: " + throwable.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    cleanup();
                    viewer.sendMessage("§cFehler beim Laden des Replays.");
                });
                return null;
            });
    }
    
    private void spawnDefenderVillage(int level) {
        SchematicManager schematicManager = plugin.getSchematicManager();
        Clipboard clipboard = schematicManager.loadSchematic(level);
        
        if (clipboard == null) {
            throw new RuntimeException("Schematic für Level " + level + " konnte nicht geladen werden!");
        }
        
        // Paste Schematic (mit Bedrock-Layer)
        schematicManager.pasteSchematicWithBedrock(plugin, baseLocation, level);
        
        LogUtil.logInfo(plugin, "[Replay] Verteidiger-Dorf (Level " + level + ") gespawnt");
    }
    
    private void teleportViewerToSkyView() {
        // Vogelperspektive: 30 Blöcke über Dorf-Mitte
        Location skyView = baseLocation.clone().add(20, 30, 20);
        skyView.setPitch(70); // Schaut nach unten
        skyView.setYaw(45); // Diagonal
        
        viewer.teleport(skyView);
        
        LogUtil.logDebug(plugin, "[Replay] Spieler zu Vogelperspektive teleportiert: " + skyView);
    }
    
    private void giveReplayControls() {
        viewer.getInventory().clear();
        
        // Slot 0: Exit
        viewer.getInventory().setItem(0, new ItemStack(Material.RED_BED, 1));
        
        // Slot 2-6: Speed-Control
        viewer.getInventory().setItem(2, new ItemStack(Material.FEATHER, 1)); // 0.5x
        viewer.getInventory().setItem(3, new ItemStack(Material.PAPER, 1));   // 1x (Default)
        viewer.getInventory().setItem(4, new ItemStack(Material.SUGAR, 2));   // 2x
        viewer.getInventory().setItem(5, new ItemStack(Material.SUGAR, 4));   // 4x
        viewer.getInventory().setItem(6, new ItemStack(Material.SUGAR, 8));   // 8x
        
        // Slot 8: Camera-Toggle
        viewer.getInventory().setItem(8, new ItemStack(Material.ENDER_EYE, 1)); // Free/Follow
    }
    
    /**
     * Beendet das Replay und räumt auf.
     */
    public void cleanup() {
        // Unregister vom Controls-Listener
        de.payne.clashmc.listeners.player.ReplayControlsListener.unregisterReplay(viewer);
        
        // Stoppe Replay-Player
        if (replayPlayer != null) {
            replayPlayer.stop();
        }
        
        // Teleportiere Spieler zurück
        if (viewer != null && viewer.isOnline()) {
            if (originalLocation != null) {
                viewer.teleport(originalLocation);
            }
            
            if (originalGameMode != null) {
                viewer.setGameMode(originalGameMode);
            }
            
            viewer.getInventory().clear();
            viewer.setFlying(false);
            viewer.setAllowFlight(false);
        }
        
        // Lösche Dorf-Schematic
        deleteVillageSchematic();
        
        // Gebe Slot frei
        if (baseLocation != null) {
            plugin.getReplayWorldManager().releaseReplaySlot(baseLocation);
        }
        
        LogUtil.logInfo(plugin, "[Replay] Cleanup abgeschlossen");
    }
    
    private void deleteVillageSchematic() {
        try {
            plugin.getDatabaseManager().villages().getVillageLevelAsync(replayData.getDefenderId())
                .thenAccept(level -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        SchematicManager schematicManager = plugin.getSchematicManager();
                        Clipboard clipboard = schematicManager.loadSchematic(level);
                        
                        if (clipboard == null) return;
                        
                        World world = baseLocation.getWorld();
                        if (world == null) return;
                        
                        BlockVector3 min = clipboard.getMinimumPoint();
                        BlockVector3 max = clipboard.getMaximumPoint();
                        
                        // Lösche alle Blöcke
                        for (int x = min.getX(); x <= max.getX(); x++) {
                            for (int y = min.getY() - 1; y <= max.getY(); y++) {
                                for (int z = min.getZ(); z <= max.getZ(); z++) {
                                    int relX = x - min.getX();
                                    int relY = y - min.getY();
                                    int relZ = z - min.getZ();
                                    
                                    Location loc = baseLocation.clone().add(relX, relY, relZ);
                                    loc.getBlock().setType(Material.AIR);
                                }
                            }
                        }
                    });
                });
        } catch (Exception e) {
            LogUtil.logError(plugin, "[Replay] Fehler beim Löschen der Schematic: " + e.getMessage());
        }
    }
}

