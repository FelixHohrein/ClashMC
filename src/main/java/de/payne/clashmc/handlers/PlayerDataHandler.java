package de.payne.clashmc.handlers;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.database.modules.PlayerDatabase;
import de.payne.clashmc.database.modules.PlayerResourcesDatabase;
import de.payne.clashmc.database.modules.VillageDatabase;
import de.payne.clashmc.maphandling.schematic.VillageBuilder;
import de.payne.clashmc.utils.LogUtil;
import org.bukkit.Bukkit;

import org.bukkit.entity.Player;




import java.sql.SQLException;
import java.util.UUID;

public class PlayerDataHandler {

    private final UUID uuid;
    private final PlayerDatabase playerDB;
    private final VillageDatabase villageDB;
    private final PlayerResourcesDatabase resourcesDB;
    private final VillageBuilder builder;
    private final Player player;
    private int kingId;

    public PlayerDataHandler(UUID uuid) {
        this.uuid = uuid;
        this.playerDB = ClashMC.getInstance().getDatabaseManager().players();
        this.villageDB = ClashMC.getInstance().getDatabaseManager().villages();
        this.resourcesDB = ClashMC.getInstance().getDatabaseManager().resources();
        this.builder = new VillageBuilder(ClashMC.getInstance(), ClashMC.getInstance().getSchematicManager(), ClashMC.getInstance().getVillageAllocator());
        this.player = Bukkit.getPlayer(uuid);
        
        try {
			this.kingId = playerDB.getPlayerIdByUUID(uuid);
		} catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(),"Fehler: Spieler-ID nicht gefunden.");
			e.printStackTrace();
		}
    }

    
    //Datenbank create or update (only language) player in kgmg_player
    public void createOrUpdatePlayer(String language) {
        try {
        	this.playerDB.createOrUpdatePlayer(uuid, language);
        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "Fehler beim Erstellen/Aktualisieren des Spielers: " + e.getMessage());
        }    
    }

    //returns true of player exist in kgmg_player
    public boolean playerExists() {
        return playerDB.playerExists(uuid);
    }
    
    //create player in 
    public void createVillageForPlayer() {
        try {
            villageDB.createVillage(this.kingId);
            LogUtil.logInfo(ClashMC.getInstance(), "Dorf für Spieler " + uuid + " erfolgreich erstellt.");

            // Weitere Initialisierungen...

        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "Fehler beim Erstellen des Dorfes für Spieler " + uuid + ": " + e.getMessage());
        }
    }

    public void upgradeVillage() {
        try {
           
            if (!villageDB.villageExists(this.kingId)) {
                player.sendMessage("§cDu hast noch kein Dorf. Es wird jetzt erstellt.");
                villageDB.createVillage(this.kingId);
            } else {
                villageDB.upgradeVillage(this.kingId);
            }

            int newLevel = villageDB.getVillageLevel(this.kingId);
            this.builder.upgradeVillage(player, newLevel);

        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "Fehler beim Upgraden des Dorfes: " + e.getMessage());
            player.sendMessage("§cInterner Fehler beim Upgraden des Dorfes.");
        }
    }

    public void resetVillageToLevel1() {
        try {
            if (!villageDB.villageExists(this.kingId)) {
                player.sendMessage("§cDu hast noch kein Dorf.");
                return;
            }

            villageDB.resetVillage(this.kingId);
            player.sendMessage("§aDein Dorf wurde zurückgesetzt.");
            this.builder.buildVillage(player, this.villageDB.getVillageLevel(this.kingId));

        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "Fehler beim Zurücksetzen des Dorfes: " + e.getMessage());
            player.sendMessage("§cInterner Fehler beim Zurücksetzen des Dorfes.");
        }
    }
}