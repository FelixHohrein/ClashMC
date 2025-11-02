package de.payne.clashmc.replay;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.google.gson.annotations.SerializedName;

/**
 * Repräsentiert einen Punkt in der Bewegung des Angreifers.
 * Wird für Replay-Movement-Tracking verwendet.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MovementPoint {
    
    @SerializedName("x")
    private double x;
    
    @SerializedName("y")
    private double y;
    
    @SerializedName("z")
    private double z;
    
    @SerializedName("yaw")
    private float yaw;
    
    @SerializedName("pitch")
    private float pitch;
    
    @SerializedName("timestamp")
    private long timestamp; // Millisekunden seit Angriffs-Start
    
    /**
     * Erstellt einen MovementPoint aus der aktuellen Spieler-Position.
     */
    public static MovementPoint fromPlayer(org.bukkit.entity.Player player, long attackStartTime) {
        long timestamp = System.currentTimeMillis() - attackStartTime;
        return new MovementPoint(
            player.getLocation().getX(),
            player.getLocation().getY(),
            player.getLocation().getZ(),
            player.getLocation().getYaw(),
            player.getLocation().getPitch(),
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
            baseLocation.getZ() + z,
            yaw,
            pitch
        );
    }
}

