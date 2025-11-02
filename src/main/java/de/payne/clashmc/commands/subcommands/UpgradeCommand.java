package de.payne.clashmc.commands.subcommands;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.payne.clashmc.commands.CommandInterface;
import de.payne.clashmc.handlers.PlayerDataHandler;

public class UpgradeCommand implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player player = (Player) sender;
        
        PlayerDataHandler handler = new PlayerDataHandler(player.getUniqueId());
        handler.upgradeVillage();
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return List.of();
    }
}
