package de.payne.clashmc.gui;

import java.sql.SQLException;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;


import de.payne.clashmc.ClashMC;
import de.payne.clashmc.economy.PlayerResources;
import de.payne.clashmc.utils.ItemStackUtil;
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
        int villageLevel = 0;
        long kingCoins = 0;
        long clashCoins = 0;
        
        int kingId = -1;
        
        try {
            kingId = ClashMC.getInstance().getDatabaseManager().players().getPlayerIdByUUID(player.getUniqueId());
        } catch (SQLException e1) {
            player.sendMessage("§cFehler beim Laden deiner Daten.");
            e1.printStackTrace();
        }

        try {
            villageLevel = ClashMC.getInstance().getDatabaseManager().villages().getVillageLevel(kingId);
            PlayerResources resources = ClashMC.getInstance().getDatabaseManager().resources().getResources(player.getUniqueId());
            if (resources != null) {
                kingCoins = resources.getKingCoins();
                clashCoins = resources.getClashCoins();
            }
        } catch (SQLException e) {
            player.sendMessage("§cFehler beim Laden deiner Daten.");
            e.printStackTrace();
        }

        inventory.setItem(0, ItemStackUtil.createItem(Material.BARRIER, "§cVerlassen", null));

        for (int i = 0; i < 27; i++) {
            if (i == 0 || (i >= 1 && i <= 8) || (i >= 18 && i <= 26) || i == 17) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, ItemStackUtil.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null));
                }
            }
        }

        inventory.setItem(9, ItemStackUtil.createItemSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjNkMDJjZGMwNzViYjFjYzVmNmZlM2M3NzExYWU0OTc3ZTM4YjkxMGQ1MGVkNjAyM2RmNzM5MTNlNWU3ZmNmZiJ9fX0=", "§eDorf-Level", List.of("§7Level: §a" + villageLevel)));
        inventory.setItem(10, ItemStackUtil.createItemSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTgwMjU5NzA5ODg4OGM1OTJlMDlkNTlhMmFkZTU3NmQ3NWQzZTQ5NDY1ZDE1NzI0YjRhODc4OWQ4NjNmNWJkNCJ9fX0", "§6King Coins", List.of("§7Deine Coins: §e" + kingCoins)));
        inventory.setItem(11, ItemStackUtil.createItemSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjA2ZWUxZDM3MDRmNDQ1ODc2MTlhMjI1MmMzMzM5YWY3ODU1MjgwMjAzYjI1OTE4MWVmZDE4NzI0NWFiZjgyNCJ9fX0=", "§aClash Coins", List.of("§7Deine Coins: §a" + clashCoins)));

        inventory.setItem(13, ItemStackUtil.createItem(Material.DIAMOND_SWORD, "§cAngriffsmenü", List.of("§7Öffne das Angriffsmenu")));
        inventory.setItem(14, ItemStackUtil.createItem(Material.MINECART, "§7Mine Shop", List.of("§7Öffne den Minen-Shop")));
        inventory.setItem(15, ItemStackUtil.createItem(Material.CHEST, "§6Shop", List.of("§7Öffne den §eShop")));
        inventory.setItem(16, ItemStackUtil.createItem(Material.COMMAND_BLOCK, "§bReplay System", List.of("§7In Entwicklung...")));

        for (int i = 9; i <= 13; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, ItemStackUtil.createItem(Material.WHITE_STAINED_GLASS_PANE, " ", null));
            }
        }
    }

}
