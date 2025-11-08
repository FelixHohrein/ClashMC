package de.payne.clashmc.database.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import de.payne.clashmc.database.DatabaseManager;

public abstract class DatabaseModule {

    protected final DatabaseManager databaseManager;

    public DatabaseModule(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    protected void close(AutoCloseable... closeables) {
        for (AutoCloseable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Optional: Hilfsmethode zum Ausführen einfacher Updates
    // Holt bei jedem Aufruf eine neue Connection aus dem Pool
    protected void executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
        }
    }
}
