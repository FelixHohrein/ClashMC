package de.payne.clashmc.attacks.equipment;

public enum LevelRange {
    WOOD(0.0, 0.15),
    STONE(0.16, 0.30),
    IRON(0.31, 0.50),
    GOLD_LEATHER(0.51, 0.70),
    GOLD_SPECIAL(0.71, 0.85),
    DIAMOND_NETHERITE(0.86, 1.0);

    private final double min;
    private final double max;

    LevelRange(double min, double max) { this.min = min; this.max = max; }

    public double getMin() { return min; }
    public double getMax() { return max; }
}
