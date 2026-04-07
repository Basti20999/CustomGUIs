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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomGUIs extends JavaPlugin implements Listener {

    private static final Pattern HEX_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");

    private final Map<String, GUIConfig> guis = new HashMap<>();
    private final Map<String, GUIConfig> titleToGui = new HashMap<>();
    private final Set<String> registeredCommands = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadGUIsFromConfig();
        registerCommands();
        getServer().getPluginManager().registerEvents(this, this);
    }

    private String translateHex(String message) {
        if (message == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            String replacement = "&x&" + hex.charAt(0) + "&" + hex.charAt(1)
                    + "&" + hex.charAt(2) + "&" + hex.charAt(3)
                    + "&" + hex.charAt(4) + "&" + hex.charAt(5);
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    private void loadGUIsFromConfig() {
        guis.clear();
        titleToGui.clear();

        ConfigurationSection guisSection = getConfig().getConfigurationSection("guis");
        if (guisSection == null) {
            getLogger().warning("No 'guis' section found in config.yml.");
            return;
        }

        for (String guiName : guisSection.getKeys(false)) {
            ConfigurationSection guiSection = guisSection.getConfigurationSection(guiName);
            if (guiSection == null) continue;

            String command = guiSection.getString("command");
            String title = translateHex(guiSection.getString("title", guiName));
            int size = guiSection.getInt("size", 27);

            if (size % 9 != 0 || size < 9 || size > 54) {
                getLogger().warning("GUI '" + guiName + "' has an invalid size (" + size + "). Must be a multiple of 9 between 9 and 54. Skipping.");
                continue;
            }

            if (titleToGui.containsKey(title)) {
                getLogger().warning("GUI '" + guiName + "' has a duplicate title. Click detection may not work correctly.");
            }

            boolean readonly = guiSection.getBoolean("readonly", true);
            Map<Integer, ItemConfig> items = new HashMap<>();

            ConfigurationSection itemsSection = guiSection.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String slotStr : itemsSection.getKeys(false)) {
                    int slot;
                    try {
                        slot = Integer.parseInt(slotStr);
                    } catch (NumberFormatException e) {
                        getLogger().warning("GUI '" + guiName + "' has a non-integer slot key: '" + slotStr + "'. Skipping.");
                        continue;
                    }

                    if (slot < 0 || slot >= size) {
                        getLogger().warning("GUI '" + guiName + "' has slot " + slot + " out of range (0-" + (size - 1) + "). Skipping.");
                        continue;
                    }

                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(slotStr);
                    if (itemSection == null) continue;

                    String materialName = itemSection.getString("material", "STONE").toUpperCase();
                    Material material = Material.matchMaterial(materialName);
                    if (material == null) {
                        getLogger().warning("GUI '" + guiName + "' slot " + slot + " has unknown material '" + materialName + "'. Skipping.");
                        continue;
                    }

                    String name = translateHex(itemSection.getString("name", ""));
                    List<String> lore = new ArrayList<>();
                    for (String line : itemSection.getStringList("lore")) {
                        lore.add(translateHex(line));
                    }
                    String clickCommand = itemSection.getString("command");

                    items.put(slot, new ItemConfig(material, name, lore, clickCommand));
                }
            }

            GUIConfig guiConfig = new GUIConfig(command, title, size, readonly, items);
            guis.put(guiName, guiConfig);
            titleToGui.put(title, guiConfig);
        }

        getLogger().info("Loaded " + guis.size() + " GUI(s) from config.");
    }

    private void registerCommands() {
        CommandMap commandMap = getServer().getCommandMap();
        String prefix = getName().toLowerCase();

        for (GUIConfig gui : guis.values()) {
            if (gui.command == null || gui.command.isEmpty()) continue;
            if (registeredCommands.contains(gui.command)) continue;

            commandMap.register(prefix, new DynamicCommand(gui.command, new GUICommandExecutor(gui), this));
            registeredCommands.add(gui.command);
        }

        if (!registeredCommands.contains("cgu")) {
            DynamicCommand adminCmd = new DynamicCommand("cgu", new AdminCommandExecutor(), this);
            adminCmd.setPermission("customguis.admin");
            commandMap.register(prefix, adminCmd);
            registeredCommands.add("cgu");
        }
    }

    // -------------------------------------------------------------------------
    // Inner classes
    // -------------------------------------------------------------------------

    private class DynamicCommand extends Command {
        private final CommandExecutor executor;

        DynamicCommand(String name, CommandExecutor executor, JavaPlugin plugin) {
            super(name);
            this.executor = executor;
            this.setPermissionMessage(ChatColor.RED + "You do not have permission to use this command.");
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            if (!testPermission(sender)) return true;
            return executor.onCommand(sender, this, commandLabel, args);
        }
    }

    private class GUICommandExecutor implements CommandExecutor {
        private final GUIConfig guiConfig;

        GUICommandExecutor(GUIConfig guiConfig) {
            this.guiConfig = guiConfig;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }

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
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                sender.sendMessage(ChatColor.GREEN + "=== CustomGUIs Admin Commands ===");
                sender.sendMessage(ChatColor.YELLOW + "/cgu reload " + ChatColor.WHITE + "- Reload the config");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("customguis.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                reloadConfig();
                loadGUIsFromConfig();
                // Note: commands already registered cannot be unregistered via the Bukkit API.
                // Newly added GUI commands from the config will be registered; removed ones
                // remain as no-ops until the next server restart.
                registerCommands();
                sender.sendMessage(ChatColor.GREEN + "CustomGUIs config reloaded successfully.");
                return true;
            }

            sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /cgu help for a list of commands.");
            return true;
        }
    }

    // -------------------------------------------------------------------------
    // Event handling
    // -------------------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        GUIConfig gui = titleToGui.get(title);
        if (gui == null) return;

        if (gui.readonly) {
            event.setCancelled(true);
        }

        // Ignore clicks in the player's own inventory (bottom half)
        if (event.getRawSlot() >= gui.size) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemConfig item = gui.items.get(event.getRawSlot());
        if (item == null || item.clickCommand == null || item.clickCommand.isEmpty()) return;

        Player player = (Player) event.getWhoClicked();
        player.performCommand(item.clickCommand);
        if (gui.readonly) {
            player.closeInventory();
        }
    }

    // -------------------------------------------------------------------------
    // Data classes
    // -------------------------------------------------------------------------

    private static class GUIConfig {
        final String command;
        final String title;
        final int size;
        final boolean readonly;
        final Map<Integer, ItemConfig> items;

        GUIConfig(String command, String title, int size, boolean readonly, Map<Integer, ItemConfig> items) {
            this.command = command;
            this.title = title;
            this.size = size;
            this.readonly = readonly;
            this.items = items;
        }
    }

    private static class ItemConfig {
        final Material material;
        final String name;
        final List<String> lore;
        final String clickCommand;

        ItemConfig(Material material, String name, List<String> lore, String clickCommand) {
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.clickCommand = clickCommand;
        }
    }
}
