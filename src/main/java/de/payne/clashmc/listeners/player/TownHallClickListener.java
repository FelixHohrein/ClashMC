package de.payne.clashmc.listeners.player;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.gui.AttackMenu;
import de.payne.clashmc.gui.MineRewardMenu;
import de.payne.clashmc.gui.ShopMenu;
import de.payne.clashmc.gui.TownHallMenu;



public class TownHallClickListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
    	
        if (!(event.getWhoClicked() instanceof Player player)) return;

        InventoryView view = event.getView();
        Inventory inventory = view.getTopInventory();

        // Nur TownHallMenu behandeln
        if (!(inventory.getHolder() instanceof TownHallMenu)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        Material type = clickedItem.getType();

        switch (slot) {
            case 0:
                player.closeInventory();
                player.sendMessage("§cMenü geschlossen.");
                break;
            case 13:
                player.sendMessage("§aDas Angriffsmenu wird geöffnet...");
                new AttackMenu(ClashMC.getInstance()).open(player);
                break;
            case 14:
                player.sendMessage("§aDer Mine Shop wird geöffnet...");
                new MineRewardMenu(player).open();
                break;
            case 15:
                player.sendMessage("§aDer Shop wird geöffnet...");
                player.openInventory(new ShopMenu(player).getInventory());
                break;
            case 16:
                player.sendMessage("§eDas Replay-System ist noch in Entwicklung.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                break;
            default:
                if (type == Material.BLACK_STAINED_GLASS_PANE) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                }
                break;
        }
    }
}