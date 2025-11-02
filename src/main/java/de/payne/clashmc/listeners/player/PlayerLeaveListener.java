package de.payne.clashmc.listeners.player;


import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import de.payne.clashmc.ClashMC;

public class PlayerLeaveListener implements Listener{

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
	    Player player = event.getPlayer();

	    if (ClashMC.getInstance().getMineManager().isInMine(player)) {
	    	ClashMC.getInstance().getMineManager().endMineSession(player);
	    }
	}
}
