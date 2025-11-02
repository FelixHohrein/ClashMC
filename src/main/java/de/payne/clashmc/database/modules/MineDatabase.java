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

import de.payne.clashmc.database.core.DatabaseModule;
import de.payne.clashmc.mine.MineBoosterType;
import de.payne.clashmc.mine.MineMaterialType;

public class MineDatabase extends DatabaseModule {

    // === MINEN-ZUSTAND (clashmc_mine) ===

    public MineDatabase(Connection connection) {
		super(connection);
	}

    
    public void createIfNotExist(int kingId) throws SQLException {
        // === clashmc_mine ===
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
        try (PreparedStatement stmt = connection.prepareStatement(sqlCheckRewards)) {
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
        String sql = "SELECT 1 FROM clashmc_mine WHERE king_id = ?";
        try (PreparedStatement stmt = super.connection.prepareStatement(sql)) {
            stmt.setInt(1, kingId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void createOrUpdateMineData(int kingId, long cooldown, int upgradeLevel) throws SQLException {
        if (hasMineData(kingId)) {
            executeUpdate("UPDATE clashmc_mine SET pickaxe_level = ?, cooldown_until = ? WHERE king_id = ?",
                    upgradeLevel, cooldown, kingId);
        } else {
            executeUpdate("INSERT INTO clashmc_mine (king_id, pickaxe_level, cooldown_until) VALUES (?, ?, ?)",
                    kingId, upgradeLevel, cooldown);
        }
    }

    public long getMineCooldown(int kingId) {
        String sql = "SELECT next_mine_available FROM clashmc_mine WHERE king_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, kingId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("next_mine_available");
                return ts != null ? ts.getTime() : 0L;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0L;
    }
    
    public void setMineCooldown(int kingId, long timestamp) {
        String sql = "UPDATE clashmc_mine SET next_mine_available = ? WHERE king_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, new Timestamp(timestamp));
            stmt.setInt(2, kingId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getPickaxeLevel(int kingId) {
        String sql = "SELECT pickaxe_level FROM clashmc_mine WHERE king_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, kingId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("pickaxe_level");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    public void setPickaxeLevel(int kingId, int newLevel) throws SQLException {
        String sql = "UPDATE clashmc_mine SET pickaxe_level = ? WHERE king_id = ?";
        executeUpdate(sql, newLevel, kingId);

    }

    public void setBooster(int kingId, int boosterSlot, MineBoosterType type, long expiresAt) throws SQLException {
        if (boosterSlot < 1 || boosterSlot > 3) return;
        
        if(type == MineBoosterType.NO_COOLDOWN) {
        	this.setMineCooldown(kingId, expiresAt-1000000000);
        }

        String sql = "UPDATE clashmc_mine SET booster_" + boosterSlot + "_type = ?, booster_" + boosterSlot + "_expires_at = ? WHERE king_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, type.name());
            stmt.setTimestamp(2, new Timestamp(expiresAt)); // <- long wert als TimeStamp
            stmt.setInt(3, kingId);
            stmt.executeUpdate();
        }
    }

    public long getBoosterExpires(int kingId, int boosterSlot) {
        if (boosterSlot < 1 || boosterSlot > 3) return 0L;
        String sql = "SELECT booster_" + boosterSlot + "_expires_at FROM clashmc_mine WHERE king_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, kingId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp(1);
                return ts != null ? ts.getTime() : 0L;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0L;
    }
    
    
    public List<MineBoosterType> getActiveBoosters(int kingId) {
        List<MineBoosterType> boosters = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (int i = 1; i <= 3; i++) {
            String typeColumn = "booster_" + i + "_type";
            String expiresColumn = "booster_" + i + "_expires_at";
            String sql = "SELECT " + typeColumn + ", " + expiresColumn + " FROM clashmc_mine WHERE king_id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, kingId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String type = rs.getString(1);
                    Timestamp expires = rs.getTimestamp(2);

                    if (type != null && expires != null && expires.getTime() > now) {
                        try {
                            boosters.add(MineBoosterType.valueOf(type));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return boosters;
    }

    // === MINING-ITEM-REWARDS (clashmc_mine_rewards) ===

    // Save or update mining reward
    public void saveOrUpdateItem(int kingId, MineMaterialType material, int amount) throws SQLException {
        if (hasItemEntry(kingId, material)) {
            executeUpdate("UPDATE clashmc_mine_rewards SET amount = ? WHERE king_id = ? AND material = ?",
                    amount, kingId, material.name());
        } else {
            executeUpdate("INSERT INTO clashmc_mine_rewards (king_id, material, amount) VALUES (?, ?, ?)",
                    kingId, material.name(), amount);
        }
    }

    public boolean hasItemEntry(int kingId, MineMaterialType material) {
        String sql = "SELECT 1 FROM clashmc_mine_rewards WHERE king_id = ? AND material = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, kingId);
            stmt.setString(2, material.name());
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


public Map<MineMaterialType, Integer> getAllItems(int kingId) {
    Map<MineMaterialType, Integer> items = new HashMap<>();
    String sql = "SELECT material, amount FROM clashmc_mine_rewards WHERE king_id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
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
        e.printStackTrace();
    }
    return items;
}

    public void clearMineRewards(int kingId) throws SQLException {
        executeUpdate("DELETE FROM clashmc_mine_rewards WHERE king_id = ?", kingId);
    }
}