package de.payne.clashmc.listeners.player;

import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.attacks.AttackManager;

public class AttackMenuClickListener implements Listener {

    private final ClashMC plugin;
    private final AttackManager attackManager;
    private final Random random = new Random();

    public AttackMenuClickListener(ClashMC plugin, AttackManager attackManager) {
        this.plugin = plugin;
        this.attackManager = attackManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle() == null) return;
        if (!event.getView().getTitle().equals("§8Angriffs-Menü")) return;  // Titel anpassen!

        event.setCancelled(true);  // Klicks generell blocken, nur eigene Aktionen erlauben

        Player attacker = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        Material mat = clickedItem.getType();

        // Schwarze Glascheiben als "Dekoration" -> Noteblock-Stumpfer Sound spielen
        if (mat == Material.BLACK_STAINED_GLASS_PANE) {
            attacker.playSound(attacker.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
            return;
        }

        // Online Angriff Item 
        if (mat == Material.IRON_SWORD) {
            // Zufälligen online registrierten Spieler wählen
            Player defender = getRandomOnlineRegisteredDefender(attacker);
            if (defender == null) {
                attacker.sendMessage("§cKein passender Spieler für Online-Angriff gefunden.");
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // Angriff starten
            attackManager.startAttack(attacker.getUniqueId(), defender.getUniqueId(), true);
            attacker.closeInventory();
            return;
        }

        // Offline Angriff Item 
        if (mat == Material.STONE_SWORD) {
            OfflinePlayer defender = getRandomOfflineDefender(attacker);
            if (defender == null) {
                attacker.sendMessage("§cKein passender Spieler für Offline-Angriff gefunden.");
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // Angriff starten
            attackManager.startAttack(attacker.getUniqueId(), defender.getUniqueId(), false);
            attacker.closeInventory();
            return;
        }
    }

    /**
     * Wählt einen zufälligen Spieler aus, der für Online-Angriffe registriert ist, 
     * online ist und dessen Dorflevel in +-20 Bereich zum Angreifer liegt.
     */
    private Player getRandomOnlineRegisteredDefender(Player attacker) {
        int attackerId = -1;
        int attackerLevel = 1;
        try {
            attackerId = plugin.getDatabaseManager().players().getPlayerIdByUUID(attacker.getUniqueId());
            attackerLevel = plugin.getDatabaseManager().villages().getVillageLevel(attackerId);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        final int attackerLevelFinal = attackerLevel;  // final für lamda

        // Spieler-IDs, die für Online registriert sind
        List<Integer> registeredPlayerIds = plugin.getDatabaseManager().attacks().getRegisteredOnlinePlayerIds();

        List<Player> candidates = Bukkit.getOnlinePlayers().stream()
            .filter(p -> !p.getUniqueId().equals(attacker.getUniqueId()))
            .filter(p -> {
                try {
                    int id = plugin.getDatabaseManager().players().getPlayerIdByUUID(p.getUniqueId());
                    if (!registeredPlayerIds.contains(id)) return false;
                    int lvl = plugin.getDatabaseManager().villages().getVillageLevel(id);
                    // Hier verwenden wir final attackerLevelFinal!
                    return lvl >= attackerLevelFinal - 20 && lvl <= attackerLevelFinal + 20;
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
            })
            .collect(Collectors.toList());

        if (candidates.isEmpty()) return null;
        return candidates.get(random.nextInt(candidates.size()));
    }

    /**
     * Wählt einen zufälligen Spieler aus, der NICHT für Online-Angriffe registriert ist,
     * also Offline-Angriffe zulässt, und dessen Dorflevel in +-20 Bereich zum Angreifer liegt.
     */
    private OfflinePlayer getRandomOfflineDefender(Player attacker) {
        int attackerId = -1;
        int attackerLevel = 1;
        try {
            attackerId = plugin.getDatabaseManager().players().getPlayerIdByUUID(attacker.getUniqueId());
            attackerLevel = plugin.getDatabaseManager().villages().getVillageLevel(attackerId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null; // Kann keinen Angreifer-Level bestimmen => abbrechen
        }

        final int attackerLevelFinal = attackerLevel; // Für Lambda final machen
        final int attackerIdFinal = attackerId;

        // Registrierte Spieler für Online-Angriffe
        List<Integer> registeredPlayerIds;
        registeredPlayerIds = plugin.getDatabaseManager().attacks().getRegisteredOnlinePlayerIds();

        // Alle Spieler aus der DB
        List<Integer> allPlayerIds;
        allPlayerIds = plugin.getDatabaseManager().players().getAllPlayerIds();

        // Filtere Spieler, die nicht registriert sind, nicht Angreifer selbst und Level im Bereich ±20 haben
        List<Integer> offlineCandidateIds = allPlayerIds.stream()
            .filter(id -> id != attackerIdFinal)
            .filter(id -> !registeredPlayerIds.contains(id))
            .filter(id -> {
                try {
                    int lvl = plugin.getDatabaseManager().villages().getVillageLevel(id);
                    return lvl >= attackerLevelFinal - 20 && lvl <= attackerLevelFinal + 20;
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false; // Bei Fehler nicht auswählen
                }
            })
            .collect(Collectors.toList());

        if (offlineCandidateIds.isEmpty()) return null;

        int selectedId = offlineCandidateIds.get(random.nextInt(offlineCandidateIds.size()));

        UUID uuid;
        try {
            uuid = plugin.getDatabaseManager().players().getUUIDByKingId(selectedId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

        if (uuid == null) return null;

        // Spieler offline => Bukkit.getPlayer liefert nur Online-Spieler, sonst null
        return Bukkit.getOfflinePlayer(uuid);
    }
}
