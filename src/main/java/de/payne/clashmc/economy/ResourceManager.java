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
    private PlayerResources resources;
    private final DatabaseManager database;
    private final int kingId;
    private final int villageLevel;
    
    public ResourceManager(Player player) {
        this.player = player;
        this.uuid = player.getUniqueId();
        this.database = ClashMC.getInstance().getDatabaseManager();
        
        int tempKingId = -1;
        int tempVillageLevel = 0;
        PlayerResources tempResources = null;
        
        try {
            tempKingId = this.database.players().getPlayerIdByUUID(uuid);
            tempResources = this.database.resources().getResources(uuid);
            tempVillageLevel = this.database.villages().getVillageLevel(tempKingId);
        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "Fehler beim Laden der Spielerdaten: " + e.getMessage());
            e.printStackTrace();
        }
        
        this.kingId = tempKingId;
        this.resources = tempResources;
        this.villageLevel = tempVillageLevel;
    }
    
    // USED FOR COLLECTOR ITEM
    public void collectResources() {
        if (this.resources == null || this.kingId == -1) {
            player.sendMessage("§cEs gibt keine Sammlerdaten für dich. Wende dich an das Team");
            LogUtil.logError(ClashMC.getInstance(), "Keine Ressourcen-Daten für Spieler " + player.getName());
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

        double efficiency = 0.05 + 0.01 * villageLevel; // 5% + 1% pro Level
        int collectedCoins = (int) ((effectiveMillis / 1000.0) * efficiency);

        if (collectedCoins <= 0) {
            player.sendMessage("§7Noch keine Clash Coins gesammelt.");
            return;
        }

        long newBalance = resources.getClashCoins() + collectedCoins;
        try {
            this.database.resources().updateClashCoins(kingId, newBalance);
            this.database.resources().updateCollectorTimestamp(kingId, now);
            // Invalidate cache after update
            ClashMC.getInstance().getCacheManager().invalidateResources(uuid);
            player.sendMessage("§a+" + collectedCoins + " Clash Coins gesammelt!");
        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "Fehler beim Aktualisieren der Clash Coins: " + e.getMessage());
            player.sendMessage("§cFehler beim Sammeln der Ressourcen.");
            e.printStackTrace();
        }
    }
    
    // REMOVE CLASH COINS
    public boolean removeClashCoins(long amount) {
        if (this.resources == null || this.kingId == -1) {
            player.sendMessage("§cFehler beim Laden deiner Ressourcen.");
            return false;
        }
        
        long newBalance = this.resources.getClashCoins() - amount;
        
        if (newBalance < 0) {
            player.sendMessage("§7Du hast nicht genügend Clash-Coins.");
            LogUtil.logDebug(ClashMC.getInstance(), "Spieler " + player.getName() + " hat nicht genügend Clash-Coins");
            return false;
        }
        
        try {
            this.database.resources().updateClashCoins(kingId, newBalance);
            // Invalidate cache after update
            ClashMC.getInstance().getCacheManager().invalidateResources(uuid);
            player.sendMessage("§7Dir wurden §e" + amount + " Clash-Coins §7abgezogen. Neuer Kontostand: §e" + newBalance + " Clash-Coins§7.");
            return true;
        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "Fehler beim Aktualisieren der Clash-Coins: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // REMOVE KING COINS
    public boolean removeKingCoins(long amount) {
        if (this.resources == null || this.kingId == -1) {
            player.sendMessage("§cFehler beim Laden deiner Ressourcen.");
            return false;
        }
        
        long newBalance = this.resources.getKingCoins() - amount;
        
        if (newBalance < 0) {
            player.sendMessage("§7Du hast nicht genügend King-Coins.");
            LogUtil.logDebug(ClashMC.getInstance(), "Spieler " + player.getName() + " hat nicht genügend King-Coins");
            return false;
        }
        
        try {
            this.database.resources().updateKingCoins(kingId, newBalance);
            // Invalidate cache after update
            ClashMC.getInstance().getCacheManager().invalidateResources(uuid);
            player.sendMessage("§7Dir wurden §e" + amount + " King-Coins §7abgezogen. Neuer Kontostand: §e" + newBalance + " King-Coins§7.");
            return true;
        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "Fehler beim Aktualisieren der King-Coins: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // ADD CLASH COINS
    public boolean addClashCoins(long amount) {
        if (this.resources == null || this.kingId == -1) {
            player.sendMessage("§cFehler beim Laden deiner Ressourcen.");
            return false;
        }
        
        long newBalance = this.resources.getClashCoins() + amount;
        
        try {
            this.database.resources().updateClashCoins(kingId, newBalance);
            // Invalidate cache after update
            ClashMC.getInstance().getCacheManager().invalidateResources(uuid);
            player.sendMessage("§7Dir wurden §e" + amount + " Clash-Coins §7hinzugefügt. Neuer Kontostand: §e" + newBalance + " Clash-Coins§7.");
            return true;
        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "Fehler beim Aktualisieren der Clash-Coins: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // ADD KING COINS
    public boolean addKingCoins(long amount) {
        if (this.resources == null || this.kingId == -1) {
            player.sendMessage("§cFehler beim Laden deiner Ressourcen.");
            return false;
        }
        
        long newBalance = this.resources.getKingCoins() + amount;
                
        try {
            this.database.resources().updateKingCoins(kingId, newBalance);
            // Invalidate cache after update
            ClashMC.getInstance().getCacheManager().invalidateResources(uuid);
            player.sendMessage("§7Dir wurden §e" + amount + " King-Coins §7hinzugefügt. Neuer Kontostand: §e" + newBalance + " King-Coins§7.");
            return true;
        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "Fehler beim Aktualisieren der King-Coins: " + e.getMessage());
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
