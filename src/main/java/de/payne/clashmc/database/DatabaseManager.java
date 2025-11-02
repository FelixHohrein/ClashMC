package de.payne.clashmc.database;

import java.sql.Connection;
import java.sql.SQLException;

import de.payne.clashmc.database.modules.*;

public class DatabaseManager {

	private final MySqlDatabase database;
	
    private final PlayerDatabase playerDB;
    private final VillageDatabase villageDB;
    private final PlayerResourcesDatabase resourcesDB;
    private final MineDatabase mineDatabase;
    private final AttackDatabase attackDatabase;

    public DatabaseManager(MySqlDatabase database) {
        this.database = database;
        
        try {
            Connection connection = database.getConnection();
            this.playerDB = new PlayerDatabase(connection);
            this.villageDB = new VillageDatabase(connection);
            this.resourcesDB = new PlayerResourcesDatabase(connection);
            this.mineDatabase = new MineDatabase(connection);
            this.attackDatabase = new AttackDatabase(connection);
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Initialisieren des DatabaseManagers: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gibt eine Connection aus dem Pool zurück
     * HikariCP gibt automatisch eine Connection aus dem Pool
     */
    public Connection getConnection() throws SQLException {
        return database.getConnection();
    }

    public PlayerDatabase players() { 
    	return playerDB; 
    }
    
    public VillageDatabase villages() { 
    	return villageDB; 
    }
    
    public PlayerResourcesDatabase resources() {
        return resourcesDB;
    }
    
    public MineDatabase mine() {
        return mineDatabase;
    }
    
    public AttackDatabase attacks() {
    	return this.attackDatabase;
    }
}
