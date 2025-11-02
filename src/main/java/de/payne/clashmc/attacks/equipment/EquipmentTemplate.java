package de.payne.clashmc.attacks.equipment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EquipmentTemplate {

    private final Material baseMaterial;
    private final String displayName;
    private final Map<Enchantment, Integer> baseEnchantments; // Basis-Verzauberungen mit max Level
    private final Map<Attribute, Double> baseAttributes; // Basiswerte wie Schaden, Geschwindigkeit etc.
    private final Map<Attribute, Double> attributeScales; // Skalierung pro Attribut (multipliziert mit scale)
    private final List<String> baseLore;
    private final Random random = new Random();

    public EquipmentTemplate(Material baseMaterial,
                             String displayName,
                             Map<Enchantment, Integer> baseEnchantments,
                             Map<Attribute, Double> baseAttributes,
                             Map<Attribute, Double> attributeScales,
                             List<String> baseLore) {
        this.baseMaterial = baseMaterial;
        this.displayName = displayName;
        this.baseEnchantments = baseEnchantments != null ? baseEnchantments : new HashMap<>();
        this.baseAttributes = baseAttributes != null ? baseAttributes : new HashMap<>();
        this.attributeScales = attributeScales != null ? attributeScales : new HashMap<>();
        this.baseLore = baseLore != null ? baseLore : new ArrayList<>();
    }

    /**
     * Erzeugt ein ItemStack basierend auf dem Template und einem Skalierungsfaktor (0.0 bis 1.0).
     * @param scale Skala für Stärke, Verzauberungen etc.
     * @return ItemStack mit skalierten Attributen und Verzauberungen
     */
    public ItemStack generateItem(double scale) {
        ItemStack item = new ItemStack(baseMaterial);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        // Displayname mit Farbcode und Scaleanzeige
        meta.setDisplayName(ChatColor.GREEN + displayName + ChatColor.GRAY + " (Level: " + (int)(scale * 100) + "%)");

        // Lore hinzufügen (Basis + dynamische Infos)
        List<String> lore = new ArrayList<>(baseLore);
        lore.add(ChatColor.GRAY + "Stärke: " + String.format("%.1f", getScaledAttribute(Attribute.ATTACK_DAMAGE, scale)));
        meta.setLore(lore);

        // Verzauberungen skalieren
        for (Map.Entry<Enchantment, Integer> ench : baseEnchantments.entrySet()) {
            int maxLevel = ench.getValue();
            int enchLevel = Math.max(1, (int) Math.ceil(maxLevel * scale)); // mindestens 1
            meta.addEnchant(ench.getKey(), enchLevel, true);
        }

        // Attribute hinzufügen mit Modifikatoren
        int modifierCount = 0;
        for (Map.Entry<Attribute, Double> entry : baseAttributes.entrySet()) {
            Attribute attr = entry.getKey();
            double baseValue = entry.getValue();
            double scaleFactor = attributeScales.getOrDefault(attr, 0.0);
            double scaledValue = baseValue + scaleFactor * scale;

            AttributeModifier modifier = new AttributeModifier(
                    UUID.randomUUID(),
                    "equip_" + attr.name() + "_" + modifierCount++,
                    scaledValue,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlot.HAND // Für Waffen; passe ggf. für Rüstung an
            );
            meta.addAttributeModifier(attr, modifier);
        }

        // Optional: zufällige Spezialeffekte (Beispiel: Feuerverzauberung mit 20% Chance)
        if (random.nextDouble() < 0.2) {
            meta.addEnchant(Enchantment.FIRE_ASPECT, 1, true);
            lore.add(ChatColor.RED + "🔥 Feuer Effekt");
        }
        if (random.nextDouble() < 0.15) {
            meta.addEnchant(Enchantment.KNOCKBACK, 2, true);
            lore.add(ChatColor.DARK_PURPLE + "💥 Rückstoß-Kraft");
        }
        if (random.nextDouble() < 0.1) {
        	meta.addEnchant(Enchantment.WIND_BURST, 3, true);
        	lore.add(ChatColor.AQUA + "🌪 Windstoß");
        }
        if (random.nextDouble() < 0.1) {
            meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
            lore.add(ChatColor.BLUE + "🌀 Weitwinkel-Angriff");
        }
        if (random.nextDouble() < 0.05) {
            meta.addEnchant(Enchantment.SHARPNESS, 5, true);
            lore.add(ChatColor.DARK_RED + "☠️ Berserker-Kraft");
        }

        // Aktualisiere Lore nach allen möglichen Änderungen
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private double getScaledAttribute(Attribute attribute, double scale) {
        double base = baseAttributes.getOrDefault(attribute, 0.0);
        double scaleFactor = attributeScales.getOrDefault(attribute, 0.0);
        return base + scaleFactor * scale;
    }
    
    public ItemStack toItemStack() {
        ItemStack item = new ItemStack(baseMaterial);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        // Name
        meta.setDisplayName(ChatColor.YELLOW + displayName);

        // Lore (nur statische Basis)
        meta.setLore(new ArrayList<>(baseLore));

        // Basisverzauberungen
        for (Map.Entry<Enchantment, Integer> enchantment : baseEnchantments.entrySet()) {
            meta.addEnchant(enchantment.getKey(), enchantment.getValue(), true);
        }

        // Attribute (Basiswerte, ohne Skalierung)
        int modifierCount = 0;
        for (Map.Entry<Attribute, Double> attrEntry : baseAttributes.entrySet()) {
            Attribute attribute = attrEntry.getKey();
            double baseValue = attrEntry.getValue();

            AttributeModifier modifier = new AttributeModifier(
                UUID.randomUUID(),
                "template_" + attribute.name() + "_" + modifierCount++,
                baseValue,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HAND // Kann erweitert werden, falls Rüstung etc. unterstützt wird
            );

            meta.addAttributeModifier(attribute, modifier);
        }

        item.setItemMeta(meta);
        return item;
    }
}
