package de.payne.clashmc.commands.subcommands;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.commands.CommandInterface;

/**
 * Admin-Command für Cache und Datenbank-Statistiken
 * Requires: clashmc.admin Permission
 */
public class StatsCommand implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("clashmc.admin")) {
            sender.sendMessage("§cDu hast keine Berechtigung für diesen Command.");
            return true;
        }
        
        sender.sendMessage("§8§m                                            ");
        sender.sendMessage("§6§lClashMC System-Statistiken");
        sender.sendMessage("");
        
        // Cache-Statistiken
        String cacheStats = ClashMC.getInstance().getCacheManager().getStatistics();
        sender.sendMessage("§e" + cacheStats.replace("\n", "\n§e"));
        sender.sendMessage("");
        
        // Connection Pool Statistiken
        if (ClashMC.getInstance().getDatabase().isConnected()) {
            String poolStats = ClashMC.getInstance().getDatabase().getPoolStats();
            sender.sendMessage("§b" + poolStats);
        } else {
            sender.sendMessage("§cDatenbank ist nicht verbunden!");
        }
        
        sender.sendMessage("");
        
        // Aktive Instances
        int activeMines = ClashMC.getInstance().getMineManager().getActiveMines().size();
        int activeAttacks = ClashMC.getInstance().getAttackManager().getActiveAttacks().size();
        
        sender.sendMessage("§aAktive Sessions:");
        sender.sendMessage("  §7Minen: §e" + activeMines);
        sender.sendMessage("  §7Angriffe: §e" + activeAttacks);
        
        sender.sendMessage("§8§m                                            ");
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return List.of();
    }
}

