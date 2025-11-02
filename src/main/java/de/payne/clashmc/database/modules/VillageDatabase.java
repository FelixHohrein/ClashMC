package de.payne.clashmc.database.modules;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import de.payne.clashmc.database.core.DatabaseModule;

public class VillageDatabase extends DatabaseModule {

    public VillageDatabase(Connection connection) {
        super(connection);
    }

    public void createVillage(int kingId) throws SQLException {
        String sql = "INSERT INTO kgmg_villages (king_id, level, last_attacked) VALUES (?, 1, NULL)";
        executeUpdate(sql, kingId);
    }

    public int getVillageLevel(int kingId) throws SQLException {
        String sql = "SELECT level FROM kgmg_villages WHERE king_id = ?";
        try (PreparedStatement stmt = super.connection.prepareStatement(sql)) {
            stmt.setInt(1, kingId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("level") : 1;
            }
        }
    }

    public void setVillageLevel(int kingId, int level) throws SQLException {
        String sql = "UPDATE kgmg_villages SET level = ? WHERE king_id = ?";
        executeUpdate(sql, level, kingId);
    }
    
    public void upgradeVillage(int kingId) throws SQLException {
        String sql = "UPDATE kgmg_villages SET level = level + 1 WHERE king_id = ?";
        super.executeUpdate(sql, kingId);
    }
    
    public void resetVillage(int kingId) throws SQLException {
        String sql = "UPDATE kgmg_villages SET level = 1, last_attacked = NULL WHERE king_id = ?";
        super.executeUpdate(sql, kingId);
    }

    public void setLastAttacked(int kingId, Timestamp attackedAt) throws SQLException {
        String sql = "UPDATE kgmg_villages SET last_attacked = ? WHERE king_id = ?";
        super.executeUpdate(sql, attackedAt, kingId);
    }

    public Timestamp getLastAttacked(int kingId) throws SQLException {
        String sql = "SELECT last_attacked FROM kgmg_villages WHERE king_id = ?";
        try (PreparedStatement stmt = super.connection.prepareStatement(sql)) {
            stmt.setInt(1, kingId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getTimestamp("last_attacked") : null;
            }
        }
    }

    public boolean villageExists(int kingId) throws SQLException {
        String sql = "SELECT king_id FROM kgmg_villages WHERE king_id = ?";
        try (PreparedStatement stmt = super.connection.prepareStatement(sql)) {
            stmt.setInt(1, kingId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}
