package de.payne.clashmc.gui;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.utils.ItemStackUtil;

public class AttackMenu {

    private final ClashMC plugin;

    public AttackMenu(ClashMC plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Angriffs-Menü");

        // Schwarze Glasscheiben als Hintergrund
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, ItemStackUtil.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null));
        }

        UUID uuid = player.getUniqueId();
        int yourLevel = 0;
        int registeredOnline = 0;
        int registeredTotal = 0;

        try {
            int playerId = plugin.getDatabaseManager().players().getPlayerIdByUUID(uuid);
            yourLevel = plugin.getDatabaseManager().villages().getVillageLevel(playerId);
            registeredTotal = plugin.getDatabaseManager().attacks().getRegisteredForOnlineAttackCount();
            registeredOnline = plugin.getDatabaseManager().attacks().getOnlineRegisteredPlayerCount();
        } catch (SQLException e) {
            player.sendMessage("§cDaten konnten nicht geladen werden.");
            e.printStackTrace();
        }

        // Online-Angriff
        inv.setItem(11, ItemStackUtil.createItem(
                Material.IRON_SWORD,
                "§aOnline-Angriff",
                List.of(
                        "§7Greife einen Spieler in Echtzeit an.",
                        "",
                        "§7Registrierte Spieler: §e" + registeredOnline + "§7/" + registeredTotal,
                        "§7Nur Spieler mit ähnlichem Dorflevel werden angezeigt."
                )
        ));

        // Info (Mitte)
        inv.setItem(13, ItemStackUtil.createItem(
                Material.BOOK,
                "§bAngriffsinfo",
                List.of(
                        "§7Du kannst andere Spieler angreifen,",
                        "§7wenn sie sich in einem ähnlichen Dorf-Level befinden.",
                        "",
                        "§7Erlaubter Levelbereich:",
                        "§e" + (yourLevel - 20) + " §7bis §e" + (yourLevel + 20)
                )
        ));

        // Offline-Angriff
        inv.setItem(15, ItemStackUtil.createItem(
                Material.STONE_SWORD,
                "§cOffline-Angriff",
                List.of(
                        "§7Greife ein Dorf an, während der Spieler offline ist.",
                        "",
                        "§7Nur Spieler, die nicht für Onlinekämpfe registriert sind,",
                        "§7werden dir angezeigt."
                )
        ));

        player.openInventory(inv);
    }
}