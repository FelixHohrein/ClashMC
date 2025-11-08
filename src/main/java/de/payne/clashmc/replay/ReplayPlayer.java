package de.payne.clashmc.replay;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.payne.clashmc.ClashMC;
import de.payne.clashmc.attacks.BrokenBlock;
import de.payne.clashmc.utils.LogUtil;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Engine für die Replay-Wiedergabe.
 * Steuert NPC-Movement, Block-Breaking und Timeline.
 */
public class ReplayPlayer {

    private final ClashMC plugin;
    private final ReplayInstance instance;
    private final ReplayData data;
    
    @Getter
    private float playbackSpeed = 1.0f; // 1x = Normal
    
    private ArmorStand attackerNPC;
    private ArmorStand defenderNPC;
    private List<BrokenBlock> brokenBlocks = new ArrayList<>();
    private List<MovementPoint> attackerMovementPoints = new ArrayList<>();
    private List<MovementPoint> defenderMovementPoints = new ArrayList<>();
    private List<CombatEvent> combatEvents = new ArrayList<>();
    
    private BukkitTask replayTask;
    private int currentAttackerMovementIndex = 0;
    private int currentDefenderMovementIndex = 0;
    private int currentBlockIndex = 0;
    private int currentCombatIndex = 0;
    private long replayStartTime;
    
    @Getter
    private boolean isPaused = false;
    @Getter
    private boolean isFollowCam = false;
    
    public ReplayPlayer(ClashMC plugin, ReplayInstance instance, ReplayData data) {
        this.plugin = plugin;
        this.instance = instance;
        this.data = data;
        
        parseReplayData();
    }
    
