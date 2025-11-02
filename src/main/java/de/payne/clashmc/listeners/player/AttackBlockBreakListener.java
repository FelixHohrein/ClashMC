package de.payne.clashmc.listeners.player;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.attacks.AttackInstance;
import de.payne.clashmc.attacks.AttackManager;
import de.payne.clashmc.utils.LogUtil;

public class AttackBlockBreakListener implements Listener {

    private final AttackManager attackManager; // Deine Klasse, die AttackInstances verwaltet

    public AttackBlockBreakListener() {
        this.attackManager = ClashMC.getInstance().getAttackManager();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if(!attackManager.getActiveAttacks().containsKey(player.getUniqueId())) {
        	return;
        }
        
        
        AttackInstance attack = attackManager.getActiveAttacks().get(player.getUniqueId());

        if(player.getUniqueId().equals(attack.getDefenderUuid())){
        	event.setCancelled(true);
        	
        } else if (attack != null) {
            // Block an AttackInstance übergeben
            attack.addBrokenBlock(event.getBlock());
            LogUtil.logDebug(ClashMC.getInstance(), "[REPLAY] Block " + event.getBlock() + " wurde gespeichert.");

        }
        
    }
    
}
