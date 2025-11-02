package de.payne.clashmc.mine;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.utils.LogUtil;

import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class MineResourceReplacer {

    /**
     * Lädt Ore-Chances aus Config.
     */
    private static LinkedHashMap<Material, Double> getBaseChances() {
        LinkedHashMap<Material, Double> chances = new LinkedHashMap<>();
        chances.put(Material.EMERALD_ORE, ClashMC.getInstance().getConfigManager().getMineOreChance("emerald"));
        chances.put(Material.DIAMOND_ORE, ClashMC.getInstance().getConfigManager().getMineOreChance("diamond"));
        chances.put(Material.GOLD_ORE, ClashMC.getInstance().getConfigManager().getMineOreChance("gold"));
        chances.put(Material.IRON_ORE, ClashMC.getInstance().getConfigManager().getMineOreChance("iron"));
        chances.put(Material.COAL_ORE, ClashMC.getInstance().getConfigManager().getMineOreChance("coal"));
        chances.put(Material.COPPER_ORE, ClashMC.getInstance().getConfigManager().getMineOreChance("copper"));
        return chances;
    }

    private static double getMaxReplacePercentage() {
        return ClashMC.getInstance().getConfigManager().getMineOreMaxReplacePercentage();
    }

    /**
     * Ersetzt Stone-Blöcke im Clipboard prozentual durch wertvolle Blöcke, abhängig vom Spielerlevel
     * und temporären Upgrade-Multiplikatoren.
     *
     * @param clipboard Clipboard mit der Mine-Schematic (wird modifiziert)
     * @param playerLevel aktuelles Dorflevel des Spielers
     * @param maxLevel maximales Dorflevel
     * @param upgradeMultipliers Map mit Material -> Multiplikator für Wahrscheinlichkeiten (optional, kann leer sein)
     */
    public static void replaceStoneBlocks(Clipboard clipboard, int playerLevel, int maxLevel, Map<Material, Double> upgradeMultipliers) {
        if (clipboard == null) return;
        if (maxLevel <= 0) maxLevel = 1;

        // Werte aus Config laden
        double maxReplacePercentage = getMaxReplacePercentage();
        LinkedHashMap<Material, Double> baseChances = getBaseChances();

        double replaceChance = ((double) playerLevel / maxLevel) * maxReplacePercentage;
        replaceChance = Math.min(replaceChance, maxReplacePercentage);

        Map<Material, Double> adjustedChances = new LinkedHashMap<>();
        double totalChance = 0;
        for (Map.Entry<Material, Double> entry : baseChances.entrySet()) {
            double multiplier = upgradeMultipliers.getOrDefault(entry.getKey(), 1.0);
            double adjusted = entry.getValue() * multiplier;
            adjustedChances.put(entry.getKey(), adjusted);
            totalChance += adjusted;
        }

        Map<Material, Double> normalizedChances = new LinkedHashMap<>();
        for (Map.Entry<Material, Double> entry : adjustedChances.entrySet()) {
            normalizedChances.put(entry.getKey(), entry.getValue() / totalChance);
        }

        Random random = new Random();
        BlockVector3 min = clipboard.getMinimumPoint();
        BlockVector3 max = clipboard.getMaximumPoint();

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockVector3 position = BlockVector3.at(x, y, z);
                    BlockState block = clipboard.getBlock(position);
                    BlockType type = block.getBlockType();

                    if (type != null && type.getId().equals("minecraft:stone")) {
                        if (random.nextDouble() <= replaceChance) {
                            Material newMat = getRandomResource(random, normalizedChances);
                            BlockType newBlockType = bukkitToBlockType(newMat);
                            if (newBlockType != null) {
                                try {
                                    BaseBlock newBlock = newBlockType.getDefaultState().toBaseBlock();
                                    clipboard.setBlock(position, newBlock);
                                } catch (WorldEditException e) {
                                    logInvalidBlock(position, newMat, e);
                                }
                            } else {
                                LogUtil.logError(ClashMC.getInstance(), "Unbekannter BlockType für Material: " + newMat);
                            }
                        }
                    }
                }
            }
        }
    }

    private static Material getRandomResource(Random random, Map<Material, Double> normalizedChances) {
        double r = random.nextDouble();
        double sum = 0;
        for (Map.Entry<Material, Double> entry : normalizedChances.entrySet()) {
            sum += entry.getValue();
            if (r <= sum) return entry.getKey();
        }
        return Material.COPPER_ORE;
    }

    private static BlockType bukkitToBlockType(Material material) {
        switch (material) {
            case STONE: return BlockTypes.STONE;
            case COPPER_ORE: return BlockTypes.COPPER_ORE;
            case COAL_ORE: return BlockTypes.COAL_ORE;
            case IRON_ORE: return BlockTypes.IRON_ORE;
            case GOLD_ORE: return BlockTypes.GOLD_ORE;
            case DIAMOND_ORE: return BlockTypes.DIAMOND_ORE;
            case EMERALD_ORE: return BlockTypes.EMERALD_ORE;
            default: return null;
        }
    }
    
    /**
     * Protokolliert Fehler beim Block-Setzen.
     *
     * @param position Position des Blocks
     * @param material Gewünschtes Material
     * @param exception Die ausgelöste Exception
     */
    private static void logInvalidBlock(BlockVector3 position, Material material, Exception exception) {
        String errorMessage = "Fehler beim Setzen des Blocks an (" + position.getX() + "," + position.getY() + "," + position.getZ()
                + ") -> " + material + ": " + exception.getMessage();
        LogUtil.logError(ClashMC.getInstance(), errorMessage);
        exception.printStackTrace();
    }
}