package de.payne.clashmc.gui;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.database.DatabaseManager;
import de.payne.clashmc.economy.PlayerResources;
import de.payne.clashmc.economy.ResourceManager;
import de.payne.clashmc.maphandling.schematic.SchematicManager;
import de.payne.clashmc.utils.ItemStackUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class UpgradeShopMenu implements InventoryHolder {

    private final SchematicManager schematicManager;
    private final DatabaseManager databaseManager;
    private final ClashMC plugin;

    public static final int ITEMS_PER_PAGE = 36;
    private static final int INVENTORY_SIZE = 54;

    private Inventory gui;
    private int page;

    public UpgradeShopMenu(ClashMC plugin) {
        this.plugin = plugin;
        this.schematicManager = plugin.getSchematicManager();
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Override
    public Inventory getInventory() {
        return gui;
    }

    public void open(Player player) {
        open(player, 1);
    }

    public void open(Player player, int page) {
        UUID uuid = player.getUniqueId();
        int playerId;
        int currentLevel;
        PlayerResources resources;
        ResourceManager resourceManager = new ResourceManager(player);

        try {
            playerId = databaseManager.players().getPlayerIdByUUID(uuid);
            currentLevel = databaseManager.villages().getVillageLevel(playerId);
            resources = databaseManager.resources().getResources(uuid);
        } catch (SQLException e) {
            player.sendMessage(Component.text("Fehler beim Laden deiner Daten.", NamedTextColor.RED));
            e.printStackTrace();
            return;
        }

        long clashCoins = resources.getClashCoins();
        List<Integer> availableLevels = schematicManager.getAvailableLevels();

        if (availableLevels.isEmpty()) {
            player.sendMessage(Component.text("Keine verfügbaren Dorflevel gefunden.", NamedTextColor.RED));
            return;
        }

        int maxPage = (int) Math.ceil(availableLevels.size() / (double) ITEMS_PER_PAGE);
        this.page = Math.max(1, Math.min(page, maxPage));

        gui = Bukkit.createInventory(this, INVENTORY_SIZE, Component.text("🏰 Dorf-Upgrades Seite " + this.page));

        // Schwarze Glas-Pane für den oberen und unteren Rahmen (Slots 2-8 und 47-53) — unverändert
        ItemStack blackPane = ItemStackUtil.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 2; i < 9; i++) {
            gui.setItem(i, blackPane);
            gui.setItem(45 + i, blackPane);
        }
        gui.setItem(45, blackPane);
        gui.setItem(46, blackPane);

        // Aktuelles Level (Slot 0) mit Schädel-Item (wie bisher)
        gui.setItem(0, ItemStackUtil.createItemSkull(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjNkMDJjZGMwNzViYjFjYzVmNmZlM2M3NzExYWU0OTc3ZTM4YjkxMGQ1MGVkNjAyM2RmNzM5MTNlNWU3ZmNmZiJ9fX0",
                "§6Aktuelles Dorflevel",
                List.of("§7Level: §e" + currentLevel)
        ));

        // Clash Coins (Slot 1) mit Schädel-Item (wie bisher)
        gui.setItem(1, ItemStackUtil.createItemSkull(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjA2ZWUxZDM3MDRmNDQ1ODc2MTlhMjI1MmMzMzM5YWY3ODU1MjgwMjAzYjI1OTE4MWVmZDE4NzI0NWFiZjgyNCJ9fX0",
                "§6Clash Coins",
                List.of("§7Guthaben: §e" + clashCoins)
        ));

        // Dorflevel-Upgrades anzeigen (Slots 9–44)
        int startIndex = (this.page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, availableLevels.size());

        for (int i = startIndex; i < endIndex; i++) {
            int level = availableLevels.get(i);
            Material material;
            List<String> lore = new ArrayList<>();

            // Farbe und Lore je nach Levelstatus anpassen
            if (level < currentLevel) {
                // Bereits erreicht: grüne Glas-Pane
                material = Material.GREEN_STAINED_GLASS_PANE;
                lore.add("§2Bereits erreicht");
            } else if (level == currentLevel) {
                // Aktuelles Level: blaue Glas-Pane
                material = Material.BLUE_STAINED_GLASS_PANE;
                lore.add("§bAktuelles Level");
            } else if (level == currentLevel + 1) {
                // Nächstes kaufbares Level: orange Glas-Pane
                material = Material.ORANGE_STAINED_GLASS_PANE;
                lore.add("§aKlicke zum Upgraden");
            } else {
                // Zukünftige Level (ab currentLevel + 2): rote Glas-Pane
                material = Material.RED_STAINED_GLASS_PANE;
                lore.add("§7Noch nicht verfügbar");
            }
            
            // Kosten nur anzeigen, wenn Level >= currentLevel + 1 (also upgrade relevant)
            if (level >= currentLevel + 1) {
                long cost = resourceManager.getUpgradeCosts(level);
                lore.add("§7Kosten: §e" + cost + " Clash Coins");
            }

            gui.setItem(9 + (i - startIndex), ItemStackUtil.createItem(
                    material,
                    "§eLevel " + level,
                    lore
            ));
        }

        // Navigation (Zurück, Shop Menu, Weiter) — wie bisher
        gui.setItem(47, ItemStackUtil.createItemSkull(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWZhNGM4MjcxMDgzNzQ4MGRmNTc1Y2EwZDY0Y2VmMmZjZGFkYWVjZTcwOTFiNzA3NmI5MjNjNjdlNWY0ZTg0OSJ9fX0=",
                "§cZurück",
                List.of("§7Vorherige Seite")
        ));
        gui.setItem(49, ItemStackUtil.createItem(
                Material.BARRIER,
                "§cZurück zum Shop Menu",
                List.of("§7Zurück zum Shop Menu")
        ));
        gui.setItem(51, ItemStackUtil.createItemSkull(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjkxYWM0MzJhYTQwZDdlN2E2ODdhYTg1MDQxZGU2MzY3MTJkNGYwMjI2MzJkZDUzNTZjODgwNTIxYWYyNzIzYSJ9fX0=",
                "§aWeiter",
                List.of("§7Nächste Seite")
        ));

        player.openInventory(gui);
    }

    public int getPage() {
        return page;
    }
}