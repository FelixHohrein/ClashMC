package de.payne.clashmc.listeners.player;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

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
        
        // Grundlegende Eigenschaften setzen (synchron, Bukkit-API)
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setInvulnerable(true);

        // ASYNC: Lade/Erstelle Spielerdaten
        databaseManager.players().getPlayerIdByUUIDAsync(uuid)
            .thenAccept(playerId -> {
                // Spieler existiert nicht → neu anlegen
                if (playerId == -1) {
                    databaseManager.players().createOrUpdatePlayerAsync(uuid, "de")
                        .thenCompose(v -> databaseManager.players().getPlayerIdByUUIDAsync(uuid))
                        .thenAccept(newPlayerId -> {
                            if (newPlayerId != -1) {
                                initializeNewPlayer(player, uuid, newPlayerId);
                            }
                        })
                        .exceptionally(throwable -> {
                            LogUtil.logError(plugin, "Fehler beim Erstellen neuer Spieler: " + throwable.getMessage());
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                player.sendMessage("§cFehler beim Laden deines Dorfes.");
                            });
                            return null;
                        });
                } else {
                    // Spieler existiert → prüfe Village, Resources, etc.
                    initializeExistingPlayer(player, uuid, playerId);
                }
            })
            .exceptionally(throwable -> {
                LogUtil.logError(plugin, "Fehler beim Join-Handling: " + throwable.getMessage());
                return null;
            });
    }

    private void initializeNewPlayer(Player player, UUID uuid, int playerId) {
        // ASYNC: Erstelle Village
        databaseManager.villages().createVillageAsync(playerId)
            .thenCompose(v -> databaseManager.villages().getVillageLevelAsync(playerId))
            .thenAccept(level -> {
                // Village-Build muss auf Main-Thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    villageBuilder.buildVillage(player, level);
                    player.sendMessage("§aDir wurde ein eigenes Dorf zugewiesen!");
                });
            })
            .exceptionally(throwable -> {
                LogUtil.logError(plugin, "Fehler beim Erstellen des Dorfes: " + throwable.getMessage());
                return null;
            });

        // ASYNC: Erstelle Resources
        try {
            databaseManager.resources().createResources(playerId, new Timestamp(System.currentTimeMillis()));
        } catch (SQLException e) {
            LogUtil.logError(plugin, "Fehler beim Erstellen der Ressourcen: " + e.getMessage());
        }

        // ASYNC: Erstelle Mine-Daten
        try {
            databaseManager.mine().createIfNotExist(playerId);
        } catch (SQLException e) {
            LogUtil.logError(plugin, "Fehler beim Erstellen der Mine-Daten: " + e.getMessage());
        }

        // ASYNC: Erstelle Attack-OptIn
        databaseManager.attacks().createOptInIfNotExists(playerId);

        // Teleportiere zum Dorf
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.teleport(allocator.getVillageCenterTeleportOrSpawnLocation(uuid));
        }, 20L); // 1 Sekunde Delay für Village-Build
    }

    private void initializeExistingPlayer(Player player, UUID uuid, int playerId) {
        // ASYNC: Prüfe ob Village existiert
        databaseManager.villages().villageExistsAsync(playerId)
            .thenAccept(exists -> {
                if (!exists) {
                    // Village existiert nicht → erstelle es
                    initializeNewPlayer(player, uuid, playerId);
                } else {
                    // Village existiert → prüfe ob in File
                    if (!allocator.getAllVillageSpawns().containsKey(uuid)) {
                        databaseManager.villages().getVillageLevelAsync(playerId)
                            .thenAccept(level -> {
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    villageBuilder.buildVillage(player, level);
                                });
                            });
                    }
                }
                
                // Teleportiere zum Dorf (auf Main-Thread)
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.teleport(allocator.getVillageCenterTeleportOrSpawnLocation(uuid));
                });
            })
            .exceptionally(throwable -> {
                LogUtil.logError(plugin, "Fehler beim Prüfen des Villages: " + throwable.getMessage());
                return null;
            });

        // ASYNC: Prüfe Resources, Mine, Attack-OptIn
        databaseManager.resources().playerResourcesExistAsync(playerId)
            .thenAccept(exists -> {
                if (!exists) {
                    try {
                        databaseManager.resources().createResources(playerId, new Timestamp(System.currentTimeMillis()));
                    } catch (SQLException e) {
                        LogUtil.logError(plugin, "Fehler beim Erstellen der Ressourcen: " + e.getMessage());
                    }
                }
            });

        try {
            if (!databaseManager.mine().hasMineData(playerId)) {
                databaseManager.mine().createIfNotExist(playerId);
            }
        } catch (SQLException e) {
            LogUtil.logError(plugin, "Fehler beim Prüfen der Mine-Daten: " + e.getMessage());
        }

        if (!databaseManager.attacks().existsInOptinTable(playerId)) {
            databaseManager.attacks().createOptInIfNotExists(playerId);
        }
    }
}
