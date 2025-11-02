package de.payne.clashmc.commands.subcommands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.payne.clashmc.commands.CommandInterface;
import de.payne.clashmc.maphandling.schematic.SchematicManager;

public class SaveSchematicCommand implements CommandInterface {

    private final SchematicManager schematicManager;

    public SaveSchematicCommand(SchematicManager schematicManager) {
        this.schematicManager = schematicManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        
    	if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können diesen Befehl ausführen.");
            return true;
        }
    	Player player = (Player) sender;
    	
        if (!player.hasPermission("clashmc.admin")) {
        	player.sendMessage(ChatColor.RED + "Du hast keine Berechtigung, diesen Befehl auszuführen.");
            return true;
        }

        if (args.length < 2) {
        	player.sendMessage(ChatColor.RED + "Verwendung: /clash saveschematic <Name>");
            return true;
        }

        String level = args[1];
        

        try {
        	int parsedLevel = Integer.parseInt(level);
            schematicManager.saveSchematic(player, parsedLevel);
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Bitte gib eine gültige Zahl an.");
            return true; 
        }        
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return List.of();
    }

}