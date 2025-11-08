package de.payne.clashmc.database.modules;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import de.payne.clashmc.database.DatabaseManager;
import de.payne.clashmc.database.core.AsyncDatabaseModule;
import de.payne.clashmc.mine.MineBoosterType;
import de.payne.clashmc.mine.MineMaterialType;
import de.payne.clashmc.utils.LogUtil;

public class MineDatabase extends AsyncDatabaseModule {

    public MineDatabase(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    // ========== SYNCHRONE METHODEN ==========
    
    public void createIfNotExist(int kingId) throws SQLException {
        if (!hasMineData(kingId)) {
            executeUpdate(
                "INSERT INTO clashmc_mine (king_id, pickaxe_level, next_mine_available, " +
                "booster_1_type, booster_1_expires_at, " +
                "booster_2_type, booster_2_expires_at, " +
                "booster_3_type, booster_3_expires_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                kingId, 1, null,
                null, null,
                null, null,
                null, null
            );
        }

        // === clashmc_mine_rewards ===
        String sqlCheckRewards = "SELECT 1 FROM clashmc_mine_rewards WHERE king_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sqlCheckRewards)) {
            stmt.setInt(1, kingId);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                for (MineMaterialType type : MineMaterialType.values()) {
                    executeUpdate("INSERT INTO clashmc_mine_rewards (king_id, material, amount) VALUES (?, ?, ?)",
                            kingId, type.name(), 0);
                }
            }
        }
    }
        
    public boolean hasMineData(int kingId) {
        try {
            return exists("SELECT 1 FROM clashmc_mine WHERE king_id = ?", kingId);
        } catch (SQLException e) {
            return false;
        }
    }

    public void createOrUpdateMineData(int kingId, long cooldown, int upgradeLevel) throws SQLException {
        if (hasMineData(kingId)) {
            executeUpdate("UPDATE clashmc_mine SET pickaxe_level = ?, next_mine_available = ? WHERE king_id = ?",
                    upgradeLevel, new Timestamp(cooldown), kingId);
        } else {
            executeUpdate("INSERT INTO clashmc_mine (king_id, pickaxe_level, next_mine_available) VALUES (?, ?, ?)",
                    kingId, upgradeLevel, new Timestamp(cooldown));
        }
    }

    public long getMineCooldown(int kingId) {
        String sql = "SELECT next_mine_available FROM clashmc_mine WHERE king_id = ?";
        try {
            return querySingleResult(sql, rs -> {
                try {
                    Timestamp ts = rs.getTimestamp("next_mine_available");
                    return ts != null ? ts.getTime() : 0L;
                } catch (SQLException e) {
                    return 0L;
                }
            }, 0L, kingId);
        } catch (SQLException e) {
            return 0L;
        }
    }
    
    public void setMineCooldown(int kingId, long timestamp) {
        try {
            executeUpdate(
                "UPDATE clashmc_mine SET next_mine_available = ? WHERE king_id = ?",
                new Timestamp(timestamp), kingId
            );
        } catch (SQLException e) {
            LogUtil.logError(plugin, "Fehler bei setMineCooldown: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getPickaxeLevel(int kingId) {
        String sql = "SELECT pickaxe_level FROM clashmc_mine WHERE king_id = ?";
        try {
            return querySingleResult(sql, rs -> {
                try {
                    return rs.getInt("pickaxe_level");
                } catch (SQLException e) {
                    return 0;
                }
            }, 0, kingId);
        } catch (SQLException e) {
            return 0;
        }
    }
    
    public void setPickaxeLevel(int kingId, int newLevel) throws SQLException {
        executeUpdate("UPDATE clashmc_mine SET pickaxe_level = ? WHERE king_id = ?", newLevel, kingId);
    }

    public void setBooster(int kingId, int boosterSlot, MineBoosterType type, long expiresAt) throws SQLException {
        if (boosterSlot < 1 || boosterSlot > 3) return;
        
        if (type == MineBoosterType.NO_COOLDOWN) {
            this.setMineCooldown(kingId, expiresAt - 1000000000);
        }

        String sql = "UPDATE clashmc_mine SET booster_" + boosterSlot + "_type = ?, booster_" + boosterSlot + "_expires_at = ? WHERE king_id = ?";
        executeUpdate(sql, type.name(), new Timestamp(expiresAt), kingId);
    }

    public long getBoosterExpires(int kingId, int boosterSlot) {
        if (boosterSlot < 1 || boosterSlot > 3) return 0L;
        String sql = "SELECT booster_" + boosterSlot + "_expires_at FROM clashmc_mine WHERE king_id = ?";
        try {
            return querySingleResult(sql, rs -> {
                try {
                    Timestamp ts = rs.getTimestamp(1);
                    return ts != null ? ts.getTime() : 0L;
                } catch (SQLException e) {
                    return 0L;
                }
            }, 0L, kingId);
        } catch (SQLException e) {
            return 0L;
        }
    }
    
    public List<MineBoosterType> getActiveBoosters(int kingId) {
        List<MineBoosterType> boosters = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (int i = 1; i <= 3; i++) {
            String typeColumn = "booster_" + i + "_type";
            String expiresColumn = "booster_" + i + "_expires_at";
            String sql = "SELECT " + typeColumn + ", " + expiresColumn + " FROM clashmc_mine WHERE king_id = ?";

            try {
                Map<String, Object> result = querySingleResult(sql, rs -> {
                    try {
                        Map<String, Object> map = new HashMap<>();
                        map.put("type", rs.getString(1));
                        map.put("expires", rs.getTimestamp(2));
                        return map;
                    } catch (SQLException e) {
                        return null;
                    }
                }, null, kingId);

                if (result != null) {
                    String type = (String) result.get("type");
                    Timestamp expires = (Timestamp) result.get("expires");

                    if (type != null && expires != null && expires.getTime() > now) {
                        try {
                            boosters.add(MineBoosterType.valueOf(type));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
            } catch (SQLException e) {
                LogUtil.logError(plugin, "Fehler bei getActiveBoosters: " + e.getMessage());
            }
        }

        return boosters;
    }

    // === MINING-ITEM-REWARDS ===

    public void saveOrUpdateItem(int kingId, MineMaterialType material, int amount) throws SQLException {
        if (hasItemEntry(kingId, material)) {
            executeUpdate("UPDATE clashmc_mine_rewards SET amount = amount + ? WHERE king_id = ? AND material = ?",
                    amount, kingId, material.name());
        } else {
            executeUpdate("INSERT INTO clashmc_mine_rewards (king_id, material, amount) VALUES (?, ?, ?)",
                    kingId, material.name(), amount);
        }
    }

    public boolean hasItemEntry(int kingId, MineMaterialType material) {
        try {
            return exists("SELECT 1 FROM clashmc_mine_rewards WHERE king_id = ? AND material = ?", 
                kingId, material.name());
        } catch (SQLException e) {
            return false;
        }
    }

    public Map<MineMaterialType, Integer> getAllItems(int kingId) {
        Map<MineMaterialType, Integer> items = new HashMap<>();
        String sql = "SELECT material, amount FROM clashmc_mine_rewards WHERE king_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, kingId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString("material");
                int amount = rs.getInt("amount");

                MineMaterialType type = MineMaterialType.fromName(name);
                if (type != null) {
                    items.put(type, amount);
                }
            }
        } catch (SQLException e) {
            LogUtil.logError(plugin, "Fehler bei getAllItems: " + e.getMessage());
            e.printStackTrace();
        }
        return items;
    }

    public void clearMineRewards(int kingId) throws SQLException {
        executeUpdate("DELETE FROM clashmc_mine_rewards WHERE king_id = ?", kingId);
    }

    // ========== ASYNCHRONE METHODEN (für häufig genutzte Operationen) ==========

    public CompletableFuture<Void> saveOrUpdateItemAsync(int kingId, MineMaterialType material, int amount) {
        return CompletableFuture.runAsync(() -> {
            try {
                saveOrUpdateItem(kingId, material, amount);
            } catch (SQLException e) {
                LogUtil.logError(plugin, "Async saveOrUpdateItem Fehler: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Integer> getPickaxeLevelAsync(int kingId) {
        return querySingleResultAsync(
            "SELECT pickaxe_level FROM clashmc_mine WHERE king_id = ?",
            rs -> {
                try {
                    return rs.getInt("pickaxe_level");
                } catch (SQLException e) {
                    return 0;
                }
            },
            0,
            kingId
        );
    }

    public CompletableFuture<List<MineBoosterType>> getActiveBoostersAsync(int kingId) {
        return CompletableFuture.supplyAsync(() -> getActiveBoosters(kingId));
    }
}
