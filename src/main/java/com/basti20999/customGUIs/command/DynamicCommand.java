package com.basti20999.customGUIs.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

/** A runtime-registered Bukkit command backed by a CommandExecutor and optional TabCompleter. */
public class DynamicCommand extends Command {

    private final CommandExecutor executor;
    private TabCompleter tabCompleter;

    public DynamicCommand(String name, CommandExecutor executor) {
        super(name);
        this.executor = executor;
        setPermissionMessage(ChatColor.RED + "You do not have permission to use this command.");
    }

    public void setTabCompleter(TabCompleter tabCompleter) {
        this.tabCompleter = tabCompleter;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!testPermission(sender)) return true;
        return executor.onCommand(sender, this, commandLabel, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (tabCompleter != null) {
            List<String> result = tabCompleter.onTabComplete(sender, this, alias, args);
            if (result != null) return result;
        }
        return Collections.emptyList();
    }
}
