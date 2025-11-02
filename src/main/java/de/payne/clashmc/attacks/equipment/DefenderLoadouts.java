package de.payne.clashmc.attacks.equipment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;

public class DefenderLoadouts {

    private final List<ScaledEquipmentTemplate> defenseTemplates = new ArrayList<>();

    public DefenderLoadouts() {
        setupLoadouts();
    }

    private void setupLoadouts() {
        // Holz-Loadout (LevelRange WOOD)
        LevelRange woodRange = LevelRange.WOOD;
        defenseTemplates.addAll(List.of(
            createTool(Material.WOODEN_PICKAXE, "Holzspitzhacke", 1, woodRange),
            createTool(Material.WOODEN_SHOVEL, "Holzschaufel", 1, woodRange),
            createTool(Material.WOODEN_AXE, "Holzaxt", 1, woodRange),
            createSword(Material.WOODEN_SWORD, "Holzschwert", 1, woodRange),
            createShield(woodRange),
            createSpecialItem(Material.COBWEB, "Spinnweben", woodRange),
            createSpecialItem(Material.HONEY_BOTTLE, "Honigflasche", woodRange)
        ));
        defenseTemplates.addAll(createArmorSet(
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
            Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            0, woodRange
        ));

        // Stein-Loadout (LevelRange STONE)
        LevelRange stoneRange = LevelRange.STONE;
        defenseTemplates.add(createTool(Material.STONE_PICKAXE, "Steinspitzhacke", 2, stoneRange));
        defenseTemplates.add(createTool(Material.STONE_SHOVEL, "Steinschaufel", 2, stoneRange));
        defenseTemplates.add(createTool(Material.STONE_AXE, "Steinaxt", 2, stoneRange));
        defenseTemplates.add(createSword(Material.STONE_SWORD, "Steinschwert", 2, stoneRange));
        defenseTemplates.add(createShield(stoneRange));
        defenseTemplates.addAll(createArmorSet(
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
            Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            1, stoneRange
        ));
        defenseTemplates.add(createSpecialItem(Material.COBWEB, "Spinnweben", stoneRange));
        defenseTemplates.add(createSpecialItem(Material.HONEY_BOTTLE, "Honigflasche", stoneRange));
        defenseTemplates.add(createSpecialItem(Material.SPLASH_POTION, "Heiltrank", stoneRange));

        // Eisen-Loadout (LevelRange IRON)
        LevelRange ironRange = LevelRange.IRON;
        defenseTemplates.add(createTool(Material.IRON_PICKAXE, "Eisenspitzhacke", 3, ironRange));
        defenseTemplates.add(createTool(Material.IRON_SHOVEL, "Eisenschaufel", 3, ironRange));
        defenseTemplates.add(createTool(Material.IRON_AXE, "Eisenaxt", 3, ironRange));
        defenseTemplates.add(createSword(Material.IRON_SWORD, "Eisenschwert", 3, ironRange));
        defenseTemplates.add(createShield(ironRange));
        defenseTemplates.addAll(createArmorSet(
            Material.IRON_HELMET, Material.IRON_CHESTPLATE,
            Material.IRON_LEGGINGS, Material.IRON_BOOTS,
            2, ironRange
        ));
        defenseTemplates.add(createSpecialItem(Material.COBWEB, "Spinnweben", ironRange));
        defenseTemplates.add(createSpecialItem(Material.HONEY_BOTTLE, "Honigflasche", ironRange));
        defenseTemplates.add(createSpecialItem(Material.SPLASH_POTION, "Heiltrank", ironRange));
        defenseTemplates.add(createSpecialItem(Material.SHIELD, "Schild", ironRange));

        // Gold-Loadout mit Leder-Rüstung (LevelRange GOLD_LEATHER)
        LevelRange goldLeatherRange = LevelRange.GOLD_LEATHER;
        defenseTemplates.add(createTool(Material.GOLDEN_PICKAXE, "Goldspitzhacke", 4, goldLeatherRange));
        defenseTemplates.add(createTool(Material.GOLDEN_SHOVEL, "Goldschaufel", 4, goldLeatherRange));
        defenseTemplates.add(createTool(Material.GOLDEN_AXE, "Goldaxt", 4, goldLeatherRange));
        defenseTemplates.add(createSword(Material.GOLDEN_SWORD, "Goldschwert", 4, goldLeatherRange));
        defenseTemplates.add(createShield(goldLeatherRange));
        defenseTemplates.addAll(createArmorSet(
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
            Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            3, goldLeatherRange
        ));
        defenseTemplates.add(createSpecialItem(Material.COBWEB, "Spinnweben", goldLeatherRange));
        defenseTemplates.add(createSpecialItem(Material.HONEY_BOTTLE, "Honigflasche", goldLeatherRange));
        defenseTemplates.add(createSpecialItem(Material.SPLASH_POTION, "Heiltrank", goldLeatherRange));
        defenseTemplates.add(createSpecialItem(Material.SHIELD, "Schild", goldLeatherRange));

        // Gold-Rüstung & Spezialwaffen (LevelRange GOLD_SPECIAL)
        LevelRange goldRange = LevelRange.GOLD_SPECIAL;
        defenseTemplates.add(createTool(Material.GOLDEN_PICKAXE, "Goldspitzhacke", 5, goldRange));
        defenseTemplates.add(createTool(Material.GOLDEN_SHOVEL, "Goldschaufel", 5, goldRange));
        defenseTemplates.add(createTool(Material.GOLDEN_AXE, "Goldaxt", 5, goldRange));
        defenseTemplates.add(createSword(Material.GOLDEN_SWORD, "Goldschwert", 5, goldRange));
        defenseTemplates.add(createShield(goldRange));
        defenseTemplates.addAll(createArmorSet(
            Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE,
            Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
            4, goldRange
        ));
        defenseTemplates.add(createSpecialItem(Material.COBWEB, "Spinnweben", goldRange));
        defenseTemplates.add(createSpecialItem(Material.HONEY_BOTTLE, "Honigflasche", goldRange));
        defenseTemplates.add(createSpecialItem(Material.SPLASH_POTION, "Heiltrank", goldRange));
        defenseTemplates.add(createSpecialItem(Material.SHIELD, "Schild", goldRange));
        defenseTemplates.add(createSpecialItem(Material.TOTEM_OF_UNDYING, "Totem des Unsterblichkeits", goldRange));

        // Diamant/Netherit + Spezial (LevelRange DIAMOND_NETHERITE)
        LevelRange diamondRange = LevelRange.DIAMOND_NETHERITE;
        defenseTemplates.add(createTool(Material.DIAMOND_PICKAXE, "Diamantspitzhacke", 6, diamondRange));
        defenseTemplates.add(createTool(Material.DIAMOND_SHOVEL, "Diamantschaufel", 6, diamondRange));
        defenseTemplates.add(createTool(Material.DIAMOND_AXE, "Diamantaxt", 6, diamondRange));
        defenseTemplates.add(createSword(Material.DIAMOND_SWORD, "Diamantschwert", 6, diamondRange));
        defenseTemplates.add(createShield(diamondRange));
        defenseTemplates.addAll(createArmorSet(
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
            5, diamondRange
        ));
        defenseTemplates.add(createSpecialItem(Material.COBWEB, "Spinnweben", diamondRange));
        defenseTemplates.add(createSpecialItem(Material.HONEY_BOTTLE, "Honigflasche", diamondRange));
        defenseTemplates.add(createSpecialItem(Material.SPLASH_POTION, "Heiltrank", diamondRange));
        defenseTemplates.add(createSpecialItem(Material.SHIELD, "Schild", diamondRange));
        defenseTemplates.add(createSpecialItem(Material.TOTEM_OF_UNDYING, "Totem des Unsterblichkeits", diamondRange));
    }

