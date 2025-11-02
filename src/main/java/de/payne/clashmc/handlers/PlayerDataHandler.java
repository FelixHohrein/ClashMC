package de.payne.clashmc.handlers;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.database.modules.PlayerDatabase;
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
    private final VillageBuilder builder;
    private final int kingId;

    public PlayerDataHandler(UUID uuid) {
        this.uuid = uuid;
        this.playerDB = ClashMC.getInstance().getDatabaseManager().players();
        this.villageDB = ClashMC.getInstance().getDatabaseManager().villages();
        this.builder = new VillageBuilder(ClashMC.getInstance(), 
                ClashMC.getInstance().getSchematicManager(), 
                ClashMC.getInstance().getVillageAllocator());
        
        int tempKingId = -1;
        try {
            tempKingId = playerDB.getPlayerIdByUUID(uuid);
        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "Fehler beim Laden der Spieler-ID: " + e.getMessage());
            e.printStackTrace();
        }
        this.kingId = tempKingId;
    }

    // Datenbank create or update (only language) player in kgmg_player
    public void createOrUpdatePlayer(String language) {
        try {
            this.playerDB.createOrUpdatePlayer(uuid, language);
        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "Fehler beim Erstellen/Aktualisieren des Spielers: " + e.getMessage());
            e.printStackTrace();
        }    
    }

    // Returns true if player exists in kgmg_player
    public boolean playerExists() {
        return playerDB.playerExists(uuid);
    }
    
    // Create player in database
    public void createVillageForPlayer() {
        if (kingId == -1) {
            LogUtil.logError(ClashMC.getInstance(), "Kann Dorf nicht erstellen: Ungültige king_id");
            return;
        }
        
        try {
            villageDB.createVillage(this.kingId);
            LogUtil.logInfo(ClashMC.getInstance(), "Dorf für Spieler " + uuid + " erfolgreich erstellt.");
        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "Fehler beim Erstellen des Dorfes für Spieler " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void upgradeVillage() {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            LogUtil.logError(ClashMC.getInstance(), "Spieler ist nicht online für Village-Upgrade");
            return;
        }
        
        if (kingId == -1) {
            player.sendMessage("§cFehler: Deine Spielerdaten konnten nicht geladen werden.");
            return;
        }
        
        try {
            if (!villageDB.villageExists(this.kingId)) {
                player.sendMessage("§cDu hast noch kein Dorf. Es wird jetzt erstellt.");
                villageDB.createVillage(this.kingId);
            } else {
                villageDB.upgradeVillage(this.kingId);
            }

            int newLevel = villageDB.getVillageLevel(this.kingId);
            // Invalidate cache after upgrade
            ClashMC.getInstance().getCacheManager().invalidateVillageLevel(this.kingId);
            this.builder.upgradeVillage(player, newLevel);
        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "Fehler beim Upgraden des Dorfes: " + e.getMessage());
            player.sendMessage("§cInterner Fehler beim Upgraden des Dorfes.");
            e.printStackTrace();
        }
    }

    public void resetVillageToLevel1() {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            LogUtil.logError(ClashMC.getInstance(), "Spieler ist nicht online für Village-Reset");
            return;
        }
        
        if (kingId == -1) {
            player.sendMessage("§cFehler: Deine Spielerdaten konnten nicht geladen werden.");
            return;
        }
        
        try {
            if (!villageDB.villageExists(this.kingId)) {
                player.sendMessage("§cDu hast noch kein Dorf.");
                return;
            }

            villageDB.resetVillage(this.kingId);
            // Invalidate cache after reset
            ClashMC.getInstance().getCacheManager().invalidateVillageLevel(this.kingId);
            ClashMC.getInstance().getCacheManager().invalidateResources(uuid);
            player.sendMessage("§aDein Dorf wurde zurückgesetzt.");
            this.builder.buildVillage(player, this.villageDB.getVillageLevel(this.kingId));
        } catch (SQLException e) {
            LogUtil.logError(ClashMC.getInstance(), "Fehler beim Zurücksetzen des Dorfes: " + e.getMessage());
            player.sendMessage("§cInterner Fehler beim Zurücksetzen des Dorfes.");
            e.printStackTrace();
        }
    }
}
