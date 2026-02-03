package com.basti20999.customGUIs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomGUIs extends JavaPlugin implements Listener {

    private Map<String, GUIConfig> guis = new HashMap<>();
    private Map<String, GUIConfig> titleToGui = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadGUIsFromConfig();
        registerCommands();
        getServer().getPluginManager().registerEvents(this, this);
    }

    private String translateHex(String message) {
        if (message == null) return "";
        Pattern pattern = Pattern.compile("#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            String replace = "&x&" + hex.charAt(0) + "&" + hex.charAt(1) + "&" + hex.charAt(2) + "&" + hex.charAt(3) + "&" + hex.charAt(4) + "&" + hex.charAt(5);
            matcher.appendReplacement(sb, replace);
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    private void loadGUIsFromConfig() {
        guis.clear();
        titleToGui.clear();
        ConfigurationSection guisSection = getConfig().getConfigurationSection("guis");
        if (guisSection == null) return;

        for (String guiName : guisSection.getKeys(false)) {
            ConfigurationSection guiSection = guisSection.getConfigurationSection(guiName);
            String command = guiSection.getString("command");
            String title = translateHex(guiSection.getString("title", guiName));
            int size = guiSection.getInt("size", 27);
            if (size % 9 != 0 || size < 9 || size > 54) {
                getLogger().warning("Invalid size for GUI " + guiName + ". Must be between 9 and 54, multiple of 9.");
                continue;
            }
            boolean readonly = guiSection.getBoolean("readonly", true);

            Map<Integer, ItemConfig> items = new HashMap<>();
            ConfigurationSection itemsSection = guiSection.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String slotStr : itemsSection.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(slotStr);
                        if (slot < 0 || slot >= size) continue;

                        ConfigurationSection itemSection = itemsSection.getConfigurationSection(slotStr);
                        Material material = Material.matchMaterial(itemSection.getString("material", "STONE").toUpperCase());
                        if (material == null) continue;

                        String name = translateHex(itemSection.getString("name", ""));
                        List<String> lore = new ArrayList<>();
                        for (String line : itemSection.getStringList("lore")) {
                            lore.add(translateHex(line));
                        }
                        String clickCommand = itemSection.getString("command");

                        items.put(slot, new ItemConfig(material, name, lore, clickCommand));
                    } catch (NumberFormatException e) {
                        getLogger().warning("Invalid slot " + slotStr + " for GUI " + guiName);
                    }
                }
            }

            GUIConfig guiConfig = new GUIConfig(command, title, size, readonly, items);
            guis.put(guiName, guiConfig);
            titleToGui.put(title, guiConfig);
        }
    }

    private void registerCommands() {
        CommandMap commandMap = getServer().getCommandMap();

        // Register dynamic GUI commands
        for (GUIConfig gui : guis.values()) {
            if (gui.command != null && !gui.command.isEmpty()) {
                DynamicCommand cmd = new DynamicCommand(gui.command, new GUICommandExecutor(gui), this);
                commandMap.register(getName().toLowerCase(), cmd);
            }
        }

        // Register /cgu command for admin functions
        DynamicCommand adminCmd = new DynamicCommand("cgu", new AdminCommandExecutor(), this);
        adminCmd.setPermission("customguis.reload");
        commandMap.register(getName().toLowerCase(), adminCmd);
    }

    private class DynamicCommand extends Command {
        private final CommandExecutor executor;
        private final JavaPlugin plugin;

        protected DynamicCommand(String name, CommandExecutor executor, JavaPlugin plugin) {
            super(name);
            this.executor = executor;
            this.plugin = plugin;
            this.setPermissionMessage(ChatColor.RED + "You do not have permission.");
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            if (!testPermission(sender)) return true;
            return executor.onCommand(sender, this, commandLabel, args);
        }
    }

    private class GUICommandExecutor implements CommandExecutor {
        private final GUIConfig guiConfig;

        public GUICommandExecutor(GUIConfig guiConfig) {
            this.guiConfig = guiConfig;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }

            Player player = (Player) sender;
            Inventory inventory = Bukkit.createInventory(null, guiConfig.size, guiConfig.title);

            for (Map.Entry<Integer, ItemConfig> entry : guiConfig.items.entrySet()) {
                int slot = entry.getKey();
                ItemConfig itemConfig = entry.getValue();

                ItemStack item = new ItemStack(itemConfig.material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    if (!itemConfig.name.isEmpty()) {
                        meta.setDisplayName(itemConfig.name);
                    }
                    if (!itemConfig.lore.isEmpty()) {
                        meta.setLore(itemConfig.lore);
                    }
                    item.setItemMeta(meta);
                }

                inventory.setItem(slot, item);
            }

            player.openInventory(inventory);
            return true;
        }
    }

    private class AdminCommandExecutor implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.GREEN + "CustomGUIs commands:");
                sender.sendMessage(ChatColor.YELLOW + "/cgu reload - Reload the config");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("customguis.reload")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                reloadConfig();
                loadGUIsFromConfig();
                registerCommands(); // Re-register in case commands changed
                sender.sendMessage(ChatColor.GREEN + "CustomGUIs config reloaded!");
                return true;
            }

            sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
            return true;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        GUIConfig gui = titleToGui.get(title);
        if (gui == null) return;

        if (gui.readonly) {
            event.setCancelled(true);
        }

        if (event.getRawSlot() >= gui.size) return; // Bottom inventory

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getRawSlot();
        ItemConfig item = gui.items.get(slot);
        if (item != null && item.clickCommand != null && !item.clickCommand.isEmpty()) {
            Player player = (Player) event.getWhoClicked();
            player.performCommand(item.clickCommand);
            if (gui.readonly) {
                player.closeInventory();
            }
        }
    }

    private static class GUIConfig {
        String command;
        String title;
        int size;
        boolean readonly;
        Map<Integer, ItemConfig> items;

        public GUIConfig(String command, String title, int size, boolean readonly, Map<Integer, ItemConfig> items) {
            this.command = command;
            this.title = title;
            this.size = size;
            this.readonly = readonly;
            this.items = items;
        }
    }

    private static class ItemConfig {
        Material material;
        String name;
        List<String> lore;
        String clickCommand;

        public ItemConfig(Material material, String name, List<String> lore, String clickCommand) {
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.clickCommand = clickCommand;
        }
    }
}