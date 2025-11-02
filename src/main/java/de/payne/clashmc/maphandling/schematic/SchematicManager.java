package de.payne.clashmc.maphandling.schematic;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;

import de.payne.clashmc.utils.LogUtil;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SchematicManager {

    private final Plugin plugin;
    private final File schematicFolder;

    public SchematicManager(Plugin plugin) {
        this.plugin = plugin;
        this.schematicFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicFolder.exists()) {
            schematicFolder.mkdirs();
        }
    }

    /**
     * Speichert die aktuell vom Spieler selektierte Region als Schematic unter dem angegebenen Namen.
     */
    public void saveSchematic(Player player, int level) {
        try {
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
            RegionSelector selector = session.getRegionSelector(BukkitAdapter.adapt(player.getWorld()));

            if (selector == null) {
                player.sendMessage(ChatColor.RED + "Du hast keine Region ausgewählt.");
                return;
            }

            Region region = selector.getRegion();
            if (region == null) {
                player.sendMessage(ChatColor.RED + "Region ist ungültig oder nicht vollständig ausgewählt.");
                return;
            }

            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            World adaptedWorld = BukkitAdapter.adapt(player.getWorld());

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(adaptedWorld)) {
                ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
                copy.setCopyingBiomes(true);
                copy.setCopyingEntities(true);
                Operations.complete(copy);
            }

            File file = new File(schematicFolder, "village_level_" + level + ".schem");

            // Nutze das Format basierend auf Dateiendung, falls unbekannt, fallback auf Sponge Schematic
            ClipboardFormat format = ClipboardFormats.findByFile(file);
            if (format == null) {
                format = BuiltInClipboardFormat.SPONGE_SCHEMATIC;
            }

            try (ClipboardWriter writer = format.getWriter(new FileOutputStream(file))) {
                writer.write(clipboard);
                player.sendMessage(ChatColor.GREEN + "Schematic erfolgreich gespeichert als §e" + file.getName());
                LogUtil.logInfo(plugin, "Schematic gespeichert: " + file.getAbsolutePath());
            }

        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Fehler beim Speichern der Schematic: " + e.getMessage());
            LogUtil.logError(plugin, "Fehler beim Speichern der Schematic: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Lädt eine Schematic aus dem Plugin-Ordner und fügt sie an der gegebenen Location ein.
     */
    public void pasteSchematic(Plugin plugin, Location location, int level) {
        File file = new File(schematicFolder, "village_level_" + level + ".schem");
        if (!file.exists()) {
            LogUtil.logError(plugin, "Schematic nicht gefunden: " + file.getAbsolutePath());
            return;
        }

        try (ClipboardReader reader = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(new FileInputStream(file))) {
            Clipboard clipboard = reader.read();
            World adaptedWorld = BukkitAdapter.adapt(location.getWorld());

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(adaptedWorld)) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()))
                        .ignoreAirBlocks(false)
                        .build();

                Operations.complete(operation);
                editSession.flushSession();

                LogUtil.logInfo(plugin, "Schematic Level" + level + "' erfolgreich eingefügt bei " + location);

            } catch (Exception e) {
                LogUtil.logError(plugin, "Fehler beim EditSession-Vorgang: " + e.getMessage());
                e.printStackTrace();
            }
            
            
            this.clearWorldEntities(Bukkit.getWorld("Clash"));
        } catch (Exception e) {
            LogUtil.logError(plugin, "Fehler beim Laden der Schematic: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Lädt eine Schematic aus dem Plugin-Ordner und fügt sie an der gegebenen Location ein + Fügt eine schicht Bedrock darunter.
     */
    public void pasteSchematicWithBedrock(Plugin plugin, Location location, int level) {
        File file = new File(schematicFolder, "village_level_" + level + ".schem");
        if (!file.exists()) {
            LogUtil.logError(plugin, "Schematic nicht gefunden: " + file.getAbsolutePath());
            return;
        }

        try (ClipboardReader reader = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(new FileInputStream(file))) {
            Clipboard clipboard = reader.read();
            World adaptedWorld = BukkitAdapter.adapt(location.getWorld());

            // Berechne Ursprung & Bereich
            BlockVector3 origin = clipboard.getRegion().getMinimumPoint();
            int width = clipboard.getRegion().getWidth();
            int length = clipboard.getRegion().getLength();

            int pasteX = location.getBlockX();
            int pasteY = location.getBlockY();
            int pasteZ = location.getBlockZ();

            int bedrockY = pasteY - 1;

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(adaptedWorld)) {

                // ✅ Bedrock-Schicht unterhalb des Schematics setzen
                for (int x = 0; x < width; x++) {
                    for (int z = 0; z < length; z++) {
                        BlockVector3 pos = BlockVector3.at(pasteX + x, bedrockY, pasteZ + z);
                        editSession.setBlock(pos, BukkitAdapter.adapt(Material.BEDROCK.createBlockData()));
                    }
                }

                // 📦 Schematic einfügen
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(pasteX, pasteY, pasteZ))
                        .ignoreAirBlocks(false)
                        .build();

                Operations.complete(operation);
                editSession.flushSession();

                LogUtil.logInfo(plugin, "Schematic (mit Bedrock) Level " + level + " erfolgreich eingefügt bei " + location);

            } catch (Exception e) {
                LogUtil.logError(plugin, "Fehler beim Einfügen mit Bedrock: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            LogUtil.logError(plugin, "Fehler beim Laden der Schematic: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Gibt eine ArrayList<Integer> mit allen möglichen Leveln zurück
     */
    
    public List<Integer> getAvailableLevels() {
        File[] files = schematicFolder.listFiles((dir, name) -> name.startsWith("village_level_") && name.endsWith(".schem"));
        if (files == null) return Collections.emptyList();

        List<Integer> levels = new ArrayList<>();
        for (File file : files) {
            try {
                String name = file.getName().replace("village_level_", "").replace(".schem", "");
                levels.add(Integer.parseInt(name));
            } catch (NumberFormatException e) {
                LogUtil.logError(plugin, "Ungültiger Level-Dateiname: " + file.getName());
            }
        }

        Collections.sort(levels);
        return levels;
    }
    
    
    public Clipboard loadSchematic(int level) {
        File file = new File(schematicFolder, "village_level_" + level + ".schem");
        if (!file.exists()) {
            LogUtil.logError(plugin, "Schematic nicht gefunden: " + file.getAbsolutePath());
            return null;
        }

        try (ClipboardReader reader = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(new FileInputStream(file))) {
            return reader.read();
        } catch (IOException e) {
            LogUtil.logError(plugin, "Fehler beim Laden der Schematic (Clipboard): " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    
    private void clearWorldEntities(org.bukkit.World welt) {
        for (Entity entity : welt.getEntities()) {
            // Entferne alle Drops (Items auf dem Boden) und lebende Mobs/Tiere
            if (entity instanceof Item || entity instanceof LivingEntity && !(entity instanceof Player)) {
                entity.remove();
            }
        }
    }
}