    /**
     * Parst Replay-Daten aus JSON.
     */
    private void parseReplayData() {
        LogUtil.logInfo(plugin, "[Replay] ===== PARSE REPLAY DATA =====");
        LogUtil.logInfo(plugin, "[Replay] ReplayData AttackID: " + data.getAttackId());
        LogUtil.logInfo(plugin, "[Replay] ReplayData vorhanden: " + (data.getReplayData() != null ? "JA" : "NEIN"));
        LogUtil.logInfo(plugin, "[Replay] MovementData vorhanden: " + (data.getMovementData() != null ? "JA" : "NEIN"));
        if (data.getMovementData() != null) {
            LogUtil.logInfo(plugin, "[Replay] MovementData Länge: " + data.getMovementData().length() + " Zeichen");
        }
        // Parse BrokenBlocks
        try {
            if (data.getReplayData() != null && !data.getReplayData().isEmpty()) {
                JsonArray blocksArray = JsonParser.parseString(data.getReplayData()).getAsJsonArray();
                
                // Prüfe ob Timestamps absolut sind (alte Replays) oder relativ (neue Replays)
                // Absolute Timestamps sind > 1000000000 (mehr als ~11 Tage in Millisekunden)
                boolean hasAbsoluteTimestamps = false;
                if (blocksArray.size() > 0) {
                    long firstTimestamp = blocksArray.get(0).getAsJsonObject().get("timestamp").getAsLong();
                    hasAbsoluteTimestamps = firstTimestamp > 1000000000L; // Größer als ~11 Tage
                }
                
                long firstBlockTimestamp = 0;
                if (hasAbsoluteTimestamps && blocksArray.size() > 0) {
                    // Konvertiere absolute zu relativen Timestamps
                    firstBlockTimestamp = blocksArray.get(0).getAsJsonObject().get("timestamp").getAsLong();
                    LogUtil.logInfo(plugin, "[Replay] Altes Replay-Format erkannt (absolute Timestamps), konvertiere zu relativ...");
                }
                
                for (JsonElement element : blocksArray) {
                    JsonObject obj = element.getAsJsonObject();
                    long timestamp = obj.get("timestamp").getAsLong();
                    
                    // Wenn absolute Timestamps: Konvertiere zu relativ (relativ zum ersten Block)
                    if (hasAbsoluteTimestamps) {
                        timestamp = timestamp - firstBlockTimestamp;
                    }
                    
                    brokenBlocks.add(new BrokenBlock(
                        obj.get("material").getAsString(),
                        obj.get("x").getAsInt(),
                        obj.get("y").getAsInt(),
                        obj.get("z").getAsInt(),
                        timestamp
                    ));
                }
            }
        } catch (Exception e) {
            LogUtil.logError(plugin, "[Replay] Fehler beim Parsen von BrokenBlocks: " + e.getMessage());
        }
        
        // Parse MovementPoints (neues Format: {"attacker": [...], "defender": [...]})
        // Rückwärtskompatibilität: Altes Format (Array) wird auch unterstützt
        try {
            if (data.getMovementData() != null && !data.getMovementData().isEmpty()) {
                JsonElement movementElement = JsonParser.parseString(data.getMovementData());
                
                // Prüfe ob es ein Objekt (neues Format) oder Array (altes Format) ist
                if (movementElement.isJsonObject()) {
                    // Neues Format: {"attacker": [...], "defender": [...]}
                    JsonObject movementRoot = movementElement.getAsJsonObject();
                    
                    // Parse Angreifer Movement-Points
                    if (movementRoot.has("attacker")) {
                        JsonArray attackerArray = movementRoot.getAsJsonArray("attacker");
                        for (JsonElement element : attackerArray) {
                            JsonObject obj = element.getAsJsonObject();
                            attackerMovementPoints.add(new MovementPoint(
                                obj.get("x").getAsDouble(),
                                obj.get("y").getAsDouble(),
                                obj.get("z").getAsDouble(),
                                obj.get("yaw").getAsFloat(),
                                obj.get("pitch").getAsFloat(),
                                obj.get("timestamp").getAsLong()
                            ));
                        }
                    }
                    
                    // Parse Verteidiger Movement-Points (nur bei Online-Angriffen)
                    if (data.isOnline() && movementRoot.has("defender")) {
                        JsonArray defenderArray = movementRoot.getAsJsonArray("defender");
                        for (JsonElement element : defenderArray) {
                            JsonObject obj = element.getAsJsonObject();
                            defenderMovementPoints.add(new MovementPoint(
                                obj.get("x").getAsDouble(),
                                obj.get("y").getAsDouble(),
                                obj.get("z").getAsDouble(),
                                obj.get("yaw").getAsFloat(),
                                obj.get("pitch").getAsFloat(),
                                obj.get("timestamp").getAsLong()
                            ));
                        }
                    }
                    
                    // Parse Combat-Events (nur bei Online-Angriffen)
                    if (data.isOnline() && movementRoot.has("combat")) {
                        JsonArray combatArray = movementRoot.getAsJsonArray("combat");
                        for (JsonElement element : combatArray) {
                            JsonObject obj = element.getAsJsonObject();
                            combatEvents.add(new CombatEvent(
                                obj.get("attacker_id").getAsInt(),
                                obj.get("x").getAsDouble(),
                                obj.get("y").getAsDouble(),
                                obj.get("z").getAsDouble(),
                                obj.get("damage").getAsDouble(),
                                obj.get("timestamp").getAsLong()
                            ));
                        }
                    }
                } else if (movementElement.isJsonArray()) {
                    // Altes Format: Array (nur Angreifer, Rückwärtskompatibilität)
                    JsonArray movementArray = movementElement.getAsJsonArray();
                    for (JsonElement element : movementArray) {
                        JsonObject obj = element.getAsJsonObject();
                        attackerMovementPoints.add(new MovementPoint(
                            obj.get("x").getAsDouble(),
                            obj.get("y").getAsDouble(),
                            obj.get("z").getAsDouble(),
                            obj.get("yaw").getAsFloat(),
                            obj.get("pitch").getAsFloat(),
                            obj.get("timestamp").getAsLong()
                        ));
                    }
                    LogUtil.logInfo(plugin, "[Replay] Altes Movement-Format erkannt (nur Angreifer)");
                }
            }
        } catch (Exception e) {
            LogUtil.logError(plugin, "[Replay] Fehler beim Parsen von MovementPoints: " + e.getMessage());
            e.printStackTrace();
        }
        
        LogUtil.logInfo(plugin, "[Replay] ===== REPLAY DATA PARSED =====");
        LogUtil.logInfo(plugin, "[Replay] Broken Blocks: " + brokenBlocks.size());
        LogUtil.logInfo(plugin, "[Replay] Angreifer Movement-Points: " + attackerMovementPoints.size());
        LogUtil.logInfo(plugin, "[Replay] Verteidiger Movement-Points: " + defenderMovementPoints.size());
        LogUtil.logInfo(plugin, "[Replay] Movement Data: " + (data.getMovementData() != null ? data.getMovementData().substring(0, Math.min(100, data.getMovementData().length())) + "..." : "NULL"));
        
        // Warnung wenn keine Movement-Points vorhanden sind
        if (attackerMovementPoints.isEmpty()) {
            LogUtil.logError(plugin, "[Replay] WARNUNG: Keine Angreifer Movement-Points gefunden! NPC wird sich nicht bewegen.");
            LogUtil.logError(plugin, "[Replay] Movement Data ist: " + (data.getMovementData() != null ? "NICHT NULL" : "NULL"));
            if (data.getMovementData() != null) {
                LogUtil.logError(plugin, "[Replay] Movement Data Länge: " + data.getMovementData().length());
            }
        }
        
        // Debug: Zeige erste und letzte Movement-Points
        if (!attackerMovementPoints.isEmpty()) {
            MovementPoint first = attackerMovementPoints.get(0);
            MovementPoint last = attackerMovementPoints.get(attackerMovementPoints.size() - 1);
            LogUtil.logDebug(plugin, "[Replay] Erster Angreifer Movement-Point: x=" + first.getX() + ", y=" + first.getY() + ", z=" + first.getZ() + ", timestamp=" + first.getTimestamp());
            LogUtil.logDebug(plugin, "[Replay] Letzter Angreifer Movement-Point: x=" + last.getX() + ", y=" + last.getY() + ", z=" + last.getZ() + ", timestamp=" + last.getTimestamp());
        }
        
        if (!defenderMovementPoints.isEmpty()) {
            MovementPoint first = defenderMovementPoints.get(0);
            MovementPoint last = defenderMovementPoints.get(defenderMovementPoints.size() - 1);
            LogUtil.logDebug(plugin, "[Replay] Erster Verteidiger Movement-Point: x=" + first.getX() + ", y=" + first.getY() + ", z=" + first.getZ() + ", timestamp=" + first.getTimestamp());
            LogUtil.logDebug(plugin, "[Replay] Letzter Verteidiger Movement-Point: x=" + last.getX() + ", y=" + last.getY() + ", z=" + last.getZ() + ", timestamp=" + last.getTimestamp());
        }
        
        // Debug: Zeige erste und letzte BrokenBlocks
        if (!brokenBlocks.isEmpty()) {
            BrokenBlock first = brokenBlocks.get(0);
            BrokenBlock last = brokenBlocks.get(brokenBlocks.size() - 1);
            LogUtil.logDebug(plugin, "[Replay] Erster Block: " + first.getMaterial() + " bei (" + first.getX() + ", " + first.getY() + ", " + first.getZ() + "), timestamp=" + first.getTimestamp());
            LogUtil.logDebug(plugin, "[Replay] Letzter Block: " + last.getMaterial() + " bei (" + last.getX() + ", " + last.getY() + ", " + last.getZ() + "), timestamp=" + last.getTimestamp());
        }
    }
    
