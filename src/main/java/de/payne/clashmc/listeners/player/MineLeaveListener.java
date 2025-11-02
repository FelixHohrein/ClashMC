package de.payne.clashmc.listeners.player;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import de.payne.clashmc.ClashMC;

public class MineLeaveListener implements Listener {

	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
	    Player player = event.getPlayer();
	    ItemStack item = event.getItem();

	    if (item == null || item.getType() != Material.COMPASS) return;
	    if (!item.hasItemMeta() || !item.getItemMeta().getDisplayName().equals("§cMine verlassen")) return;

	    event.setCancelled(true);
	    // Trigger deinen "zurück zum Dorf"-Code
	    ClashMC.getInstance().getMineManager().endMineSession(player);
	}
}
