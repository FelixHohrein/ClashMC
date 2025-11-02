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
