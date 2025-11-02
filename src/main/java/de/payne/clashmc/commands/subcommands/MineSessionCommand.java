package de.payne.clashmc.commands.subcommands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.payne.clashmc.commands.CommandInterface;
import de.payne.clashmc.mine.MineManager;

public class MineSessionCommand implements CommandInterface {

    private final MineManager mineManager;

    public MineSessionCommand(MineManager mineManager) {
        this.mineManager = mineManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können diesen Befehl ausführen.");
            return true;
        }

        Player executor = (Player) sender;

        if (!executor.hasPermission("clashmc.admin")) {
            executor.sendMessage(ChatColor.RED + "Du hast keine Berechtigung, diesen Befehl auszuführen.");
            return true;
        }

        if (args.length < 2) {
            executor.sendMessage(ChatColor.RED + "Benutzung: /clash mine <start|stop|status> [Spielername]");
            return true;
        }

        String action = args[1].toLowerCase();
        Player target = executor;

        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                executor.sendMessage(ChatColor.RED + "Spieler '" + args[2] + "' wurde nicht gefunden oder ist offline.");
                return true;
            }
        }

        switch (action) {
            case "start":
                mineManager.startMineSession(target);
                executor.sendMessage(ChatColor.GREEN + "Minesession für " + target.getName() + " gestartet.");
                break;
            case "stop":
                mineManager.endMineSession(target);
                executor.sendMessage(ChatColor.YELLOW + "Minesession für " + target.getName() + " gestoppt.");
                break;
            case "status":
                boolean active = mineManager.isInMine(target);
                if (active) {
                    executor.sendMessage(ChatColor.GREEN + target.getName() + " befindet sich aktuell in einer aktiven Minesession.");
                } else {
                    executor.sendMessage(ChatColor.RED + target.getName() + " befindet sich aktuell in keiner Minesession.");
                }
                break;
            default:
                executor.sendMessage(ChatColor.RED + "Ungültige Aktion. Verwende: start, stop oder status.");
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player player)) return List.of();

        if (args.length == 2) {
            return List.of("start", "stop", "status").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }

        if (args.length == 3 && player.hasPermission("clashmc.admin")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }

        return List.of();
    }
}