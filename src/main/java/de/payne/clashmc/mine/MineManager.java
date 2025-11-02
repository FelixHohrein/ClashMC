package de.payne.clashmc.mine;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
                Location origin = new Location(mineWorld, baseX, 50, baseZ);
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
        if (player == null) {
            LogUtil.logError(plugin, "[MineManager] endMineSession aufgerufen mit null player");
            return;
        }
        endMineSession(player.getUniqueId());
    }

    public void endMineSession(UUID playerId) {
        MineInstance instance = activeMines.remove(playerId);
        
        if (instance == null) {
            LogUtil.logDebug(plugin, "[MineManager] Keine aktive Mine für Spieler " + playerId + " gefunden.");
            return;
        }
        
        // WICHTIG: ActionBar-Task canceln (auch wenn Player offline)
        if (instance.getActionbarTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(instance.getActionbarTaskId());
        }
        
        // Timer-Task canceln
        if (instance.getTimerTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(instance.getTimerTaskId());
        }
        
        Player player = Bukkit.getPlayer(playerId);
        
        // Wenn Player offline ist, trotzdem Cleanup durchführen
        if (player == null || !player.isOnline()) {
            LogUtil.logInfo(plugin, "[MineManager] Spieler " + playerId + " ist offline, führe trotzdem Cleanup durch");
            availableMineOrigins.add(instance.getOrigin());
            instance.cleanup();
            return;
        }
        
        // Player ist online - sammle Items und speichere
        // Sammle Items vom Inventar
        if (player.getInventory() != null) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null || item.getType() == Material.AIR || 
                    item.getType() == Material.COMPASS || item.getType() == Material.DIAMOND_PICKAXE) {
                    continue;
                }

                MineMaterialType type = MineMaterialType.fromMaterial(item.getType());
                if (type != null) {
                    instance.getCollectedItems().put(type, 
                        instance.getCollectedItems().getOrDefault(type, 0) + item.getAmount());
                }
            }
        }
        
        // ASYNC: Speichere Items in Datenbank
        ClashMC.getInstance().getDatabaseManager().players().getPlayerIdByUUIDAsync(playerId)
            .thenAccept(kingId -> {
                if (kingId == -1) {
                    LogUtil.logError(plugin, "[MineManager] Ungültige KING-ID für Spieler " + playerId);
                    return;
                }
                
                // Speichere alle gesammelten Items async
                for (Map.Entry<MineMaterialType, Integer> entry : instance.getCollectedItems().entrySet()) {
                    MineMaterialType type = entry.getKey();
                    int amount = entry.getValue();
                    
                    ClashMC.getInstance().getDatabaseManager().mine().saveOrUpdateItemAsync(kingId, type, amount)
                        .exceptionally(throwable -> {
                            LogUtil.logError(plugin, "[MineManager] Fehler beim Speichern von " + type.name() + ": " + throwable.getMessage());
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                if (player != null && player.isOnline()) {
                                    player.sendMessage("§cFehler beim Speichern von " + type.getDisplayName());
                                }
                            });
                            return null;
                        });
                }
            })
            .exceptionally(throwable -> {
                LogUtil.logError(plugin, "[MineManager] Fehler beim Laden der KING-ID: " + throwable.getMessage());
                return null;
            });

        // Teleportiere zurück und clear Inventar
        try {
            Location spawnLocation = ClashMC.getInstance().getVillageAllocator().getVillageCenterTeleportOrSpawnLocation(playerId);
            if (spawnLocation != null) {
                player.teleport(spawnLocation);
            }
            player.getInventory().clear();
            player.setGameMode(GameMode.ADVENTURE);
            player.sendMessage("§aDeine Mine-Session wurde gespeichert.");
        } catch (Exception e) {
            LogUtil.logError(plugin, "[MineManager] Fehler beim Teleportieren des Spielers: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Ursprung der Mine wieder freigeben und Cleanup
        availableMineOrigins.add(instance.getOrigin());
        instance.cleanup();
    }

    public boolean isInMine(Player player) {
        return player != null && activeMines.containsKey(player.getUniqueId());
    }
    
    public boolean isInMine(UUID playerId) {
        return activeMines.containsKey(playerId);
    }
}
