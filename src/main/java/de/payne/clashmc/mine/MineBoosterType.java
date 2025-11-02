package de.payne.clashmc.mine;

import org.bukkit.Material;

import lombok.Getter;

@Getter
public enum MineBoosterType {
	RESOURCE_MULTIPLIER(1000, 30*60, Material.RAW_GOLD, "Gold"), // 30 Min
	PICKAXE_LEVEL_PLUS(1000, 30*60, Material.DIAMOND, "Diamant"), // 30 MIn
	NO_COOLDOWN(200, 1*60*60, Material.RAW_IRON, "Eisen"); // 1 stunde
	
	private final int cost;
	private final long durationInSeconds;
	private final Material materialToPay;
	private final String anzeigeName;
	
	MineBoosterType(int cost, long durationInSeconds, Material materialToPay, String anzeigeName){
		this.cost = cost;
		this.durationInSeconds = durationInSeconds;
		this.materialToPay = materialToPay;
		this.anzeigeName = anzeigeName;
	}
	
}
