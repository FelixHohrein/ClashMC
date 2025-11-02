package de.payne.clashmc.config;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.utils.LogUtil;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Zentraler Manager für alle Config-Werte.
 * Lädt Werte aus config.yml und bietet typsichere Getter-Methoden.
 */
public class ConfigManager {

    private final ClashMC plugin;
    private FileConfiguration config;

    public ConfigManager(ClashMC plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Lädt/Reloaded die Config.
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        LogUtil.logInfo(plugin, "Konfiguration geladen!");
    }

    /**
     * Reloaded die Config und gibt Feedback über geänderte Werte.
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        LogUtil.logInfo(plugin, "Konfiguration neu geladen!");
    }

    // ====================================
    // DORF-SYSTEM (Village)
    // ====================================

    public int getVillageGridChunksPerVillage() {
        return config.getInt("village.grid.chunks-per-village", 5);
    }

    public int getVillageGridGapBetweenVillages() {
        return config.getInt("village.grid.gap-between-villages", 1);
    }

    public int getVillageUpgradeCostBase() {
        return config.getInt("village.upgrade-costs.base", 1000);
    }

    public int getVillageUpgradeCostLevelMultiplier() {
        return config.getInt("village.upgrade-costs.level-multiplier", 500);
    }

    public int getVillageUpgradeCostExponentMultiplier() {
        return config.getInt("village.upgrade-costs.exponent-multiplier", 30);
    }

    public double getVillageUpgradeCostExponent() {
        return config.getDouble("village.upgrade-costs.exponent", 1.5);
    }

    /**
     * Berechnet die Upgrade-Kosten für ein bestimmtes Level.
     * Formel: base + (level_multiplier * level) + (exponent_multiplier * level^exponent)
     */
    public int calculateVillageUpgradeCost(int level) {
        int base = getVillageUpgradeCostBase();
        int levelMult = getVillageUpgradeCostLevelMultiplier();
        int expMult = getVillageUpgradeCostExponentMultiplier();
        double exponent = getVillageUpgradeCostExponent();
        
        return base + (levelMult * level) + (int) (expMult * Math.pow(level, exponent));
    }

    public int getVillageMaxLevel() {
        return config.getInt("village.max-level", 100);
    }

    public int getVillageSpawnHeight() {
        return config.getInt("village.spawn-height", 66);
    }

    // ====================================
    // WIRTSCHAFTS-SYSTEM (Economy)
    // ====================================

    public double getCollectorBaseEfficiency() {
        return config.getDouble("economy.collector.base-efficiency", 0.05);
    }

    public double getCollectorLevelBonus() {
        return config.getDouble("economy.collector.level-bonus", 0.01);
    }

    public int getCollectorMaxOfflineHours() {
        return config.getInt("economy.collector.max-offline-hours", 12);
    }

    public int getCollectorMinCollectionInterval() {
        return config.getInt("economy.collector.min-collection-interval", 60);
    }

    /**
     * Berechnet die Collector-Effizienz basierend auf Dorflevel.
     * Formel: base_efficiency + (level_bonus * villageLevel)
     */
    public double calculateCollectorEfficiency(int villageLevel) {
        return getCollectorBaseEfficiency() + (getCollectorLevelBonus() * villageLevel);
    }

    public long getStartingClashCoins() {
        return config.getLong("economy.starting-coins.clash-coins", 0);
    }

    public long getStartingKingCoins() {
        return config.getLong("economy.starting-coins.king-coins", 0);
    }

    // ====================================
    // ANGRIFFS-SYSTEM (Attacks)
    // ====================================

    public int getAttackDurationSeconds() {
        return config.getInt("attacks.duration-seconds", 180);
    }

    public int getAttackLevelRange() {
        return config.getInt("attacks.level-range", 20);
    }

    public int getAttackClashCoinsMultiplier() {
        return config.getInt("attacks.rewards.clash-coins-multiplier", 10);
    }

    public double getAttackKingCoinsThreshold() {
        return config.getDouble("attacks.rewards.king-coins-threshold", 90.0);
    }

    public int getAttackKingCoinsReward() {
        return config.getInt("attacks.rewards.king-coins-reward", 1);
    }

    /**
     * Berechnet Clash Coins Belohnung basierend auf Schadensprozent.
     * Formel: damage_percent * multiplier
     */
    public long calculateAttackClashCoinsReward(double damagePercent) {
        return Math.round(damagePercent * getAttackClashCoinsMultiplier());
    }

    /**
     * Prüft ob King Coins verdient wurden.
     */
    public int calculateAttackKingCoinsReward(double damagePercent) {
        return damagePercent >= getAttackKingCoinsThreshold() ? getAttackKingCoinsReward() : 0;
    }

    public int getAttackInstanceSlotsX() {
        return config.getInt("attacks.instance.slots-x", 10);
    }

    public int getAttackInstanceSlotsZ() {
        return config.getInt("attacks.instance.slots-z", 10);
    }

    public int getAttackInstanceSpacing() {
        return config.getInt("attacks.instance.spacing", 128);
    }

