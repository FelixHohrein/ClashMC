package de.payne.clashmc.gui;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


import de.payne.clashmc.ClashMC;
import de.payne.clashmc.economy.PlayerResources;
import de.payne.clashmc.mine.MineBoosterType;
import de.payne.clashmc.mine.MineMaterialType;
import de.payne.clashmc.utils.ItemStackUtil;
import de.payne.clashmc.utils.LogUtil;

public class MineRewardMenu {

    private static final String TITLE = "§6Minenbelohnung";

    private static final String COINS_BASE64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjA2ZWUxZDM3MDRmNDQ1ODc2MTlhMjI1MmMzMzM5YWY3ODU1MjgwMjAzYjI1OTE4MWVmZDE4NzI0NWFiZjgyNCJ9fX0=";
    private static final String KINGCOINS_BASE64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTgwMjU5NzA5ODg4OGM1OTJlMDlkNTlhMmFkZTU3NmQ3NWQzZTQ5NDY1ZDE1NzI0YjRhODc4OWQ4NjNmNWJkNCJ9fX0=";
    private static final String BACK_BASE64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTJkN2E3NTFlYjA3MWUwOGRiYmM5NWJjNWQ5ZDY2ZTVmNTFkYzY3MTI2NDBhZDJkZmEwM2RlZmJiNjhhN2YzYSJ9fX0=";

    private final Player player;
    private int kingID = -1;
    private Map<MineMaterialType, Integer> mineItems;
    private PlayerResources resources;
    
    
    public MineRewardMenu(Player player) {
    	this.player = player;
    	
    	try {
			this.kingID = ClashMC.getInstance().getDatabaseManager().players().getPlayerIdByUUID(player.getUniqueId());
			this.resources = ClashMC.getInstance().getDatabaseManager().resources().getResources(player.getUniqueId());
		} catch (SQLException e) {
			LogUtil.logError(ClashMC.getInstance(), "Fehler beim laden der Datenbankeinträge für: " + player.getName() + " (" + player.getUniqueId() + ")");
			e.printStackTrace();
		}
    	this.mineItems = ClashMC.getInstance().getDatabaseManager().mine().getAllItems(this.kingID);
    	
    }
    
    public void open() {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // 1. Reihe: Schwarze Glasscheiben (Slots 0–8)
        for (int i = 0; i <= 8; i++) {
            inv.setItem(i, ItemStackUtil.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null));
        }

        // 2. & 3. Reihe: MineMaterialType-Items (Slots 9–26)
        int slot = 9;
        for (MineMaterialType type : MineMaterialType.values()) {
            int amount = mineItems.getOrDefault(type, 0);
            if (amount <= -1) continue;

            ItemStack item = ItemStackUtil.createItem(
                type.getMaterial(),
                "§e" + type.getDisplayName(),
                List.of("", "§7Anzahl: §c" + amount)
            );
            inv.setItem(slot++, item);
            if (slot > 26) break; // Maximal bis Slot 26 (3. Reihe)
        }

