package de.payne.clashmc.replay;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.utils.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Verwaltet die Replay-Welt und die verfügbaren Replay-Slots.
 */
public class ReplayWorldManager {

    private final ClashMC plugin;
    private final List<Location> availableReplayOrigins = new ArrayList<>();
    private final boolean[][] slotsOccupied; // Grid-basierte Slot-Verwaltung
    
    private static final int MAX_X_SLOTS = 10;
    private static final int MAX_Z_SLOTS = 10;
    private static final int SPACING = 128; // Abstand zwischen Replays (gleich wie Attacks)
    
    public ReplayWorldManager(ClashMC plugin) {
        this.plugin = plugin;
        this.slotsOccupied = new boolean[MAX_X_SLOTS][MAX_Z_SLOTS];
        generateReplayOrigins();
    }
    
    private void generateReplayOrigins() {
        World replayWorld = Bukkit.getWorld("Replays");
        if (replayWorld == null) {
            LogUtil.logError(plugin, "Replay-Welt 'Replays' wurde nicht gefunden!");
            LogUtil.logError(plugin, "Bitte erstelle die Welt mit: /mv create Replays VOID");
            return;
        }
        
        for (int x = 0; x < MAX_X_SLOTS; x++) {
            for (int z = 0; z < MAX_Z_SLOTS; z++) {
                int baseX = x * SPACING;
                int baseZ = z * SPACING;
                Location origin = new Location(replayWorld, baseX, 100, baseZ); // Y=100 für Vogelperspektive
                availableReplayOrigins.add(origin);
            }
        }
        
        LogUtil.logInfo(plugin, "[ReplayWorld] " + availableReplayOrigins.size() + " Replay-Slots verfügbar");
    }
    
    /**
     * Claimed einen freien Replay-Slot.
     */
    public synchronized Location claimReplaySlot() {
        for (int x = 0; x < MAX_X_SLOTS; x++) {
            for (int z = 0; z < MAX_Z_SLOTS; z++) {
                if (!slotsOccupied[x][z]) {
                    slotsOccupied[x][z] = true;
                    int index = x * MAX_Z_SLOTS + z;
                    if (index < availableReplayOrigins.size()) {
                        Location origin = availableReplayOrigins.get(index);
                        LogUtil.logDebug(plugin, "[ReplayWorld] Slot [" + x + "," + z + "] claimed");
                        return origin.clone();
                    }
                }
            }
        }
        LogUtil.logError(plugin, "[ReplayWorld] Keine freien Replay-Slots verfügbar!");
        return null;
    }
    
    /**
     * Gibt einen Replay-Slot frei.
     */
    public synchronized void releaseReplaySlot(Location origin) {
        if (origin == null) return;
        
        for (int i = 0; i < availableReplayOrigins.size(); i++) {
            Location loc = availableReplayOrigins.get(i);
            if (isSameOrigin(loc, origin)) {
                int x = i / MAX_Z_SLOTS;
                int z = i % MAX_Z_SLOTS;
                slotsOccupied[x][z] = false;
                LogUtil.logDebug(plugin, "[ReplayWorld] Slot [" + x + "," + z + "] freigegeben");
                return;
            }
        }
    }
    
    private boolean isSameOrigin(Location loc1, Location loc2) {
        return loc1.getWorld() == loc2.getWorld() &&
               loc1.getBlockX() == loc2.getBlockX() &&
               loc1.getBlockZ() == loc2.getBlockZ();
    }
    
    /**
     * Prüft ob die Replay-Welt existiert.
     */
    public boolean isReplayWorldAvailable() {
        return Bukkit.getWorld("Replays") != null;
    }
    
    /**
     * Gibt die Anzahl aktiver Replays zurück.
     */
    public int getActiveReplaysCount() {
        int count = 0;
        for (int x = 0; x < MAX_X_SLOTS; x++) {
            for (int z = 0; z < MAX_Z_SLOTS; z++) {
                if (slotsOccupied[x][z]) count++;
            }
        }
        return count;
    }
}

