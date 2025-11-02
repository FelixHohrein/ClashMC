package de.payne.clashmc.mine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;

import de.payne.clashmc.utils.LogUtil;

public class MineSchematicManager {

    private final Plugin plugin;
    private final File schematicFile;

    public MineSchematicManager(Plugin plugin) {
        this.plugin = plugin;
        File folder = new File(plugin.getDataFolder(), "schematics/mine");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.schematicFile = new File(folder, "mine.schem");
    }

    public void saveMineSchematic(Player player) {
        try {
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
            RegionSelector selector = session.getRegionSelector(BukkitAdapter.adapt(player.getWorld()));

            if (selector == null || !selector.isDefined()) {
                player.sendMessage(ChatColor.RED + "Du hast keine gültige Region ausgewählt.");
                return;
            }

            Region region = selector.getRegion();
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            World adaptedWorld = BukkitAdapter.adapt(player.getWorld());

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(adaptedWorld)) {
                ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
                copy.setCopyingBiomes(true);
                copy.setCopyingEntities(true);
                Operations.complete(copy);
            }

            ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
            if (format == null) {
                format = BuiltInClipboardFormat.SPONGE_SCHEMATIC;
            }

            try (ClipboardWriter writer = format.getWriter(new FileOutputStream(schematicFile))) {
                writer.write(clipboard);
                player.sendMessage(ChatColor.GREEN + "Mine-Schematic wurde gespeichert.");
                LogUtil.logInfo(plugin, "Mine-Schematic gespeichert: " + schematicFile.getAbsolutePath());
            }

        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Fehler beim Speichern der Mine-Schematic.");
            LogUtil.logError(plugin, "Fehler beim Speichern der Mine-Schematic: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void pasteMineSchematic(Location location) {
        if (!schematicFile.exists()) {
            LogUtil.logError(plugin, "Mine-Schematic nicht gefunden: " + schematicFile.getAbsolutePath());
            return;
        }

        try (ClipboardReader reader = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(new FileInputStream(schematicFile))) {
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

                LogUtil.logInfo(plugin, "Mine-Schematic erfolgreich eingefügt bei " + location);
            }

        } catch (Exception e) {
            LogUtil.logError(plugin, "Fehler beim Einfügen der Mine-Schematic: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Clipboard loadMineSchematic() {
        if (!schematicFile.exists()) {
            LogUtil.logError(plugin, "Mine-Schematic nicht gefunden: " + schematicFile.getAbsolutePath());
            return null;
        }

        try (ClipboardReader reader = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(new FileInputStream(schematicFile))) {
            return reader.read();
        } catch (IOException e) {
            LogUtil.logError(plugin, "Fehler beim Laden der Mine-Schematic: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public boolean hasSchematic() {
        return schematicFile.exists();
    }
}
