package de.payne.clashmc.database.migrations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class MigrationManager {

    private final Connection connection;
    private final Logger logger;

    public MigrationManager(Connection connection, Logger logger) {
        this.connection = connection;
        this.logger = logger;
    }

    public void migrate() {
    	try (Statement statement = connection.createStatement()) {
    		logger.info("[MIGRATION] Starte Datenbankmigrationen...");

    		// Tabelle: Spieler
    		statement.executeUpdate(
    				"CREATE TABLE IF NOT EXISTS kgmg_players (" +
    						"id INT AUTO_INCREMENT PRIMARY KEY," +
    						"uuid VARCHAR(36) NOT NULL," +
    						"first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
    						"language VARCHAR(10) DEFAULT 'de'" +
    						") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
    				);

    		// Tabelle: Dörfer
    		statement.executeUpdate(
    				"CREATE TABLE IF NOT EXISTS kgmg_villages (" +
    						"king_id INT PRIMARY KEY NOT NULL," +
    						"level INT DEFAULT 1," +
    						"last_attacked TIMESTAMP NULL," +
    						"FOREIGN KEY (king_id) REFERENCES kgmg_players(id) ON DELETE CASCADE" +
    						") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
    				);

    		// Tabelle: Ressourcen
    		statement.executeUpdate(
    				"CREATE TABLE IF NOT EXISTS kgmg_player_resources (" +
    						"king_id INT PRIMARY KEY NOT NULL," +
    						"clash_coins BIGINT NOT NULL DEFAULT 0," +
    						"king_coins BIGINT NOT NULL DEFAULT 0," +
    						"last_collected TIMESTAMP NULL," +
    						"FOREIGN KEY (king_id) REFERENCES kgmg_players(id) ON DELETE CASCADE" +
    						") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
    				);

    		// Haupttabelle für Mine-Status pro Spieler

    		statement.executeUpdate(
    				"CREATE TABLE IF NOT EXISTS clashmc_mine (" +
    						"king_id INT PRIMARY KEY NOT NULL," +
    						"pickaxe_level INT DEFAULT 1," +
    						"next_mine_available TIMESTAMP NULL," +

            			    "booster_1_type VARCHAR(32) DEFAULT NULL," +
            			    "booster_1_expires_at TIMESTAMP NULL," +

            			    "booster_2_type VARCHAR(32) DEFAULT NULL," +
            			    "booster_2_expires_at TIMESTAMP NULL," +

            			    "booster_3_type VARCHAR(32) DEFAULT NULL," +
            			    "booster_3_expires_at TIMESTAMP NULL," +

            			    "FOREIGN KEY (king_id) REFERENCES kgmg_players(id) ON DELETE CASCADE" +
            			    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
    				);
    		
    		
    		//Dynamische Itemtabelle: speichert beliebige Materialien und Mengen pro Spieler
    		statement.executeUpdate(
    				"CREATE TABLE IF NOT EXISTS clashmc_mine_rewards (" +
    					    "king_id INT NOT NULL," +
    					    "material VARCHAR(64) NOT NULL," +
    					    "amount INT NOT NULL DEFAULT 0," +

    					    "PRIMARY KEY (king_id, material)," +
    					    "FOREIGN KEY (king_id) REFERENCES kgmg_players(id) ON DELETE CASCADE" +
    					") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
    				);
    		
    		// Angriffe (online/offline)
    		statement.executeUpdate(
    		    "CREATE TABLE IF NOT EXISTS kgmg_attacks (" +
    		        "id INT AUTO_INCREMENT PRIMARY KEY," +
    		        "attacker_id INT NOT NULL," +
    		        "defender_id INT NOT NULL," +
    		        "is_online BOOLEAN NOT NULL," +
    		        "damage_percent DECIMAL(5,2) NOT NULL DEFAULT 0.0," +
    		        "loot_clash_coins BIGINT DEFAULT 0," +
    		        "loot_king_coins BIGINT DEFAULT 0," +
    		        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
    		        "FOREIGN KEY (attacker_id) REFERENCES kgmg_players(id) ON DELETE CASCADE," +
    		        "FOREIGN KEY (defender_id) REFERENCES kgmg_players(id) ON DELETE CASCADE" +
    		    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
    		);

    		// Angriff-Replays (nur offline relevant)
    		statement.executeUpdate(
    		    "CREATE TABLE IF NOT EXISTS kgmg_attack_replays (" +
    		        "attack_id INT PRIMARY KEY," +
    		        "replay_data LONGTEXT NOT NULL," +
    		        "FOREIGN KEY (attack_id) REFERENCES kgmg_attacks(id) ON DELETE CASCADE" +
    		    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
    		);
    		
    		// Migration: Füge movement_data Spalte hinzu (falls nicht vorhanden)
    		try {
    		    statement.executeUpdate(
    		        "ALTER TABLE kgmg_attack_replays ADD COLUMN movement_data LONGTEXT"
    		    );
    		    logger.info("[MIGRATION] movement_data Spalte zu kgmg_attack_replays hinzugefügt");
    		} catch (SQLException e) {
    		    // Spalte existiert bereits oder anderer Fehler
    		    if (!e.getMessage().contains("Duplicate column")) {
    		        logger.warning("[MIGRATION] Fehler beim Hinzufügen von movement_data: " + e.getMessage());
    		    }
    		}

    		// Online-Angriffsstatus
    		statement.executeUpdate(
    		    "CREATE TABLE IF NOT EXISTS kgmg_attack_optin (" +
    		        "king_id INT PRIMARY KEY," +
    		        "is_online_enabled BOOLEAN NOT NULL DEFAULT FALSE," +
    		        "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
    		        "FOREIGN KEY (king_id) REFERENCES kgmg_players(id) ON DELETE CASCADE" +
    		    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
    		);
    		

    		logger.info("[MIGRATION] Migration abgeschlossen.");
    	} catch (SQLException e) {
    		logger.severe("[MIGRATION] Fehler bei der Migration: " + e.getMessage());
    		e.printStackTrace();
    	}
    }
}