    // Hilfsmethoden zum Erstellen der Items

    private ScaledEquipmentTemplate createTool(Material material, String displayName, int efficiencyLevel, LevelRange levelRange) {
        return new ScaledEquipmentTemplate(
            new EquipmentTemplate(
                material,
                displayName,
                Map.of(Enchantment.EFFICIENCY, efficiencyLevel, Enchantment.UNBREAKING, 2),
                Map.of(Attribute.ATTACK_DAMAGE, getAttackDamageForTool(material)),
                Map.of(Attribute.ATTACK_DAMAGE, getAttackDamageForTool(material) / 2),
                List.of("Effizienz " + efficiencyLevel)
            ),
            levelRange.getMin(),
            levelRange.getMax()
        );
    }

    private ScaledEquipmentTemplate createSword(Material material, String displayName, int sharpnessLevel, LevelRange levelRange) {
        return new ScaledEquipmentTemplate(
            new EquipmentTemplate(
                material,
                displayName,
                Map.of(Enchantment.SHARPNESS, sharpnessLevel, Enchantment.UNBREAKING, 3),
                Map.of(Attribute.ATTACK_DAMAGE, getSwordDamage(material)),
                Map.of(Attribute.ATTACK_DAMAGE, getSwordDamage(material) / 2),
                List.of("Schärfe " + sharpnessLevel)
            ),
            levelRange.getMin(),
            levelRange.getMax()
        );
    }