        // 4. Reihe: Schwarze Glasscheiben (Slots 27–35)
        for (int i = 27; i <= 35; i++) {
            inv.setItem(i, ItemStackUtil.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null));
        }

        // Clash Coins Trade Item (Slot 36)
        List<String> clashLore = new ArrayList<>();
        clashLore.add("");
        clashLore.add("§7Tausche unnötige Items ein:");
        int sum = 0;

        for (Map.Entry<MineMaterialType, Integer> entry : mineItems.entrySet()) {
            MineMaterialType type = entry.getKey();
            if (!type.isValuable()) {
                int amount = entry.getValue();
                int value = type.getValue();
                int total = value * amount;
                sum += total;

                clashLore.add("§8" + amount + "x " + type.getDisplayName() + " = " + total + " Clash Coins");
            }
        }

        clashLore.add("");
        clashLore.add("§7Gesamt: §e" + sum);
        clashLore.add("§7Neuer Kontostand: §6" + (this.resources.getClashCoins() + sum));

        ItemStack clashCoins = ItemStackUtil.createItemSkull(
            COINS_BASE64,
            "§eClash Coins tauschen",
            clashLore
        );
        inv.setItem(36, clashCoins);


        
     // Booster (Slots 37–39)
        int boosterSlot = 37;
        long now = System.currentTimeMillis();

        for (int i = 0; i < MineBoosterType.values().length; i++) {
            MineBoosterType boosterType = MineBoosterType.values()[i];

            String displayName = switch (boosterType) {
                case RESOURCE_MULTIPLIER -> "§aBooster: Ressourcenglück";
                case PICKAXE_LEVEL_PLUS -> "§bBooster: Spitzhacke +20";
                case NO_COOLDOWN -> "§6Booster: Kein Cooldown";
            };

            List<String> lore = new ArrayList<>();
            lore.add("");

            switch (boosterType) {
                case RESOURCE_MULTIPLIER -> {
                    lore.add("§7Erhöht die Erzwahrscheinlichkeit in deiner nächsten Mine");
                    lore.add("§7Dauer: §e" + this.formatDuration(boosterType.getDurationInSeconds()));
                }
                case PICKAXE_LEVEL_PLUS -> {
                    lore.add("§7Temporär +20 Level auf deine Spitzhacke");
                    lore.add("§7Dauer: §e" + this.formatDuration(boosterType.getDurationInSeconds()));
                }
                case NO_COOLDOWN -> {
                    lore.add("§7Erlaube sofortigen Mineneintritt nach Verlassen");
                }
            }

            // ✨ Booster-Zeit überprüfen und ggf. anzeigen
            long expiresAt = ClashMC.getInstance().getDatabaseManager().mine().getBoosterExpires(kingID, i + 1);
            if (expiresAt > now) {
                long remaining = (expiresAt - now) / 1000;
                lore.add("");
                lore.add("§a✔ Aktiver Booster!");
                lore.add("§7Noch aktiv für: §e" + formatDuration((int) remaining));
            } else {
                lore.add("");
                lore.add("§7Kosten: §c" + boosterType.getCost() + "x " + boosterType.getAnzeigeName());
                lore.add("");
                lore.add("§e§lKlicke zum Aktivieren");
            }

            Material material = switch (boosterType) {
                case RESOURCE_MULTIPLIER -> Material.EXPERIENCE_BOTTLE;
                case PICKAXE_LEVEL_PLUS -> Material.NETHER_STAR;
                case NO_COOLDOWN -> Material.CLOCK;
            };

            ItemStack boosterItem = ItemStackUtil.createItem(
                material,
                displayName,
                lore
            );

            inv.setItem(boosterSlot++, boosterItem);
        }

        // Spitzhacken-Upgrade-Anzeige (Slot 40)
        int pickaxeLevel = ClashMC.getInstance().getDatabaseManager().mine().getPickaxeLevel(this.kingID);
        int upgradeCost = (int) (75 * Math.pow(1.8, pickaxeLevel));

        int efficiency = pickaxeLevel / 2;
        int fortune = pickaxeLevel - efficiency;

        ItemStack pickaxe = ItemStackUtil.createItem(
            Material.DIAMOND_PICKAXE,
            "§bSpitzhacken-Upgrade",
            List.of(
                "",
                "§7Aktuelles Level: §e" + pickaxeLevel,
                "§7Nach Upgrade: §a" + (pickaxeLevel + 1),
                "",
                "§7Effizienz: §a" + efficiency + " §8→ §a" + ((pickaxeLevel + 1) / 2),
                "§7Glück: §a" + fortune + " §8→ §a" + ((pickaxeLevel + 1) - ((pickaxeLevel + 1) / 2)),
                "",
                "§7Kosten: §c" + upgradeCost + " Kohle"
            )
        );
        inv.setItem(40, pickaxe);

        // King Coins Trade Item (Slot 41)
        int emeralds = mineItems.getOrDefault(MineMaterialType.EMERALD, 0);
        ItemStack kingCoins = ItemStackUtil.createItemSkull(
            KINGCOINS_BASE64,
            "§6King Coins tauschen",
            List.of(
                "",
                "§7Tausche Smaragde ein:",
                "§8" + emeralds + " Smaragde = " + emeralds + " King Coins",
                "",
                "§7Aktuelle King Coins: §e" + this.resources.getKingCoins(),
                "§7Neuer Kontostand: §b" + (this.resources.getKingCoins() + emeralds)
            )
        );
        inv.setItem(41, kingCoins);


        // 6. Reihe: Schwarzes Glas (Slots 45–53)
        for (int i = 45; i <= 53; i++) {
            inv.setItem(i, ItemStackUtil.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null));
        }

        // Zurück-Button (Slot 49)
        ItemStack back = ItemStackUtil.createItemSkull(
            BACK_BASE64,
            "§cZurück",
            List.of("§7Klicke, um zurückzukehren.")
        );
        inv.setItem(49, back);

        player.openInventory(inv);
    }
    
    
    // Hilfsmethode für MM:SS-Format
    private String formatDuration(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
