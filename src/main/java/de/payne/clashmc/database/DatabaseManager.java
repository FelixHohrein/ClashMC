package de.payne.clashmc.database;

import java.sql.Connection;

import de.payne.clashmc.database.modules.*;
import lombok.Getter;

public class DatabaseManager {

	@Getter
	private final Connection connection;
	
    private final PlayerDatabase playerDB;
    private final VillageDatabase villageDB;
    private final PlayerResourcesDatabase resourcesDB;
    private final MineDatabase mineDatabase;
    private final AttackDatabase attackDatabase;

    public DatabaseManager(Connection connection) {
    	this.connection = connection;
        this.playerDB = new PlayerDatabase(connection);
        this.villageDB = new VillageDatabase(connection);
        this.resourcesDB = new PlayerResourcesDatabase(connection);
        this.mineDatabase = new MineDatabase(connection);
        this.attackDatabase = new AttackDatabase(connection);

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
