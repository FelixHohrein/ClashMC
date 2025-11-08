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
        
        // DatabaseModule-Klassen werden mit DatabaseManager initialisiert
        // Sie holen sich bei jedem Aufruf eine neue Connection aus dem Pool
        this.playerDB = new PlayerDatabase(this);
        this.villageDB = new VillageDatabase(this);
        this.resourcesDB = new PlayerResourcesDatabase(this);
        this.mineDatabase = new MineDatabase(this);
        this.attackDatabase = new AttackDatabase(this);
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
