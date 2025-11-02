package de.payne.clashmc.attacks.equipment;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import de.payne.clashmc.ClashMC;

public class EquipmentManager {

    private final AttackerLoadouts attackerLoadouts;
    private final DefenderLoadouts defenderLoadouts;

    public EquipmentManager() {
        this.attackerLoadouts = new AttackerLoadouts();
        this.defenderLoadouts = new DefenderLoadouts();
    }

    public AttackerLoadouts getAttackerLoadouts() {
        return attackerLoadouts;
    }

    public DefenderLoadouts getDefenderLoadouts() {
        return defenderLoadouts;
    }
    
    private List<ItemStack> assignAttackerLoadout(int kingId) {
        double percentage = getLevelPercentage(kingId);
        return attackerLoadouts.getAttackTemplates().stream()
                .filter(t -> t.isValidForScale(percentage))
                .map(t -> t.template.generateItem(percentage))
                .toList();
    }

    private List<ItemStack> assignDefenderLoadout(int kingId) {
        double percentage = getLevelPercentage(kingId);
        return defenderLoadouts.getDefenseTemplates().stream()
                .filter(t -> t.isValidForScale(percentage))
                .map(t -> t.template.generateItem(percentage))
                .toList();
    }

    private double getLevelPercentage(int kingId) {
        double level = 1;
        try {
            level = ClashMC.getInstance().getDatabaseManager().villages().getVillageLevel(kingId);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0.0;
        }
        double maxLevel = ClashMC.getInstance().getSchematicManager().getAvailableLevels().getLast();
        return level / maxLevel;  // Skaliert auf 0.0 bis 1.0
    }
    
    private void equipPlayer(Player player, List<ItemStack> items) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();

        ItemStack helmet = null;
        ItemStack chestplate = null;
        ItemStack leggings = null;
        ItemStack boots = null;
        ItemStack sword = null;

        List<ItemStack> tools = new ArrayList<>();
        List<ItemStack> others = new ArrayList<>();

        for (ItemStack item : items) {
            if (item == null) continue;
            Material mat = item.getType();

            // Rüstungsteile erkennen
            if (mat.name().endsWith("_HELMET") || mat.name().endsWith("_SKULL")) {
                helmet = item;
            } else if (mat.name().endsWith("_CHESTPLATE")) {
                chestplate = item;
            } else if (mat.name().endsWith("_LEGGINGS")) {
                leggings = item;
            } else if (mat.name().endsWith("_BOOTS")) {
                boots = item;
            }
            // Schwert erkennen
            else if (mat.name().endsWith("_SWORD")) {
                if (sword == null) sword = item;
                else others.add(item);
            }
            // Werkzeuge erkennen
            else if (mat.name().endsWith("_PICKAXE") || mat.name().endsWith("_AXE") || mat.name().endsWith("_SHOVEL") || mat.name().endsWith("_HOE")) {
                tools.add(item);
            }
            else {
                others.add(item);
            }
        }

        // Rüstung anlegen
        inventory.setHelmet(helmet);
        inventory.setChestplate(chestplate);
        inventory.setLeggings(leggings);
        inventory.setBoots(boots);

        // Hotbar füllen: Erst Schwert, dann Werkzeuge, dann andere Items
        int slot = 0;
        if (sword != null) inventory.setItem(slot++, sword);
        for (ItemStack tool : tools) {
            if (slot >= 9) break;
            inventory.setItem(slot++, tool);
        }
        for (ItemStack other : others) {
            if (slot >= 9) break;
            inventory.setItem(slot++, other);
        }

        // Übrige Items in restliches Inventar (optional, falls noch was übrig ist)
        // Da wir alle Items verteilt haben, ist das hier meist leer, kann aber hinzugefügt werden, falls gewünscht

        player.updateInventory();
    }
    
    public void giveAttackerLoadout(Player player, int kingId) {
        List<ItemStack> items = assignAttackerLoadout(kingId);
        equipPlayer(player, items);
    }

    public void giveDefenderLoadout(Player player, int kingId) {
        List<ItemStack> items = assignDefenderLoadout(kingId);
        equipPlayer(player, items);
    }
}