    /**
     * Startet die Wiedergabe.
     */
    public void start() {
        if (!instance.getViewer().isOnline()) {
            LogUtil.logError(plugin, "[Replay] Viewer ist nicht online!");
            return;
        }
        
        LogUtil.logInfo(plugin, "[Replay] ===== REPLAY START =====");
        LogUtil.logInfo(plugin, "[Replay] BaseLocation: " + instance.getBaseLocation());
        LogUtil.logInfo(plugin, "[Replay] Attacker Movement-Points: " + attackerMovementPoints.size());
        LogUtil.logInfo(plugin, "[Replay] Defender Movement-Points: " + defenderMovementPoints.size());
        
        // Spawne NPCs (Angreifer + Verteidiger bei Online-Angriffen)
        spawnNPCs();
        
        // Starte Wiedergabe
        replayStartTime = System.currentTimeMillis();
        LogUtil.logInfo(plugin, "[Replay] Replay-Task gestartet um " + replayStartTime);
        startReplayTick();
    }
    
    private void spawnNPCs() {
        try {
            World world = instance.getBaseLocation().getWorld();
            if (world == null) return;
            
            // Spawne Angreifer-NPC
            int attackerId = data.getAttackerId();
            java.util.UUID attackerUuid = plugin.getDatabaseManager().players().getUUIDByKingId(attackerId);
            String attackerName = attackerUuid != null 
                ? Bukkit.getOfflinePlayer(attackerUuid).getName() 
                : "Angreifer";
            
            // Spawn-Location: Genau an der Angreifer-Spawn-Position (wie im Original-Angriff)
            // WICHTIG: baseLocation ist die Ecke des Dorfes, wir müssen zur Spawn-Position addieren
            Location attackerSpawnLoc = instance.getBaseLocation().clone().add(2, 1, 2);
            
            // Prüfe ob die Spawn-Position über dem Void ist - wenn ja, finde den höchsten Block
            if (attackerSpawnLoc.getY() < 100) {
                // Wenn unter Y=100, dann ist es wahrscheinlich im Void
                // Finde den höchsten Block an dieser Position
                int highestY = world.getHighestBlockYAt(attackerSpawnLoc);
                if (highestY > 0) {
                    attackerSpawnLoc.setY(highestY + 1);
                    LogUtil.logInfo(plugin, "[Replay] Spawn-Position korrigiert: Y=" + attackerSpawnLoc.getY() + " (höchster Block: " + highestY + ")");
                } else {
                    // Kein Block gefunden, verwende Y=100 als Fallback
                    attackerSpawnLoc.setY(100);
                    LogUtil.logInfo(plugin, "[Replay] Spawn-Position auf Y=100 gesetzt (kein Block gefunden)");
                }
            }
            
            LogUtil.logInfo(plugin, "[Replay] BaseLocation: " + instance.getBaseLocation());
            LogUtil.logInfo(plugin, "[Replay] Attacker Spawn Location: " + attackerSpawnLoc);
            
            attackerNPC = (ArmorStand) world.spawnEntity(attackerSpawnLoc, EntityType.ARMOR_STAND);
            attackerNPC.setVisible(true);
            attackerNPC.setMarker(false); // WICHTIG: false = sichtbar, true = unsichtbar (Marker-Mode)
            attackerNPC.setGravity(false);
            attackerNPC.setBasePlate(false);
            attackerNPC.setArms(true);
            attackerNPC.setSmall(false); // Normal-Größe
            attackerNPC.setCustomName("§6" + attackerName);
            attackerNPC.setCustomNameVisible(true);
            attackerNPC.setItemInHand(new ItemStack(Material.IRON_SWORD));
            // Rüstung hinzufügen
            attackerNPC.setHelmet(new ItemStack(Material.IRON_HELMET));
            attackerNPC.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            attackerNPC.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            attackerNPC.setBoots(new ItemStack(Material.IRON_BOOTS));
            attackerNPC.setCanPickupItems(false);
            attackerNPC.setCollidable(false);
            attackerNPC.setInvulnerable(true); // Kann nicht beschädigt werden
            
            LogUtil.logInfo(plugin, "[Replay] Angreifer-NPC gespawnt: " + attackerName + " bei " + attackerSpawnLoc);
            LogUtil.logInfo(plugin, "[Replay] Angreifer Movement-Points: " + attackerMovementPoints.size());
            LogUtil.logInfo(plugin, "[Replay] NPC sichtbar: " + attackerNPC.isVisible() + ", Marker: " + attackerNPC.isMarker());
            
            // Spawne Verteidiger-NPC (nur bei Online-Angriffen)
            if (data.isOnline() && !defenderMovementPoints.isEmpty()) {
                int defenderId = data.getDefenderId();
                java.util.UUID defenderUuid = plugin.getDatabaseManager().players().getUUIDByKingId(defenderId);
                String defenderName = defenderUuid != null 
                    ? Bukkit.getOfflinePlayer(defenderUuid).getName() 
                    : "Verteidiger";
                
                // Spawn-Location: Genau an der Verteidiger-Spawn-Position (wie im Original-Angriff)
                Location defenderSpawnLoc = instance.getBaseLocation().clone().add(10, 1, 10);
                
                defenderNPC = (ArmorStand) world.spawnEntity(defenderSpawnLoc, EntityType.ARMOR_STAND);
                defenderNPC.setVisible(true);
                defenderNPC.setMarker(false); // WICHTIG: false = sichtbar, true = unsichtbar (Marker-Mode)
                defenderNPC.setGravity(false);
                defenderNPC.setBasePlate(false);
                defenderNPC.setArms(true);
                defenderNPC.setSmall(false); // Normal-Größe
                defenderNPC.setCustomName("§c" + defenderName);
                defenderNPC.setCustomNameVisible(true);
                defenderNPC.setItemInHand(new ItemStack(Material.IRON_SWORD));
                // Rüstung hinzufügen
                defenderNPC.setHelmet(new ItemStack(Material.IRON_HELMET));
                defenderNPC.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                defenderNPC.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                defenderNPC.setBoots(new ItemStack(Material.IRON_BOOTS));
                defenderNPC.setCanPickupItems(false);
                defenderNPC.setCollidable(false);
                defenderNPC.setInvulnerable(true); // Kann nicht beschädigt werden
                
                LogUtil.logInfo(plugin, "[Replay] Verteidiger-NPC gespawnt: " + defenderName + " bei " + defenderSpawnLoc);
                LogUtil.logInfo(plugin, "[Replay] Verteidiger Movement-Points: " + defenderMovementPoints.size());
                LogUtil.logInfo(plugin, "[Replay] NPC sichtbar: " + defenderNPC.isVisible() + ", Marker: " + defenderNPC.isMarker());
            }
            
        } catch (Exception e) {
            LogUtil.logError(plugin, "[Replay] Fehler beim Spawnen der NPCs: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void teleportViewerToSkyView() {
        // 30 Blöcke über Dorf-Mitte
        Location skyView = instance.getBaseLocation().clone().add(20, 30, 20);
        skyView.setPitch(70); // Schaut nach unten
        skyView.setYaw(45); // Diagonal
        
        instance.getViewer().teleport(skyView);
    }
    
    private void giveReplayControls() {
        Player viewer = instance.getViewer();
        viewer.getInventory().clear();
        
        // Slot 0: Exit
        ItemStack exit = new ItemStack(Material.RED_BED);
        org.bukkit.inventory.meta.ItemMeta exitMeta = exit.getItemMeta();
        exitMeta.setDisplayName("§cReplay beenden");
        exit.setItemMeta(exitMeta);
        viewer.getInventory().setItem(0, exit);
        
        // Slot 2-6: Speed-Control
        createSpeedItem(viewer, 2, Material.FEATHER, "§70.5x", 0.5f);
        createSpeedItem(viewer, 3, Material.PAPER, "§f1x (Normal)", 1.0f);
        createSpeedItem(viewer, 4, Material.SUGAR, "§a2x", 2.0f);
        createSpeedItem(viewer, 5, Material.SUGAR, "§a4x", 4.0f);
        createSpeedItem(viewer, 6, Material.SUGAR, "§a8x", 8.0f);
        
        // Slot 8: Camera-Toggle
        ItemStack camera = new ItemStack(Material.ENDER_EYE);
        org.bukkit.inventory.meta.ItemMeta cameraMeta = camera.getItemMeta();
        cameraMeta.setDisplayName("§bKamera: §fFrei");
        cameraMeta.setLore(List.of("§7Klicken um NPC zu folgen"));
        camera.setItemMeta(cameraMeta);
        viewer.getInventory().setItem(8, camera);
    }
    
    private void createSpeedItem(Player viewer, int slot, Material material, String name, float speed) {
        ItemStack item = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eGeschwindigkeit: " + name);
        item.setItemMeta(meta);
        viewer.getInventory().setItem(slot, item);
    }
    
    private void startReplayTick() {
        replayTask = new BukkitRunnable() {
            private int tickCount = 0;
            
            @Override
            public void run() {
                if (isPaused || !instance.getViewer().isOnline()) {
                    if (tickCount == 0) {
                        LogUtil.logInfo(plugin, "[Replay] Replay-Task gestoppt: paused=" + isPaused + ", online=" + instance.getViewer().isOnline());
                    }
                    return;
                }
                
                tickCount++;
                if (tickCount % 20 == 0) { // Alle 1 Sekunde loggen
                    LogUtil.logDebug(plugin, "[Replay] Replay-Task läuft: Tick=" + tickCount + ", AttackerIndex=" + currentAttackerMovementIndex + "/" + attackerMovementPoints.size());
                }
                
                long elapsedMillis = (long) ((System.currentTimeMillis() - replayStartTime) * playbackSpeed);
                
                // Update Movement (beide NPCs)
                updateNPCMovement(elapsedMillis);
                
                // Update Block-Breaking
                updateBlockBreaking(elapsedMillis);
                
                // Update Combat-Events
                updateCombatEvents(elapsedMillis);
                
                // Update Camera (Follow-Mode)
                if (isFollowCam && attackerNPC != null) {
                    updateFollowCamera();
                }
                
                // Update ActionBar
                updateActionBar(elapsedMillis);
                
                // Check if Replay finished
                if (isReplayFinished(elapsedMillis)) {
                    cancel();
                    instance.getViewer().sendMessage("§a§lReplay beendet!");
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        instance.cleanup();
                    }, 20L * 3); // 3 Sekunden warten
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Jeder Tick für smooth movement
    }
    
    private void updateNPCMovement(long elapsedMillis) {
        World world = instance.getBaseLocation().getWorld();
        if (world == null) return;
        
        // Update Angreifer-NPC Movement mit Interpolation für smooth movement
        if (attackerNPC != null && !attackerNPC.isDead()) {
            if (!attackerMovementPoints.isEmpty()) {
                // Finde nächsten Movement-Point
                MovementPoint nextPoint = null;
                MovementPoint prevPoint = null;
                
                // Finde vorherigen und nächsten Point für Interpolation
                for (int i = 0; i < attackerMovementPoints.size(); i++) {
                    MovementPoint point = attackerMovementPoints.get(i);
                    if (point.getTimestamp() <= elapsedMillis) {
                        prevPoint = point;
                        currentAttackerMovementIndex = i + 1;
                    } else {
                        nextPoint = point;
                        break;
                    }
                }
                
                // Interpolation zwischen prevPoint und nextPoint
                if (prevPoint != null) {
                    Location targetLoc;
                    if (nextPoint != null && prevPoint.getTimestamp() < nextPoint.getTimestamp()) {
                        // Interpolation zwischen zwei Points
                        double progress = (double)(elapsedMillis - prevPoint.getTimestamp()) / 
                                         (nextPoint.getTimestamp() - prevPoint.getTimestamp());
                        progress = Math.max(0, Math.min(1, progress)); // Clamp zwischen 0 und 1
                        
                        Location prevLoc = prevPoint.toLocation(world, instance.getBaseLocation());
                        Location nextLoc = nextPoint.toLocation(world, instance.getBaseLocation());
                        
                        targetLoc = prevLoc.clone();
                        targetLoc.setX(prevLoc.getX() + (nextLoc.getX() - prevLoc.getX()) * progress);
                        targetLoc.setY(prevLoc.getY() + (nextLoc.getY() - prevLoc.getY()) * progress);
                        targetLoc.setZ(prevLoc.getZ() + (nextLoc.getZ() - prevLoc.getZ()) * progress);
                        targetLoc.setYaw((float)(prevLoc.getYaw() + (nextLoc.getYaw() - prevLoc.getYaw()) * progress));
                        targetLoc.setPitch((float)(prevLoc.getPitch() + (nextLoc.getPitch() - prevLoc.getPitch()) * progress));
                    } else {
                        // Nur prevPoint verwenden
                        targetLoc = prevPoint.toLocation(world, instance.getBaseLocation());
                    }
                    
                    // Teleportiere nur wenn sich Position geändert hat (verhindert zu viele Teleports bei hoher Speed)
                    Location currentLoc = attackerNPC.getLocation();
                    if (currentLoc.distanceSquared(targetLoc) > 0.01) { // Nur teleportieren wenn > 0.1 Blöcke entfernt
                        attackerNPC.teleport(targetLoc);
                    }
                }
            } else {
                // Keine Movement-Points: NPC bleibt an Spawn-Position
                LogUtil.logDebug(plugin, "[Replay] Keine Angreifer Movement-Points vorhanden!");
            }
        } else if (attackerNPC == null) {
            LogUtil.logError(plugin, "[Replay] Angreifer-NPC ist null!");
        }
        
        // Update Verteidiger-NPC Movement (nur bei Online-Angriffen) - gleiche Logik
        if (defenderNPC != null && !defenderNPC.isDead()) {
            if (!defenderMovementPoints.isEmpty()) {
                MovementPoint nextPoint = null;
                MovementPoint prevPoint = null;
                
                for (int i = 0; i < defenderMovementPoints.size(); i++) {
                    MovementPoint point = defenderMovementPoints.get(i);
                    if (point.getTimestamp() <= elapsedMillis) {
                        prevPoint = point;
                        currentDefenderMovementIndex = i + 1;
                    } else {
                        nextPoint = point;
                        break;
                    }
                }
                
                if (prevPoint != null) {
                    Location targetLoc;
                    if (nextPoint != null && prevPoint.getTimestamp() < nextPoint.getTimestamp()) {
                        double progress = (double)(elapsedMillis - prevPoint.getTimestamp()) / 
                                         (nextPoint.getTimestamp() - prevPoint.getTimestamp());
                        progress = Math.max(0, Math.min(1, progress));
                        
                        Location prevLoc = prevPoint.toLocation(world, instance.getBaseLocation());
                        Location nextLoc = nextPoint.toLocation(world, instance.getBaseLocation());
                        
                        targetLoc = prevLoc.clone();
                        targetLoc.setX(prevLoc.getX() + (nextLoc.getX() - prevLoc.getX()) * progress);
                        targetLoc.setY(prevLoc.getY() + (nextLoc.getY() - prevLoc.getY()) * progress);
                        targetLoc.setZ(prevLoc.getZ() + (nextLoc.getZ() - prevLoc.getZ()) * progress);
                        targetLoc.setYaw((float)(prevLoc.getYaw() + (nextLoc.getYaw() - prevLoc.getYaw()) * progress));
                        targetLoc.setPitch((float)(prevLoc.getPitch() + (nextLoc.getPitch() - prevLoc.getPitch()) * progress));
                    } else {
                        targetLoc = prevPoint.toLocation(world, instance.getBaseLocation());
                    }
                    
                    Location currentLoc = defenderNPC.getLocation();
                    if (currentLoc.distanceSquared(targetLoc) > 0.01) {
                        defenderNPC.teleport(targetLoc);
                    }
                }
            } else if (data.isOnline()) {
                // Online-Angriff aber keine Movement-Points
                LogUtil.logDebug(plugin, "[Replay] Keine Verteidiger Movement-Points vorhanden (Online-Angriff)!");
            }
        }
    }
    
    private void updateBlockBreaking(long elapsedMillis) {
        // Finde alle Blöcke die bis jetzt zerstört sein sollten
        while (currentBlockIndex < brokenBlocks.size()) {
            BrokenBlock block = brokenBlocks.get(currentBlockIndex);
            
            // Timestamp ist bereits relativ zum Angriffsstart
            if (block.getTimestamp() <= elapsedMillis) {
                // Zerstöre Block
                breakBlock(block);
                currentBlockIndex++;
                
                // Debug: Logge erste paar Blöcke
                if (currentBlockIndex <= 5) {
                    LogUtil.logDebug(plugin, "[Replay] Block zerstört: " + block.getMaterial() + " bei (" + block.getX() + ", " + block.getY() + ", " + block.getZ() + "), timestamp=" + block.getTimestamp() + ", elapsed=" + elapsedMillis);
                }
            } else {
                break;
            }
        }
    }
    
    private void breakBlock(BrokenBlock block) {
        Location blockLoc = instance.getBaseLocation().clone().add(block.getX(), block.getY(), block.getZ());
        org.bukkit.block.Block bukkitBlock = blockLoc.getBlock();
        
        // Prüfe ob Block überhaupt existiert (nicht im Void)
        if (bukkitBlock.getType() == Material.AIR || bukkitBlock.getType() == Material.VOID_AIR || bukkitBlock.getType() == Material.BEDROCK) {
            LogUtil.logDebug(plugin, "[Replay] Block bereits zerstört/Void/Bedrock: " + block.getMaterial() + " bei " + blockLoc);
            return; // Block bereits zerstört, im Void oder Bedrock
        }
        
        World world = blockLoc.getWorld();
        if (world == null) return;
        
        Material blockType = bukkitBlock.getType();
        
        // Partikel-Effekt (vor dem Zerstören)
        Location particleLoc = blockLoc.clone().add(0.5, 0.5, 0.5);
        
        // Spawne Partikel basierend auf Block-Typ
        try {
            // Versuche Block-Partikel zu spawnen
            world.spawnParticle(Particle.BLOCK, particleLoc, 30, 
                0.3, 0.3, 0.3, 0.1, bukkitBlock.getBlockData());
        } catch (Exception e) {
            // Fallback: Explosion-Partikel
            world.spawnParticle(Particle.EXPLOSION, particleLoc, 5, 0.2, 0.2, 0.2, 0.1);
        }
        
        // Sound-Effekt basierend auf Block-Typ
        Sound breakSound = Sound.BLOCK_STONE_BREAK;
        if (blockType.name().contains("WOOD") || blockType.name().contains("LOG") || blockType.name().contains("PLANKS")) {
            breakSound = Sound.BLOCK_WOOD_BREAK;
        } else if (blockType.name().contains("GLASS")) {
            breakSound = Sound.BLOCK_GLASS_BREAK;
        } else if (blockType.name().contains("METAL") || blockType.name().contains("IRON") || blockType.name().contains("GOLD")) {
            breakSound = Sound.BLOCK_METAL_BREAK;
        }
        
        // Spiele Sound für alle Spieler in der Nähe
        world.playSound(blockLoc, breakSound, 0.8f, 1.0f);
        
        // Setze Block zu AIR
        bukkitBlock.setType(Material.AIR);
        
        LogUtil.logDebug(plugin, "[Replay] Block zerstört: " + blockType + " bei " + blockLoc);
    }
    
    private void updateFollowCamera() {
        if (attackerNPC == null) return;
        
        // Kamera folgt Angreifer-NPC von hinten-oben
        Location npcLoc = attackerNPC.getLocation();
        Vector direction = npcLoc.getDirection().multiply(-3); // 3 Blöcke hinter NPC
        direction.setY(2); // 2 Blöcke höher
        
        Location camLoc = npcLoc.clone().add(direction);
        camLoc.setDirection(npcLoc.toVector().subtract(camLoc.toVector()));
        
        instance.getViewer().teleport(camLoc);
    }
    
    private void updateActionBar(long elapsedMillis) {
        long totalDuration = getTotalDuration();
        double progress = (double) elapsedMillis / totalDuration * 100.0;
        progress = Math.min(progress, 100.0);
        
        String progressBar = createProgressBar(progress);
        String timeFormatted = formatTime(elapsedMillis) + " / " + formatTime(totalDuration);
        
        String message = "§6Replay §7[" + progressBar + "§7] §e" + 
                         String.format("%.0f%%", progress) + " §7| §e" + timeFormatted +
                         " §7| Speed: §e" + playbackSpeed + "x";
        
        instance.getViewer().sendActionBar(message);
    }
    
    private String createProgressBar(double percent) {
        int totalBars = 20;
        int filledBars = (int) (percent / 100.0 * totalBars);
        
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < totalBars; i++) {
            if (i < filledBars) {
                bar.append("§a█");
            } else {
                bar.append("§7█");
            }
        }
        return bar.toString();
    }
    
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    private long getTotalDuration() {
        long maxTimestamp = 0;
        
        for (BrokenBlock block : brokenBlocks) {
            maxTimestamp = Math.max(maxTimestamp, block.getTimestamp());
        }
        
        for (MovementPoint point : attackerMovementPoints) {
            maxTimestamp = Math.max(maxTimestamp, point.getTimestamp());
        }
        
        for (MovementPoint point : defenderMovementPoints) {
            maxTimestamp = Math.max(maxTimestamp, point.getTimestamp());
        }
        
        for (CombatEvent event : combatEvents) {
            maxTimestamp = Math.max(maxTimestamp, event.getTimestamp());
        }
        
        return maxTimestamp > 0 ? maxTimestamp : 180000; // Default: 3 Minuten
    }
    
    private boolean isReplayFinished(long elapsedMillis) {
        return elapsedMillis >= getTotalDuration() && 
               currentBlockIndex >= brokenBlocks.size() && 
               currentAttackerMovementIndex >= attackerMovementPoints.size() &&
               currentDefenderMovementIndex >= defenderMovementPoints.size() &&
               currentCombatIndex >= combatEvents.size();
    }
    
    /**
     * Visualisiert Combat-Events während der Wiedergabe.
     */
    private void updateCombatEvents(long elapsedMillis) {
        World world = instance.getBaseLocation().getWorld();
        if (world == null) return;
        
        // Finde alle Combat-Events die bis jetzt stattgefunden haben sollten
        while (currentCombatIndex < combatEvents.size()) {
            CombatEvent event = combatEvents.get(currentCombatIndex);
            
            if (event.getTimestamp() <= elapsedMillis) {
                // Visualisiere Combat-Event
                visualizeCombatEvent(event, world);
                currentCombatIndex++;
            } else {
                break;
            }
        }
    }
    
    /**
     * Visualisiert ein Combat-Event (Partikel, Sound, ArmorStand-Rotation).
     */
    private void visualizeCombatEvent(CombatEvent event, World world) {
        Location eventLoc = event.toLocation(world, instance.getBaseLocation());
        
        // Bestimme welcher NPC angegriffen hat
        ArmorStand attackingNPC = (event.getAttackerId() == 1) ? attackerNPC : defenderNPC;
        
        if (attackingNPC == null || attackingNPC.isDead()) {
            return;
        }
        
        // Partikel-Effekt (Schlag-Partikel)
        Location particleLoc = eventLoc.clone().add(0, 1, 0);
        world.spawnParticle(Particle.SWEEP_ATTACK, particleLoc, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.CRIT, particleLoc, 10, 0.3, 0.3, 0.3, 0.1);
        
        // Sound-Effekt
        world.playSound(eventLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.0f);
        world.playSound(eventLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.5f, 1.2f);
        
        // ArmorStand-Rotation für "Swing"-Effekt
        // Kurz rotieren und zurück
        float originalYaw = attackingNPC.getLocation().getYaw();
        Location npcLoc = attackingNPC.getLocation();
        
        // Rotiere NPC kurz (Swing-Animation)
        npcLoc.setYaw(originalYaw + 30f);
        attackingNPC.teleport(npcLoc);
        
        // Nach 2 Ticks zurückrotieren
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (attackingNPC != null && !attackingNPC.isDead()) {
                Location currentLoc = attackingNPC.getLocation();
                currentLoc.setYaw(originalYaw);
                attackingNPC.teleport(currentLoc);
            }
        }, 2L);
        
        LogUtil.logDebug(plugin, "[Replay] Combat-Event visualisiert: " + 
            (event.getAttackerId() == 1 ? "Angreifer" : "Verteidiger") + 
            " bei " + eventLoc + ", Schaden: " + event.getDamage());
    }
    
