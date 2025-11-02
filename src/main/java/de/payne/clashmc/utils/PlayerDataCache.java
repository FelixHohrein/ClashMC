package de.payne.clashmc.utils;

import java.util.UUID;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.cache.CacheManager;
import de.payne.clashmc.economy.PlayerResources;
import lombok.Getter;

/**
 * Helper class to cache player data for GUI operations
 * Uses the CacheManager for efficient data loading
 */
@Getter
public class PlayerDataCache {
    
    private final int kingId;
    private final int villageLevel;
    private final long clashCoins;
    private final long kingCoins;
    private final boolean valid;
    
    public PlayerDataCache(UUID playerUuid) {
        CacheManager cache = ClashMC.getInstance().getCacheManager();
        
        int tempKingId = cache.getKingId(playerUuid);
        int tempVillageLevel = 0;
        long tempClashCoins = 0;
        long tempKingCoins = 0;
        boolean tempValid = false;
        
        if (tempKingId != -1) {
            tempVillageLevel = cache.getVillageLevel(tempKingId);
            PlayerResources resources = cache.getResources(playerUuid);
            if (resources != null) {
                tempClashCoins = resources.getClashCoins();
                tempKingCoins = resources.getKingCoins();
                tempValid = true;
            }
        }
        
        this.kingId = tempKingId;
        this.villageLevel = tempVillageLevel;
        this.clashCoins = tempClashCoins;
        this.kingCoins = tempKingCoins;
        this.valid = tempValid;
    }
}
