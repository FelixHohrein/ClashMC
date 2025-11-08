package de.payne.clashmc.attacks;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.utils.LogUtil;
import lombok.Getter;

public class AttackManager {

    private final ClashMC plugin;
    @Getter
    private final Map<UUID, AttackInstance> activeAttacks = new HashMap<>();
    private final AttackInstanceManager instanceManager;
    
    public AttackManager(ClashMC plugin) {
        this.plugin = plugin;
        this.instanceManager = new AttackInstanceManager();
    }

    public void startAttack(UUID attackerUuid, UUID defenderUuid, boolean isOnline) {
        Player attacker = Bukkit.getPlayer(attackerUuid);
        if (attacker == null || !attacker.isOnline()) {
            LogUtil.logError(this.plugin, "[Attacks] Angreifer ist nicht online");
            return;
        }
        
        int defenderId = -1;
        int attackerId = -1;
        int level = -1;

        try {
            attackerId = plugin.getDatabaseManager().players().getPlayerIdByUUID(attackerUuid);
            defenderId = plugin.getDatabaseManager().players().getPlayerIdByUUID(defenderUuid);
            level = plugin.getDatabaseManager().villages().getVillageLevel(defenderId);
        } catch (SQLException e) {
            LogUtil.logError(this.plugin, "[Attacks] Fehler beim Laden der Spielerdaten: " + e.getMessage());
            attacker.sendMessage("§cFehler beim Starten des Angriffs. Bitte wende dich an einen Admin.");
            e.printStackTrace();
            return;
        }

        if (attackerId == -1 || defenderId == -1 || level == -1) {
            LogUtil.logError(this.plugin, "[Attacks] Ungültige Spielerdaten: attackerId=" + attackerId + ", defenderId=" + defenderId + ", level=" + level);
            attacker.sendMessage("§cFehler: Spielerdaten konnten nicht geladen werden.");
            return;
        }
        
        Location instanceLocation = instanceManager.claimFreeSlot();
        if (instanceLocation == null) {
            attacker.sendMessage("§cEs sind aktuell keine freien Angriffsslots verfügbar.");
            return;
        }
        
        plugin.getSchematicManager().pasteSchematicWithBedrock(this.plugin, instanceLocation, level);

        AttackInstance instance = new AttackInstance(attackerUuid, defenderUuid, instanceLocation, isOnline);
        activeAttacks.put(attackerUuid, instance);

        instance.scanDestructibleBlocks(level, plugin.getSchematicManager());
        
        // Teleport and equip attacker
        attacker.teleport(instance.getAttackerSpawn());
        plugin.getEquipmentManager().giveAttackerLoadout(attacker, attackerId);
        attacker.setGameMode(GameMode.ADVENTURE);
        attacker.setFoodLevel(20);
        attacker.setHealth(20);     
        attacker.setInvulnerable(false);

        if (isOnline) {
            Player defender = Bukkit.getPlayer(defenderUuid);
            if (defender != null && defender.isOnline()) {
                defender.teleport(instance.getDefenderSpawn());
                // FIX: Defender bekommt jetzt korrektes Equipment (war vorher giveAttackerLoadout)
                plugin.getEquipmentManager().giveDefenderLoadout(defender, defenderId);
                defender.setGameMode(GameMode.ADVENTURE);
                defender.setFoodLevel(20);
                defender.setHealth(20);   
                defender.setInvulnerable(false);
            } else {
                LogUtil.logError(this.plugin, "[Attacks] Defender ist nicht mehr online, wechsle zu Offline-Modus");
                instance.spawnOfflineDefense();
            }
        } else {
            instance.spawnOfflineDefense();
        }

        // Start Timer
        instance.startBattle(() -> finishAttack(attackerUuid, defenderUuid, instance));
    }

