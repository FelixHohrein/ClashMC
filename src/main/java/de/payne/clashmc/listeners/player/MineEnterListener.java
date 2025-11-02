package de.payne.clashmc.listeners.player;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.database.modules.MineDatabase;
import de.payne.clashmc.database.modules.PlayerDatabase;
import de.payne.clashmc.mine.MineBoosterType;
import de.payne.clashmc.mine.MineManager;
import de.payne.clashmc.utils.LogUtil;

public class MineEnterListener implements Listener {

    private final MineManager mineManager;
    private final PlayerDatabase playerDatabase;
    private final MineDatabase mineDatabase;

    public MineEnterListener() {
        this.mineManager = ClashMC.getInstance().getMineManager();
        this.playerDatabase = ClashMC.getInstance().getDatabaseManager().players();
        this.mineDatabase = ClashMC.getInstance().getDatabaseManager().mine();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.COAL_BLOCK) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        int kingId = -1;
		try {
			kingId = playerDatabase.getPlayerIdByUUID(uuid);
		} catch (SQLException e) {
			LogUtil.logError(ClashMC.getInstance(), "UUID des Spielers "+ player.getName() + " nicht in der Datenbank mit KING ID verknüpft.");
			e.printStackTrace();
		}

        // Prüfung ob noch Cooldown
        long nextAvailable = mineDatabase.getMineCooldown(kingId);
        long now = System.currentTimeMillis();
        if (nextAvailable > now) {
            long secondsLeft = (nextAvailable - now) / 1000;
            long minutes = secondsLeft / 60;
            long seconds = secondsLeft % 60;

            String timeFormatted = String.format("%02d:%02d", minutes, seconds);
            player.sendMessage(ChatColor.RED + "⛏️ Deine Mine ist erst in " + ChatColor.YELLOW + timeFormatted + ChatColor.RED + " verfügbar.");

            // Sound-Feedback bei zu früherem Klick
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);

            return;
        }

        // Mine starten
        mineManager.startMineSession(player);
        // Sound & Partikel bei erfolgreichem Eintritt
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 50, 0.5, 0.75, 0.5, 0.1);
        
        // Cooldown setzen (z. B. 10 Minuten = 600.000ms)
        List<MineBoosterType> boosters = mineDatabase.getActiveBoosters(kingId);
        boolean hasNoCooldown = boosters.contains(MineBoosterType.NO_COOLDOWN);

        if (!hasNoCooldown) {
            long cooldownMillis = 10 * 60 * 1000;
            mineDatabase.setMineCooldown(kingId, now + cooldownMillis);
        }
    }
}