    public boolean isAttackEquipmentScalingEnabled() {
        return config.getBoolean("attacks.equipment.enable-scaling", true);
    }

    public boolean isReplaySavingEnabled() {
        return config.getBoolean("attacks.replay.save-replays", true);
    }

    // ====================================
    // MINE-SYSTEM
    // ====================================

    public int getMineSessionDurationMinutes() {
        return config.getInt("mine.session-duration-minutes", 10);
    }

    public int getMineSessionCooldownMinutes() {
        return config.getInt("mine.session-cooldown-minutes", 10);
    }

    public int getMineInstanceSlotsX() {
        return config.getInt("mine.instance.slots-x", 10);
    }

    public int getMineInstanceSlotsZ() {
        return config.getInt("mine.instance.slots-z", 10);
    }

    public int getMineInstanceSpacing() {
        return config.getInt("mine.instance.spacing", 40);
    }

    public int getMineInstanceSpawnHeight() {
        return config.getInt("mine.instance.spawn-height", 50);
    }

    public double getMineOreMaxReplacePercentage() {
        return config.getDouble("mine.ore-distribution.max-replace-percentage", 0.50);
    }

    public double getMineOreChance(String oreType) {
        return config.getDouble("mine.ore-distribution.chances." + oreType.toLowerCase(), 0.0);
    }

    public int getMinePickaxeUpgradeCostBase() {
        return config.getInt("mine.pickaxe.upgrade-cost-base", 75);
    }

    public double getMinePickaxeUpgradeCostGrowth() {
        return config.getDouble("mine.pickaxe.upgrade-cost-growth", 1.8);
    }

    /**
     * Berechnet Spitzhacken-Upgrade-Kosten.
     * Formel: base * (growth^level)
     */
    public int calculatePickaxeUpgradeCost(int level) {
        int base = getMinePickaxeUpgradeCostBase();
        double growth = getMinePickaxeUpgradeCostGrowth();
        return (int) (base * Math.pow(growth, level));
    }

    public double getMinePickaxeEfficiencyRatio() {
        return config.getDouble("mine.pickaxe.efficiency-ratio", 0.5);
    }

    public int getMineBoosterResourceMultiplierCost() {
        return config.getInt("mine.boosters.resource-multiplier.cost", 1000);
    }

    public long getMineBoosterResourceMultiplierDuration() {
        return config.getLong("mine.boosters.resource-multiplier.duration-seconds", 1800);
    }

    public double getMineBoosterResourceMultiplier(String material) {
        return config.getDouble("mine.boosters.resource-multiplier.multipliers." + material.toLowerCase(), 1.0);
    }

    public int getMineBoosterPickaxeLevelPlusCost() {
        return config.getInt("mine.boosters.pickaxe-level-plus.cost", 1000);
    }

    public long getMineBoosterPickaxeLevelPlusDuration() {
        return config.getLong("mine.boosters.pickaxe-level-plus.duration-seconds", 1800);
    }

    public int getMineBoosterPickaxeLevelBonus() {
        return config.getInt("mine.boosters.pickaxe-level-plus.level-bonus", 20);
    }

    public int getMineBoosterNoCooldownCost() {
        return config.getInt("mine.boosters.no-cooldown.cost", 200);
    }

    public long getMineBoosterNoCooldownDuration() {
        return config.getLong("mine.boosters.no-cooldown.duration-seconds", 3600);
    }

    // ====================================
    // WELTEN-SYSTEM (Worlds)
    // ====================================

    public String getWorldVillage() {
        return config.getString("worlds.village-world", "Clash");
    }

    public String getWorldAttack() {
        return config.getString("worlds.attack-world", "Attacks");
    }

    public String getWorldMine() {
        return config.getString("worlds.mine-world", "mine");
    }

    public boolean getWorldSettingDisableMobs() {
        return config.getBoolean("worlds.settings.disable-mobs", true);
    }

    public boolean getWorldSettingDisableWeather() {
        return config.getBoolean("worlds.settings.disable-weather", true);
    }

    public boolean getWorldSettingDisableDayNightCycle() {
        return config.getBoolean("worlds.settings.disable-day-night-cycle", true);
    }

    public boolean getWorldSettingKeepInventory() {
        return config.getBoolean("worlds.settings.keep-inventory", true);
    }

    public boolean getWorldSettingDisablePvp() {
        return config.getBoolean("worlds.settings.disable-pvp", true);
    }

    // ====================================
    // PERFORMANCE & CACHING
    // ====================================

    public int getDatabaseMaxConnections() {
        return config.getInt("performance.database.max-connections", 10);
    }

    public int getDatabaseMinIdleConnections() {
        return config.getInt("performance.database.min-idle-connections", 2);
    }

    public long getDatabaseIdleTimeout() {
        return config.getLong("performance.database.idle-timeout", 600000);
    }

    public long getDatabaseMaxLifetime() {
        return config.getLong("performance.database.max-lifetime", 1800000);
    }

    public int getDatabaseCachePrepStmts() {
        return config.getInt("performance.database.cache-prep-stmts", 250);
    }

    public long getCacheTTLKingId() {
        return config.getLong("performance.cache.ttl.king-id", 1800000);
    }

