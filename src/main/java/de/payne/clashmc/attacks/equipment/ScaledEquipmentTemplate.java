package de.payne.clashmc.attacks.equipment;

public class ScaledEquipmentTemplate {

    public final EquipmentTemplate template;
    public final double minLevelPercent;
    public final double maxLevelPercent;

    public ScaledEquipmentTemplate(EquipmentTemplate template, double minLevelPercent, double maxLevelPercent) {
        this.template = template;
        this.minLevelPercent = minLevelPercent;
        this.maxLevelPercent = maxLevelPercent;
    }

    public boolean isValidForScale(double scale) {
        return scale >= minLevelPercent && scale <= maxLevelPercent;
    }
}
