package de.payne.clashmc.mine;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import com.google.common.collect.Maps;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.utils.ItemStackUtil;
import de.payne.clashmc.utils.LogUtil;
import lombok.Getter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class MineInstance {

    private final Plugin plugin;
    private final Player player;
    
    @Getter
    private final Location origin;
    
    @Getter
    private int actionbarTaskId = -1;
    
    @Getter
    private int timerTaskId = -1;
    
    private final MineManager mineManager;
    private final int timeToMine = 10;
    private static final Vector RELATIVE_TELEPORT_OFFSET = new Vector(9, 10, 29);
    
    @Getter
    private Map<MineMaterialType, Integer> collectedItems;

    public MineInstance(Plugin plugin, Player player, Location origin, MineManager mineManager) {
        this.plugin = plugin;
        this.player = player;
        this.origin = origin;
        this.mineManager = mineManager;
        this.collectedItems = Maps.newHashMap();
    }

    public void start() {
        // Hole MineSchematicManager und lade Schematic
        MineSchematicManager schematicManager = ClashMC.getInstance().getMineSchematicManager();
        Clipboard clipboard = schematicManager.loadMineSchematic();

        if (clipboard == null) {
            player.sendMessage("§cFehler beim Laden der Mine-Schematic.");
            mineManager.endMineSession(player);
            return;
        }

        // Hole villageLevel aus der DB (try/catch wegen DB-Ausnahme)
        int villageLevel = 1;
        int maxLevel = 1;
        int kingId = -1;
        try {
            kingId = ClashMC.getInstance().getDatabaseManager().players().getPlayerIdByUUID(player.getUniqueId());
            villageLevel = ClashMC.getInstance().getDatabaseManager().villages().getVillageLevel(kingId);

            // maxLevel aus SchematicManager (Liste der verfügbaren Level)
            List<Integer> availableLevels = ClashMC.getInstance().getSchematicManager().getAvailableLevels();
            maxLevel = availableLevels.isEmpty() ? 1 : Collections.max(availableLevels);
        } catch (Exception e) {
            LogUtil.logError(plugin, "Fehler beim Ermitteln des Dorflevels für Spieler " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }

        // Ressourcen in Stone-Blöcken ersetzen (villageLevel und maxLevel nutzen)
        Map<Material, Double> upgradeMultipliers = new HashMap<>(); 
        if (ClashMC.getInstance().getDatabaseManager().mine().getActiveBoosters(kingId).contains(MineBoosterType.RESOURCE_MULTIPLIER)) {
            upgradeMultipliers.put(Material.COAL_ORE, 3.0);
            upgradeMultipliers.put(Material.IRON_ORE, 2.7);
            upgradeMultipliers.put(Material.GOLD_ORE, 2.5);
            upgradeMultipliers.put(Material.DIAMOND_ORE, 2.2);
            upgradeMultipliers.put(Material.EMERALD_ORE, 10.0);
        }
        MineResourceReplacer.replaceStoneBlocks(clipboard, villageLevel, maxLevel, upgradeMultipliers);

        // FIX: Chunks VORHER laden und BEHALTEN (nicht sofort unloaden!)
        World world = origin.getWorld();
        if (world != null) {
            BlockVector3 dimensions = clipboard.getDimensions();
            int chunkRadiusX = (int) Math.ceil(dimensions.getX() / 16.0);
            int chunkRadiusZ = (int) Math.ceil(dimensions.getZ() / 16.0);
            
            int chunkX = origin.getBlockX() >> 4;
            int chunkZ = origin.getBlockZ() >> 4;

            // Lade Chunks synchron VOR dem Schematic-Paste
            for (int dx = -chunkRadiusX; dx <= chunkRadiusX; dx++) {
                for (int dz = -chunkRadiusZ; dz <= chunkRadiusZ; dz++) {
                    world.loadChunk(chunkX + dx, chunkZ + dz, true);
                }
            }
            LogUtil.logDebug(plugin, "[MineInstance] " + ((chunkRadiusX * 2 + 1) * (chunkRadiusZ * 2 + 1)) + " Chunks geladen");
        }

        // Mine in Welt einfügen (WorldEdit EditSession)
        try (EditSession editSession = WorldEdit.getInstance()
                .getEditSessionFactory()
                .getEditSession(BukkitAdapter.adapt(origin.getWorld()), -1)) {

            editSession.setFastMode(false); // Chunks korrekt synchronisieren

            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ()))
                    .ignoreAirBlocks(false)
                    .build();
            
            Operations.complete(operation);
            LogUtil.logDebug(plugin, "[MineInstance] Schematic erfolgreich eingefügt");

        } catch (WorldEditException e) {
            LogUtil.logError(plugin, "Fehler beim Einfügen der Mine-Schematic: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cFehler beim Einfügen der Mine!");
            mineManager.endMineSession(player);
            return;
        }

        // Spieler teleportieren
        Location teleportLocation = origin.clone().add(RELATIVE_TELEPORT_OFFSET);
        player.teleport(teleportLocation);
        
        // Leave Item - Slot 8 (ganz rechts)
        player.getInventory().setItem(8, this.teleporterItemStack());
        
        // Spitzhacke
        int pickaxeLevel = ClashMC.getInstance().getDatabaseManager().mine().getPickaxeLevel(kingId);
        if (ClashMC.getInstance().getDatabaseManager().mine().getActiveBoosters(kingId).contains(MineBoosterType.PICKAXE_LEVEL_PLUS)) {
            pickaxeLevel += 20;
        }
        player.getInventory().setItem(0, ItemStackUtil.createPickaxeWithUpgradeLevel(pickaxeLevel));
        
        player.sendMessage("§aWillkommen in deiner Mine! Du hast " + timeToMine + " Minuten Zeit.");
        int totalSeconds = timeToMine * 60;
        AtomicInteger secondsLeft = new AtomicInteger(totalSeconds);

        // ActionBar - Timer
        this.actionbarTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            // Prüfe ob Player noch online ist
            if (!player.isOnline()) {
                Bukkit.getScheduler().cancelTask(actionbarTaskId);
                return;
            }
            
            int remaining = secondsLeft.getAndDecrement();
            if (remaining <= 0) {
                Bukkit.getScheduler().cancelTask(actionbarTaskId);
                return;
            }

            int minutes = remaining / 60;
            int seconds = remaining % 60;
            String timeFormatted = String.format("§eVerbleibende Zeit: §a%02d:%02d", minutes, seconds);

            try {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(timeFormatted));
            } catch (Exception e) {
                // Player könnte disconnected sein
                LogUtil.logDebug(plugin, "[MineInstance] Konnte ActionBar nicht senden: " + e.getMessage());
                Bukkit.getScheduler().cancelTask(actionbarTaskId);
            }
        }, 0L, 20L); // alle 20 Ticks = 1 Sekunde
        
        // Timer zum Beenden starten (10 Minuten = 20 Ticks * 60 Sekunden * timeToMine)
        this.timerTaskId = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.sendMessage("§cDeine Minenzeit ist abgelaufen. Du wurdest zurück teleportiert.");
            }
            mineManager.endMineSession(player.getUniqueId());
        }, 20L * 60 * timeToMine).getTaskId();
    }

    public void cleanup() {
        MineSchematicManager schematicManager = ClashMC.getInstance().getMineSchematicManager();
        Clipboard clipboard = schematicManager.loadMineSchematic();

        if (clipboard == null) {
            LogUtil.logError(plugin, "[MineInstance] Cleanup - clipboard == null!");
            return;
        }
        
        World world = origin.getWorld();
        if (world == null) {
            LogUtil.logError(plugin, "[MineInstance] Cleanup - World für origin ist null");
            return;
        }

        BlockVector3 min = clipboard.getMinimumPoint();
        BlockVector3 max = clipboard.getMaximumPoint();

        LogUtil.logDebug(plugin, "[MineInstance] Lösche Mine Blöcke von " + min + " bis " + max);

        // Lösche Blöcke async für bessere Performance
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int y = min.getY(); y <= max.getY(); y++) {
                    for (int z = min.getZ(); z <= max.getZ(); z++) {
                        // Koordinaten relativ zum Schematic Ursprung
                        int relX = x - min.getX();
                        int relY = y - min.getY();
                        int relZ = z - min.getZ();

                        final int finalX = relX;
                        final int finalY = relY;
                        final int finalZ = relZ;
                        
                        // Setze Blöcke sync
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Location loc = origin.clone().add(finalX, finalY, finalZ);
                            loc.getBlock().setType(Material.AIR);
                        });
                    }
                }
            }
            LogUtil.logDebug(plugin, "[MineInstance] Mine wurde erfolgreich gelöscht");
        });
    }
    
    private ItemStack teleporterItemStack() {
        ItemStack leaveItem = new ItemStack(Material.COMPASS);
        ItemMeta meta = leaveItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cMine verlassen");
            leaveItem.setItemMeta(meta);
        }
        return leaveItem;
    }
}
