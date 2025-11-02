package de.payne.clashmc.listeners.player;


import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.database.DatabaseManager;
import de.payne.clashmc.maphandling.schematic.VillageAllocator;
import de.payne.clashmc.maphandling.schematic.VillageBuilder;
import de.payne.clashmc.utils.LogUtil;

public class PlayerJoinListener implements Listener {

	private final ClashMC plugin;
	private final DatabaseManager databaseManager;
	private final VillageAllocator allocator;
	private final VillageBuilder villageBuilder;

	public PlayerJoinListener(ClashMC plugin) {
		this.plugin = plugin;
		this.databaseManager = plugin.getDatabaseManager();
		this.allocator = plugin.getVillageAllocator();
		this.villageBuilder = new VillageBuilder(plugin, plugin.getSchematicManager(), allocator);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		// Grundlegende Eigenschaften
	    player.setGameMode(GameMode.ADVENTURE);
	    player.setHealth(player.getMaxHealth());
	    player.setFoodLevel(20);
	    player.setSaturation(20f);
	    player.setInvulnerable(true); // Kein Schaden durch z. B. Fallschaden oder Mobs
		try {
			int playerId = databaseManager.players().getPlayerIdByUUID(uuid);

			// Spieler existiert nicht → neu anlegen in db struktur
			if (playerId == -1) {
				databaseManager.players().createOrUpdatePlayer(uuid, "de"); // Sprache ggf. dynamisch wählen
				playerId = databaseManager.players().getPlayerIdByUUID(uuid); // ID nachziehen
			}

			// Dorfprüfung und ggf. erstellen (auch angelegt in village file)
			if (!databaseManager.villages().villageExists(playerId)) {
				databaseManager.villages().createVillage(playerId);
				villageBuilder.buildVillage(player, databaseManager.villages().getVillageLevel(playerId));
				player.sendMessage("§aDir wurde ein eigenes Dorf zugewiesen!");
			} else {
				LogUtil.logDebug(plugin, "Spieler " + player.getName() + " hat bereits ein Dorf.");
				if(!allocator.getAllVillageSpawns().containsKey(uuid)) {
					villageBuilder.buildVillage(player, databaseManager.villages().getVillageLevel(playerId));

				} else {
					System.out.println("Spieler steht in der VillageFile");
				}

			}
			if(!databaseManager.resources().playerResourcesExist(playerId)) {
				databaseManager.resources().createResources(playerId, new Timestamp(System.currentTimeMillis()));
			}
			
			if (!databaseManager.mine().hasMineData(playerId)) {
			    databaseManager.mine().createIfNotExist(playerId);
			}
			if (!databaseManager.attacks().existsInOptinTable(playerId)) {
				databaseManager.attacks().createOptInIfNotExists(playerId);
			}
					
		} catch (SQLException e) {
			LogUtil.logError(plugin, "Fehler beim Zuweisen des Dorfes für " + player.getName() + ": " + e.getMessage());
			player.sendMessage("§cFehler beim Laden deines Dorfes.");
		}
		//teleportiert in die mitte der eigenen village
		player.teleport(allocator.getVillageCenterTeleportOrSpawnLocation(uuid));
	}

}
