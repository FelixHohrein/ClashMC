package de.payne.clashmc.mine;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
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
import lombok.Setter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;



public class MineInstance {

    private final Plugin plugin;
    private final Player player;
    
    @Getter
    private final Location origin;
    
    @Getter
    private int actionbarTaskId = -1;
    
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
            plugin.getLogger().warning("Fehler beim Ermitteln des Dorflevels für Spieler " + player.getName());
            e.printStackTrace();
        }

        // Ressourcen in Stone-Blöcken ersetzen (villageLevel und maxLevel nutzen)
        Map<Material, Double> upgradeMultipliers = new HashMap<>(); 
        if (ClashMC.getInstance().getDatabaseManager().mine().getActiveBoosters(kingId).contains(MineBoosterType.RESOURCE_MULTIPLIER)) { //multiplikator
            upgradeMultipliers.put(Material.COAL_ORE, 3.0);
            upgradeMultipliers.put(Material.IRON_ORE, 2.7);
            upgradeMultipliers.put(Material.GOLD_ORE, 2.5);
            upgradeMultipliers.put(Material.DIAMOND_ORE, 2.2);
            upgradeMultipliers.put(Material.EMERALD_ORE, 10.0);
        }
        MineResourceReplacer.replaceStoneBlocks(clipboard, villageLevel, maxLevel, upgradeMultipliers);

        
     // Chunks rund um die Mine laden
        if (clipboard != null) {
            BlockVector3 dimensions = clipboard.getDimensions();
            int chunkRadiusX = (int) Math.ceil(dimensions.getX() / 16.0);
            int chunkRadiusZ = (int) Math.ceil(dimensions.getZ() / 16.0);
            
            World world = origin.getWorld();
            if (world != null) {
                int chunkX = origin.getBlockX() >> 4;
                int chunkZ = origin.getBlockZ() >> 4;

                for (int dx = -chunkRadiusX; dx <= chunkRadiusX; dx++) {
                    for (int dz = -chunkRadiusZ; dz <= chunkRadiusZ; dz++) {
                        world.loadChunk(chunkX + dx, chunkZ + dz, true);
                    }
                }
            }
            
            int chunkX = origin.getBlockX() >> 4;
            int chunkZ = origin.getBlockZ() >> 4;

            for (int dx = -chunkRadiusX; dx <= chunkRadiusX; dx++) {
                for (int dz = -chunkRadiusZ; dz <= chunkRadiusZ; dz++) {
                    Chunk chunk = world.getChunkAt(chunkX + dx, chunkZ + dz);
                    if (chunk.isLoaded()) {
                        world.unloadChunk(chunk);
                    }
                }
            }
        }

        // Mine in Welt einfügen (WorldEdit EditSession)
        try (EditSession editSession = WorldEdit.getInstance()
                .getEditSessionFactory()
                .getEditSession(BukkitAdapter.adapt(origin.getWorld()), -1)) {

            editSession.setFastMode(false); // <- wichtig: Chunks korrekt synchronisieren

            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ()))
                    .ignoreAirBlocks(false)
                    .build();
            
            Operations.complete(operation);

        } catch (WorldEditException e) {
            plugin.getLogger().severe("Fehler beim Einfügen der Mine-Schematic");
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
        
        player.sendMessage("§aWillkommen in deiner Mine! Du hast 10 Minuten Zeit.");
        int totalSeconds = timeToMine * 60;
        AtomicInteger secondsLeft = new AtomicInteger(totalSeconds);

        //action bar - timer
        this.actionbarTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            int remaining = secondsLeft.getAndDecrement();
            if (remaining <= 0) return;

            int minutes = remaining / 60;
            int seconds = remaining % 60;
            String timeFormatted = String.format("§eVerbleibende Zeit: §a%02d:%02d", minutes, seconds);

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(timeFormatted));
        }, 0L, 20L); // alle 20 Ticks = 1 Sekunde
        

        // Timer zum Beenden starten (10 Minuten = 20 Ticks * 60 Sekunden * 10)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            mineManager.endMineSession(player);
            player.sendMessage("§cDeine Minenzeit ist abgelaufen. Du wurdest zurück teleportiert.");
        }, 20L * 60 * timeToMine);
    }

    public void cleanup() {
        MineSchematicManager schematicManager = ClashMC.getInstance().getMineSchematicManager();
        Clipboard clipboard = schematicManager.loadMineSchematic();

        if (clipboard == null){
            LogUtil.logError(plugin, "Cleanup methode - clipboard == null!");
            return;
        }
        
        World world = origin.getWorld();
        if (world == null) {
            LogUtil.logError(plugin, "Cleanup methode - World für origin ist null");
            return;
        }

        BlockVector3 min = clipboard.getMinimumPoint();
        BlockVector3 max = clipboard.getMaximumPoint();

        LogUtil.logInfo(plugin,"[MineInstance] Lösche Mine Blöcke von " + min + " bis " + max);

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    // Koordinaten relativ zum Schematic Ursprung
                    int relX = x - min.getX();
                    int relY = y - min.getY();
                    int relZ = z - min.getZ();

                    Location loc = origin.clone().add(relX, relY, relZ);
                    loc.getBlock().setType(Material.AIR);
                }
            }
        }
        LogUtil.logInfo(plugin,"[MineInstance] Mine wurde erfolgreich gelöscht.");

    }
    
    private ItemStack teleporterItemStack() {
    	
        ItemStack leaveItem = new ItemStack(Material.COMPASS);
        ItemMeta meta = leaveItem.getItemMeta();
        meta.setDisplayName("§cMine verlassen");
        leaveItem.setItemMeta(meta);
        
        return leaveItem;
    }
}