    /**
     * Stoppt die Wiedergabe.
     */
    public void stop() {
        if (replayTask != null && !replayTask.isCancelled()) {
            replayTask.cancel();
        }
        
        if (attackerNPC != null && !attackerNPC.isDead()) {
            attackerNPC.remove();
        }
        
        if (defenderNPC != null && !defenderNPC.isDead()) {
            defenderNPC.remove();
        }
        
        LogUtil.logInfo(plugin, "[Replay] Wiedergabe gestoppt");
    }
    
    /**
     * Ändert die Wiedergabe-Geschwindigkeit.
     * Passt replayStartTime an, damit keine Zeit übersprungen wird.
     */
    public void setPlaybackSpeed(float speed) {
        float oldSpeed = this.playbackSpeed;
        this.playbackSpeed = Math.max(0.1f, Math.min(speed, 10.0f));
        
        // Berechne bereits vergangene Zeit mit alter Geschwindigkeit
        long currentElapsedMillis = (long) ((System.currentTimeMillis() - replayStartTime) * oldSpeed);
        
        // Passe replayStartTime an, damit die neue Geschwindigkeit ab jetzt gilt
        // Formel: replayStartTime = now - (elapsedMillis / newSpeed)
        replayStartTime = System.currentTimeMillis() - (long)(currentElapsedMillis / playbackSpeed);
        
        instance.getViewer().sendMessage("§eGeschwindigkeit: §a" + playbackSpeed + "x");
    }
    
    /**
     * Togglet den Follow-Cam Modus.
     */
    public void toggleFollowCam() {
        isFollowCam = !isFollowCam;
        
        if (isFollowCam) {
            instance.getViewer().sendMessage("§aKamera folgt jetzt dem NPC");
        } else {
            instance.getViewer().sendMessage("§7Kamera ist jetzt frei beweglich");
            // Teleportiere zurück zur Vogelperspektive
            Location skyView = instance.getBaseLocation().clone().add(20, 30, 20);
            skyView.setPitch(70);
            skyView.setYaw(45);
            instance.getViewer().teleport(skyView);
        }
    }
    
    /**
     * Pause/Resume Toggle.
     */
    public void togglePause() {
        isPaused = !isPaused;
        instance.getViewer().sendMessage(isPaused ? "§ePause" : "§aWiedergabe läuft");
    }
}

