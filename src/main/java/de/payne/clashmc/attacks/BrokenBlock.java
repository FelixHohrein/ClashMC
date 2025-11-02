package de.payne.clashmc.attacks;

import lombok.Getter;

public class BrokenBlock {
    
	@Getter
	private final String material;
    @Getter
	private final int x, y, z;
    @Getter
    private final long timestamp;

    public BrokenBlock(String material, int x, int y, int z, long timestamp) {
        this.material = material;
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
    }
}
