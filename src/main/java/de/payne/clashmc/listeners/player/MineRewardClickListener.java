package de.payne.clashmc.listeners.player;

import java.sql.SQLException;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.database.modules.MineDatabase;
import de.payne.clashmc.economy.PlayerResources;
import de.payne.clashmc.economy.ResourceManager;
import de.payne.clashmc.gui.MineRewardMenu;
import de.payne.clashmc.gui.TownHallMenu;
import de.payne.clashmc.mine.MineBoosterType;
import de.payne.clashmc.mine.MineMaterialType;
import de.payne.clashmc.utils.LogUtil;

public class MineRewardClickListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals("§6Minenbelohnung")) return;

        event.setCancelled(true); // Kein Verschieben im Menü erlaubt
        int slot = event.getSlot();

        // Sound bei Klick auf Glas oder Materialien (Slots 0–8, 9–26, 27–35, 45–53 außer Slot 49)
        if ((slot >= 0 && slot <= 8) || (slot >= 27 && slot <= 35) || (slot >= 45 && slot <= 53 && slot != 49)) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            return;
        }

        int kingID;
        PlayerResources resources;
        try {
            kingID = ClashMC.getInstance().getDatabaseManager().players().getPlayerIdByUUID(player.getUniqueId());
            resources = ClashMC.getInstance().getDatabaseManager().resources().getResources(player.getUniqueId());
        } catch (SQLException e) {
            player.sendMessage("§cFehler beim Laden der Ressourcen.");
            e.printStackTrace();
            return;
        }

        ResourceManager manager = new ResourceManager(player);
        MineDatabase mineDb = ClashMC.getInstance().getDatabaseManager().mine();
        Map<MineMaterialType, Integer> items = mineDb.getAllItems(kingID);

        // Clash Coins eintauschen (Slot 36)
        if (slot == 36) {
            int total = 0;

            for (Map.Entry<MineMaterialType, Integer> entry : items.entrySet()) {
                if (!entry.getKey().isValuable()) {
                    int amount = entry.getValue();
                    total += entry.getKey().getValue() * amount;

                    try {
                        mineDb.saveOrUpdateItem(kingID, entry.getKey(), 0);
                    } catch (SQLException e) {
                        LogUtil.logError(ClashMC.getInstance(), "[MINEGUI] Fehler beim Zurücksetzen von Materialien");
                        e.printStackTrace();
                    }
                }
            }

            if (total == 0) {
                player.sendMessage("§cKeine wertlosen Materialien vorhanden.");
                return;
            }

            manager.addClashCoins(total);
            player.sendMessage("§aDu hast §e" + total + " §aClash Coins erhalten!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            player.closeInventory();
            new MineRewardMenu(player).open();
            return;
        }

        // King Coins eintauschen (Slot 41)
        if (slot == 41) {
            int emeralds = items.getOrDefault(MineMaterialType.EMERALD, 0);
            if (emeralds <= 0) {
                player.sendMessage("§cDu hast keine Emeralds zum Tauschen.");
                return;
            }

            manager.addKingCoins(emeralds);
            try {
                mineDb.saveOrUpdateItem(kingID, MineMaterialType.EMERALD, 0);
            } catch (SQLException e) {
                LogUtil.logError(ClashMC.getInstance(), "[MINEGUI] Fehler beim Updaten der Emeralds");
                e.printStackTrace();
            }

            player.sendMessage("§aDu hast §b" + emeralds + " §aKing Coins erhalten!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            player.closeInventory();
            new MineRewardMenu(player).open();
            return;
        }

        // Spitzhacke-Upgrade (Slot 40)
        if (slot == 40) {
            int currentLevel = mineDb.getPickaxeLevel(kingID);
            int upgradeCost = (int) (75 * Math.pow(1.8, currentLevel));
            int coal = items.getOrDefault(MineMaterialType.COAL, 0);

            if (coal < upgradeCost) {
                player.sendMessage("§cDu benötigst §e" + upgradeCost + " Kohle§c für ein Upgrade.");
                return;
            }

            try {
                mineDb.saveOrUpdateItem(kingID, MineMaterialType.COAL, coal - upgradeCost);
                mineDb.setPickaxeLevel(kingID, currentLevel + 1);
            } catch (SQLException e) {
                LogUtil.logError(ClashMC.getInstance(), "[MINEGUI] Fehler beim Upgraden der Spitzhacke");
                e.printStackTrace();
            }

            player.sendMessage("§aDeine Spitzhacke wurde auf §eLevel " + (currentLevel + 1) + " §aupgegradet!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            player.closeInventory();
            new MineRewardMenu(player).open();
            return;
        }
        
     // Booster-Klicks (Slots 37–39 → entsprechend MineBoosterType.values())
        if (slot >= 37 && slot <= 39) {
            int boosterSlot = slot - 36; // Slot 37 = boosterSlot 1, Slot 38 = 2, Slot 39 = 3
            MineBoosterType boosterType = MineBoosterType.values()[slot - 37];
            Material requiredMaterial = boosterType.getMaterialToPay();
            MineMaterialType materialType = MineMaterialType.fromMaterial(requiredMaterial);

            if (materialType == null) {
                player.sendMessage("§cInterner Fehler: Ungültiges Material für diesen Booster.");
                return;
            }

            int requiredAmount = boosterType.getCost();

            // ✅ Booster bereits aktiv?
            long currentTime = System.currentTimeMillis();
            long boosterExpiresAt = mineDb.getBoosterExpires(kingID, boosterSlot);
            if (boosterExpiresAt > currentTime) {
                long remainingSeconds = (boosterExpiresAt - currentTime) / 1000;
                player.sendMessage("§eDieser Booster ist bereits aktiv! Noch §b" + remainingSeconds + " Sekunden§e.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
                return;
            }

            // Spielerinventar prüfen (DB-Version)
            Map<MineMaterialType, Integer> item = mineDb.getAllItems(kingID);
            int currentAmount = item.getOrDefault(materialType, 0);

            if (currentAmount < requiredAmount) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
                player.sendMessage("§cDu hast nicht genug §e" + materialType.getDisplayName() + " §cfür diesen Booster.");
                return;
            }

            // Booster-Aktivierung speichern
            long expiresAt = System.currentTimeMillis() + boosterType.getDurationInSeconds() * 1000L;
            try {
                // Update Booster Type und ExpiresAt Time in der DB
                mineDb.setBooster(kingID, boosterSlot, boosterType, expiresAt);

                // Material in der DB abziehen
                int newAmount = currentAmount - requiredAmount;
                mineDb.saveOrUpdateItem(kingID, materialType, newAmount);

            } catch (SQLException e) {
                player.sendMessage("§cFehler beim Aktivieren des Boosters.");
                e.printStackTrace();
                return;
            }

            player.sendMessage("§aBooster §e" + boosterType.name() + " §aaktiviert für §e" + boosterType.getDurationInSeconds() + " Sekunden§a!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            new MineRewardMenu(player).open();
            return;
        }
        

        // Zurück-Button (Slot 49)
        if (slot == 49) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            player.sendMessage("§7Zurück zum vorherigen Menü...");
            player.closeInventory();
            TownHallMenu menu = new TownHallMenu(player);
            menu.open();
        }
    }
}