    private List<ScaledEquipmentTemplate> createArmorSet(Material helmet, Material chestplate, Material leggings, Material boots, int protectionLevel, LevelRange levelRange) {
        List<ScaledEquipmentTemplate> armorPieces = new ArrayList<>();
        armorPieces.add(createArmorPiece(helmet, "Helm", protectionLevel, levelRange));
        armorPieces.add(createArmorPiece(chestplate, "Brustplatte", protectionLevel, levelRange));
        armorPieces.add(createArmorPiece(leggings, "Beinlinge", protectionLevel, levelRange));
        armorPieces.add(createArmorPiece(boots, "Stiefel", protectionLevel, levelRange));
        return armorPieces;
    }

    private ScaledEquipmentTemplate createArmorPiece(Material material, String pieceName, int protectionLevel, LevelRange levelRange) {
        return new ScaledEquipmentTemplate(
            new EquipmentTemplate(
                material,
                material.name().substring(0, 1).toUpperCase() + material.name().substring(1).toLowerCase() + " " + pieceName,
                Map.of(Enchantment.PROTECTION, protectionLevel, Enchantment.UNBREAKING, 3),
                Map.of(Attribute.ARMOR, getArmorValue(material)),
                Map.of(Attribute.ARMOR_TOUGHNESS, getArmorToughness(material)),
                List.of("Schutz " + protectionLevel)
            ),
            levelRange.getMin(),
            levelRange.getMax()
        );
    }

    private ScaledEquipmentTemplate createShield(LevelRange levelRange) {
        return new ScaledEquipmentTemplate(
            new EquipmentTemplate(
                Material.SHIELD,
                "Schild",
                Map.of(Enchantment.UNBREAKING, 3),
                Map.of(Attribute.ARMOR, 2.0),
                Map.of(),
                List.of("Schutzschild")
            ),
            levelRange.getMin(),
            levelRange.getMax()
        );
    }

    private ScaledEquipmentTemplate createSpecialItem(Material material, String displayName, LevelRange levelRange) {
        return new ScaledEquipmentTemplate(
            new EquipmentTemplate(
                material,
                displayName,
                Map.of(),
                Map.of(),
                Map.of(),
                List.of()
            ),
            levelRange.getMin(),
            levelRange.getMax()
        );
    }

    // Hilfsmethoden für Attributwerte (stark vereinfacht)

    private double getAttackDamageForTool(Material material) {
        switch (material) {
            case WOODEN_PICKAXE:
                return 2.0;
            case STONE_PICKAXE:
                return 3.0;
            case IRON_PICKAXE:
                return 4.0;
            case GOLDEN_PICKAXE:
                return 3.0;
            case DIAMOND_PICKAXE:
                return 5.0;
            case NETHERITE_PICKAXE:
                return 6.0;
            default:
                return 1.0;
        }
    }

    private double getSwordDamage(Material material) {
        switch (material) {
            case WOODEN_SWORD:
                return 4.0;
            case STONE_SWORD:
                return 5.0;
            case IRON_SWORD:
                return 6.0;
            case GOLDEN_SWORD:
                return 4.0;
            case DIAMOND_SWORD:
                return 7.0;
            case NETHERITE_SWORD:
                return 8.0;
            default:
                return 3.0;
        }
    }

    private double getArmorValue(Material material) {
        switch (material) {
            case LEATHER_HELMET:
                return 1.0;
            case LEATHER_CHESTPLATE:
                return 3.0;
            case LEATHER_LEGGINGS:
                return 2.0;
            case LEATHER_BOOTS:
                return 1.0;

            case IRON_HELMET:
                return 2.0;
            case IRON_CHESTPLATE:
                return 6.0;
            case IRON_LEGGINGS:
                return 5.0;
            case IRON_BOOTS:
                return 2.0;

            case GOLDEN_HELMET:
                return 2.0;
            case GOLDEN_CHESTPLATE:
                return 5.0;
            case GOLDEN_LEGGINGS:
                return 3.0;
            case GOLDEN_BOOTS:
                return 1.0;

            case NETHERITE_HELMET:
                return 3.0;
            case NETHERITE_CHESTPLATE:
                return 8.0;
            case NETHERITE_LEGGINGS:
                return 6.0;
            case NETHERITE_BOOTS:
                return 3.0;

            default:
                return 0.0;
        }
    }

    private double getArmorToughness(Material material) {
        switch (material) {
            case IRON_HELMET:
            case IRON_CHESTPLATE:
            case IRON_LEGGINGS:
            case IRON_BOOTS:
                return 0.0;

            case GOLDEN_HELMET:
            case GOLDEN_CHESTPLATE:
            case GOLDEN_LEGGINGS:
            case GOLDEN_BOOTS:
                return 0.0;

            case NETHERITE_HELMET:
            case NETHERITE_CHESTPLATE:
            case NETHERITE_LEGGINGS:
            case NETHERITE_BOOTS:
                return 3.0;

            default:
                return 0.0;
        }
    }

    public List<ScaledEquipmentTemplate> getDefenseTemplates() {
        return defenseTemplates;
    }
}