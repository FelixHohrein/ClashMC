package de.payne.clashmc.commands.subcommands;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.payne.clashmc.commands.CommandInterface;

public class TopCommand implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player player = (Player) sender;
        player.sendMessage("§6Top-Spieler (Demo):");
        player.sendMessage("§e1. Steve - Level 10");
        player.sendMessage("§e2. Alex - Level 9");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return List.of();
    }
}

