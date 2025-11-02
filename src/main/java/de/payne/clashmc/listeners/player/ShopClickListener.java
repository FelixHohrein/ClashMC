package de.payne.clashmc.listeners.player;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.gui.ShopMenu;
import de.payne.clashmc.gui.TownHallMenu;
import de.payne.clashmc.gui.UpgradeShopMenu;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class ShopClickListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) return;

        InventoryView view = event.getView();
        Inventory inventory = view.getTopInventory();

        // Nur ShopMenu behandeln
        InventoryHolder holder = inventory.getHolder();
        if (!(holder instanceof ShopMenu)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        Material type = clickedItem.getType();

        switch (slot) {
            case 0 -> {
                player.closeInventory();
                player.openInventory(new TownHallMenu(player).getInventory());
            }
            case 13 -> {
            	player.closeInventory();
                new UpgradeShopMenu(ClashMC.getInstance()).open(player);
            }
            case 14 -> {
                // Öffne Angriff-Shop
            }
            case 15 -> {
                // Öffne Verteidigungs-Shop
            }
            case 16 -> {
                // Öffne King-Shop
            }
            default -> {
                if (type == Material.BLACK_STAINED_GLASS_PANE) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                }
            }
        }
    }
}
