package de.payne.clashmc.attacks.equipment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;

public class AttackerLoadouts {

    private final List<ScaledEquipmentTemplate> attackTemplates = new ArrayList<>();

    public AttackerLoadouts() {
        setupLoadouts();
    }

    private void setupLoadouts() {
        // Holz-Loadout
        LevelRange woodRange = LevelRange.WOOD;
        attackTemplates.addAll(List.of(
            createTool(Material.WOODEN_PICKAXE, "Holzspitzhacke", 1, woodRange),
            createTool(Material.WOODEN_SHOVEL, "Holzschaufel", 1, woodRange),
            createTool(Material.WOODEN_AXE, "Holzaxt", 1, woodRange),
            createSword(Material.WOODEN_SWORD, "Holzschwert", 1, woodRange),
            createSpecialItem(Material.TNT, "TNT", woodRange),
            createSpecialItem(Material.COBWEB, "Spinnweben", woodRange)
        ));
        attackTemplates.addAll(createArmorSet(
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
            Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            0, woodRange
        ));

        // Stein-Loadout
        LevelRange stoneRange = LevelRange.STONE;
        attackTemplates.add(createTool(Material.STONE_PICKAXE, "Steinspitzhacke", 2, stoneRange));
        attackTemplates.add(createTool(Material.STONE_SHOVEL, "Steinschaufel", 2, stoneRange));
        attackTemplates.add(createTool(Material.STONE_AXE, "Steinaxt", 2, stoneRange));
        attackTemplates.add(createSword(Material.STONE_SWORD, "Steinschwert", 2, stoneRange));
        attackTemplates.addAll(createArmorSet(
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
            Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            1, stoneRange
        ));
        attackTemplates.add(createSpecialItem(Material.TNT, "TNT", stoneRange));
        attackTemplates.add(createSpecialItem(Material.COBWEB, "Spinnweben", stoneRange));

     // Eisen-Loadout
        LevelRange ironRange = LevelRange.IRON;
        attackTemplates.add(createTool(Material.IRON_PICKAXE, "Eisenspitzhacke", 3, ironRange));
        attackTemplates.add(createTool(Material.IRON_SHOVEL, "Eisenschaufel", 3, ironRange));
        attackTemplates.add(createTool(Material.IRON_AXE, "Eisenaxt", 3, ironRange));
        attackTemplates.add(createSword(Material.IRON_SWORD, "Eisenschwert", 3, ironRange));
        attackTemplates.addAll(createArmorSet(
            Material.IRON_HELMET, Material.IRON_CHESTPLATE,
            Material.IRON_LEGGINGS, Material.IRON_BOOTS,
            2, ironRange
        ));
        attackTemplates.add(createSpecialItem(Material.TNT, "TNT", ironRange));
        attackTemplates.add(createSpecialItem(Material.COBWEB, "Spinnweben", ironRange));
        attackTemplates.add(createSpecialItem(Material.TRIDENT, "Dreizack", ironRange));

        // Gold-Loadout mit Leder-Rüstung
        LevelRange goldLeatherRange = LevelRange.GOLD_LEATHER;
        attackTemplates.add(createTool(Material.GOLDEN_PICKAXE, "Goldspitzhacke", 4, goldLeatherRange));
        attackTemplates.add(createTool(Material.GOLDEN_SHOVEL, "Goldschaufel", 4, goldLeatherRange));
        attackTemplates.add(createTool(Material.GOLDEN_AXE, "Goldaxt", 4, goldLeatherRange));
        attackTemplates.add(createSword(Material.GOLDEN_SWORD, "Goldschwert", 4, goldLeatherRange));
        attackTemplates.addAll(createArmorSet(
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
            Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            3, goldLeatherRange
        ));
        attackTemplates.add(createSpecialItem(Material.TNT, "TNT", goldLeatherRange));
        attackTemplates.add(createSpecialItem(Material.COBWEB, "Spinnweben", goldLeatherRange));
        attackTemplates.add(createSpecialItem(Material.CROSSBOW, "Armbrust", goldLeatherRange));

        // Gold-Rüstung & Spezialwaffen
        LevelRange goldRange = LevelRange.GOLD_SPECIAL;
        attackTemplates.add(createTool(Material.GOLDEN_PICKAXE, "Goldspitzhacke", 5, goldRange));
        attackTemplates.add(createTool(Material.GOLDEN_SHOVEL, "Goldschaufel", 5, goldRange));
        attackTemplates.add(createTool(Material.GOLDEN_AXE, "Goldaxt", 5, goldRange));
        attackTemplates.add(createSword(Material.GOLDEN_SWORD, "Goldschwert", 5, goldRange));
        attackTemplates.addAll(createArmorSet(
            Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE,
            Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
            4, goldRange
        ));
        attackTemplates.add(createSpecialItem(Material.TNT, "TNT", goldRange));
        attackTemplates.add(createSpecialItem(Material.COBWEB, "Spinnweben", goldRange));
        attackTemplates.add(createSpecialItem(Material.CROSSBOW, "Armbrust", goldRange));
        attackTemplates.add(createSpecialItem(Material.FIREWORK_ROCKET, "Rauchbombe", goldRange));

        // Diamant/Netherit + Spezial
        LevelRange diamondRange = LevelRange.DIAMOND_NETHERITE;
        attackTemplates.add(createTool(Material.DIAMOND_PICKAXE, "Diamantspitzhacke", 6, diamondRange));
        attackTemplates.add(createTool(Material.DIAMOND_SHOVEL, "Diamantschaufel", 6, diamondRange));
        attackTemplates.add(createTool(Material.DIAMOND_AXE, "Diamantaxt", 6, diamondRange));
        attackTemplates.add(createSword(Material.DIAMOND_SWORD, "Diamantschwert", 6, diamondRange));
        attackTemplates.addAll(createArmorSet(
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
            5, diamondRange
        ));
        attackTemplates.add(createSpecialItem(Material.TNT, "TNT", diamondRange));
        attackTemplates.add(createSpecialItem(Material.COBWEB, "Spinnweben", diamondRange));
        attackTemplates.add(createSpecialItem(Material.CROSSBOW, "Armbrust", diamondRange));
        attackTemplates.add(createSpecialItem(Material.TRIDENT, "Dreizack", diamondRange));
        attackTemplates.add(createSpecialItem(Material.FIREWORK_ROCKET, "Rauchbombe", diamondRange));
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
        armorPieces.add(createArmorPiece(leggings, "Hose", protectionLevel, levelRange));
        armorPieces.add(createArmorPiece(boots, "Stiefel", protectionLevel, levelRange));
        return armorPieces;
    }

