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

import java.util.List;

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
        LogUtil.logInfo(plugin, "[Replay] ===== REPLAY INSTANCE ERSTELLT =====");
        LogUtil.logInfo(plugin, "[Replay] Viewer: " + viewer.getName());
        LogUtil.logInfo(plugin, "[Replay] ReplayData: AttackID=" + replayData.getAttackId());
        
        this.plugin = plugin;
        this.viewer = viewer;
        this.replayData = replayData;
        
        LogUtil.logInfo(plugin, "[Replay] Hole Replay-Slot...");
        this.baseLocation = plugin.getReplayWorldManager().claimReplaySlot();
        
        if (baseLocation == null) {
            LogUtil.logError(plugin, "[Replay] FEHLER: Keine freien Replay-Slots verfügbar!");
            throw new RuntimeException("Keine freien Replay-Slots verfügbar!");
        }
        
        LogUtil.logInfo(plugin, "[Replay] Replay-Slot erhalten: " + baseLocation);
        LogUtil.logInfo(plugin, "[Replay] ReplayInstance-Konstruktor beendet!");
    }
    
    /**
     * Startet die Replay-Wiedergabe.
     */
    public void start() {
        LogUtil.logInfo(plugin, "[Replay] ===== REPLAY INSTANCE START =====");
        LogUtil.logInfo(plugin, "[Replay] Viewer: " + viewer.getName());
        LogUtil.logInfo(plugin, "[Replay] ReplayData: AttackID=" + replayData.getAttackId() + ", AttackerID=" + replayData.getAttackerId() + ", DefenderID=" + replayData.getDefenderId());
        LogUtil.logInfo(plugin, "[Replay] BaseLocation: " + baseLocation);
        
        if (!viewer.isOnline()) {
            LogUtil.logError(plugin, "[Replay] Viewer ist nicht online!");
            cleanup();
            return;
        }
        
        // Speichere Original-State
        originalGameMode = viewer.getGameMode();
        originalLocation = viewer.getLocation().clone();
        
        LogUtil.logInfo(plugin, "[Replay] Lade Verteidiger-Level aus Database...");
        LogUtil.logInfo(plugin, "[Replay] DefenderID: " + replayData.getDefenderId());
        
        // Lade Verteidiger-Level aus Database
        plugin.getDatabaseManager().villages().getVillageLevelAsync(replayData.getDefenderId())
            .thenAccept(defenderLevel -> {
                LogUtil.logInfo(plugin, "[Replay] Verteidiger-Level geladen: " + defenderLevel);
                LogUtil.logInfo(plugin, "[Replay] Schedule Server-Thread Task...");
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        LogUtil.logInfo(plugin, "[Replay] ===== SERVER THREAD TASK GESTARTET =====");
                        LogUtil.logInfo(plugin, "[Replay] Starte Replay auf Server-Thread...");
                        
                        // Spawne Verteidiger-Dorf
                        LogUtil.logInfo(plugin, "[Replay] Spawne Verteidiger-Dorf...");
                        spawnDefenderVillage(defenderLevel);
                        
                        // Teleportiere Viewer in Vogelperspektive
                        LogUtil.logInfo(plugin, "[Replay] Teleportiere Viewer...");
                        teleportViewerToSkyView();
                        
                        // Setze Spectator-Mode
                        viewer.setGameMode(GameMode.SPECTATOR);
                        viewer.setAllowFlight(true);
                        viewer.setFlying(true);
                        
                        // Gebe Replay-Controls
                        LogUtil.logInfo(plugin, "[Replay] Gebe Replay-Controls...");
                        giveReplayControls();
                        
                        // Starte Replay-Player
                        LogUtil.logInfo(plugin, "[Replay] Erstelle ReplayPlayer...");
                        replayPlayer = new ReplayPlayer(plugin, this, replayData);
                        
                        // Registriere bei Controls-Listener
                        de.payne.clashmc.listeners.player.ReplayControlsListener.registerReplay(viewer, ReplayInstance.this);
                        
                        LogUtil.logInfo(plugin, "[Replay] Starte ReplayPlayer...");
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
        LogUtil.logInfo(plugin, "[Replay] ===== SPAWN DEFENDER VILLAGE =====");
        LogUtil.logInfo(plugin, "[Replay] Level: " + level);
        LogUtil.logInfo(plugin, "[Replay] BaseLocation: " + baseLocation);
        
        SchematicManager schematicManager = plugin.getSchematicManager();
        Clipboard clipboard = schematicManager.loadSchematic(level);
        
        if (clipboard == null) {
            LogUtil.logError(plugin, "[Replay] Schematic für Level " + level + " konnte nicht geladen werden!");
            throw new RuntimeException("Schematic für Level " + level + " konnte nicht geladen werden!");
        }
        
        LogUtil.logInfo(plugin, "[Replay] Schematic geladen, starte Paste...");
        
        // Paste Schematic synchron (WorldEdit erfordert das)
        // WICHTIG: Dies blockiert den Server-Thread, aber ist notwendig für WorldEdit
        // Das wird bereits in einem async CompletableFuture aufgerufen, 
        // daher blockiert es nicht den Haupt-Thread
        try {
            schematicManager.pasteSchematicWithBedrock(plugin, baseLocation, level);
            LogUtil.logInfo(plugin, "[Replay] Verteidiger-Dorf (Level " + level + ") gespawnt");
        } catch (Exception e) {
            LogUtil.logError(plugin, "[Replay] Fehler beim Spawnen des Dorfes: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Fehler beim Spawnen des Dorfes", e);
        }
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
        ItemStack exit = new ItemStack(Material.RED_BED, 1);
        org.bukkit.inventory.meta.ItemMeta exitMeta = exit.getItemMeta();
        exitMeta.setDisplayName("§c§lReplay beenden");
        exitMeta.setLore(List.of("§7Klicken um das Replay zu verlassen"));
        exit.setItemMeta(exitMeta);
        viewer.getInventory().setItem(0, exit);
        
        // Slot 2-6: Speed-Control
        createSpeedItem(viewer, 2, Material.FEATHER, "§70.5x", 0.5f, "§7Langsame Wiedergabe");
        createSpeedItem(viewer, 3, Material.PAPER, "§f1x (Normal)", 1.0f, "§7Normale Geschwindigkeit");
        createSpeedItem(viewer, 4, Material.SUGAR, "§a2x", 2.0f, "§7Doppelte Geschwindigkeit");
        createSpeedItem(viewer, 5, Material.SUGAR, "§a4x", 4.0f, "§7Vierfache Geschwindigkeit");
        createSpeedItem(viewer, 6, Material.SUGAR, "§a8x", 8.0f, "§7Achtfache Geschwindigkeit");
        
        // Slot 8: Camera-Toggle
        ItemStack camera = new ItemStack(Material.ENDER_EYE, 1);
        org.bukkit.inventory.meta.ItemMeta cameraMeta = camera.getItemMeta();
        cameraMeta.setDisplayName("§b§lKamera: §fFrei");
        cameraMeta.setLore(List.of("§7Klicken um dem NPC zu folgen"));
        camera.setItemMeta(cameraMeta);
        viewer.getInventory().setItem(8, camera);
    }
    
    private void createSpeedItem(Player viewer, int slot, Material material, String name, float speed, String lore) {
        ItemStack item = new ItemStack(material, 1);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§lGeschwindigkeit: " + name);
        meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        viewer.getInventory().setItem(slot, item);
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
                        
                        if (clipboard == null) {
                            LogUtil.logError(plugin, "[Replay] Clipboard ist null beim Löschen der Schematic");
                            return;
                        }
                        
                        World world = baseLocation.getWorld();
                        if (world == null) {
                            LogUtil.logError(plugin, "[Replay] World ist null beim Löschen der Schematic");
                            return;
                        }
                        
                        BlockVector3 min = clipboard.getMinimumPoint();
                        BlockVector3 max = clipboard.getMaximumPoint();
                        
                        LogUtil.logInfo(plugin, "[Replay] Lösche Dorf-Schematic von " + min + " bis " + max);
                        
                        // Lösche alle Blöcke
                        int blocksDeleted = 0;
                        for (int x = min.getX(); x <= max.getX(); x++) {
                            for (int y = min.getY() - 1; y <= max.getY(); y++) {
                                for (int z = min.getZ(); z <= max.getZ(); z++) {
                                    int relX = x - min.getX();
                                    int relY = y - min.getY();
                                    int relZ = z - min.getZ();
                                    
                                    Location loc = baseLocation.clone().add(relX, relY, relZ);
                                    loc.getBlock().setType(Material.AIR);
                                    blocksDeleted++;
                                }
                            }
                        }
                        
                        LogUtil.logInfo(plugin, "[Replay] Dorf-Schematic gelöscht: " + blocksDeleted + " Blöcke entfernt");
                    });
                });
        } catch (Exception e) {
            LogUtil.logError(plugin, "[Replay] Fehler beim Löschen der Schematic: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

