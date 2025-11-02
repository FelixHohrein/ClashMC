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
    	
        int defenderId = -1;
        int attackerId = -1;

		try {
			attackerId = plugin.getDatabaseManager().players().getPlayerIdByUUID(attackerUuid);
			defenderId = plugin.getDatabaseManager().players().getPlayerIdByUUID(defenderUuid);
		} catch (SQLException e) {
			LogUtil.logError(this.plugin, "[Attacks] attacker und Defender nicht in der Datenbank gefunden (KINGid)");
			e.printStackTrace();
		}

        if (attackerId == -1 || defenderId == -1) {
			LogUtil.logError(this.plugin, "[Attacks] attacker und Defender KingId = -1");
            return;
        }

        // Load schematic of defender village
        int level = -1;
		try {
			level = plugin.getDatabaseManager().villages().getVillageLevel(defenderId);
		} catch (SQLException e) {
			LogUtil.logError(this.plugin, "[Attacks] Dorflevel von KINGid " + defenderId + " konnte nicht gefunden werden.");
			e.printStackTrace();
		}
        
		if(level == -1) {
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

        instance.scanDestructibleBlocks(level, ClashMC.getInstance().getSchematicManager());
        // Teleport both players
        Bukkit.getPlayer(attackerUuid).teleport(instance.getAttackerSpawn());
        ClashMC.getInstance().getEquipmentManager().giveAttackerLoadout(attacker, attackerId);
        attacker.setGameMode(GameMode.ADVENTURE);
        attacker.setFoodLevel(20);
        attacker.setHealth(20);     
	    attacker.setInvulnerable(false); // Kein Schaden durch z. B. Fallschaden oder Mobs

        if (isOnline) {
        	Player defender = Bukkit.getPlayer(defenderUuid);
        	defender.teleport(instance.getDefenderSpawn());
            ClashMC.getInstance().getEquipmentManager().giveAttackerLoadout(defender, defenderId);
            defender.setGameMode(GameMode.ADVENTURE);
            defender.setFoodLevel(20);
            defender.setHealth(20);   
    	    defender.setInvulnerable(false); // Kein Schaden durch z. B. Fallschaden oder Mobs

        } else {
            // Spawn Defender-Golems, Traps etc.
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
			LogUtil.logError(this.plugin, "[Attacks] attacker und Defender nicht in der Datenbank gefunden (KINGid)");
			e.printStackTrace();
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
        	if(Bukkit.getPlayer(attackerUuid).isOnline()) {
        		Player attacker = Bukkit.getPlayer(attackerUuid);
        		if(Bukkit.getOfflinePlayer(defenderUuid).isOnline()) {
        			Player defender = Bukkit.getPlayer(defenderUuid);
        			attacker.sendMessage("Du hast Spieler " + defender.getName() + " " + clashCoinsLoot + " Clash-Coins gestohlen!");
        			defender.sendMessage("Dir wurden von Spieler " + attacker.getName() + " " + clashCoinsLoot + " Clash-Coins gestohlen!");
        		}
    			attacker.sendMessage("Du hast Spieler " + Bukkit.getOfflinePlayer(defenderUuid).getName() + " " + clashCoinsLoot + " Clash-Coins gestohlen!");
        	}
			plugin.getDatabaseManager().resources().addClashCoins(attackerId, clashCoinsLoot);
	        plugin.getDatabaseManager().resources().removeClashCoins(defenderId, clashCoinsLoot / 2); // Beispiel
		} catch (SQLException e) {
			LogUtil.logError(this.plugin, "[Attacks] Es konnten keine Coins hinzugefügt/ abgezogen werden.");
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
