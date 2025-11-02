package de.payne.clashmc.economy;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.entity.Player;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.database.DatabaseManager;
import de.payne.clashmc.utils.LogUtil;

public class ResourceManager {

	private final Player player;
	private final UUID uuid;
	private  PlayerResources resources;
	private final DatabaseManager database;
	private int kingId;
	private int villageLevel;
	
	public ResourceManager(Player player) {
		this.player = player;
		this.uuid = player.getUniqueId();
		this.database = ClashMC.getInstance().getDatabaseManager();
		try {
			this.resources = ClashMC.getInstance().getDatabaseManager().resources().getResources(uuid);
			this.kingId = this.database.players().getPlayerIdByUUID(uuid);
			this.villageLevel = this.database.villages().getVillageLevel(kingId);
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}
	}
	
	//USED FOR COLLECTOR ITEM
	public void collectResources() {

		try {
			kingId = ClashMC.getInstance().getDatabaseManager().players().getPlayerIdByUUID(uuid);
		} catch (SQLException e) {
			LogUtil.logError(ClashMC.getInstance(), "Fehler beim laden der KingID");
			e.printStackTrace();
		}

		if (this.resources == null) {
			player.sendMessage("§cEs gibt keine Sammlerdaten für dich. Wende dich an das Team");
			return;
		}

		long now = System.currentTimeMillis();
		long lastCollected = resources.getLastCollectorUse();

		long elapsedMillis = now - lastCollected;
		long maxElapsed = TimeUnit.HOURS.toMillis(12);
		long effectiveMillis = Math.min(elapsedMillis, maxElapsed);

		if (effectiveMillis < TimeUnit.MINUTES.toMillis(1)) {
			player.sendMessage("§7Noch nicht genug Ressourcen gesammelt.");
			return;
		}

		double efficiency = 0.05 + 0.01 * villageLevel; 														// 5 % + 1% pro level
		int collectedCoins = (int) ((effectiveMillis / 1000.0) * efficiency);

		if (collectedCoins <= 0) {
			player.sendMessage("§7Noch keine Clash Coins gesammelt.");
			return;
		}


		long newBalance = resources.getClashCoins() + collectedCoins;
		try {
			this.database.resources().updateClashCoins(kingId, newBalance);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			this.database.resources().updateCollectorTimestamp(kingId, now);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		player.sendMessage("§a+" + collectedCoins + " Clash Coins gesammelt!");
	}
	
	//REMOVE CLASH COINS
	public boolean removeClashCoins(long amount) {
		long newBalance = this.resources.getClashCoins() - amount;
		
		if(newBalance < 0) {
			player.sendMessage("§7Du hast nicht genügend Clash-Coins.");
			LogUtil.logDebug(ClashMC.getInstance(), "Der spieler hat nicht genügend ClashCoins.");
			return false;
		}
		try {
			this.database.resources().updateClashCoins(kingId, newBalance);
			player.sendMessage("§7Dir wurden " + amount + "Clash-Coins abgezogen. Dein Neuer Kontostand: " + newBalance + " Clash-Coins.");
			return true;
		} catch (SQLException e) {
			LogUtil.logError(ClashMC.getInstance(), "Fehler beim aktualisieren der ClashCoins");
			e.printStackTrace();
			return false;
		}
	}
	
	//REMOVE KING COINS
	public boolean removeKingCoins(long amount) {
		
		long newBalance = this.resources.getKingCoins() - amount;
		
		if(newBalance < 0) {
			player.sendMessage("§7Du hast nicht genügend Clash-Coins.");
			LogUtil.logDebug(ClashMC.getInstance(), "Der spieler hat nicht genügend ClashCoins.");
			return false;
		}
		
		try {
			this.database.resources().updateKingCoins(kingId, newBalance);
			player.sendMessage("§7Dir wurden " + amount + "King-Coins abgezogen. Dein Neuer Kontostand: " + newBalance + " King-Coins.");
			return true;
		} catch (SQLException e) {
			LogUtil.logError(ClashMC.getInstance(), "Fehler beim aktualisieren der KingCoins");
			e.printStackTrace();
			return false;
		}
	}
	
	//REMOVE CLASH COINS
	public boolean addClashCoins(long amount) {
		long newBalance = this.resources.getClashCoins() + amount;
		
		try {
			this.database.resources().updateClashCoins(kingId, newBalance);
			player.sendMessage("§7Dir wurden " + amount + "Clash-Coins hinzugefügt. Dein Neuer Kontostand: " + newBalance + " Clash-Coins.");
			return true;
		} catch (SQLException e) {
			LogUtil.logError(ClashMC.getInstance(), "Fehler beim aktualisieren der ClashCoins");
			e.printStackTrace();
			return false;
		}
	}
	
	//REMOVE KING COINS
	public boolean addKingCoins(long amount) {
		
		long newBalance = this.resources.getKingCoins() + amount;
				
		try {
			this.database.resources().updateKingCoins(kingId, newBalance);
			player.sendMessage("§7Dir wurden " + amount + "King-Coins hinzugefügt. Dein Neuer Kontostand: " + newBalance + " King-Coins.");
			return true;
		} catch (SQLException e) {
			LogUtil.logError(ClashMC.getInstance(), "Fehler beim aktualisieren der KingCoins");
			e.printStackTrace();
			return false;
		}
	}
	
	public long getUpgradeCosts(int level) {
	    // Parameter der Kostenformel
	    long baseCost = 1000L;
	    long linearFactor = 500L;
	    double progressiveFactor = 30.0;

	    // Kosten berechnen: base + linear*L + progressive*L^(1.5)
	    double cost = baseCost + linearFactor * level + progressiveFactor * Math.pow(level, 1.5);

	    return (long) cost;
	}
	
	public long calculateUpgradeCost(Player player) {
	    int nextLevel = this.villageLevel + 1;
	    return getUpgradeCosts(nextLevel);
	}
	
}
