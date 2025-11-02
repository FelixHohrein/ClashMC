package de.payne.clashmc.listeners.player;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.gui.ReplayListMenu;
import de.payne.clashmc.replay.ReplayData;
import de.payne.clashmc.replay.ReplayInstance;
import de.payne.clashmc.utils.LogUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ReplayListClickListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ReplayListMenu)) return;
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        ReplayListMenu menu = (ReplayListMenu) event.getInventory().getHolder();
        int slot = event.getSlot();
        
        // Pagination
        if (slot == 48 && clickedItem.getType() == Material.ARROW) {
            // Vorherige Seite
            if (menu.getPage() > 0) {
                player.closeInventory();
                new ReplayListMenu(player, menu.getPage() - 1).open();
            }
            return;
        }
        
        if (slot == 50 && clickedItem.getType() == Material.ARROW) {
            // Nächste Seite
            player.closeInventory();
            new ReplayListMenu(player, menu.getPage() + 1).open();
            return;
        }
        
        // Zurück zum Rathaus
        if (slot == 53) {
            player.closeInventory();
            new de.payne.clashmc.gui.TownHallMenu(player).open();
            return;
        }
        
        // Info-Item
        if (slot == 49) return;
        
        // Replay-Item geklickt
        ReplayData replay = menu.getReplayAtSlot(slot);
        if (replay == null) return;
        
        player.closeInventory();
        
        // Starte Replay
        ClashMC.getInstance().getServer().getScheduler().runTaskAsynchronously(ClashMC.getInstance(), () -> {
            try {
                ReplayInstance replayInstance = new ReplayInstance(ClashMC.getInstance(), player, replay);
                replayInstance.start();
            } catch (Exception e) {
                LogUtil.logError(ClashMC.getInstance(), "[Replay] Fehler beim Starten des Replays: " + e.getMessage());
                e.printStackTrace();
                
                player.sendMessage("§cFehler beim Laden des Replays.");
            }
        });
    }
}

