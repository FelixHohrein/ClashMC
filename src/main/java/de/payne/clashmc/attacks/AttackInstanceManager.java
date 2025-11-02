package de.payne.clashmc.attacks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class AttackInstanceManager {

    private static final String WORLD_NAME = "Attacks";
    private static final int REGION_SIZE = 128; // Abstand zwischen Instanzen (wie in der Mine)
    private static final int MAX_X_SLOTS = 10;  // 10 x 10 Slots = 100 Angriffe gleichzeitig
    private static final int MAX_Z_SLOTS = 10;

    private final World attackWorld;
    private final boolean[][] slotMatrix = new boolean[MAX_X_SLOTS][MAX_Z_SLOTS];

    public AttackInstanceManager() {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world == null) {
            throw new IllegalStateException("Angriffswelt '" + WORLD_NAME + "' wurde nicht geladen!");
        }
        this.attackWorld = world;
    }

    public synchronized Location claimFreeSlot() {
        for (int x = 0; x < MAX_X_SLOTS; x++) {
            for (int z = 0; z < MAX_Z_SLOTS; z++) {
                if (!slotMatrix[x][z]) {
                    slotMatrix[x][z] = true;
                    return generateLocation(x, z);
                }
            }
        }
        return null; // Keine freien Slots mehr
    }

    public synchronized void releaseSlot(Location location) {
        int xIndex = location.getBlockX() / REGION_SIZE;
        int zIndex = location.getBlockZ() / REGION_SIZE;

        if (xIndex >= 0 && xIndex < MAX_X_SLOTS && zIndex >= 0 && zIndex < MAX_Z_SLOTS) {
            slotMatrix[xIndex][zIndex] = false;
        }
    }

    public Location generateLocation(int xIndex, int zIndex) {
        int x = xIndex * REGION_SIZE;
        int z = zIndex * REGION_SIZE;
        int y = attackWorld.getMinHeight() + 1;

        return new Location(attackWorld, x, y, z);
    }

    public World getAttackWorld() {
        return attackWorld;
    }
}