    public long getCacheTTLVillageLevel() {
        return config.getLong("performance.cache.ttl.village-level", 300000);
    }

    public long getCacheTTLResources() {
        return config.getLong("performance.cache.ttl.resources", 30000);
    }

    public int getCacheCleanupInterval() {
        return config.getInt("performance.cache.cleanup-interval", 60);
    }

    public int getChunksPreloadRadius() {
        return config.getInt("performance.chunks.preload-radius", 3);
    }

    public long getActionBarUpdateInterval() {
        return config.getLong("performance.actionbar-update-interval", 20);
    }

    // ====================================
    // GUI-SYSTEM
    // ====================================

    public String getGuiTownHallTitle() {
        return config.getString("gui.townhall.title", "§6§lDein Dorf");
    }

    public int getGuiTownHallSize() {
        return config.getInt("gui.townhall.size", 27);
    }

    public String getGuiAttackTitle() {
        return config.getString("gui.attack.title", "§c§lAngriffe");
    }

    public int getGuiAttackSize() {
        return config.getInt("gui.attack.size", 27);
    }

    public String getGuiShopTitle() {
        return config.getString("gui.shop.title", "§e§lShop");
    }

    public int getGuiShopSize() {
        return config.getInt("gui.shop.size", 27);
    }

    public String getGuiMineRewardTitle() {
        return config.getString("gui.mine-reward.title", "§b§lMine Belohnungen");
    }

    public int getGuiMineRewardSize() {
        return config.getInt("gui.mine-reward.size", 54);
    }

    // ====================================
    // NACHRICHTEN & FORMATTING
    // ====================================

    public String getMessagePrefix() {
        return config.getString("messages.prefix", "§8[§6ClashMC§8]§r ");
    }

    public String getMessageErrorPrefix() {
        return config.getString("messages.error-prefix", "§8[§cFehler§8]§r ");
    }

    public String getMessageSuccessPrefix() {
        return config.getString("messages.success-prefix", "§8[§a✓§8]§r ");
    }

    public String getActionBarAttackTimer() {
        return config.getString("messages.actionbar.attack-timer", "§6Zeit: §e%time% §7| §6Schaden: §e%damage%%");
    }

    public String getActionBarMineTimer() {
        return config.getString("messages.actionbar.mine-timer", "§bVerbleibende Zeit: §e%time%");
    }

    public String getTimeFormat() {
        return config.getString("messages.time-format.format", "MM:SS");
    }

    // ====================================
    // DEBUG & LOGGING
    // ====================================

    public boolean isDebugEnabled() {
        return config.getBoolean("debug.enabled", false);
    }

    public boolean isDebugCache() {
        return config.getBoolean("debug.systems.cache", false);
    }

    public boolean isDebugDatabase() {
        return config.getBoolean("debug.systems.database", false);
    }

    public boolean isDebugChunks() {
        return config.getBoolean("debug.systems.chunks", false);
    }

    public boolean isDebugAttacks() {
        return config.getBoolean("debug.systems.attacks", false);
    }

    public boolean isDebugMine() {
        return config.getBoolean("debug.systems.mine", false);
    }

    // ====================================
    // FEATURES (Enable/Disable)
    // ====================================

    public boolean isReplaySystemEnabled() {
        return config.getBoolean("features.replay-system", true);
    }

    public boolean isBoosterSystemEnabled() {
        return config.getBoolean("features.booster-system", true);
    }

    public boolean isCollectorSystemEnabled() {
        return config.getBoolean("features.collector-system", true);
    }

    public boolean isAttackSystemEnabled() {
        return config.getBoolean("features.attack-system", true);
    }

    public boolean isMineSystemEnabled() {
        return config.getBoolean("features.mine-system", true);
    }

    public boolean isCacheSystemEnabled() {
        return config.getBoolean("features.cache-system", true);
    }

    /**
     * Validiert die Config-Werte und gibt Warnungen bei ungewöhnlichen Einstellungen aus.
     */
    public void validateConfig() {
        // Village
        if (getVillageMaxLevel() < 1) {
            LogUtil.logError(plugin, "Config: village.max-level muss >= 1 sein!");
        }
        
        // Economy
        if (getCollectorBaseEfficiency() < 0 || getCollectorBaseEfficiency() > 1) {
            LogUtil.logError(plugin, "Config: economy.collector.base-efficiency sollte zwischen 0 und 1 liegen!");
        }
        
        // Attacks
        if (getAttackDurationSeconds() < 10) {
            LogUtil.logError(plugin, "Config: attacks.duration-seconds ist sehr kurz (<10 Sekunden)!");
        }
        
        // Mine
        if (getMineSessionDurationMinutes() < 1) {
            LogUtil.logError(plugin, "Config: mine.session-duration-minutes muss >= 1 sein!");
        }
        
        // Performance
        if (getDatabaseMaxConnections() < 1) {
            LogUtil.logError(plugin, "Config: performance.database.max-connections muss >= 1 sein!");
        }
        
        LogUtil.logInfo(plugin, "Config-Validierung abgeschlossen.");
    }
}

