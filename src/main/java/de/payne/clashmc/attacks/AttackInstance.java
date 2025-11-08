package de.payne.clashmc.attacks;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.maphandling.schematic.SchematicManager;
import de.payne.clashmc.maphandling.schematic.VillageBuilder;
import de.payne.clashmc.mine.MineSchematicManager;
import de.payne.clashmc.utils.LogUtil;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class AttackInstance {

	@Getter
    private final UUID attackerUuid;
	@Getter
    private final UUID defenderUuid;
    @Getter
    private final Location baseLocation;
    private final boolean isOnline;
    
    @Getter
    private int totalDestructibleBlocks = 0;

    @Getter
    private final List<BrokenBlock> brokenBlocks = new ArrayList<>();
    
    @Getter
    private final List<de.payne.clashmc.replay.MovementPoint> attackerMovementPoints = new ArrayList<>();
    @Getter
    private final List<de.payne.clashmc.replay.MovementPoint> defenderMovementPoints = new ArrayList<>();
    @Getter
    private final List<de.payne.clashmc.replay.CombatEvent> combatEvents = new ArrayList<>();
    
    private long attackStartTime;
    private BukkitRunnable actionBarTask;
    private BukkitRunnable attackerMovementTrackingTask;
    private BukkitRunnable defenderMovementTrackingTask;
    
    // Tod-Tracking: Map<UUID, AnzahlTode>
    private final Map<UUID, Integer> deathCounts = new HashMap<>();
    // Cooldown-Tracking: Map<UUID, System.currentTimeMillis() wann Respawn möglich>
    private final Map<UUID, Long> respawnCooldowns = new HashMap<>();

    
    public AttackInstance(UUID attackerUuid, UUID defenderUuid, Location baseLocation, boolean isOnline) {
        this.attackerUuid = attackerUuid;
        this.defenderUuid = defenderUuid;
        this.baseLocation = baseLocation;
        this.isOnline = isOnline;
    }

    public Location getAttackerSpawn() {
        return baseLocation.clone().add(2, 1, 2);
    }

    public Location getDefenderSpawn() {
        return baseLocation.clone().add(10, 1, 10);
    }

    public void startBattle(Runnable onFinish) {
        Player attacker = Bukkit.getPlayer(attackerUuid);
        if (attacker == null || !attacker.isOnline()) {
            onFinish.run();
            return;
        }

        // Attack-Start-Time für Movement-Tracking
        this.attackStartTime = System.currentTimeMillis();
        
        // Starte Movement-Tracking für Angreifer
        startAttackerMovementTracking(attacker);
        
        // Starte Movement-Tracking für Verteidiger (nur bei Online-Angriffen)
        if (isOnline) {
            Player defender = Bukkit.getPlayer(defenderUuid);
            if (defender != null && defender.isOnline()) {
                startDefenderMovementTracking(defender);
            }
        }

        // Attack-Duration aus Config
        final int maxDurationSeconds = ClashMC.getInstance().getConfigManager().getAttackDurationSeconds();

        // Starte den ActionBar-Task
        actionBarTask = new BukkitRunnable() {
            int secondsPassed = 0;

            @Override
            public void run() {
                int timeLeft = maxDurationSeconds - secondsPassed;

                if (timeLeft < 0) {
                    // Zeit abgelaufen
                    sendActionBar(attacker, "§cAngriff beendet!");
                    cancel();
                    onFinish.run();
                    return;
                }

                // Zeit formatieren MM:SS
                int minutes = timeLeft / 60;
                int seconds = timeLeft % 60;
                String timeFormatted = String.format("%02d:%02d", minutes, seconds);

                // Schaden in Prozent berechnen
                double damagePercent = calculateDamage(); // double z.B. 62.5
                String damageFormatted = String.format("%.1f", damagePercent);

                // ActionBar-Text zusammenbauen
                String message = String.format("§6Verbleibende Zeit: §e%s §7| §6Schaden: §e%s%%", timeFormatted, damageFormatted);

                sendActionBar(attacker, message);

                secondsPassed++;
            }
        };

        actionBarTask.runTaskTimer(ClashMC.getInstance(), 0L, 20L); // alle 20 Ticks = 1 Sekunde

        // Dein bisheriger Timer für onFinish aus startBattle
        Bukkit.getScheduler().runTaskLater(ClashMC.getInstance(), () -> {
            if (actionBarTask != null && !actionBarTask.isCancelled()) {
                actionBarTask.cancel();
            }
            // Stoppe Movement-Tracking
            stopMovementTracking();
            onFinish.run();
        }, 20 * maxDurationSeconds);
    }

    public void spawnOfflineDefense() {
        // Z. B. Eisengolems, ArmorStands mit Pfeilkanonen etc.
    }

    
    public void scanDestructibleBlocks(int villageLevel, SchematicManager schematicManager) {
        totalDestructibleBlocks = 0;

        Clipboard clipboard = schematicManager.loadSchematic(villageLevel);
        if (clipboard == null) {
            LogUtil.logError(ClashMC.getInstance(), "Konnte Schematic für Level " + villageLevel + " nicht laden.");
            return;
        }

        World world = baseLocation.getWorld();
        int baseX = baseLocation.getBlockX();
        int baseY = baseLocation.getBlockY();
        int baseZ = baseLocation.getBlockZ();

        int width = clipboard.getRegion().getWidth();
        int height = clipboard.getRegion().getHeight();
        int length = clipboard.getRegion().getLength();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    Block block = world.getBlockAt(baseX + x, baseY + y, baseZ + z);
                    if (isDestructible(block)) {
                        totalDestructibleBlocks++;
                    }
                }
            }
        }
    }

    public boolean isDestructible(Block block) {
        Material type = block.getType();
        return !(type == Material.AIR || type == Material.BARRIER || type == Material.BEDROCK || type == Material.GRASS_BLOCK || type == Material.TALL_GRASS || type == Material.SHORT_GRASS || type == Material.POPPY || type == Material.DANDELION || type == Material.COARSE_DIRT || type == Material.DIRT_PATH || type == Material.ROOTED_DIRT);
    }
    
    public boolean isDestructible(Material type) {
        return !(type == Material.AIR || type == Material.BARRIER || type == Material.BEDROCK || type == Material.GRASS_BLOCK || type == Material.TALL_GRASS || type == Material.SHORT_GRASS || type == Material.POPPY || type == Material.DANDELION || type == Material.COARSE_DIRT || type == Material.DIRT_PATH || type == Material.ROOTED_DIRT);
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }
    
    
    /**
     * Berechnet den Zerstörungsgrad des Dorfes in Prozent.
     * Nutzt die Anzahl zerstörter relevanter Blöcke (nicht-Luft, nicht-Bedrock)
     * im Verhältnis zu den vorher gescannten zerstörbaren Blöcken.
     */
    public double calculateDamage() {
        if (totalDestructibleBlocks == 0) return 0.0;

        long destroyedRelevantBlocks = brokenBlocks.stream()
                .filter(bb -> {
                    Material mat = Material.getMaterial(bb.getMaterial());
                    return mat != null && isDestructible(mat);
                })
                .count();

        return (destroyedRelevantBlocks * 100.0) / totalDestructibleBlocks;
    }

    /**
     * Clash Coin Belohnung: 1% Zerstörung = 10 Coins.
     */
    public long calculateClashCoinReward() {
        double damage = calculateDamage();
        return Math.round(damage * 10); // z. B. 62.5 % → 625 Coins
    }

    /**
     * King Coin Belohnung: nur bei mindestens 90% Zerstörung.
     */
    public long calculateKingCoinReward() {
        return calculateDamage() >= 90.0 ? 1 : 0;
    }
    

    //zu broken blocks hinzufügen
    public void addBrokenBlock(Block block) {
        Location relativeLoc = block.getLocation().clone().subtract(baseLocation);
        // Timestamp relativ zum Angriffsstart (wie bei Movement-Points)
        long relativeTimestamp = System.currentTimeMillis() - attackStartTime;
        brokenBlocks.add(new BrokenBlock(
            block.getType().name(),
            relativeLoc.getBlockX(),
            relativeLoc.getBlockY(),
            relativeLoc.getBlockZ(),
            relativeTimestamp
        ));
    }
    
    // Methode, um alle gebrochenen Blöcke als JSON String zu exportieren
    public JsonArray getBrokenBlocksAsJsonArray() {
        JsonArray array = new JsonArray();
        for (BrokenBlock bb : brokenBlocks) {
            JsonObject obj = new JsonObject();
            obj.addProperty("material", bb.getMaterial());
            obj.addProperty("x", bb.getX());
            obj.addProperty("y", bb.getY());
            obj.addProperty("z", bb.getZ());
            obj.addProperty("timestamp", bb.getTimestamp());
            array.add(obj);
        }
        return array;
    }

    
    public String getBrokenBlocksAsJsonString() {
        return getBrokenBlocksAsJsonArray().toString();
    }
    
    /**
     * Startet das Movement-Tracking für den Angreifer.
     * Speichert Position alle 10 Ticks (0.5 Sekunden).
     */
    private void startAttackerMovementTracking(Player attacker) {
        attackerMovementTrackingTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (attacker == null || !attacker.isOnline()) {
                    cancel();
                    return;
                }
                
                // Speichere aktuelle Position (relativ zur baseLocation)
                de.payne.clashmc.replay.MovementPoint point = 
                    de.payne.clashmc.replay.MovementPoint.fromPlayer(attacker, attackStartTime, baseLocation);
                attackerMovementPoints.add(point);
            }
        };
        
        // Start: sofort, Repeat: alle 10 Ticks (0.5 Sekunden)
        attackerMovementTrackingTask.runTaskTimer(ClashMC.getInstance(), 0L, 10L);
    }
    
    /**
     * Startet das Movement-Tracking für den Verteidiger (nur bei Online-Angriffen).
     * Speichert Position alle 10 Ticks (0.5 Sekunden).
     */
    private void startDefenderMovementTracking(Player defender) {
        defenderMovementTrackingTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (defender == null || !defender.isOnline()) {
                    cancel();
                    return;
                }
                
                // Speichere aktuelle Position (relativ zur baseLocation)
                de.payne.clashmc.replay.MovementPoint point = 
                    de.payne.clashmc.replay.MovementPoint.fromPlayer(defender, attackStartTime, baseLocation);
                defenderMovementPoints.add(point);
            }
        };
        
        // Start: sofort, Repeat: alle 10 Ticks (0.5 Sekunden)
        defenderMovementTrackingTask.runTaskTimer(ClashMC.getInstance(), 0L, 10L);
    }
    
    /**
     * Stoppt das Movement-Tracking für beide Spieler.
     */
    private void stopMovementTracking() {
        if (attackerMovementTrackingTask != null && !attackerMovementTrackingTask.isCancelled()) {
            attackerMovementTrackingTask.cancel();
        }
        if (defenderMovementTrackingTask != null && !defenderMovementTrackingTask.isCancelled()) {
            defenderMovementTrackingTask.cancel();
        }
    }
    
    /**
     * Konvertiert Movement-Points zu JSON (beide Spieler).
     * Format: {"attacker": [...], "defender": [...], "combat": [...]}
     */
    public JsonObject getMovementPointsAsJsonObject() {
        JsonObject root = new JsonObject();
        
        // Angreifer Movement-Points
        JsonArray attackerArray = new JsonArray();
        for (de.payne.clashmc.replay.MovementPoint mp : attackerMovementPoints) {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", mp.getX());
            obj.addProperty("y", mp.getY());
            obj.addProperty("z", mp.getZ());
            obj.addProperty("yaw", mp.getYaw());
            obj.addProperty("pitch", mp.getPitch());
            obj.addProperty("timestamp", mp.getTimestamp());
            attackerArray.add(obj);
        }
        root.add("attacker", attackerArray);
        
        // Verteidiger Movement-Points (nur bei Online-Angriffen)
        if (isOnline && !defenderMovementPoints.isEmpty()) {
            JsonArray defenderArray = new JsonArray();
            for (de.payne.clashmc.replay.MovementPoint mp : defenderMovementPoints) {
                JsonObject obj = new JsonObject();
                obj.addProperty("x", mp.getX());
                obj.addProperty("y", mp.getY());
                obj.addProperty("z", mp.getZ());
                obj.addProperty("yaw", mp.getYaw());
                obj.addProperty("pitch", mp.getPitch());
                obj.addProperty("timestamp", mp.getTimestamp());
                defenderArray.add(obj);
            }
            root.add("defender", defenderArray);
        }
        
        // Combat-Events (nur bei Online-Angriffen)
        if (isOnline && !combatEvents.isEmpty()) {
            root.add("combat", getCombatEventsAsJsonArray());
        }
        
        return root;
    }
    
    public String getMovementPointsAsJsonString() {
        return getMovementPointsAsJsonObject().toString();
    }
    
    public void cleanup() {
        Player attacker = Bukkit.getPlayer(attackerUuid);
        Player defender = Bukkit.getPlayer(defenderUuid);
        
        // ASYNC: Lade defender Level via Cache (schnell, non-blocking)
        ClashMC.getInstance().getDatabaseManager().players().getPlayerIdByUUIDAsync(defenderUuid)
            .thenCompose(defenderKingId -> {
                if (defenderKingId == -1) {
                    LogUtil.logError(ClashMC.getInstance(), "[AttackInstance] Cleanup: Ungültige Defender King-ID");
                    return java.util.concurrent.CompletableFuture.completedFuture(1); // Default Level
                }
                return ClashMC.getInstance().getDatabaseManager().villages().getVillageLevelAsync(defenderKingId);
            })
            .thenAccept(level -> {
                // Cleanup-Logik auf Main-Thread ausführen (Bukkit API erfordert Main-Thread)
                Bukkit.getScheduler().runTask(ClashMC.getInstance(), () -> {
                    performCleanup(attacker, defender, level);
                });
            })
            .exceptionally(throwable -> {
                LogUtil.logError(ClashMC.getInstance(), "[AttackInstance] Cleanup async error: " + throwable.getMessage());
                // Fallback: Cleanup mit Default-Level ausführen
                Bukkit.getScheduler().runTask(ClashMC.getInstance(), () -> {
                    performCleanup(attacker, defender, 1);
                });
                return null;
            });
    }
    
    private void performCleanup(Player attacker, Player defender, int level) {
        // 1. Spieler zurückteleportieren (z. B. in ihre Welt/Hauptwelt)
        if (attacker != null && attacker.isOnline()) {
            attacker.teleport(ClashMC.getInstance().getVillageAllocator().getVillageCenterTeleportOrSpawnLocation(attackerUuid)); // Ziel anpassen
            attacker.getInventory().clear();
            attacker.setGameMode(GameMode.ADVENTURE);
            attacker.setFoodLevel(20);
            attacker.setHealth(20);
            attacker.sendMessage("§aDu wurdest aus dem Angriff zurückteleportiert.");
    	    attacker.setInvulnerable(true); // Kein Schaden durch z. B. Fallschaden oder Mobs

        }

        if (isOnline && defender != null && defender.isOnline()) {
            defender.teleport(ClashMC.getInstance().getVillageAllocator().getVillageCenterTeleportOrSpawnLocation(defenderUuid)); // Ziel anpassen
            defender.getInventory().clear();
            defender.setGameMode(GameMode.ADVENTURE);
            defender.setFoodLevel(20);
            defender.setHealth(20);
            defender.sendMessage("§cDer Angriff auf dein Dorf ist beendet.");
            defender.setInvulnerable(true); // Kein Schaden durch z. B. Fallschaden oder Mobs

        }


        // 3. Optional: Chunk löschen / säubern (wenn z. B. WorldEdit-Strukturen)
        // Einfachheit halber: mit WorldEdit die Region mit Luft überschreiben
        SchematicManager schematicManager = ClashMC.getInstance().getSchematicManager();
        Clipboard clipboard = schematicManager.loadSchematic(level);

        if (clipboard == null){
            LogUtil.logError(ClashMC.getInstance(), "Cleanup methode - clipboard == null!");
            return;
        }
        
        World world = baseLocation.getWorld();
        if (world == null) {
            LogUtil.logError(ClashMC.getInstance(), "Cleanup methode - World für origin ist null");
            return;
        }

        BlockVector3 min = clipboard.getMinimumPoint();
        BlockVector3 max = clipboard.getMaximumPoint();

        LogUtil.logInfo(ClashMC.getInstance(),"[AttackInstance] Lösche Attack Blöcke von " + min + " bis " + max);

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY() - 1; y <= max.getY(); y++) { // auch unterste Schicht
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    int relX = x - min.getX();
                    int relY = y - min.getY();
                    int relZ = z - min.getZ();

                    Location loc = baseLocation.clone().add(relX, relY, relZ);
                    loc.getBlock().setType(Material.AIR);
                }
            }
        }
        
        
        
        // 4. Partikel, Scoreboards, Bossbars, etc. zurücksetzen (falls verwendet)
        // attacker.hideBossBar(...), scoreboard reset, etc.

        // Hinweis: Slot-Freigabe erfolgt im AttackManager nach cleanup()
        LogUtil.logInfo(ClashMC.getInstance(),"[AttackInstance] erfolgreich gelöscht.");
    }

    public boolean isOnline() {
        return isOnline;
    }
    
    /**
     * Wird aufgerufen wenn ein Spieler während des Angriffs stirbt.
     * Gibt true zurück wenn Respawn möglich ist, false wenn noch Cooldown.
     */
    public boolean handlePlayerDeath(UUID playerUuid) {
        // Erhöhe Tod-Zähler
        int deathCount = deathCounts.getOrDefault(playerUuid, 0) + 1;
        deathCounts.put(playerUuid, deathCount);
        
        // Berechne Cooldown: Start bei 3 Sekunden, steigt mit jedem Tod
        long cooldownSeconds = 3L + (deathCount - 1); // 3, 4, 5, 6, ...
        long cooldownMillis = cooldownSeconds * 1000L;
        long respawnTime = System.currentTimeMillis() + cooldownMillis;
        
        respawnCooldowns.put(playerUuid, respawnTime);
        
        // Prüfe ob Respawn sofort möglich ist
        if (cooldownSeconds == 3) {
            // Erster Tod: Sofort Respawn möglich
            return true;
        }
        
        // Für spätere Tode: Cooldown wird in handleRespawn geprüft
        return false;
    }
    
    /**
     * Prüft ob Respawn möglich ist und führt ihn durch.
     * Gibt true zurück wenn Respawn durchgeführt wurde, false wenn noch Cooldown.
     */
    public boolean handleRespawn(UUID playerUuid) {
        Long respawnTime = respawnCooldowns.get(playerUuid);
        if (respawnTime == null) {
            // Kein Cooldown gesetzt, Respawn sofort möglich
            return respawnPlayer(playerUuid);
        }
        
        long now = System.currentTimeMillis();
        if (now >= respawnTime) {
            // Cooldown abgelaufen, Respawn möglich
            return respawnPlayer(playerUuid);
        } else {
            // Noch Cooldown
            long remainingSeconds = (respawnTime - now) / 1000L + 1;
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                player.sendMessage("§cRespawn in §e" + remainingSeconds + " §cSekunden...");
            }
            return false;
        }
    }
    
    /**
     * Führt den Respawn durch: Teleportiert Spieler und gibt Loadout.
     */
    private boolean respawnPlayer(UUID playerUuid) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null || !player.isOnline()) {
            return false;
        }
        
        try {
            // Bestimme Spawn-Location und Loadout basierend auf Rolle
            Location spawnLoc;
            int playerId;
            
            if (playerUuid.equals(attackerUuid)) {
                spawnLoc = getAttackerSpawn();
                playerId = ClashMC.getInstance().getDatabaseManager().players().getPlayerIdByUUID(attackerUuid);
                ClashMC.getInstance().getEquipmentManager().giveAttackerLoadout(player, playerId);
            } else if (playerUuid.equals(defenderUuid) && isOnline) {
                spawnLoc = getDefenderSpawn();
                playerId = ClashMC.getInstance().getDatabaseManager().players().getPlayerIdByUUID(defenderUuid);
                ClashMC.getInstance().getEquipmentManager().giveDefenderLoadout(player, playerId);
            } else {
                // Defender ist offline, kein Respawn möglich
                return false;
            }
            
            // Teleportiere und setze Stats
            player.teleport(spawnLoc);
            player.setGameMode(GameMode.ADVENTURE);
            player.setFoodLevel(20);
            player.setHealth(20.0);
            player.setInvulnerable(false);
            
            int deathCount = deathCounts.getOrDefault(playerUuid, 0);
            player.sendMessage("§aDu wurdest wiederbelebt! §7(Tode: §c" + deathCount + "§7)");
            
            return true;
            
        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "[AttackInstance] Fehler beim Respawn: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Prüft ob ein Spieler in diesem Angriff ist.
     */
    public boolean isPlayerInAttack(UUID playerUuid) {
        return playerUuid.equals(attackerUuid) || (isOnline && playerUuid.equals(defenderUuid));
    }
    
    /**
     * Fügt ein Combat-Event hinzu.
     */
    public void addCombatEvent(boolean isAttackerHitting, Location location, double damage) {
        combatEvents.add(de.payne.clashmc.replay.CombatEvent.fromDamage(
            isAttackerHitting, location, damage, attackStartTime, baseLocation
        ));
    }
    
    /**
     * Konvertiert Combat-Events zu JSON.
     */
    public JsonArray getCombatEventsAsJsonArray() {
        JsonArray array = new JsonArray();
        for (de.payne.clashmc.replay.CombatEvent event : combatEvents) {
            JsonObject obj = new JsonObject();
            obj.addProperty("attacker_id", event.getAttackerId());
            obj.addProperty("x", event.getX());
            obj.addProperty("y", event.getY());
            obj.addProperty("z", event.getZ());
            obj.addProperty("damage", event.getDamage());
            obj.addProperty("timestamp", event.getTimestamp());
            array.add(obj);
        }
        return array;
    }
}