package de.payne.clashmc.commands.subcommands;

import org.bukkit.command.CommandSender;

import de.payne.clashmc.mine.MineSchematicManager;
import de.payne.clashmc.commands.CommandInterface;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.util.List;

public class SaveMineCommand implements CommandInterface {

    private final MineSchematicManager mineSchematicManager;

    public SaveMineCommand(MineSchematicManager mineSchematicManager) {
        this.mineSchematicManager = mineSchematicManager;
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

        mineSchematicManager.saveMineSchematic(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return List.of();
    }
}