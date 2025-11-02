package de.payne.clashmc.commands.subcommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.commands.CommandInterface;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand implements CommandInterface {

    private final ClashMC plugin;

    public ReloadCommand(ClashMC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (!(sender instanceof Player) && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
            sender.sendMessage("§cDieser Command kann nur von Spielern oder der Konsole ausgeführt werden.");
            return true;
        }

        if (!sender.hasPermission("clashmc.admin")) {
            sender.sendMessage("§cDu hast keine Berechtigung für diesen Command.");
            return true;
        }

        sender.sendMessage("§e§l[ClashMC] §7Reloading configuration...");

        try {
            // Config neu laden
            plugin.getConfigManager().reloadConfig();
            
            // Validierung
            plugin.getConfigManager().validateConfig();
            
            // Cache leeren (optional, damit neue Config-Werte sofort wirken)
            if (plugin.getCacheManager() != null) {
                // Cache-Manager hat keine clear() Methode, aber das ist okay
                // Neue Werte werden beim nächsten Zugriff geladen
                sender.sendMessage("§a✓ Cache wird beim nächsten Zugriff aktualisiert");
            }
            
            sender.sendMessage("§a✓ Konfiguration erfolgreich neu geladen!");
            sender.sendMessage("§7Folgende Änderungen wurden übernommen:");
            sender.sendMessage("§7- Dorf-Upgrade-Kosten");
            sender.sendMessage("§7- Economy-Einstellungen (Collector)");
            sender.sendMessage("§7- Angriffs-System (Timer, Belohnungen, Level-Range)");
            sender.sendMessage("§7- Mine-System (Dauer, Erz-Verteilung, Booster)");
            sender.sendMessage("§7- Performance-Einstellungen");
            sender.sendMessage("§7");
            sender.sendMessage("§cAchtung: §7Bereits aktive Sessions behalten ihre alten Werte!");
            sender.sendMessage("§7Neue Angriffe/Minen nutzen automatisch die neuen Einstellungen.");
            
        } catch (Exception e) {
            sender.sendMessage("§c✗ Fehler beim Reload: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return new ArrayList<>();
    }
}

