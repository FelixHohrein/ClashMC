package de.payne.clashmc.gui;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import de.payne.clashmc.utils.ItemStackUtil;
import de.payne.clashmc.utils.PlayerDataCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ShopMenu implements InventoryHolder {

    private final Player player;
    private final Inventory inventory;

    public ShopMenu(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Shop", NamedTextColor.GOLD));
        loadItems();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
    }
    
    private void loadItems() {
        PlayerDataCache data = new PlayerDataCache(player.getUniqueId());
        
        if (!data.isValid()) {
            player.sendMessage("§cFehler beim Laden deiner Ressourcen.");
            return;
        }

        // Rand mit schwarzem Glas
        for (int i = 0; i < 27; i++) {
            if (i <= 8 || i >= 18 || i % 9 == 0 || i % 9 == 8 || i == 17) {
                inventory.setItem(i, ItemStackUtil.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null));
            }
        }

        // Zurück button
        inventory.setItem(0, ItemStackUtil.createItemSkull(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTJkN2E3NTFlYjA3MWUwOGRiYmM5NWJjNWQ5ZDY2ZTVmNTFkYzY3MTI2NDBhZDJkZmEwM2RlZmJiNjhhN2YzYSJ9fX0=", 
                "§cZurück", null));
        
        // Village level
        inventory.setItem(10, ItemStackUtil.createItemSkull(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjNkMDJjZGMwNzViYjFjYzVmNmZlM2M3NzExYWU0OTc3ZTM4YjkxMGQ1MGVkNjAyM2RmNzM5MTNlNWU3ZmNmZiJ9fX0=", 
                "§eDorf-Level", 
                List.of("§7Level: §a" + data.getVillageLevel())
        ));
        
        // Coin-Anzeigen
        inventory.setItem(11, ItemStackUtil.createItemSkull(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTgwMjU5NzA5ODg4OGM1OTJlMDlkNTlhMmFkZTU3NmQ3NWQzZTQ5NDY1ZDE1NzI0YjRhODc4OWQ4NjNmNWJkNCJ9fX0", 
                "§6King Coins", 
                List.of("§7Deine Coins: §e" + data.getKingCoins())
        ));

        inventory.setItem(12, ItemStackUtil.createItemSkull(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjA2ZWUxZDM3MDRmNDQ1ODc2MTlhMjI1MmMzMzM5YWY3ODU1MjgwMjAzYjI1OTE4MWVmZDE4NzI0NWFiZjgyNCJ9fX0=", 
                "§aClash Coins", 
                List.of("§7Deine Coins: §a" + data.getClashCoins())
        ));

        // Shop-Kategorien
        inventory.setItem(13, ItemStackUtil.createItemSkull(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWFkNmM4MWY4OTlhNzg1ZWNmMjZiZTFkYzQ4ZWFlMmJjZmU3NzdhODYyMzkwZjU3ODVlOTViZDgzYmQxNGQifX19", 
                "§bUpgrade-Shop", 
                List.of("§7Verbessere deine Gebäude")
        ));
        inventory.setItem(14, ItemStackUtil.createItem(Material.IRON_SWORD, "§cAngriffs-Shop", List.of("§7Wähle Angriffs-Items")));
        inventory.setItem(15, ItemStackUtil.createItem(Material.SHIELD, "§9Verteidigungs-Shop", List.of("§7Wähle Verteidigungs-Items")));

        // King-Shop (Premium nur mit King-Coins)
        inventory.setItem(16, ItemStackUtil.createItemSkull(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjJmYWYyYWU0NWViM2JiYzQxMDU3MjlkYTE5YjViM2UyZjExMTU5ZGMwZTU3ZDlhMTJmZjk1MzcxYmExODNjZiJ9fX0=", 
                "§6King-Shop", 
                List.of("§7Nur mit §eKing Coins§7 zugänglich")
        ));
    }
}
