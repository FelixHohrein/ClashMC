package de.payne.clashmc.commands;

import org.bukkit.command.CommandSender;

import java.util.List;

import org.bukkit.command.Command;

public interface CommandInterface {

  public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args);
  public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args);

}
