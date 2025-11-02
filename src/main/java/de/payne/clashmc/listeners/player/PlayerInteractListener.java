package de.payne.clashmc.listeners.player;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import de.payne.clashmc.economy.ResourceManager;

public class PlayerInteractListener implements Listener {

    private static final Material COLLECTOR_BLOCK_TYPE = Material.LAPIS_BLOCK;

    @EventHandler
    public void onCollectorClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != COLLECTOR_BLOCK_TYPE) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        ResourceManager resourceManager = new ResourceManager(player);
        resourceManager.collectResources();
        
        player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
    }
}
