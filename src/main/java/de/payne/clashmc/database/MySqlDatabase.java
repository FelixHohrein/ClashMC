package de.payne.clashmc.database;

import de.payne.clashmc.database.migrations.MigrationManager;
import de.payne.clashmc.files.DatabaseHandler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySqlDatabase extends DatabaseHandler {

    private Connection connection;
    private final Plugin plugin;

    public MySqlDatabase(final Plugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }


    // Öffnet eine neue Verbindung, falls keine aktive besteht und Verbindungen erlaubt sind.

    public void openConnection() {
        if (!super.isConnectionAllowed()) return;
        if (isConnected()) return;

        try {
            this.connection = DriverManager.getConnection(
                "jdbc:mysql://" + super.getHost() + ":" + super.getPort() + "/" + super.getDatabase()
                + "?autoReconnect=true&useSSL=false&serverTimezone=UTC",
                super.getUsername(), super.getPassword()
            );
            this.plugin.getLogger().info("Verbindung zur Datenbank hergestellt!");

            new MigrationManager(connection, this.plugin.getLogger()).migrate();

        } catch (SQLException e) {
            this.plugin.getLogger().severe("Verbindung zur Datenbank fehlgeschlagen! : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Reconnect-Handling & Health Check
    public void keepAlive() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, () -> {
            try {
                if (connection == null || connection.isClosed()) {
                    openConnection();
                } else if (!connection.isValid(2)) {
                    closeConnection();
                    openConnection();
                }
            } catch (SQLException e) {
                this.plugin.getLogger().warning("[DB] Verbindung verloren. Neuer Versuch...");
                openConnection();
            }
        }, 20L * 60, 20L * 60); // alle 60 Sekunden prüfen
    }

    
    // Schliesst die aktive Verbindung.
    public void closeConnection() {
        if (!isConnected()) return;

        try {
            connection.close();
            connection = null;
            plugin.getLogger().info("Datenbankverbindung geschlossen.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Schließen der Datenbankverbindung: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
     // Prüft, ob aktuell eine funktionierende Verbindung besteht.

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // Gibt die aktuelle Verbindung zurück oder stellt sie her, falls nötig.
    
    public Connection getConnection() {
        if (!isConnected()) {
            openConnection();
        }
        return connection;
    }
}
