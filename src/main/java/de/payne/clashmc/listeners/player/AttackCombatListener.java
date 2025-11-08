package de.payne.clashmc.listeners.player;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.attacks.AttackInstance;
import de.payne.clashmc.attacks.AttackManager;
import de.payne.clashmc.utils.LogUtil;

/**
 * Listener für Kampf-Events während Angriffen.
 * Speichert Angriffe zwischen Spielern für Replay-System.
 */
public class AttackCombatListener implements Listener {

    private final AttackManager attackManager;

    public AttackCombatListener() {
        this.attackManager = ClashMC.getInstance().getAttackManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Nur Player vs Player Events
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();
        
        // Prüfe ob beide Spieler in einem aktiven Angriff sind
        AttackInstance attackInstance = null;
        
        // Prüfe ob Angreifer in einem aktiven Angriff ist
        if (attackManager.getActiveAttacks().containsKey(attacker.getUniqueId())) {
            attackInstance = attackManager.getActiveAttacks().get(attacker.getUniqueId());
        } else {
            // Prüfe ob Verteidiger in einem aktiven Angriff ist
            for (AttackInstance instance : attackManager.getActiveAttacks().values()) {
                if (instance.isPlayerInAttack(attacker.getUniqueId()) && 
                    instance.isPlayerInAttack(victim.getUniqueId())) {
                    attackInstance = instance;
                    break;
                }
            }
        }
        
        if (attackInstance == null) {
            return; // Nicht in einem Angriff
        }
        
        // Prüfe ob beide Spieler Teil dieses Angriffs sind
        if (!attackInstance.isPlayerInAttack(attacker.getUniqueId()) || 
            !attackInstance.isPlayerInAttack(victim.getUniqueId())) {
            return; // Nicht beide Spieler in diesem Angriff
        }
        
        // Bestimme ob Angreifer der Attacker oder Defender ist
        boolean isAttackerHitting = attacker.getUniqueId().equals(attackInstance.getAttackerUuid());
        
        // Speichere Combat-Event
        double damage = event.getFinalDamage();
        attackInstance.addCombatEvent(isAttackerHitting, attacker.getLocation(), damage);
        
        LogUtil.logDebug(ClashMC.getInstance(), "[Combat] Angriff gespeichert: " + 
            attacker.getName() + " -> " + victim.getName() + " (" + damage + " Schaden)");
    }
}

