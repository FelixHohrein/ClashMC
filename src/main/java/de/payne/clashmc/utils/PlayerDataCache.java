package de.payne.clashmc.utils;

import java.sql.SQLException;
import java.util.UUID;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.economy.PlayerResources;
import lombok.Getter;

/**
 * Helper class to cache player data for GUI operations
 * to avoid duplicate database calls
 */
@Getter
public class PlayerDataCache {
    
    private final int kingId;
    private final int villageLevel;
    private final long clashCoins;
    private final long kingCoins;
    private final boolean valid;
    
    public PlayerDataCache(UUID playerUuid) {
        int tempKingId = -1;
        int tempVillageLevel = 0;
        long tempClashCoins = 0;
        long tempKingCoins = 0;
        boolean tempValid = false;
        
        try {
            tempKingId = ClashMC.getInstance().getDatabaseManager().players().getPlayerIdByUUID(playerUuid);
            if (tempKingId != -1) {
                tempVillageLevel = ClashMC.getInstance().getDatabaseManager().villages().getVillageLevel(tempKingId);
                PlayerResources resources = ClashMC.getInstance().getDatabaseManager().resources().getResources(playerUuid);
                if (resources != null) {
                    tempClashCoins = resources.getClashCoins();
                    tempKingCoins = resources.getKingCoins();
                    tempValid = true;
                }
            }
        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "Fehler beim Laden der Spielerdaten: " + e.getMessage());
            e.printStackTrace();
        }
        
        this.kingId = tempKingId;
        this.villageLevel = tempVillageLevel;
        this.clashCoins = tempClashCoins;
        this.kingCoins = tempKingCoins;
        this.valid = tempValid;
    }
}