    private ScaledEquipmentTemplate createArmorPiece(Material material, String pieceName, int protectionLevel, LevelRange levelRange) {
        return new ScaledEquipmentTemplate(
            new EquipmentTemplate(
                material,
                material.name().toLowerCase() + " " + pieceName,
                Map.of(Enchantment.PROTECTION, protectionLevel, Enchantment.UNBREAKING, 3),
                Map.of(Attribute.ARMOR, 3.0 + protectionLevel * 1.5),
                Map.of(Attribute.ARMOR, 1.5 + protectionLevel * 0.75),
                List.of("Schutzstufe " + protectionLevel)
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
                List.of("Spezieller Gegenstand")
            ),
            levelRange.getMin(),
            levelRange.getMax()
        );
    }

    private double getAttackDamageForTool(Material material) {
        return switch (material) {
            case WOODEN_PICKAXE -> 2.0;
            case STONE_PICKAXE -> 3.0;
            case IRON_PICKAXE -> 4.0;
            case GOLDEN_PICKAXE -> 3.5;
            case DIAMOND_PICKAXE -> 5.0;
            default -> 1.0;
        };
    }

    private double getSwordDamage(Material material) {
        return switch (material) {
            case WOODEN_SWORD -> 4.0;
            case STONE_SWORD -> 5.0;
            case IRON_SWORD -> 6.0;
            case GOLDEN_SWORD -> 4.5;
            case DIAMOND_SWORD -> 7.0;
            default -> 3.0;
        };
    }

    public List<ScaledEquipmentTemplate> getAttackTemplates() {
        return attackTemplates;
    }
}