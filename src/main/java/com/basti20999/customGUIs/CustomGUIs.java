package com.basti20999.customGUIs;

import com.basti20999.customGUIs.command.AdminCommand;
import com.basti20999.customGUIs.command.DynamicCommand;
import com.basti20999.customGUIs.command.GUICommand;
import com.basti20999.customGUIs.config.GUIConfig;
import com.basti20999.customGUIs.gui.GUIListener;
import com.basti20999.customGUIs.gui.GUIManager;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public class CustomGUIs extends JavaPlugin {

    private GUIManager guiManager;
    private final Set<String> registeredCommands = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        guiManager = new GUIManager(this);
        guiManager.reload();
        registerCommands();
        getServer().getPluginManager().registerEvents(new GUIListener(guiManager), this);
    }

    /**
     * Register commands for all loaded GUIs and the /cgu admin command.
     * Already-registered commands are skipped (Bukkit limitation on unregistration).
     */
    public void registerCommands() {
        CommandMap commandMap = getServer().getCommandMap();
        String prefix = getName().toLowerCase();

        for (GUIConfig gui : guiManager.getGUIs().values()) {
            if (gui.command() == null || gui.command().isEmpty()) continue;
            if (registeredCommands.contains(gui.command())) continue;
            commandMap.register(prefix, new DynamicCommand(gui.command(), new GUICommand(guiManager, gui.id())));
            registeredCommands.add(gui.command());
        }

        if (!registeredCommands.contains("cgu")) {
            AdminCommand admin = new AdminCommand(guiManager, this::registerCommands);
            DynamicCommand adminCmd = new DynamicCommand("cgu", admin);
            adminCmd.setPermission("customguis.admin");
            adminCmd.setTabCompleter(admin);
            commandMap.register(prefix, adminCmd);
            registeredCommands.add("cgu");
        }
    }
}
