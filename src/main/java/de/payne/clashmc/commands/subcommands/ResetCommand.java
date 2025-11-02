package de.payne.clashmc.commands.subcommands;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.payne.clashmc.ClashMC;
import de.payne.clashmc.commands.CommandInterface;
import de.payne.clashmc.handlers.PlayerDataHandler;

public class ResetCommand implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 2) return false;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cSpieler nicht gefunden.");
            return true;
        }
        
        PlayerDataHandler handler = new PlayerDataHandler(target.getUniqueId());
        handler.resetVillageToLevel1();
		target.teleport(ClashMC.getInstance().getVillageAllocator().getVillageCenterTeleportOrSpawnLocation(target.getUniqueId()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 2)
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        return List.of();
    }
}
