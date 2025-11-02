package de.payne.clashmc.database.modules;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.database.core.DatabaseModule;
import de.payne.clashmc.economy.PlayerResources;
import de.payne.clashmc.utils.LogUtil;

public class PlayerResourcesDatabase extends DatabaseModule {

    public PlayerResourcesDatabase(Connection connection) {
        super(connection);
    }
    
    public boolean playerResourcesExist(int playerId) {
        String sql = "SELECT king_id FROM kgmg_player_resources WHERE king_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, playerId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "Fehler bei playerResourcesExist(): " + e.getMessage());
            return false;
        }
    }
    
    public void createResources(int kingId, Timestamp lastConnected) throws SQLException {
        String sql = "INSERT INTO kgmg_player_resources (king_id, clash_coins, king_coins, last_collected) VALUES (?, 100, 100, ?)";
        executeUpdate(sql, kingId, lastConnected);
    }

    public void addClashCoins(int kingId, long amount) throws SQLException {
        String sql = "UPDATE kgmg_player_resources SET clash_coins = clash_coins + ? WHERE king_id = ?";
        executeUpdate(sql, amount, kingId);
    }

    public void addKingCoins(int kingId, long amount) throws SQLException {
        String sql = "UPDATE kgmg_player_resources SET king_coins = king_coins + ? WHERE king_id = ?";
        executeUpdate(sql, amount, kingId);
    }
    
    public void removeClashCoins(int kingId, long amount) throws SQLException {
        String sql = "UPDATE kgmg_player_resources SET clash_coins = clash_coins - ? WHERE king_id = ?";
        executeUpdate(sql, amount, kingId);
    }

    public void removeKingCoins(int kingId, long amount) throws SQLException {
        String sql = "UPDATE kgmg_player_resources SET king_coins = king_coins - ? WHERE king_id = ?";
        executeUpdate(sql, amount, kingId);
    }

    public PlayerResources getResources(UUID uuid) throws SQLException {
        int kingId = ClashMC.getInstance().getDatabaseManager().players().getPlayerIdByUUID(uuid);

        String sql = "SELECT clash_coins, king_coins, last_collected FROM kgmg_player_resources WHERE king_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, kingId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("last_collected");
                    return new PlayerResources(
                            uuid,
                            rs.getLong("clash_coins"),
                            rs.getLong("king_coins"),
                            ts != null ? ts.getTime() : 0L
                    );
                }
            }
        }
        return null;
    }

    public void setLastCollected(int kingId, Timestamp timestamp) throws SQLException {
        String sql = "UPDATE kgmg_player_resources SET last_collected = ? WHERE king_id = ?";
        executeUpdate(sql, timestamp, kingId);
    }

    public Timestamp getLastCollected(int kingId) throws SQLException {
        String sql = "SELECT last_collected FROM kgmg_player_resources WHERE king_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, kingId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getTimestamp("last_collected") : null;
            }
        }
    }

    public void updateKingCoins(int kingId, long amount) throws SQLException {
        String sql = "UPDATE kgmg_player_resources SET king_coins = ? WHERE king_id = ?";
        executeUpdate(sql, amount, kingId);
    }

    public void updateClashCoins(int kingId, long amount) throws SQLException {
        String sql = "UPDATE kgmg_player_resources SET clash_coins = ? WHERE king_id = ?";
        executeUpdate(sql, amount, kingId);
    }

    public void updateCollectorTimestamp(int kingId, long timestamp) throws SQLException {
        String sql = "UPDATE kgmg_player_resources SET last_collected = ? WHERE king_id = ?";
        executeUpdate(sql, new Timestamp(timestamp), kingId);
    }
}