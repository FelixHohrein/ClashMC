package de.payne.clashmc.listeners.player;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.attacks.AttackInstance;
import de.payne.clashmc.attacks.AttackManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * Listener für Spieler-Tode während Angriffen.
 * Handhabt Respawn mit Cooldown-System.
 */
public class PlayerDeathListener implements Listener {

    private final ClashMC plugin;
    private final AttackManager attackManager;

    public PlayerDeathListener(ClashMC plugin) {
        this.plugin = plugin;
        this.attackManager = plugin.getAttackManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUuid = player.getUniqueId();

        // Prüfe ob Spieler in einem aktiven Angriff ist
        if (!attackManager.isInAttack(playerUuid)) {
            return; // Nicht in Angriff, Standard-Death-Handling
        }

        // Finde AttackInstance
        AttackInstance instance = attackManager.getActiveAttacks().get(playerUuid);
        if (instance == null) {
            // Spieler könnte Defender sein, suche in allen Instances
            for (AttackInstance inst : attackManager.getActiveAttacks().values()) {
                if (inst.isPlayerInAttack(playerUuid)) {
                    instance = inst;
                    break;
                }
            }
        }

        if (instance == null) {
            return; // Keine Instance gefunden
        }

        // Final für Lambda
        final AttackInstance finalInstance = instance;

        // Verhindere Standard-Respawn
        event.setKeepInventory(false);
        event.setKeepLevel(false);
        event.getDrops().clear();

        // Handle Death in AttackInstance
        boolean canRespawnImmediately = finalInstance.handlePlayerDeath(playerUuid);

        if (canRespawnImmediately) {
            // Erster Tod: Sofort Respawn nach 1 Tick
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        finalInstance.handleRespawn(playerUuid);
                    }
                }
            }.runTaskLater(plugin, 1L);
        } else {
            // Spätere Tode: Cooldown, starte Cooldown-Check-Task
            startRespawnCooldownCheck(playerUuid, finalInstance);
        }

        player.sendMessage("§cDu bist gestorben! §7Respawn wird vorbereitet...");
    }

    /**
     * Startet einen Task der regelmäßig prüft ob Respawn möglich ist.
     */
    private void startRespawnCooldownCheck(UUID playerUuid, AttackInstance instance) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Player player = plugin.getServer().getPlayer(playerUuid);
                if (player == null || !player.isOnline()) {
                    cancel();
                    return;
                }

                // Prüfe ob Spieler noch in Angriff ist
                if (!attackManager.isInAttack(playerUuid) && !instance.isPlayerInAttack(playerUuid)) {
                    cancel();
                    return;
                }

                // Prüfe ob Respawn möglich ist
                if (instance.handleRespawn(playerUuid)) {
                    // Respawn erfolgreich, Task beenden
                    cancel();
                } else {
                    // Noch Cooldown, weiter prüfen (alle 20 Ticks = 1 Sekunde)
                    // Task läuft weiter
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Start nach 1 Sekunde, dann alle 1 Sekunde
    }
}

