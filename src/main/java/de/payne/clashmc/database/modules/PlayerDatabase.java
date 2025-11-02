package de.payne.clashmc.database.modules;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.database.core.AsyncDatabaseModule;
import de.payne.clashmc.utils.LogUtil;

public class PlayerDatabase extends AsyncDatabaseModule {

    public PlayerDatabase(Connection connection) {
        super(connection);
    }

    // ========== SYNCHRONE METHODEN (für Legacy-Code) ==========

    public void createOrUpdatePlayer(UUID uuid, String language) throws SQLException {
        String sql = "INSERT INTO kgmg_players (uuid, language, first_join) VALUES (?, ?, NOW()) " +
                     "ON DUPLICATE KEY UPDATE language = VALUES(language)";
        executeUpdate(sql, uuid.toString(), language);
    }
    
    public boolean playerExists(UUID uuid) {
        String sql = "SELECT id FROM kgmg_players WHERE uuid = ?";
        try {
            return exists(sql, uuid.toString());
        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "Fehler bei playerExists(): " + e.getMessage());
            return false;
        }
    }
    
    public void setPlayerLanguage(UUID uuid, String language) throws SQLException {
        String sql = "UPDATE kgmg_players SET language = ? WHERE uuid = ?";
        executeUpdate(sql, language, uuid.toString());
    }
    
    public String getPlayerLanguage(UUID uuid) throws SQLException {
        String sql = "SELECT language FROM kgmg_players WHERE uuid = ?";
        return querySingleResult(sql, rs -> {
            try {
                return rs.getString("language");
            } catch (SQLException e) {
                return null;
            }
        }, null, uuid.toString());
    }
    
    public Timestamp getJoinDate(UUID uuid) throws SQLException {
        String sql = "SELECT first_join FROM kgmg_players WHERE uuid = ?";
        return querySingleResult(sql, rs -> {
            try {
                return rs.getTimestamp("first_join");
            } catch (SQLException e) {
                return null;
            }
        }, null, uuid.toString());
    }
    
    public int getPlayerIdByUUID(UUID uuid) throws SQLException {
        String sql = "SELECT id FROM kgmg_players WHERE uuid = ?";
        return querySingleResult(sql, rs -> {
            try {
                return rs.getInt("id");
            } catch (SQLException e) {
                return -1;
            }
        }, -1, uuid.toString());
    }
    
    public UUID getUUIDByKingId(int kingId) throws SQLException {
        String sql = "SELECT uuid FROM kgmg_players WHERE id = ?";
        return querySingleResult(sql, rs -> {
            try {
                String uuidStr = rs.getString("uuid");
                return uuidStr != null ? UUID.fromString(uuidStr) : null;
            } catch (SQLException e) {
                return null;
            }
        }, null, kingId);
    }
    
    public List<Integer> getAllPlayerIds() {
        List<Integer> playerIds = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT id FROM kgmg_players")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                playerIds.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            LogUtil.logError(plugin, "Fehler bei getAllPlayerIds(): " + e.getMessage());
            e.printStackTrace();
        }
        return playerIds;
    }

    // ========== ASYNCHRONE METHODEN ==========

    public CompletableFuture<Void> createOrUpdatePlayerAsync(UUID uuid, String language) {
        return executeUpdateAsync(
            "INSERT INTO kgmg_players (uuid, language, first_join) VALUES (?, ?, NOW()) " +
            "ON DUPLICATE KEY UPDATE language = VALUES(language)",
            uuid.toString(), language
        );
    }

    public CompletableFuture<Boolean> playerExistsAsync(UUID uuid) {
        return existsAsync("SELECT id FROM kgmg_players WHERE uuid = ?", uuid.toString());
    }

    public CompletableFuture<Integer> getPlayerIdByUUIDAsync(UUID uuid) {
        return querySingleResultAsync(
            "SELECT id FROM kgmg_players WHERE uuid = ?",
            rs -> {
                try {
                    return rs.getInt("id");
                } catch (SQLException e) {
                    return -1;
                }
            },
            -1,
            uuid.toString()
        );
    }

    public CompletableFuture<UUID> getUUIDByKingIdAsync(int kingId) {
        return querySingleResultAsync(
            "SELECT uuid FROM kgmg_players WHERE id = ?",
            rs -> {
                try {
                    String uuidStr = rs.getString("uuid");
                    return uuidStr != null ? UUID.fromString(uuidStr) : null;
                } catch (SQLException e) {
                    return null;
                }
            },
            null,
            kingId
        );
    }
}
