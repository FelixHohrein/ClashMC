package de.payne.clashmc.mine;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.utils.LogUtil;
import lombok.Getter;

public class MineManager {

    private final Plugin plugin;
    @Getter
    private final Map<UUID, MineInstance> activeMines = new HashMap<>();
    
    private final List<Location> availableMineOrigins = new ArrayList<>();

    private final int MINE_WIDTH = 22;  // Falls benötigt, aktuell nicht genutzt
    private final int MINE_LENGTH = 32; // Falls benötigt, aktuell nicht genutzt

    public MineManager(Plugin plugin) {
        this.plugin = plugin;
        generateMineOrigins();
    }

    private void generateMineOrigins() {
        World mineWorld = Bukkit.getWorld("mine");
        if (mineWorld == null) {
            plugin.getLogger().severe("Welt 'mine' wurde nicht gefunden!");
            return;
        }

        int spacing = 40; // Abstand zwischen Minen
        int countX = 10;
        int countZ = 10;

        for (int x = 0; x < countX; x++) {
            for (int z = 0; z < countZ; z++) {
                int baseX = x * spacing;
                int baseZ = z * spacing;
                Location origin = new Location(mineWorld, baseX, 50, baseZ); // Y=50 Höhe als Beispiel
                availableMineOrigins.add(origin);
            }
        }
    }

    public void startMineSession(Player player) {
        UUID playerId = player.getUniqueId();

        if (activeMines.containsKey(playerId)) {
            player.sendMessage("§cDu bist bereits in einer Mine!");
            return;
        }

        if (availableMineOrigins.isEmpty()) {
            player.sendMessage("§cEs sind momentan keine freien Minenplätze verfügbar. Bitte versuche es später erneut.");
            return;
        }

        Location mineOrigin = availableMineOrigins.remove(0);
        MineInstance instance = new MineInstance(plugin, player, mineOrigin, this);
        activeMines.put(playerId, instance);
        instance.start();
    }

    public void endMineSession(Player player) {
        endMineSession(player.getUniqueId());
    }

    public void endMineSession(UUID playerId) {
        MineInstance instance = activeMines.remove(playerId);
        
        if (instance != null) {
        	Player player = Bukkit.getPlayer(playerId);
            Bukkit.getScheduler().cancelTask(instance.getActionbarTaskId()); // stoppe Actionbar-Task

            //sammelt items vom invenatar in map
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null || item.getType() == Material.AIR || item.getType() == Material.COMPASS || item.getType() == Material.DIAMOND_PICKAXE) continue;

                MineMaterialType type = MineMaterialType.fromMaterial(item.getType());
                if (type != null) {
                	instance.getCollectedItems().put(type, instance.getCollectedItems().getOrDefault(type, 0) + item.getAmount());
                }
            }
                        
            int kingId = -1;
			try {
				kingId = ClashMC.getInstance().getDatabaseManager().players().getPlayerIdByUUID(playerId);
			} catch (SQLException e) {
	            LogUtil.logError(plugin,"[MineManager] Es konnte keine KING-ID gefunden werden für Spieler " + player.getName() + " (" + player.getUniqueId() + ")");
				e.printStackTrace();
			}
            
            for (Map.Entry<MineMaterialType, Integer> entry : instance.getCollectedItems().entrySet()) {
                MineMaterialType type = entry.getKey();
                int amount = entry.getValue();

                try {
                    ClashMC.getInstance().getDatabaseManager().mine().saveOrUpdateItem(kingId, type, amount);
                } catch (SQLException e) {
                    e.printStackTrace();
    	            LogUtil.logError(plugin,"[MineManager] Fehler beim Speichern von " + type.name() + "bei " +player.getName() + " (" + player.getUniqueId() + ")");
                    player.sendMessage("§cFehler beim Speichern, wende dich an ein Teammitglied.");
                }
            }

            player.sendMessage("§aDeine Mine-Session wurde gespeichert.");
            
        	if(player.isOnline()) {
        		player.teleport(ClashMC.getInstance().getVillageAllocator().getVillageCenterTeleportOrSpawnLocation(playerId));
        		player.getInventory().clear();
        	}
            // Ursprung der Mine wieder freigeben
            availableMineOrigins.add(instance.getOrigin());
            instance.cleanup();
        } else {
            LogUtil.logError(plugin,"[MineManager] Keine aktive Mine für Spieler " + playerId + " gefunden.");
        }
    }

    public boolean isInMine(Player player) {
        return activeMines.containsKey(player.getUniqueId());
    }
    
    
}