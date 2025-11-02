package de.payne.clashmc.mine;

import de.payne.clashmc.ClashMC;
import org.bukkit.Material;

import lombok.Getter;

public enum MineBoosterType {
	RESOURCE_MULTIPLIER(Material.RAW_GOLD, "Gold"),
	PICKAXE_LEVEL_PLUS(Material.DIAMOND, "Diamant"),
	NO_COOLDOWN(Material.RAW_IRON, "Eisen");
	
	@Getter
	private final Material materialToPay;
	@Getter
	private final String anzeigeName;
	
	MineBoosterType(Material materialToPay, String anzeigeName){
		this.materialToPay = materialToPay;
		this.anzeigeName = anzeigeName;
	}
	
	/**
	 * Holt die Kosten aus der Config.
	 */
	public int getCost() {
		switch (this) {
			case RESOURCE_MULTIPLIER:
				return ClashMC.getInstance().getConfigManager().getMineBoosterResourceMultiplierCost();
			case PICKAXE_LEVEL_PLUS:
				return ClashMC.getInstance().getConfigManager().getMineBoosterPickaxeLevelPlusCost();
			case NO_COOLDOWN:
				return ClashMC.getInstance().getConfigManager().getMineBoosterNoCooldownCost();
			default:
				return 1000;
		}
	}
	
	/**
	 * Holt die Dauer in Sekunden aus der Config.
	 */
	public long getDurationInSeconds() {
		switch (this) {
			case RESOURCE_MULTIPLIER:
				return ClashMC.getInstance().getConfigManager().getMineBoosterResourceMultiplierDuration();
			case PICKAXE_LEVEL_PLUS:
				return ClashMC.getInstance().getConfigManager().getMineBoosterPickaxeLevelPlusDuration();
			case NO_COOLDOWN:
				return ClashMC.getInstance().getConfigManager().getMineBoosterNoCooldownDuration();
			default:
				return 1800;
		}
	}
}
