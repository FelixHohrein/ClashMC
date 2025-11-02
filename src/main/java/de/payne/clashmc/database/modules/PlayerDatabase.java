package de.payne.clashmc.database.modules;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.database.core.DatabaseModule;
import de.payne.clashmc.utils.LogUtil;

public class PlayerDatabase extends DatabaseModule {

    public PlayerDatabase(Connection connection) {
        super(connection);
    }

    public void createOrUpdatePlayer(UUID uuid, String language) throws SQLException {
        String sql = "INSERT INTO kgmg_players (uuid, language, first_join) VALUES (?, ?, NOW()) " +
                     "ON DUPLICATE KEY UPDATE language = VALUES(language)";
        executeUpdate(sql, uuid.toString(), language);
    }
    
    public boolean playerExists(UUID uuid) {
        String sql = "SELECT id FROM kgmg_players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "Fehler bei playerExists(): " + e.getMessage());
            return false;
        }
    }
    
    public void setPlayerLanguage(UUID uuid, String language) throws SQLException {
        String sql = "UPDATE kgmg_players SET language = ? WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, language);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        }
    }
    
    public String getPlayerLanguage(UUID uuid) throws SQLException {
        String sql = "SELECT language FROM kgmg_players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("language") : null;
            }
        }
    }
    
    public Timestamp getJoinDate(UUID uuid) throws SQLException {
        String sql = "SELECT first_join FROM kgmg_players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getTimestamp("first_join") : null;
            }
        }
    }
    
    public int getPlayerIdByUUID(UUID uuid) throws SQLException {
        String sql = "SELECT id FROM kgmg_players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("id") : -1;
            }
        }
    }
    
    public UUID getUUIDIdByKingId(int kingId) throws SQLException {
        String sql = "SELECT uuid FROM kgmg_players WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, kingId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? UUID.fromString(rs.getString("uuid")) : null;
            }
        }
    }
    
    /**
     * Gibt eine Liste aller Spieler-IDs aus der Tabelle kgmg_players zurück.
     */
    public List<Integer> getAllPlayerIds() {
        List<Integer> playerIds = new ArrayList<>();
        try (PreparedStatement ps = super.connection.prepareStatement(
                "SELECT id FROM kgmg_players")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                playerIds.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return playerIds;
    }
}
