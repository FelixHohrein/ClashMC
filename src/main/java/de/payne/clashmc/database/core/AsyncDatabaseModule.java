package de.payne.clashmc.database.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.utils.LogUtil;

/**
 * Erweiterte DatabaseModule-Klasse mit async Support
 * Bietet sowohl synchrone als auch asynchrone Methoden
 */
public abstract class AsyncDatabaseModule extends DatabaseModule {

    protected final Plugin plugin;

    public AsyncDatabaseModule(Connection connection) {
        super(connection);
        this.plugin = ClashMC.getInstance();
    }

    /**
     * Führt eine Query asynchron aus und gibt das Ergebnis zurück
     * 
     * @param <T> Return-Typ
     * @param queryFunction Funktion die die Query ausführt
     * @return CompletableFuture mit Ergebnis
     */
    protected <T> CompletableFuture<T> executeQueryAsync(Function<Connection, T> queryFunction) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return queryFunction.apply(connection);
            } catch (Exception e) {
                LogUtil.logError(plugin, "Async Query Fehler: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Führt ein Update asynchron aus
     * 
     * @param sql SQL-Statement
     * @param params Parameter
     * @return CompletableFuture<Void>
     */
    protected CompletableFuture<Void> executeUpdateAsync(String sql, Object... params) {
        return CompletableFuture.runAsync(() -> {
            try {
                executeUpdate(sql, params);
            } catch (SQLException e) {
                LogUtil.logError(plugin, "Async Update Fehler: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Helper: Führt eine Query aus und gibt ein einzelnes Ergebnis zurück
     */
    protected <T> T querySingleResult(String sql, Function<ResultSet, T> mapper, T defaultValue, Object... params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapper.apply(rs) : defaultValue;
            }
        }
    }

    /**
     * Helper: Async Version von querySingleResult
     */
    protected <T> CompletableFuture<T> querySingleResultAsync(String sql, Function<ResultSet, T> mapper, T defaultValue, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return querySingleResult(sql, mapper, defaultValue, params);
            } catch (SQLException e) {
                LogUtil.logError(plugin, "Async Query Error: " + e.getMessage());
                e.printStackTrace();
                return defaultValue;
            }
        });
    }

    /**
     * Helper: Prüft ob ein Eintrag existiert
     */
    protected boolean exists(String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Helper: Async Version von exists
     */
    protected CompletableFuture<Boolean> existsAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return exists(sql, params);
            } catch (SQLException e) {
                LogUtil.logError(plugin, "Async Exists Error: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Führt einen Runnable auf dem Main-Thread aus (für Bukkit-API Calls)
     */
    protected void runSync(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Wrapper für Exception-Handling in CompletableFuture
     */
    protected <T> CompletableFuture<T> handleAsync(CompletableFuture<T> future, T defaultValue) {
        return future.exceptionally(throwable -> {
            LogUtil.logError(plugin, "Async Operation fehlgeschlagen: " + throwable.getMessage());
            throwable.printStackTrace();
            return defaultValue;
        });
    }
}

