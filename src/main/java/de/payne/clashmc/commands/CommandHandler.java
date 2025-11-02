package de.payne.clashmc.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.google.common.collect.Lists;


import java.util.HashMap;
import java.util.List;

public class CommandHandler implements CommandExecutor, TabCompleter {

	// command Storage
	private static HashMap<String, CommandInterface> commands = new HashMap<String, CommandInterface>();

	// Methode to register in onEnable
	public void register(String name, CommandInterface cmd) {
		commands.put(name, cmd);
	}

	// check if string(s) exist or not
	public boolean exists(String name) {
		return commands.containsKey(name);
	}

	// getter methode for the executor
	public CommandInterface getExecutor(String name) {
		return commands.get(name);
	}

	// Checks for all sub commands
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		if (!(sender instanceof Player)) {
			return true;
		}

		if (args.length == 0) {
			getExecutor("clash").onCommand(sender, cmd, commandLabel, args);
			return true;
		}

		if (args.length > 0) {
			// checkt ob der subcommand (args) in der onenable mit der register methode
			// registriert wurde
			if (!(exists(args[0]))) {
				sender.sendMessage("Dieser Befehl existiert nicht");
				return true;
			}

			// getting the right executer from subcommand
			this.getExecutor(args[0]).onCommand(sender, cmd, commandLabel, args);
			return true;
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		List<String> l = Lists.newArrayList();
		
		if(sender.hasPermission("clashmc.admin")) {
			l.add("reset");
			l.add("saveschematic");
			l.add("setspawn");
			l.add("upgrade");
			l.add("savemine");
		}
		
		l.add("info");
		l.add("top");
		l.add("help");

		if (args.length > 0) {
		// checkt ob der subcommand (args) in der onenable mit der register methode
		// registriert wurde
			if (!(exists(args[0]))) {
				return l;
			} else {
				// getting the right executer from subcommand
				return this.getExecutor(args[0]).onTabComplete(sender, cmd, commandLabel, args);
			}
		}
		return l;
	}
}
