package com.basti20999.customGUIs.command;

import com.basti20999.customGUIs.config.GUIConfig;
import com.basti20999.customGUIs.gui.GUIManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("help", "reload", "list", "open");

    private final GUIManager manager;
    private final Runnable afterReload;

    public AdminCommand(GUIManager manager, Runnable afterReload) {
        this.manager = manager;
        this.afterReload = afterReload;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("customguis.admin.reload")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                manager.reload();
                if (afterReload != null) afterReload.run();
                sender.sendMessage(ChatColor.GREEN + "CustomGUIs config reloaded successfully.");
            }
            case "list" -> {
                if (!sender.hasPermission("customguis.admin.list")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                var guis = manager.getGUIs();
                if (guis.isEmpty()) {
                    sender.sendMessage(ChatColor.YELLOW + "No GUIs loaded.");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "Loaded GUIs (" + guis.size() + "):");
                    for (GUIConfig g : guis.values()) {
                        String cmd2 = g.command() != null ? "/" + g.command() : "(no command)";
                        sender.sendMessage(ChatColor.YELLOW + "  " + g.id() + ChatColor.GRAY + " → " + cmd2
                                + ChatColor.DARK_GRAY + " [" + g.size() + " slots]");
                    }
                }
            }
            case "open" -> {
                if (!sender.hasPermission("customguis.admin.open")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /cgu open <gui-id> [player]");
                    return true;
                }
                String guiId = args[1];
                Player target;
                if (args.length >= 3) {
                    target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "Player '" + args[2] + "' not found.");
                        return true;
                    }
                } else if (sender instanceof Player p) {
                    target = p;
                } else {
                    sender.sendMessage(ChatColor.RED + "Specify a player when running from console.");
                    return true;
                }

                if (!manager.openGUI(guiId, target)) {
                    sender.sendMessage(ChatColor.RED + "GUI '" + guiId + "' not found. Use /cgu list to see available GUIs.");
                } else {
                    if (target != sender) {
                        sender.sendMessage(ChatColor.GREEN + "Opened GUI '" + guiId + "' for " + target.getName() + ".");
                    }
                }
            }
            default -> sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /cgu help.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return filterStart(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            return filterStart(new ArrayList<>(manager.getGUIs().keySet()), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("open")) {
            return filterStart(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()), args[2]);
        }
        return Collections.emptyList();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== CustomGUIs Admin Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/cgu reload" + ChatColor.WHITE + " — Reload config.yml");
        sender.sendMessage(ChatColor.YELLOW + "/cgu list" + ChatColor.WHITE + " — List all loaded GUIs");
        sender.sendMessage(ChatColor.YELLOW + "/cgu open <id> [player]" + ChatColor.WHITE + " — Open a GUI");
        sender.sendMessage(ChatColor.YELLOW + "/cgu help" + ChatColor.WHITE + " — Show this help");
    }

    private List<String> filterStart(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
