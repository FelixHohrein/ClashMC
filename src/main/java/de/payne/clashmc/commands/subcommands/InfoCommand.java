package de.payne.clashmc.commands.subcommands;


import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.payne.clashmc.commands.CommandInterface;
import de.payne.clashmc.gui.TownHallMenu;

public class InfoCommand implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Command kann nur von einem Spieler ausgeführt werden.");
            return true;
        }
        
    	Player player = (Player) sender;
    	
        // Ausgabe von Coins, Level, etc.
        player.sendMessage("§7Dein Dorf-Level: §a" + /* level */ 1);
        player.sendMessage("§7Coins: §6" + /* coins */ 1000);
        
        TownHallMenu menu = new TownHallMenu(player);
        player.openInventory(menu.getInventory());
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return List.of();
    }
}

