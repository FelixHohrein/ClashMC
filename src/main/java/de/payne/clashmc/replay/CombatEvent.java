package de.payne.clashmc.replay;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.google.gson.annotations.SerializedName;

/**
 * Repräsentiert einen Kampf-Event (Angriff zwischen Spielern).
 * Wird für Replay-Kampf-Tracking verwendet.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CombatEvent {
    
    @SerializedName("attacker_id")
    private int attackerId; // 1 = Angreifer, 2 = Verteidiger
    
    @SerializedName("x")
    private double x;
    
    @SerializedName("y")
    private double y;
    
    @SerializedName("z")
    private double z;
    
    @SerializedName("damage")
    private double damage;
    
    @SerializedName("timestamp")
    private long timestamp; // Millisekunden seit Angriffs-Start
    
    /**
     * Erstellt einen CombatEvent aus einem Damage-Event.
     * @param isAttacker true wenn der Angreifer schlägt, false wenn der Verteidiger schlägt
     * @param location Position des Angreifers
     * @param damage Schaden
     * @param attackStartTime Start-Zeit des Angriffs
     * @param baseLocation Base-Location des Angriffs
     */
    public static CombatEvent fromDamage(boolean isAttacker, org.bukkit.Location location, 
                                         double damage, long attackStartTime, org.bukkit.Location baseLocation) {
        long timestamp = System.currentTimeMillis() - attackStartTime;
        
        // Berechne relative Position zur baseLocation
        double relX = location.getX() - baseLocation.getX();
        double relY = location.getY() - baseLocation.getY();
        double relZ = location.getZ() - baseLocation.getZ();
        
        return new CombatEvent(
            isAttacker ? 1 : 2, // 1 = Angreifer, 2 = Verteidiger
            relX,
            relY,
            relZ,
            damage,
            timestamp
        );
    }
    
    /**
     * Konvertiert zu Bukkit Location (relativ zur Base-Location).
     */
    public org.bukkit.Location toLocation(org.bukkit.World world, org.bukkit.Location baseLocation) {
        return new org.bukkit.Location(
            world,
            baseLocation.getX() + x,
            baseLocation.getY() + y,
            baseLocation.getZ() + z
        );
    }
}

