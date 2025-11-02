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

        int defenderId = -1;
        int attackerId = -1;

        try {
            attackerId = plugin.getDatabaseManager().players().getPlayerIdByUUID(attackerUuid);
            defenderId = plugin.getDatabaseManager().players().getPlayerIdByUUID(defenderUuid);
        } catch (SQLException e) {
            LogUtil.logError(this.plugin, "[Attacks] Fehler beim Laden der Spieler-IDs: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        if (attackerId == -1 || defenderId == -1) {
            LogUtil.logError(this.plugin, "[Attacks] Ungültige Spieler-IDs nach Angriff");
            return;
        }
        
        int attackId = plugin.getDatabaseManager().attacks().createAttack(
                attackerId,
                defenderId,
                instance.isOnline(),
                damagePercent,
                clashCoinsLoot,
                kingCoinsLoot
        );

        // Update resources
        try {
            Player attacker = Bukkit.getPlayer(attackerUuid);
            if (attacker != null && attacker.isOnline()) {
                Player defender = Bukkit.getPlayer(defenderUuid);
                if (defender != null && defender.isOnline()) {
                    attacker.sendMessage("§aDu hast Spieler " + defender.getName() + " §e" + clashCoinsLoot + " Clash-Coins §agestohlen!");
                    defender.sendMessage("§cDir wurden von Spieler " + attacker.getName() + " §e" + clashCoinsLoot + " Clash-Coins §cgestohlen!");
                } else {
                    attacker.sendMessage("§aDu hast Spieler " + Bukkit.getOfflinePlayer(defenderUuid).getName() + " §e" + clashCoinsLoot + " Clash-Coins §agestohlen!");
                }
            }
            plugin.getDatabaseManager().resources().addClashCoins(attackerId, clashCoinsLoot);
            plugin.getDatabaseManager().resources().removeClashCoins(defenderId, clashCoinsLoot / 2);
        } catch (SQLException e) {
            LogUtil.logError(this.plugin, "[Attacks] Es konnten keine Coins hinzugefügt/abgezogen werden: " + e.getMessage());
            e.printStackTrace();
        }
        
        String replayJson = instance.getBrokenBlocksAsJsonString();  
        plugin.getDatabaseManager().attacks().saveReplay(attackId, replayJson);

        // Teleportation zurück, Messages, GUI öffnen etc.
        instance.cleanup();
        instanceManager.releaseSlot(instance.getBaseLocation());
    }

    public boolean isInAttack(UUID uuid) {
        return activeAttacks.containsKey(uuid);
    }
}
