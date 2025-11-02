package de.payne.clashmc.listeners.player;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.replay.ReplayInstance;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Listener für Replay-Controls (Items im Inventar).
 */
public class ReplayControlsListener implements Listener {

    // Speichert aktive Replay-Instances pro Spieler
    private static final Map<UUID, ReplayInstance> activeReplays = new HashMap<>();
    
    public static void registerReplay(Player player, ReplayInstance instance) {
        activeReplays.put(player.getUniqueId(), instance);
    }
    
    public static void unregisterReplay(Player player) {
        activeReplays.remove(player.getUniqueId());
    }
    
    public static ReplayInstance getReplayInstance(Player player) {
        return activeReplays.get(player.getUniqueId());
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || item.getType() == Material.AIR) return;
        
        ReplayInstance instance = activeReplays.get(player.getUniqueId());
        if (instance == null) return;
        
        event.setCancelled(true);
        
        Material type = item.getType();
        int slot = player.getInventory().getHeldItemSlot();
        
        // Exit (Slot 0)
        if (type == Material.RED_BED) {
            player.sendMessage("§cReplay beendet.");
            instance.cleanup();
            unregisterReplay(player);
            return;
        }
        
        // Speed-Control (Slots 2-6)
        if (type == Material.FEATHER && slot == 2) {
            instance.getReplayPlayer().setPlaybackSpeed(0.5f);
            return;
        }
        if (type == Material.PAPER && slot == 3) {
            instance.getReplayPlayer().setPlaybackSpeed(1.0f);
            return;
        }
        if (type == Material.SUGAR) {
            if (slot == 4) instance.getReplayPlayer().setPlaybackSpeed(2.0f);
            if (slot == 5) instance.getReplayPlayer().setPlaybackSpeed(4.0f);
            if (slot == 6) instance.getReplayPlayer().setPlaybackSpeed(8.0f);
            return;
        }
        
        // Camera-Toggle (Slot 8)
        if (type == Material.ENDER_EYE && slot == 8) {
            instance.getReplayPlayer().toggleFollowCam();
            
            // Update Item-Lore
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (instance.getReplayPlayer().isFollowCam()) {
                meta.setDisplayName("§bKamera: §eNPC folgen");
                meta.setLore(List.of("§7Klicken für freie Kamera"));
            } else {
                meta.setDisplayName("§bKamera: §fFrei");
                meta.setLore(List.of("§7Klicken um NPC zu folgen"));
            }
            item.setItemMeta(meta);
        }
    }
}

