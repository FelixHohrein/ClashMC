package de.payne.clashmc.commands;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClashCommand implements CommandInterface {

    private void sendHelp(Player p) {
        p.sendMessage("§6ClashMC Hilfe:");
        p.sendMessage("§e/clash §7- Zeigt diese Hilfe");
        p.sendMessage("§e/clash info §7- Zeigt dein Dorf-Level und Coins");
        p.sendMessage("§e/clash top §7- Zeigt die besten Spieler");
        if (p.hasPermission("clashmc.admin")) {
            p.sendMessage("§cAdmin-Befehle:");
            p.sendMessage("§c/clash reset <Spieler> §7- Setzt das Dorf zurück");
            p.sendMessage("§c/clash saveschematic <Name> §7- Setzt globalen Spawn");
            p.sendMessage("§c/clash upgrade §7- Verbessert dein Dorf");
            p.sendMessage("§c/clash addcoins <Spieler> <Kingcoins/Clashcoins> <Betrag> §7- Verbessert dein Dorf");
            p.sendMessage("§c/clash mine <start|stop|status> [Spielername] §7- Verbessert dein Dorf");

        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player p = (Player) sender;
        sendHelp(p);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return List.of();
    }
}