    public void finishAttack(UUID attackerUuid, UUID defenderUuid, AttackInstance instance) {
        activeAttacks.remove(attackerUuid);

        double damagePercent = instance.calculateDamage();
        long clashCoinsLoot = instance.calculateClashCoinReward();
        long kingCoinsLoot = instance.calculateKingCoinReward();
        String replayJson = instance.getBrokenBlocksAsJsonString();
        String movementJson = instance.getMovementPointsAsJsonString();

        // ASYNC: Lade Player-IDs
        plugin.getDatabaseManager().players().getPlayerIdByUUIDAsync(attackerUuid)
            .thenCombine(
                plugin.getDatabaseManager().players().getPlayerIdByUUIDAsync(defenderUuid),
                (attackerId, defenderId) -> {
                    if (attackerId == -1 || defenderId == -1) {
                        LogUtil.logError(this.plugin, "[Attacks] Ungültige Spieler-IDs nach Angriff");
                        return -1;
                    }
                    
                    // ASYNC: Erstelle Attack-Eintrag
                    return plugin.getDatabaseManager().attacks().createAttack(
                        attackerId, defenderId, instance.isOnline(), 
                        damagePercent, clashCoinsLoot, kingCoinsLoot
                    );
                }
            )
            .thenAccept(attackId -> {
                if (attackId == -1) return;
                
                // ASYNC: Speichere Replay (Blocks + Movement)
                plugin.getDatabaseManager().attacks().saveReplayAsync(attackId, replayJson);
                plugin.getDatabaseManager().attacks().saveMovementDataAsync(attackId, movementJson);
            })
            .exceptionally(throwable -> {
                LogUtil.logError(this.plugin, "[Attacks] Async Fehler: " + throwable.getMessage());
                throwable.printStackTrace();
                return null;
            });

        // ASYNC: Update Resources
        plugin.getDatabaseManager().players().getPlayerIdByUUIDAsync(attackerUuid)
            .thenCombine(
                plugin.getDatabaseManager().players().getPlayerIdByUUIDAsync(defenderUuid),
                (attackerId, defenderId) -> {
                    if (attackerId != -1 && defenderId != -1) {
                        // Send messages on main thread
                        Player attacker = Bukkit.getPlayer(attackerUuid);
                        if (attacker != null && attacker.isOnline()) {
                            Player defender = Bukkit.getPlayer(defenderUuid);
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                if (defender != null && defender.isOnline()) {
                                    attacker.sendMessage("§aDu hast Spieler " + defender.getName() + " §e" + clashCoinsLoot + " Clash-Coins §agestohlen!");
                                    defender.sendMessage("§cDir wurden von Spieler " + attacker.getName() + " §e" + clashCoinsLoot + " Clash-Coins §cgestohlen!");
                                } else {
                                    attacker.sendMessage("§aDu hast Spieler " + Bukkit.getOfflinePlayer(defenderUuid).getName() + " §e" + clashCoinsLoot + " Clash-Coins §agestohlen!");
                                }
                            });
                        }
                        
                        // Async coin updates
                        plugin.getDatabaseManager().resources().addClashCoinsAsync(attackerId, clashCoinsLoot);
                        plugin.getDatabaseManager().resources().removeClashCoinsAsync(defenderId, clashCoinsLoot / 2);
                    }
                    return null;
                }
            )
            .exceptionally(throwable -> {
                LogUtil.logError(this.plugin, "[Attacks] Fehler bei Ressourcen-Update: " + throwable.getMessage());
                return null;
            });

        // Cleanup läuft synchron (Bukkit-API erfordert main thread)
        instance.cleanup();
        instanceManager.releaseSlot(instance.getBaseLocation());
    }

    public boolean isInAttack(UUID uuid) {
        // Prüfe ob Spieler Angreifer ist
        if (activeAttacks.containsKey(uuid)) {
            return true;
        }
        
        // Prüfe ob Spieler Defender in einer aktiven AttackInstance ist
        for (AttackInstance instance : activeAttacks.values()) {
            if (instance.isPlayerInAttack(uuid)) {
                return true;
            }
        }
        
        return false;
    }
}
