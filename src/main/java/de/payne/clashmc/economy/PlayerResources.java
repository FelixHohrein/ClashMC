package de.payne.clashmc.economy;

import java.util.UUID;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PlayerResources {

	private final UUID uuid;
	private final long clashCoins;
	private final long kingCoins;
	private final long lastCollectorUse;
	
}
