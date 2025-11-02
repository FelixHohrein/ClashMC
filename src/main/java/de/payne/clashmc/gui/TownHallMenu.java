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

public class TownHallMenu implements InventoryHolder {

    private final Player player;
    private final Inventory inventory;

    public TownHallMenu(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Rathaus", NamedTextColor.DARK_GRAY));
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
            player.sendMessage("§cFehler beim Laden deiner Daten.");
            return;
        }

        inventory.setItem(0, ItemStackUtil.createItem(Material.BARRIER, "§cVerlassen", null));

        for (int i = 0; i < 27; i++) {
            if (i == 0 || (i >= 1 && i <= 8) || (i >= 18 && i <= 26) || i == 17) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, ItemStackUtil.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null));
                }
            }
        }

        inventory.setItem(9, ItemStackUtil.createItemSkull(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjNkMDJjZGMwNzViYjFjYzVmNmZlM2M3NzExYWU0OTc3ZTM4YjkxMGQ1MGVkNjAyM2RmNzM5MTNlNWU3ZmNmZiJ9fX0=", 
                "§eDorf-Level", 
                List.of("§7Level: §a" + data.getVillageLevel())
        ));
        
        inventory.setItem(10, ItemStackUtil.createItemSkull(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTgwMjU5NzA5ODg4OGM1OTJlMDlkNTlhMmFkZTU3NmQ3NWQzZTQ5NDY1ZDE1NzI0YjRhODc4OWQ4NjNmNWJkNCJ9fX0", 
                "§6King Coins", 
                List.of("§7Deine Coins: §e" + data.getKingCoins())
        ));
        
        inventory.setItem(11, ItemStackUtil.createItemSkull(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjA2ZWUxZDM3MDRmNDQ1ODc2MTlhMjI1MmMzMzM5YWY3ODU1MjgwMjAzYjI1OTE4MWVmZDE4NzI0NWFiZjgyNCJ9fX0=", 
                "§aClash Coins", 
                List.of("§7Deine Coins: §a" + data.getClashCoins())
        ));

        inventory.setItem(13, ItemStackUtil.createItem(Material.DIAMOND_SWORD, "§cAngriffsmenü", List.of("§7Öffne das Angriffsmenu")));
        inventory.setItem(14, ItemStackUtil.createItem(Material.MINECART, "§7Mine Shop", List.of("§7Öffne den Minen-Shop")));
        inventory.setItem(15, ItemStackUtil.createItem(Material.CHEST, "§6Shop", List.of("§7Öffne den §eShop")));
        inventory.setItem(16, ItemStackUtil.createItem(
            Material.ENDER_EYE, 
            "§b§lReplay-System", 
            List.of(
                "§7Schaue dir vergangene Angriffe an!",
                "",
                "§7Deine Angriffe und",
                "§7Angriffe auf dein Dorf",
                "",
                "§eKlicken zum Öffnen!"
            )
        ));

        for (int i = 9; i <= 13; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, ItemStackUtil.createItem(Material.WHITE_STAINED_GLASS_PANE, " ", null));
            }
        }
    }
}
