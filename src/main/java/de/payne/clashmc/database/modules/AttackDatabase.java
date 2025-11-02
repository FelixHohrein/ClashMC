package de.payne.clashmc.database.modules;


import java.sql.*;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.database.core.DatabaseModule;
import de.payne.clashmc.utils.LogUtil;

public class AttackDatabase extends DatabaseModule {

    public AttackDatabase(Connection connection) {
        super(connection);
    }

    
    
    public void createOptInIfNotExists(int playerId) {
        try (PreparedStatement statement = super.connection.prepareStatement(
                 "INSERT IGNORE INTO kgmg_attack_optin (king_id, is_online_enabled) VALUES (?, TRUE)"
             )) {
            statement.setInt(1, playerId);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public boolean existsInOptinTable(int kingId) {
        boolean exists = false;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT king_id FROM kgmg_attack_optin WHERE king_id = ?")) {
            ps.setInt(1, kingId);
            try (ResultSet rs = ps.executeQuery()) {
                exists = rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace(); // oder Logging
        }
        return exists;
    }
    
    // Spieler für Online-Kämpfe aktivieren/deaktivieren
    public void setOnlineAttackEnabled(int playerId, boolean enabled) {
        try (PreparedStatement ps = super.connection.prepareStatement(
                "REPLACE INTO kgmg_attack_optin (king_id, is_online_enabled, last_updated) VALUES (?, ?, NOW())")) {
            ps.setInt(1, playerId);
            ps.setBoolean(2, enabled);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isOnlineAttackEnabled(int playerId) {
        try (PreparedStatement ps = super.connection.prepareStatement(
                "SELECT is_online_enabled FROM kgmg_attack_optin WHERE king_id = ?")) {
            ps.setInt(1, playerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("is_online_enabled");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Angriff speichern
    public int createAttack(int attackerId, int defenderId, boolean isOnline, double damagePercent, long lootClashCoins, long lootKingCoins) {
        try (PreparedStatement ps = super.connection.prepareStatement(
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
                return rs.getInt(1); // attack_id
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Replay speichern
    public void saveReplay(int attackId, String replayData) {
        try (PreparedStatement ps = super.connection.prepareStatement(
                "INSERT INTO kgmg_attack_replays (attack_id, replay_data) VALUES (?, ?)")) {
            ps.setInt(1, attackId);
            ps.setString(2, replayData);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "[Replay] Fehler beim Speichern des Replays für Attack ID " + attackId);
            e.printStackTrace();
        }
    }

    // Replay abrufen
    public Optional<String> getReplay(int attackId) {
        try (PreparedStatement ps = super.connection.prepareStatement(
                "SELECT replay_data FROM kgmg_attack_replays WHERE attack_id = ?")) {
            ps.setInt(1, attackId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.ofNullable(rs.getString("replay_data"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // Letzte Angriffe gegen einen Spieler anzeigen
    public List<Integer> getRecentAttackIds(int defenderId, int limit) {
        List<Integer> attacks = new ArrayList<>();
        try (PreparedStatement ps = super.connection.prepareStatement(
                "SELECT id FROM kgmg_attacks WHERE defender_id = ? ORDER BY created_at DESC LIMIT ?")) {
            ps.setInt(1, defenderId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                attacks.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return attacks;
    }
    
    /**
     * Anzahl aller Spieler, die sich für Online-Angriffe registriert haben (Einträge in kgmg_attack_optin).
     */
    public int getRegisteredForOnlineAttackCount() {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) AS count FROM kgmg_attack_optin WHERE is_online_enabled = TRUE")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    public int getOnlineRegisteredPlayerCount() {
        int count = 0;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT king_id FROM kgmg_attack_optin WHERE is_online_enabled = TRUE")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int playerId = rs.getInt("king_id");
                // Prüfen, ob ein Spieler mit dieser playerId online ist
                if (isPlayerOnlineById(playerId)) {
                    count++;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }
    
    /**
     * Gibt eine Liste aller Spieler-IDs (king_id) zurück, die für Online-Angriffe registriert sind (is_online_enabled = TRUE).
     */
    public List<Integer> getRegisteredOnlinePlayerIds() {
        List<Integer> playerIds = new ArrayList<>();
        try (PreparedStatement ps = super.connection.prepareStatement(
                "SELECT king_id FROM kgmg_attack_optin WHERE is_online_enabled = TRUE")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                playerIds.add(rs.getInt("king_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return playerIds;
    }
    
    /**
     * Hilfsmethode, um anhand der playerId (id aus kgmg_players) zu prüfen, ob Spieler aktuell online ist.
     * Da Bukkit nur UUIDs kennt, brauchst du eine Möglichkeit, playerId -> UUID zu übersetzen.
     * Falls du eine Datenbankmethode hast, die UUID zu playerId liefert, brauchst du auch die Umkehrung.
     * 
     * Hier als Beispiel, du musst die Methode selbst anpassen je nach Datenhaltung.
     */
    private boolean isPlayerOnlineById(int playerId) {
        // Beispiel: Hole UUID des Spielers über deine PlayerDatabase
        UUID uuid = null;
		try {
			uuid = ClashMC.getInstance().getDatabaseManager().players().getUUIDIdByKingId(playerId);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if (uuid == null) return false;

        Player player = Bukkit.getPlayer(uuid);
        return player != null && player.isOnline();
    }
}
