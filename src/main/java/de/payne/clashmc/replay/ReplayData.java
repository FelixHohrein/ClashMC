package de.payne.clashmc.replay;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.sql.Timestamp;

/**
 * Enthält alle Daten für ein Replay.
 */
@Getter
@AllArgsConstructor
public class ReplayData {
    private final int attackId;
    private final int attackerId;
    private final int defenderId;
    private final boolean isOnline;
    private final double damagePercent;
    private final long clashCoinsLooted;
    private final long kingCoinsLooted;
    private final Timestamp attackTime;
    private final String replayData; // JSON: BrokenBlocks
    private final String movementData; // JSON: MovementPoints
    
    /**
     * Prüft ob dieses Replay älter als X Tage ist.
     */
    public boolean isOlderThan(int days) {
        if (attackTime == null) return true;
        
        long now = System.currentTimeMillis();
        long attackTimeMillis = attackTime.getTime();
        long daysDiff = (now - attackTimeMillis) / (1000 * 60 * 60 * 24);
        
        return daysDiff > days;
    }
    
    /**
     * Gibt das Alter des Replays in Tagen zurück.
     */
    public long getAgeDays() {
        if (attackTime == null) return 999;
        
        long now = System.currentTimeMillis();
        long attackTimeMillis = attackTime.getTime();
        return (now - attackTimeMillis) / (1000 * 60 * 60 * 24);
    }
}

