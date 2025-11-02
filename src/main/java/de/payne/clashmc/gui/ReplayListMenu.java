package de.payne.clashmc.gui;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.replay.ReplayData;
import de.payne.clashmc.utils.ItemStackUtil;
import de.payne.clashmc.utils.LogUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI für Replay-Auswahl.
 * Zeigt alle verfügbaren Replays für einen Spieler.
 */
public class ReplayListMenu implements InventoryHolder {

    private final Player player;
    private final Inventory inventory;
    private final List<ReplayData> replays;
    private final int page;
    private final boolean isAdmin;
    
    @Getter
    private final int maxDays = 7; // Spieler sehen nur <7 Tage
    
    private static final int REPLAYS_PER_PAGE = 45; // 5 Reihen
    
    public ReplayListMenu(Player player) {
        this(player, 0);
    }
    
    public ReplayListMenu(Player player, int page) {
        this.player = player;
        this.page = page;
        this.isAdmin = player.hasPermission("clashmc.admin.replay");
        
        // Lade Replays aus Database
        List<ReplayData> loadedReplays;
        try {
            int kingId = ClashMC.getInstance().getCacheManager().getKingId(player.getUniqueId());
            loadedReplays = ClashMC.getInstance().getDatabaseManager().attacks()
                .getReplaysByPlayer(kingId, isAdmin, maxDays);
        } catch (Exception e) {
            LogUtil.logError(ClashMC.getInstance(), "[ReplayMenu] Fehler beim Laden von Replays: " + e.getMessage());
            loadedReplays = new ArrayList<>();
        }
        this.replays = loadedReplays;
        
        String title = isAdmin ? "§c§lAdmin: Alle Replays" : "§6§lDeine Replays";
        this.inventory = Bukkit.createInventory(this, 54, title);
        
        build();
    }
    
    private void build() {
        inventory.clear();
        
        if (replays.isEmpty()) {
            // Keine Replays vorhanden
            ItemStack noReplays = ItemStackUtil.createItem(
                Material.BARRIER,
                "§cKeine Replays verfügbar",
                List.of(
                    "§7Du hast noch keine Angriffe gemacht",
                    "§7oder es sind bereits mehr als " + maxDays + " Tage vergangen."
                )
            );
            inventory.setItem(22, noReplays);
        } else {
            // Zeige Replays (mit Pagination)
            int startIndex = page * REPLAYS_PER_PAGE;
            int endIndex = Math.min(startIndex + REPLAYS_PER_PAGE, replays.size());
            
            for (int i = startIndex; i < endIndex; i++) {
                ReplayData replay = replays.get(i);
                int slot = i - startIndex;
                
                inventory.setItem(slot, createReplayItem(replay));
            }
            
            // Pagination-Controls
            if (page > 0) {
                // Zurück-Button
                inventory.setItem(48, ItemStackUtil.createItem(
                    Material.ARROW,
                    "§e← Vorherige Seite",
                    List.of("§7Seite " + page)
                ));
            }
            
            if (endIndex < replays.size()) {
                // Weiter-Button
                inventory.setItem(50, ItemStackUtil.createItem(
                    Material.ARROW,
                    "§eNächste Seite →",
                    List.of("§7Seite " + (page + 2))
                ));
            }
        }
        
        // Info-Item
        ItemStack info = ItemStackUtil.createItem(
            Material.BOOK,
            "§b§lReplay-System Info",
            List.of(
                "§7Hier siehst du alle Angriffe",
                "§7" + (isAdmin ? "§c(Admin-Modus: Unbegrenzt)" : "der letzten " + maxDays + " Tage"),
                "",
                "§7Klicke auf ein Replay, um es anzusehen!"
            )
        );
        inventory.setItem(49, info);
        
        // Zurück-Button
        ItemStack back = ItemStackUtil.createItem(
            Material.ARROW,
            "§cZurück",
            List.of("§7Zurück zum Rathaus")
        );
        inventory.setItem(53, back);
    }
    
    private ItemStack createReplayItem(ReplayData replay) {
        try {
            // Hole Spieler-Namen
            String attackerName = getPlayerName(replay.getAttackerId());
            String defenderName = getPlayerName(replay.getDefenderId());
            
            int kingId = ClashMC.getInstance().getCacheManager().getKingId(player.getUniqueId());
            boolean isAttacker = replay.getAttackerId() == kingId;
            
            Material material = isAttacker ? Material.IRON_SWORD : Material.SHIELD;
            String title = isAttacker 
                ? "§c⚔ Angriff auf " + defenderName
                : "§e🛡 Verteidigung gegen " + attackerName;
            
            List<String> lore = new ArrayList<>();
            lore.add("§7" + formatTimestamp(replay.getAttackTime()));
            lore.add("§7Vor " + replay.getAgeDays() + " Tagen");
            lore.add("");
            lore.add("§7Schaden: §e" + String.format("%.1f%%", replay.getDamagePercent()));
            lore.add("§7Clash Coins: §e" + replay.getClashCoinsLooted());
            if (replay.getKingCoinsLooted() > 0) {
                lore.add("§7King Coins: §6" + replay.getKingCoinsLooted());
            }
            lore.add("");
            lore.add("§7Typ: " + (replay.isOnline() ? "§aOnline" : "§7Offline"));
            
            // Movement-Data vorhanden?
            if (replay.getMovementData() != null && !replay.getMovementData().isEmpty()) {
                lore.add("§a✓ Smooth Replay verfügbar");
            } else {
                lore.add("§7⚠ Legacy Replay (ohne Movement)");
            }
            
            lore.add("");
            lore.add("§eKlicken zum Ansehen!");
            
            ItemStack item = ItemStackUtil.createItem(material, title, lore);
            // Speichere attack_id im ItemMeta (für Click-Handler)
            item.setAmount(Math.max(1, Math.min(64, replay.getAttackId())));
            
            return item;
            
        } catch (Exception e) {
            LogUtil.logError(ClashMC.getInstance(), "[ReplayMenu] Fehler beim Erstellen von Replay-Item: " + e.getMessage());
            return ItemStackUtil.createItem(Material.BARRIER, "§cFehler", List.of("§7Replay konnte nicht geladen werden"));
        }
    }
    
    private String getPlayerName(int kingId) {
        try {
            java.util.UUID uuid = ClashMC.getInstance().getDatabaseManager().players().getUUIDByKingId(kingId);
            if (uuid != null) {
                return Bukkit.getOfflinePlayer(uuid).getName();
            }
        } catch (Exception e) {
            // Ignore
        }
        return "Unbekannt";
    }
    
    private String formatTimestamp(java.sql.Timestamp timestamp) {
        if (timestamp == null) return "Unbekannt";
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm");
        return sdf.format(timestamp);
    }
    
    public void open() {
        player.openInventory(inventory);
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Gibt das Replay für einen Slot zurück.
     */
    public ReplayData getReplayAtSlot(int slot) {
        int index = (page * REPLAYS_PER_PAGE) + slot;
        if (index >= 0 && index < replays.size()) {
            return replays.get(index);
        }
        return null;
    }
    
    public int getPage() {
        return page;
    }
}

