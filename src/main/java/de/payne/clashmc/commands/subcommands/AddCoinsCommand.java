package de.payne.clashmc.commands.subcommands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import de.payne.clashmc.commands.CommandInterface;
import de.payne.clashmc.economy.ResourceManager;

public class AddCoinsCommand implements CommandInterface {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length != 4) {
			sender.sendMessage("§cBenutzung: /clash addcoins <Spieler> <clashcoins|kingcoins> <Menge>");
			return true;
		}

		Player target = Bukkit.getPlayer(args[1]);
		if (target == null) {
			sender.sendMessage("§cSpieler nicht gefunden.");
			return true;
		}

		String type = args[2].toLowerCase();
		int amount;
		try {
			amount = Integer.parseInt(args[3]);
		} catch (NumberFormatException e) {
			sender.sendMessage("§cUngültiger Betrag.");
			return true;
		}

		ResourceManager resourceManager = new ResourceManager(target);
		switch (type) {
			case "clashcoins":
				resourceManager.addClashCoins(amount);
				sender.sendMessage("§a" + amount + " Clash Coins an " + target.getName() + " gegeben.");
				break;
			case "kingcoins":
				resourceManager.addKingCoins(amount);
				sender.sendMessage("§a" + amount + " King Coins an " + target.getName() + " gegeben.");
				break;
			default:
				sender.sendMessage("§cUngültiger Typ. Verfügbar: clashcoins, kingcoins");
				break;
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		List<String> list = new ArrayList<>();

		if (args.length == 2) {
			// Tab für Spielernamen
			for (Player player : Bukkit.getOnlinePlayers()) {
				list.add(player.getName());
			}
		} else if (args.length == 3) {
			// clashcoins oder kingcoins
			list.add("clashcoins");
			list.add("kingcoins");
		} else if (args.length == 4) {
			// Optional: Menge vorschlagen
			list.add("10");
			list.add("100");
			list.add("1000");
		}

		return list;
	}
}
