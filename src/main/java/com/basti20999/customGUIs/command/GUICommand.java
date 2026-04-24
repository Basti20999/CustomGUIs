package com.basti20999.customGUIs.command;

import com.basti20999.customGUIs.config.GUIConfig;
import com.basti20999.customGUIs.gui.GUIManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GUICommand implements CommandExecutor {

    private final GUIManager manager;
    private final String guiId;

    public GUICommand(GUIManager manager, String guiId) {
        this.manager = manager;
        this.guiId = guiId;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        GUIConfig config = manager.getGUI(guiId);
        if (config == null) {
            // GUI was removed after reload; command stub still registered
            player.sendMessage(ChatColor.RED + "That GUI no longer exists. A server restart is required to remove its command.");
            return true;
        }

        manager.openGUI(guiId, player);
        return true;
    }
}
