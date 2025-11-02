package de.payne.clashmc.database.modules;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.database.core.AsyncDatabaseModule;
import de.payne.clashmc.utils.LogUtil;

public class AttackDatabase extends AsyncDatabaseModule {

    public AttackDatabase(Connection connection) {
        super(connection);
    }

    // ========== SYNCHRONE METHODEN ==========
    
    public void createOptInIfNotExists(int playerId) {
        try {
            executeUpdate(
                "INSERT IGNORE INTO kgmg_attack_optin (king_id, is_online_enabled) VALUES (?, TRUE)",
                playerId
            );
        } catch (SQLException e) {
            LogUtil.logError(plugin, "Fehler bei createOptInIfNotExists: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean existsInOptinTable(int kingId) {
        try {
            return exists("SELECT king_id FROM kgmg_attack_optin WHERE king_id = ?", kingId);
        } catch (SQLException e) {
            return false;
        }
    }
    
    public void setOnlineAttackEnabled(int playerId, boolean enabled) {
        try {
            executeUpdate(
                "REPLACE INTO kgmg_attack_optin (king_id, is_online_enabled, last_updated) VALUES (?, ?, NOW())",
                playerId, enabled
            );
        } catch (SQLException e) {
            LogUtil.logError(plugin, "Fehler bei setOnlineAttackEnabled: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isOnlineAttackEnabled(int playerId) {
        String sql = "SELECT is_online_enabled FROM kgmg_attack_optin WHERE king_id = ?";
        try {
            return querySingleResult(sql, rs -> {
                try {
                    return rs.getBoolean("is_online_enabled");
                } catch (SQLException e) {
                    return false;
                }
            }, false, playerId);
        } catch (SQLException e) {
            return false;
        }
    }

    public int createAttack(int attackerId, int defenderId, boolean isOnline, double damagePercent, long lootClashCoins, long lootKingCoins) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO kgmg_attacks (attacker_id, defender_id, is_online, damage_percent, loot_clash_coins, loot_king_coins) VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, attackerId);
            ps.setInt(2, defenderId);
            ps.setBoolean(3, isOnline);
            ps.setDouble(4, damagePercent);
            ps.setLong(5, lootClashCoins);
            ps.setLong(6, lootKingCoins);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LogUtil.logError(plugin, "Fehler bei createAttack: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public void saveReplay(int attackId, String replayData) {
        try {
            executeUpdate(
                "INSERT INTO kgmg_attack_replays (attack_id, replay_data) VALUES (?, ?)",
                attackId, replayData
            );
        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "[Replay] Fehler beim Speichern des Replays für Attack ID " + attackId);
            e.printStackTrace();
        }
    }
    
    /**
     * Speichert Movement-Daten für einen Angriff (Async).
     */
    public java.util.concurrent.CompletableFuture<Void> saveMovementDataAsync(int attackId, String movementData) {
        return executeUpdateAsync(
            "UPDATE kgmg_attack_replays SET movement_data = ? WHERE attack_id = ?",
            movementData, attackId
        );
    }
    
    /**
     * Lädt Replays für einen Spieler (Angreifer + Verteidiger).
     * Spieler sehen nur Replays <7 Tage, Admins sehen alle.
     */
    public java.util.List<de.payne.clashmc.replay.ReplayData> getReplaysByPlayer(int kingId, boolean isAdmin, int maxDays) {
        java.util.List<de.payne.clashmc.replay.ReplayData> replays = new java.util.ArrayList<>();
        
        String sql;
        if (isAdmin) {
            // Admins sehen ALLE Replays
            sql = "SELECT a.id, a.attacker_id, a.defender_id, a.is_online, a.damage_percent, " +
                  "a.loot_clash_coins, a.loot_king_coins, a.created_at, " +
                  "r.replay_data, r.movement_data " +
                  "FROM kgmg_attacks a " +
                  "LEFT JOIN kgmg_attack_replays r ON a.id = r.attack_id " +
                  "WHERE r.replay_data IS NOT NULL " +
                  "ORDER BY a.created_at DESC";
        } else {
            // Spieler sehen nur eigene Replays <maxDays Tage
            sql = "SELECT a.id, a.attacker_id, a.defender_id, a.is_online, a.damage_percent, " +
                  "a.loot_clash_coins, a.loot_king_coins, a.created_at, " +
                  "r.replay_data, r.movement_data " +
                  "FROM kgmg_attacks a " +
                  "LEFT JOIN kgmg_attack_replays r ON a.id = r.attack_id " +
                  "WHERE (a.attacker_id = ? OR a.defender_id = ?) " +
                  "AND r.replay_data IS NOT NULL " +
                  "AND a.created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                  "ORDER BY a.created_at DESC";
        }
        
        try {
            if (isAdmin) {
                try (java.sql.PreparedStatement stmt = connection.prepareStatement(sql)) {
                    try (java.sql.ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            replays.add(mapReplayData(rs));
                        }
                    }
                }
            } else {
                try (java.sql.PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, kingId);
                    stmt.setInt(2, kingId);
                    stmt.setInt(3, maxDays);
                    try (java.sql.ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            replays.add(mapReplayData(rs));
                        }
                    }
                }
            }
        } catch (java.sql.SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "[Replay] Fehler beim Laden von Replays: " + e.getMessage());
            e.printStackTrace();
        }
        
        return replays;
    }
    
    /**
     * Lädt ein spezifisches Replay.
     */
    public java.util.Optional<de.payne.clashmc.replay.ReplayData> getReplayData(int attackId) {
        String sql = "SELECT a.id, a.attacker_id, a.defender_id, a.is_online, a.damage_percent, " +
                     "a.loot_clash_coins, a.loot_king_coins, a.created_at, " +
                     "r.replay_data, r.movement_data " +
                     "FROM kgmg_attacks a " +
                     "LEFT JOIN kgmg_attack_replays r ON a.id = r.attack_id " +
                     "WHERE a.id = ?";
        
        try (java.sql.PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, attackId);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return java.util.Optional.of(mapReplayData(rs));
                }
            }
        } catch (java.sql.SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "[Replay] Fehler beim Laden von Replay " + attackId + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return java.util.Optional.empty();
    }
    
    /**
     * Mappt ResultSet zu ReplayData.
     */
    private de.payne.clashmc.replay.ReplayData mapReplayData(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new de.payne.clashmc.replay.ReplayData(
            rs.getInt("id"),
            rs.getInt("attacker_id"),
            rs.getInt("defender_id"),
            rs.getBoolean("is_online"),
            rs.getDouble("damage_percent"),
            rs.getLong("loot_clash_coins"),
            rs.getLong("loot_king_coins"),
            rs.getTimestamp("created_at"),
            rs.getString("replay_data"),
            rs.getString("movement_data")
        );
    }

    public Optional<String> getReplay(int attackId) {
        String sql = "SELECT replay_data FROM kgmg_attack_replays WHERE attack_id = ?";
        try {
            return Optional.ofNullable(querySingleResult(sql, rs -> {
                try {
                    return rs.getString("replay_data");
                } catch (SQLException e) {
                    return null;
                }
            }, null, attackId));
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    public List<Integer> getRecentAttackIds(int defenderId, int limit) {
        List<Integer> attacks = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM kgmg_attacks WHERE defender_id = ? ORDER BY created_at DESC LIMIT ?")) {
            ps.setInt(1, defenderId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                attacks.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            LogUtil.logError(plugin, "Fehler bei getRecentAttackIds: " + e.getMessage());
            e.printStackTrace();
        }
        return attacks;
    }
    
    public int getRegisteredForOnlineAttackCount() {
        String sql = "SELECT COUNT(*) AS count FROM kgmg_attack_optin WHERE is_online_enabled = TRUE";
        try {
            return querySingleResult(sql, rs -> {
                try {
                    return rs.getInt("count");
                } catch (SQLException e) {
                    return 0;
                }
            }, 0);
        } catch (SQLException e) {
            return 0;
        }
    }
    
    public int getOnlineRegisteredPlayerCount() {
        int count = 0;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT king_id FROM kgmg_attack_optin WHERE is_online_enabled = TRUE")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int playerId = rs.getInt("king_id");
                if (isPlayerOnlineById(playerId)) {
                    count++;
                }
            }
        } catch (SQLException e) {
            LogUtil.logError(plugin, "Fehler bei getOnlineRegisteredPlayerCount: " + e.getMessage());
            e.printStackTrace();
        }
        return count;
    }
    
    public List<Integer> getRegisteredOnlinePlayerIds() {
        List<Integer> playerIds = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT king_id FROM kgmg_attack_optin WHERE is_online_enabled = TRUE")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                playerIds.add(rs.getInt("king_id"));
            }
        } catch (SQLException e) {
            LogUtil.logError(plugin, "Fehler bei getRegisteredOnlinePlayerIds: " + e.getMessage());
            e.printStackTrace();
        }
        return playerIds;
    }
    
    private boolean isPlayerOnlineById(int playerId) {
        try {
            UUID uuid = ClashMC.getInstance().getDatabaseManager().players().getUUIDByKingId(playerId);
            if (uuid == null) return false;
            Player player = Bukkit.getPlayer(uuid);
            return player != null && player.isOnline();
        } catch (SQLException e) {
            return false;
        }
    }

    // ========== ASYNCHRONE METHODEN (für kritische Operationen) ==========

    public CompletableFuture<Integer> createAttackAsync(int attackerId, int defenderId, boolean isOnline, 
                                                         double damagePercent, long lootClashCoins, long lootKingCoins) {
        return CompletableFuture.supplyAsync(() -> {
            return createAttack(attackerId, defenderId, isOnline, damagePercent, lootClashCoins, lootKingCoins);
        });
    }

    public CompletableFuture<Void> saveReplayAsync(int attackId, String replayData) {
        return CompletableFuture.runAsync(() -> {
            saveReplay(attackId, replayData);
        });
    }
}
