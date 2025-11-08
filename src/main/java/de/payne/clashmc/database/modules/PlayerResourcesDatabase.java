package de.payne.clashmc.database.modules;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.database.DatabaseManager;
import de.payne.clashmc.database.core.AsyncDatabaseModule;
import de.payne.clashmc.economy.PlayerResources;

public class PlayerResourcesDatabase extends AsyncDatabaseModule {

    public PlayerResourcesDatabase(DatabaseManager databaseManager) {
        super(databaseManager);
    }
    
    // ========== SYNCHRONE METHODEN (für Legacy-Code) ==========
    
    public boolean playerResourcesExist(int playerId) {
        String sql = "SELECT king_id FROM kgmg_player_resources WHERE king_id = ?";
        try {
            return exists(sql, playerId);
        } catch (SQLException e) {
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
        if (kingId == -1) return null;

        String sql = "SELECT clash_coins, king_coins, last_collected FROM kgmg_player_resources WHERE king_id = ?";
        return querySingleResult(sql, rs -> {
            try {
                Timestamp ts = rs.getTimestamp("last_collected");
                return new PlayerResources(
                    uuid,
                    rs.getLong("clash_coins"),
                    rs.getLong("king_coins"),
                    ts != null ? ts.getTime() : 0L
                );
            } catch (SQLException e) {
                return null;
            }
        }, null, kingId);
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

    // ========== ASYNCHRONE METHODEN ==========

    public CompletableFuture<Boolean> playerResourcesExistAsync(int playerId) {
        return existsAsync("SELECT king_id FROM kgmg_player_resources WHERE king_id = ?", playerId);
    }

    public CompletableFuture<PlayerResources> getResourcesAsync(UUID uuid) {
        return ClashMC.getInstance().getDatabaseManager().players().getPlayerIdByUUIDAsync(uuid)
            .thenCompose(kingId -> {
                if (kingId == -1) {
                    return CompletableFuture.completedFuture(null);
                }
                
                return querySingleResultAsync(
                    "SELECT clash_coins, king_coins, last_collected FROM kgmg_player_resources WHERE king_id = ?",
                    rs -> {
                        try {
                            Timestamp ts = rs.getTimestamp("last_collected");
                            return new PlayerResources(
                                uuid,
                                rs.getLong("clash_coins"),
                                rs.getLong("king_coins"),
                                ts != null ? ts.getTime() : 0L
                            );
                        } catch (SQLException e) {
                            return null;
                        }
                    },
                    null,
                    kingId
                );
            });
    }

    public CompletableFuture<Void> updateClashCoinsAsync(int kingId, long amount) {
        return executeUpdateAsync(
            "UPDATE kgmg_player_resources SET clash_coins = ? WHERE king_id = ?",
            amount, kingId
        ).thenRun(() -> {
            // Invalidate cache - finde UUID für kingId
            try {
                UUID uuid = ClashMC.getInstance().getDatabaseManager().players().getUUIDByKingId(kingId);
                if (uuid != null) {
                    ClashMC.getInstance().getCacheManager().invalidateResources(uuid);
                }
            } catch (SQLException e) {
                // Ignore - cache invalidation ist nicht kritisch
            }
        });
    }

    public CompletableFuture<Void> updateKingCoinsAsync(int kingId, long amount) {
        return executeUpdateAsync(
            "UPDATE kgmg_player_resources SET king_coins = ? WHERE king_id = ?",
            amount, kingId
        ).thenRun(() -> {
            // Invalidate cache
            try {
                UUID uuid = ClashMC.getInstance().getDatabaseManager().players().getUUIDByKingId(kingId);
                if (uuid != null) {
                    ClashMC.getInstance().getCacheManager().invalidateResources(uuid);
                }
            } catch (SQLException e) {
                // Ignore
            }
        });
    }

    public CompletableFuture<Void> addClashCoinsAsync(int kingId, long amount) {
        return executeUpdateAsync(
            "UPDATE kgmg_player_resources SET clash_coins = clash_coins + ? WHERE king_id = ?",
            amount, kingId
        ).thenRun(() -> {
            try {
                UUID uuid = ClashMC.getInstance().getDatabaseManager().players().getUUIDByKingId(kingId);
                if (uuid != null) {
                    ClashMC.getInstance().getCacheManager().invalidateResources(uuid);
                }
            } catch (SQLException e) {
                // Ignore
            }
        });
    }

    public CompletableFuture<Void> removeClashCoinsAsync(int kingId, long amount) {
        return executeUpdateAsync(
            "UPDATE kgmg_player_resources SET clash_coins = clash_coins - ? WHERE king_id = ?",
            amount, kingId
        ).thenRun(() -> {
            try {
                UUID uuid = ClashMC.getInstance().getDatabaseManager().players().getUUIDByKingId(kingId);
                if (uuid != null) {
                    ClashMC.getInstance().getCacheManager().invalidateResources(uuid);
                }
            } catch (SQLException e) {
                // Ignore
            }
        });
    }
}
