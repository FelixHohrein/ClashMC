package de.payne.clashmc.cache;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.economy.PlayerResources;
import de.payne.clashmc.utils.LogUtil;

/**
 * Zentraler Cache-Manager für ClashMC
 * Cached häufig geladene Daten um Datenbank-Anfragen zu reduzieren
 */
public class CacheManager {

    private final ClashMC plugin;
    
    // Cache Maps mit TTL (Time-To-Live)
    private final Map<UUID, CachedEntry<Integer>> kingIdCache = new ConcurrentHashMap<>();
    private final Map<Integer, CachedEntry<Integer>> villageLevelCache = new ConcurrentHashMap<>();
    private final Map<UUID, CachedEntry<PlayerResources>> resourcesCache = new ConcurrentHashMap<>();
    
    // Cache TTL Settings (in Millisekunden)
    private static final long KING_ID_TTL = TimeUnit.MINUTES.toMillis(30); // 30 Minuten
    private static final long VILLAGE_LEVEL_TTL = TimeUnit.MINUTES.toMillis(5); // 5 Minuten
    private static final long RESOURCES_TTL = TimeUnit.SECONDS.toMillis(30); // 30 Sekunden
    
    private final CacheStatistics statistics = new CacheStatistics();
    
    public CacheManager(ClashMC plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }
    
    /**
     * Holt die King-ID eines Spielers (cached)
     */
    public Integer getKingId(UUID playerUuid) {
        CachedEntry<Integer> cached = kingIdCache.get(playerUuid);
        
        if (cached != null && !cached.isExpired()) {
            statistics.incrementHits("king_id");
            return cached.getValue();
        }
        
        statistics.incrementMisses("king_id");
        
        try {
            int kingId = plugin.getDatabaseManager().players().getPlayerIdByUUID(playerUuid);
            kingIdCache.put(playerUuid, new CachedEntry<>(kingId, KING_ID_TTL));
            return kingId;
        } catch (SQLException e) {
            LogUtil.logError(plugin, "Fehler beim Laden der King-ID aus DB: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Holt das Village-Level eines Spielers (cached)
     */
    public Integer getVillageLevel(int kingId) {
        CachedEntry<Integer> cached = villageLevelCache.get(kingId);
        
        if (cached != null && !cached.isExpired()) {
            statistics.incrementHits("village_level");
            return cached.getValue();
        }
        
        statistics.incrementMisses("village_level");
        
        try {
            int level = plugin.getDatabaseManager().villages().getVillageLevel(kingId);
            villageLevelCache.put(kingId, new CachedEntry<>(level, VILLAGE_LEVEL_TTL));
            return level;
        } catch (SQLException e) {
            LogUtil.logError(plugin, "Fehler beim Laden des Village-Levels aus DB: " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * Holt die Ressourcen eines Spielers (cached)
     */
    public PlayerResources getResources(UUID playerUuid) {
        CachedEntry<PlayerResources> cached = resourcesCache.get(playerUuid);
        
        if (cached != null && !cached.isExpired()) {
            statistics.incrementHits("resources");
            return cached.getValue();
        }
        
        statistics.incrementMisses("resources");
        
        try {
            PlayerResources resources = plugin.getDatabaseManager().resources().getResources(playerUuid);
            if (resources != null) {
                resourcesCache.put(playerUuid, new CachedEntry<>(resources, RESOURCES_TTL));
            }
            return resources;
        } catch (SQLException e) {
            LogUtil.logError(plugin, "Fehler beim Laden der Ressourcen aus DB: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Invalidiert den Cache für einen Spieler (nach Updates)
     */
    public void invalidatePlayer(UUID playerUuid) {
        kingIdCache.remove(playerUuid);
        resourcesCache.remove(playerUuid);
        
        // Auch Village Level invalidieren wenn King-ID bekannt
        Integer kingId = kingIdCache.get(playerUuid) != null ? kingIdCache.get(playerUuid).getValue() : null;
        if (kingId != null) {
            villageLevelCache.remove(kingId);
        }
        
        LogUtil.logDebug(plugin, "[Cache] Invalidated cache for player " + playerUuid);
    }
    
    /**
     * Invalidiert nur das Village-Level (nach Upgrade)
     */
    public void invalidateVillageLevel(int kingId) {
        villageLevelCache.remove(kingId);
        LogUtil.logDebug(plugin, "[Cache] Invalidated village level for king_id " + kingId);
    }
    
    /**
     * Invalidiert nur die Ressourcen (nach Transaktionen)
     */
    public void invalidateResources(UUID playerUuid) {
        resourcesCache.remove(playerUuid);
        LogUtil.logDebug(plugin, "[Cache] Invalidated resources for player " + playerUuid);
    }
    
    /**
     * Löscht alle Caches
     */
    public void clearAll() {
        kingIdCache.clear();
        villageLevelCache.clear();
        resourcesCache.clear();
        statistics.reset();
        LogUtil.logInfo(plugin, "[Cache] All caches cleared");
    }
    
    /**
     * Startet einen Task der abgelaufene Cache-Einträge entfernt
     */
    private void startCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            cleanupExpiredEntries();
        }, 20L * 60, 20L * 60); // Alle 60 Sekunden
    }
    
    /**
     * Entfernt abgelaufene Cache-Einträge
     */
    private void cleanupExpiredEntries() {
        int removed = 0;
        
        int sizeBefore = kingIdCache.size() + villageLevelCache.size() + resourcesCache.size();
        
        kingIdCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        villageLevelCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        resourcesCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        int sizeAfter = kingIdCache.size() + villageLevelCache.size() + resourcesCache.size();
        removed = sizeBefore - sizeAfter;
        
        if (removed > 0) {
            LogUtil.logDebug(plugin, "[Cache] Cleanup: " + removed + " expired entries removed");
        }
    }
    
    /**
     * Gibt Cache-Statistiken zurück
     */
    public String getStatistics() {
        return String.format("Cache Stats:\n" +
                "  King-ID Cache: %d entries\n" +
                "  Village Level Cache: %d entries\n" +
                "  Resources Cache: %d entries\n" +
                "  Total Hits: %d\n" +
                "  Total Misses: %d\n" +
                "  Hit Rate: %.2f%%",
                kingIdCache.size(),
                villageLevelCache.size(),
                resourcesCache.size(),
                statistics.getTotalHits(),
                statistics.getTotalMisses(),
                statistics.getHitRate() * 100
        );
    }
    
    /**
     * Cache-Eintrag mit TTL
     */
    private static class CachedEntry<T> {
        private final T value;
        private final long expiryTime;
        
        public CachedEntry(T value, long ttl) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + ttl;
        }
        
        public T getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
    
    /**
     * Cache-Statistiken
     */
    public static class CacheStatistics {
        private final Map<String, Long> hits = new ConcurrentHashMap<>();
        private final Map<String, Long> misses = new ConcurrentHashMap<>();
        
        public void incrementHits(String key) {
            hits.merge(key, 1L, Long::sum);
        }
        
        public void incrementMisses(String key) {
            misses.merge(key, 1L, Long::sum);
        }
        
        public long getTotalHits() {
            return hits.values().stream().mapToLong(Long::longValue).sum();
        }
        
        public long getTotalMisses() {
            return misses.values().stream().mapToLong(Long::longValue).sum();
        }
        
        public double getHitRate() {
            long total = getTotalHits() + getTotalMisses();
            return total == 0 ? 0 : (double) getTotalHits() / total;
        }
        
        public void reset() {
            hits.clear();
            misses.clear();
        }
    }
}

