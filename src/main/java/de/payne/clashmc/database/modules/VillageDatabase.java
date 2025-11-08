package de.payne.clashmc.database.modules;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.CompletableFuture;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.database.DatabaseManager;
import de.payne.clashmc.database.core.AsyncDatabaseModule;

public class VillageDatabase extends AsyncDatabaseModule {

    public VillageDatabase(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    // ========== SYNCHRONE METHODEN (für Legacy-Code) ==========

    public void createVillage(int kingId) throws SQLException {
        String sql = "INSERT INTO kgmg_villages (king_id, level, last_attacked) VALUES (?, 1, NULL)";
        executeUpdate(sql, kingId);
    }

    public int getVillageLevel(int kingId) throws SQLException {
        String sql = "SELECT level FROM kgmg_villages WHERE king_id = ?";
        return querySingleResult(sql, rs -> {
            try {
                return rs.getInt("level");
            } catch (SQLException e) {
                return 1;
            }
        }, 1, kingId);
    }

    public void setVillageLevel(int kingId, int level) throws SQLException {
        String sql = "UPDATE kgmg_villages SET level = ? WHERE king_id = ?";
        executeUpdate(sql, level, kingId);
    }
    
    public void upgradeVillage(int kingId) throws SQLException {
        String sql = "UPDATE kgmg_villages SET level = level + 1 WHERE king_id = ?";
        executeUpdate(sql, kingId);
    }
    
    public void resetVillage(int kingId) throws SQLException {
        String sql = "UPDATE kgmg_villages SET level = 1, last_attacked = NULL WHERE king_id = ?";
        executeUpdate(sql, kingId);
    }

    public void setLastAttacked(int kingId, Timestamp attackedAt) throws SQLException {
        String sql = "UPDATE kgmg_villages SET last_attacked = ? WHERE king_id = ?";
        executeUpdate(sql, attackedAt, kingId);
    }

    public Timestamp getLastAttacked(int kingId) throws SQLException {
        String sql = "SELECT last_attacked FROM kgmg_villages WHERE king_id = ?";
        return querySingleResult(sql, rs -> {
            try {
                return rs.getTimestamp("last_attacked");
            } catch (SQLException e) {
                return null;
            }
        }, null, kingId);
    }

    public boolean villageExists(int kingId) throws SQLException {
        String sql = "SELECT king_id FROM kgmg_villages WHERE king_id = ?";
        return exists(sql, kingId);
    }

    // ========== ASYNCHRONE METHODEN ==========

    public CompletableFuture<Void> createVillageAsync(int kingId) {
        return executeUpdateAsync(
            "INSERT INTO kgmg_villages (king_id, level, last_attacked) VALUES (?, 1, NULL)",
            kingId
        );
    }

    public CompletableFuture<Integer> getVillageLevelAsync(int kingId) {
        return querySingleResultAsync(
            "SELECT level FROM kgmg_villages WHERE king_id = ?",
            rs -> {
                try {
                    return rs.getInt("level");
                } catch (SQLException e) {
                    return 1;
                }
            },
            1,
            kingId
        );
    }

    public CompletableFuture<Void> setVillageLevelAsync(int kingId, int level) {
        return executeUpdateAsync(
            "UPDATE kgmg_villages SET level = ? WHERE king_id = ?",
            level, kingId
        ).thenRun(() -> {
            // Invalidate cache after update
            ClashMC.getInstance().getCacheManager().invalidateVillageLevel(kingId);
        });
    }

    public CompletableFuture<Void> upgradeVillageAsync(int kingId) {
        return executeUpdateAsync(
            "UPDATE kgmg_villages SET level = level + 1 WHERE king_id = ?",
            kingId
        ).thenRun(() -> {
            // Invalidate cache after upgrade
            ClashMC.getInstance().getCacheManager().invalidateVillageLevel(kingId);
        });
    }

    public CompletableFuture<Void> resetVillageAsync(int kingId) {
        return executeUpdateAsync(
            "UPDATE kgmg_villages SET level = 1, last_attacked = NULL WHERE king_id = ?",
            kingId
        ).thenRun(() -> {
            // Invalidate cache after reset
            ClashMC.getInstance().getCacheManager().invalidateVillageLevel(kingId);
        });
    }

    public CompletableFuture<Boolean> villageExistsAsync(int kingId) {
        return existsAsync("SELECT king_id FROM kgmg_villages WHERE king_id = ?", kingId);
    }
}
