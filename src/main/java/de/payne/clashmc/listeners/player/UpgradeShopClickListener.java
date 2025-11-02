package de.payne.clashmc.listeners.player;

import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.economy.ResourceManager;
import de.payne.clashmc.gui.ShopMenu;
import de.payne.clashmc.gui.UpgradeShopMenu;
import de.payne.clashmc.handlers.PlayerDataHandler;

public class UpgradeShopClickListener implements Listener {

	private final ClashMC plugin;

	public UpgradeShopClickListener(ClashMC plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) return;

		if (!(event.getView().getTopInventory().getHolder() instanceof UpgradeShopMenu upgradeMenu)) return;

		event.setCancelled(true);

		int slot = event.getRawSlot();

		if (slot >= event.getInventory().getSize()) return;

		ItemStack clickedItem = event.getCurrentItem();
		if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

		// Sound abspielen je nach Slot / Item
		Material mat = clickedItem.getType();

		// Schwarze Glas-Scheiben (Rahmen)
		if (mat == Material.BLACK_STAINED_GLASS_PANE) {
			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
			return; // Keine weitere Aktion
		}

		int currentPage = upgradeMenu.getPage();
		int totalLevels = plugin.getSchematicManager().getAvailableLevels().size();
		int maxPage = (int) Math.ceil(totalLevels / (double) UpgradeShopMenu.ITEMS_PER_PAGE);
		if (maxPage < 1) maxPage = 1;

		switch (slot) {
			case 47 -> { // Zurück
				player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
				if (currentPage > 1) {
					upgradeMenu.open(player, currentPage - 1);
				}
			}
			case 49 -> { // Home (Barrier)
				player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1f);
				player.closeInventory();
				new ShopMenu(player).open();
			}
			case 51 -> { // Weiter
				player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
				if (currentPage < maxPage) {
					upgradeMenu.open(player, currentPage + 1);
				}
			}
			default -> {
				if (slot >= 9 && slot <= 44) {
					if (!clickedItem.hasItemMeta()) return;
					ItemMeta meta = clickedItem.getItemMeta();
					if (meta == null || !meta.hasDisplayName()) return;

					String name = meta.getDisplayName();
					if (!name.startsWith("§eLevel ")) return;

					try {
						int clickedLevel = Integer.parseInt(name.replace("§eLevel ", "").trim());

						UUID uuid = player.getUniqueId();
						int playerId = plugin.getDatabaseManager().players().getPlayerIdByUUID(uuid);
						int currentLevel = plugin.getDatabaseManager().villages().getVillageLevel(playerId);
						int nextLevel = currentLevel + 1;

						if (clickedLevel == nextLevel) {
							
							ResourceManager resources = new ResourceManager(player);
							// Erfolgreicher Kauf
							if(resources.removeClashCoins(resources.calculateUpgradeCost(player))) {
								player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
								
								player.closeInventory();
								PlayerDataHandler handler = new PlayerDataHandler(uuid);
								handler.upgradeVillage();

								player.sendMessage("§aDein Dorf wurde auf §eLevel " + clickedLevel + " §aaufgewertet!");
								player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
								// Nicht genügend Coins	
							} else {
								player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
							}
							// Erfolgreicher Kauf

						} else if (clickedLevel <= currentLevel) {
							player.sendMessage("§eDein Dorf ist bereits auf §eLevel " + currentLevel + "§e oder höher.");
							player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
						} else {
							player.sendMessage("§cDu musst zuerst §eLevel " + nextLevel + " §ckaufen!");
							player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
						}
					
					} catch (NumberFormatException | SQLException e) {
						player.sendMessage("§cFehler beim Verarbeiten des Levels.");
						e.printStackTrace();
					}
				}
			}
		}
	}
}