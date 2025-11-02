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
    
    private ArmorStand npc;
    private List<BrokenBlock> brokenBlocks = new ArrayList<>();
    private List<MovementPoint> movementPoints = new ArrayList<>();
    
    private BukkitTask replayTask;
    private int currentMovementIndex = 0;
    private int currentBlockIndex = 0;
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
        // Parse BrokenBlocks
        try {
            if (data.getReplayData() != null && !data.getReplayData().isEmpty()) {
                JsonArray blocksArray = JsonParser.parseString(data.getReplayData()).getAsJsonArray();
                for (JsonElement element : blocksArray) {
                    JsonObject obj = element.getAsJsonObject();
                    brokenBlocks.add(new BrokenBlock(
                        obj.get("material").getAsString(),
                        obj.get("x").getAsInt(),
                        obj.get("y").getAsInt(),
                        obj.get("z").getAsInt(),
                        obj.get("timestamp").getAsLong()
                    ));
                }
            }
        } catch (Exception e) {
            LogUtil.logError(plugin, "[Replay] Fehler beim Parsen von BrokenBlocks: " + e.getMessage());
        }
        
        // Parse MovementPoints
        try {
            if (data.getMovementData() != null && !data.getMovementData().isEmpty()) {
                JsonArray movementArray = JsonParser.parseString(data.getMovementData()).getAsJsonArray();
                for (JsonElement element : movementArray) {
                    JsonObject obj = element.getAsJsonObject();
                    movementPoints.add(new MovementPoint(
                        obj.get("x").getAsDouble(),
                        obj.get("y").getAsDouble(),
                        obj.get("z").getAsDouble(),
                        obj.get("yaw").getAsFloat(),
                        obj.get("pitch").getAsFloat(),
                        obj.get("timestamp").getAsLong()
                    ));
                }
            }
        } catch (Exception e) {
            LogUtil.logError(plugin, "[Replay] Fehler beim Parsen von MovementPoints: " + e.getMessage());
        }
        
        LogUtil.logInfo(plugin, "[Replay] Parsed: " + brokenBlocks.size() + " Blöcke, " + movementPoints.size() + " Movement-Points");
    }
    
    /**
     * Startet die Wiedergabe.
     */
    public void start() {
        if (!instance.getViewer().isOnline()) return;
        
        // Spawne NPC
        spawnNPC();
        
        // Starte Wiedergabe
        replayStartTime = System.currentTimeMillis();
        startReplayTick();
    }
    
    private void spawnNPC() {
        try {
            // Hole Angreifer-Namen
            int attackerId = data.getAttackerId();
            java.util.UUID attackerUuid = plugin.getDatabaseManager().players().getUUIDByKingId(attackerId);
            String attackerName = attackerUuid != null 
                ? Bukkit.getOfflinePlayer(attackerUuid).getName() 
                : "Angreifer";
            
            // Spawn-Location: Erste Movement-Position oder Dorf-Eingang
            Location spawnLoc;
            if (!movementPoints.isEmpty()) {
                spawnLoc = movementPoints.get(0).toLocation(instance.getBaseLocation().getWorld(), instance.getBaseLocation());
            } else {
                spawnLoc = instance.getBaseLocation().clone().add(10, 1, 10);
            }
            
            // Spawne ArmorStand als NPC
            World world = instance.getBaseLocation().getWorld();
            npc = (ArmorStand) world.spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
            npc.setVisible(true);
            npc.setGravity(false);
            npc.setBasePlate(false);
            npc.setArms(true);
            npc.setCustomName("§6" + attackerName);
            npc.setCustomNameVisible(true);
            
            // Gebe NPC Waffe (Schwert)
            npc.setItemInHand(new ItemStack(Material.IRON_SWORD));
            
            LogUtil.logInfo(plugin, "[Replay] NPC gespawnt: " + attackerName);
            
        } catch (Exception e) {
            LogUtil.logError(plugin, "[Replay] Fehler beim Spawnen des NPCs: " + e.getMessage());
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
            @Override
            public void run() {
                if (isPaused || !instance.getViewer().isOnline()) {
                    return;
                }
                
                long elapsedMillis = (long) ((System.currentTimeMillis() - replayStartTime) * playbackSpeed);
                
                // Update Movement
                updateNPCMovement(elapsedMillis);
                
                // Update Block-Breaking
                updateBlockBreaking(elapsedMillis);
                
                // Update Camera (Follow-Mode)
                if (isFollowCam && npc != null) {
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
        if (npc == null || movementPoints.isEmpty()) return;
        
        // Finde nächsten Movement-Point
        while (currentMovementIndex < movementPoints.size()) {
            MovementPoint point = movementPoints.get(currentMovementIndex);
            
            if (point.getTimestamp() <= elapsedMillis) {
                // Teleportiere NPC zu dieser Position
                Location targetLoc = point.toLocation(instance.getBaseLocation().getWorld(), instance.getBaseLocation());
                npc.teleport(targetLoc);
                currentMovementIndex++;
            } else {
                break;
            }
        }
    }
    
    private void updateBlockBreaking(long elapsedMillis) {
        // Finde alle Blöcke die bis jetzt zerstört sein sollten
        while (currentBlockIndex < brokenBlocks.size()) {
            BrokenBlock block = brokenBlocks.get(currentBlockIndex);
            
            if (block.getTimestamp() <= elapsedMillis) {
                // Zerstöre Block
                breakBlock(block);
                currentBlockIndex++;
            } else {
                break;
            }
        }
    }
    
    private void breakBlock(BrokenBlock block) {
        Location blockLoc = instance.getBaseLocation().clone().add(block.getX(), block.getY(), block.getZ());
        org.bukkit.block.Block bukkitBlock = blockLoc.getBlock();
        
        // Partikel-Effekt
        World world = blockLoc.getWorld();
        if (world != null) {
            world.spawnParticle(Particle.BLOCK, blockLoc.add(0.5, 0.5, 0.5), 10, 
                0.2, 0.2, 0.2, 0.1, bukkitBlock.getBlockData());
            world.playSound(blockLoc, Sound.BLOCK_STONE_BREAK, 0.5f, 1.0f);
        }
        
        // Setze Block zu AIR
        bukkitBlock.setType(Material.AIR);
    }
    
    private void updateFollowCamera() {
        if (npc == null) return;
        
        // Kamera folgt NPC von hinten-oben
        Location npcLoc = npc.getLocation();
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
        
        for (MovementPoint point : movementPoints) {
            maxTimestamp = Math.max(maxTimestamp, point.getTimestamp());
        }
        
        return maxTimestamp > 0 ? maxTimestamp : 180000; // Default: 3 Minuten
    }
    
    private boolean isReplayFinished(long elapsedMillis) {
        return elapsedMillis >= getTotalDuration() && 
               currentBlockIndex >= brokenBlocks.size() && 
               currentMovementIndex >= movementPoints.size();
    }
    
    /**
     * Stoppt die Wiedergabe.
     */
    public void stop() {
        if (replayTask != null && !replayTask.isCancelled()) {
            replayTask.cancel();
        }
        
        if (npc != null && !npc.isDead()) {
            npc.remove();
        }
        
        LogUtil.logInfo(plugin, "[Replay] Wiedergabe gestoppt");
    }
    
    /**
     * Ändert die Wiedergabe-Geschwindigkeit.
     */
    public void setPlaybackSpeed(float speed) {
        this.playbackSpeed = Math.max(0.1f, Math.min(speed, 10.0f));
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

