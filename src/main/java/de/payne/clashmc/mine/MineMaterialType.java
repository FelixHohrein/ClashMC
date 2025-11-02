package de.payne.clashmc.mine;

import org.bukkit.Material;

import lombok.Getter;

@Getter
public enum MineMaterialType {

    COBBLESTONE(Material.COBBLESTONE, "Stein", 1, false),
    RAIL(Material.RAIL, "Schienen", 3, false),
    OAK_WOOD(Material.OAK_PLANKS, "Holz", 2, false),
    OAK_FENCE(Material.OAK_FENCE, "Holzzaun", 2, false),
    COPPER(Material.RAW_COPPER, "Kupfer", 5, false),
    COAL(Material.COAL, "Kohle", 0, true),
    IRON(Material.RAW_IRON, "Eisen", 0, true),
    GOLD(Material.RAW_GOLD, "Gold", 0, true),
    DIAMOND(Material.DIAMOND, "Diamant", 0, true),
    EMERALD(Material.EMERALD, "Smaragd", 0, true);

    private final Material material;
    private final String displayName;
    private final int value;
    private final boolean isValuable;

    MineMaterialType(Material material, String displayName, int value, boolean isValuable) {
        this.material = material;
        this.displayName = displayName;
        this.value = value;
        this.isValuable = isValuable;
    }

    public static MineMaterialType fromMaterial(Material material) {
        for (MineMaterialType type : values()) {
            if (type.getMaterial() == material) return type;
        }
        return null;
    }

    public static MineMaterialType fromName(String name) {
        try {
            return MineMaterialType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}