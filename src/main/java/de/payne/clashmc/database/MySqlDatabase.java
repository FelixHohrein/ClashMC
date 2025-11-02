package de.payne.clashmc.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.payne.clashmc.database.migrations.MigrationManager;
import de.payne.clashmc.files.DatabaseHandler;

import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.SQLException;

public class MySqlDatabase extends DatabaseHandler {

    private HikariDataSource dataSource;
    private final Plugin plugin;

    public MySqlDatabase(final Plugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    /**
     * Öffnet eine neue Verbindung mit HikariCP Connection Pool
     */
    public void openConnection() {
        if (!super.isConnectionAllowed()) {
            plugin.getLogger().warning("Datenbankverbindung ist nicht erlaubt!");
            return;
        }
        
        if (isConnected()) {
            plugin.getLogger().info("Datenbankverbindung ist bereits aktiv.");
            return;
        }

        try {
            HikariConfig config = new HikariConfig();
            
            // JDBC URL
            String jdbcUrl = "jdbc:mysql://" + super.getHost() + ":" + super.getPort() + "/" + super.getDatabase()
                + "?autoReconnect=true&useSSL=false&serverTimezone=UTC&characterEncoding=utf8";
            
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(super.getUsername());
            config.setPassword(super.getPassword());
            
            // HikariCP Performance-Optimierungen
            config.setMaximumPoolSize(10); // Max 10 Connections im Pool
            config.setMinimumIdle(2); // Min 2 idle Connections
            config.setConnectionTimeout(30000); // 30 Sekunden Timeout
            config.setIdleTimeout(600000); // 10 Minuten idle timeout
            config.setMaxLifetime(1800000); // 30 Minuten max lifetime
            config.setLeakDetectionThreshold(60000); // 60 Sekunden leak detection
            
            // Connection Pool Settings
            config.setPoolName("ClashMC-HikariPool");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            
            this.dataSource = new HikariDataSource(config);
            this.plugin.getLogger().info("HikariCP Connection Pool erfolgreich initialisiert!");
            this.plugin.getLogger().info("  -> Max Pool Size: " + config.getMaximumPoolSize());
            this.plugin.getLogger().info("  -> Min Idle: " + config.getMinimumIdle());

            // Führe Migrations aus
            new MigrationManager(getConnection(), this.plugin.getLogger()).migrate();

        } catch (SQLException e) {
            this.plugin.getLogger().severe("Fehler beim Initialisieren des Connection Pools: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            this.plugin.getLogger().severe("Unerwarteter Fehler bei der Datenbankverbindung: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Keepalive ist mit HikariCP nicht mehr nötig - der Pool managed das automatisch
     */
    public void keepAlive() {
        // HikariCP verwaltet Connections automatisch
        // Diese Methode wird beibehalten für Kompatibilität, macht aber nichts mehr
        plugin.getLogger().info("HikariCP Connection Pool ist aktiv - automatisches Keep-Alive ist aktiviert");
    }

    /**
     * Schließt den Connection Pool und alle Connections
     */
    public void closeConnection() {
        if (!isConnected()) {
            return;
        }

        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                plugin.getLogger().info("HikariCP Connection Pool geschlossen.");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Fehler beim Schließen des Connection Pools: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Prüft, ob der Connection Pool aktiv ist
     */
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    /**
     * Gibt eine Connection aus dem Pool zurück
     * HikariCP verwaltet die Connection automatisch
     */
    public Connection getConnection() throws SQLException {
        if (!isConnected()) {
            throw new SQLException("Connection Pool ist nicht initialisiert oder wurde geschlossen!");
        }
        return dataSource.getConnection();
    }
    
    /**
     * Gibt Statistiken über den Connection Pool zurück
     */
    public String getPoolStats() {
        if (!isConnected()) {
            return "Connection Pool ist nicht aktiv";
        }
        
        return String.format("HikariCP Stats: Active=%d, Idle=%d, Total=%d, Waiting=%d",
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getHikariPoolMXBean().getTotalConnections(),
            dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }
}
