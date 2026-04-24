package com.basti20999.customGUIs.config;

import com.basti20999.customGUIs.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Logger;

public final class ConfigLoader {

    private ConfigLoader() {}

    /** Parse the "guis" section and return a map of id → GUIConfig. */
    public static Map<String, GUIConfig> load(JavaPlugin plugin) {
        Logger log = plugin.getLogger();
        ConfigurationSection guisSection = plugin.getConfig().getConfigurationSection("guis");
        if (guisSection == null) {
            log.warning("No 'guis' section found in config.yml.");
            return Collections.emptyMap();
        }

        Map<String, GUIConfig> result = new LinkedHashMap<>();

        for (String id : guisSection.getKeys(false)) {
            ConfigurationSection sec = guisSection.getConfigurationSection(id);
            if (sec == null) continue;

            String command = sec.getString("command");
            String rawTitle = sec.getString("title", id);
            int size = sec.getInt("size", 27);

            if (size % 9 != 0 || size < 9 || size > 54) {
                log.warning("GUI '" + id + "' invalid size (" + size + "). Must be 9-54, multiple of 9. Skipping.");
                continue;
            }

            boolean readonly = sec.getBoolean("readonly", true);
            boolean closeOnClick = sec.getBoolean("close-on-click", readonly);
            Sound openSound = parseSound(sec.getString("open-sound"), id, log);

            // Build per-slot item map
            Map<Integer, ItemConfig> items = new LinkedHashMap<>();

            // Parse explicit item slots
            ConfigurationSection itemsSec = sec.getConfigurationSection("items");
            if (itemsSec != null) {
                for (String slotStr : itemsSec.getKeys(false)) {
                    int slot;
                    try {
                        slot = Integer.parseInt(slotStr);
                    } catch (NumberFormatException e) {
                        log.warning("GUI '" + id + "' non-integer slot key '" + slotStr + "'. Skipping.");
                        continue;
                    }
                    if (slot < 0 || slot >= size) {
                        log.warning("GUI '" + id + "' slot " + slot + " out of range (0-" + (size - 1) + "). Skipping.");
                        continue;
                    }
                    ConfigurationSection itemSec = itemsSec.getConfigurationSection(slotStr);
                    if (itemSec == null) continue;
                    ItemConfig cfg = parseItem(itemSec, id, slotStr, log);
                    if (cfg != null) items.put(slot, cfg);
                }
            }

            // Apply border shortcut (perimeter slots not already occupied)
            ConfigurationSection borderSec = sec.getConfigurationSection("border");
            if (borderSec != null) {
                ItemConfig borderItem = parseItem(borderSec, id, "border", log);
                if (borderItem != null) {
                    for (int slot : perimeterSlots(size)) {
                        items.putIfAbsent(slot, borderItem);
                    }
                }
            }

            // Apply fill-empty shortcut (all remaining empty slots)
            ConfigurationSection fillSec = sec.getConfigurationSection("fill-empty");
            if (fillSec != null) {
                ItemConfig fillItem = parseItem(fillSec, id, "fill-empty", log);
                if (fillItem != null) {
                    for (int slot = 0; slot < size; slot++) {
                        items.putIfAbsent(slot, fillItem);
                    }
                }
            }

            // Title is translated here (without player placeholders; those are resolved at open time)
            String title = ColorUtil.translate(rawTitle);

            result.put(id, new GUIConfig(id, command, title, size, readonly, closeOnClick, openSound, items));
        }

        log.info("Loaded " + result.size() + " GUI(s) from config.");
        return result;
    }

    // -------------------------------------------------------------------------

    private static ItemConfig parseItem(ConfigurationSection sec, String guiId, String label, Logger log) {
        String materialName = sec.getString("material", "STONE").toUpperCase();
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            log.warning("GUI '" + guiId + "' item '" + label + "' unknown material '" + materialName + "'. Skipping.");
            return null;
        }

        String name = ColorUtil.translate(sec.getString("name", ""));
        List<String> lore = new ArrayList<>();
        for (String line : sec.getStringList("lore")) {
            lore.add(ColorUtil.translate(line));
        }
        int amount = Math.max(1, Math.min(64, sec.getInt("amount", 1)));
        boolean glow = sec.getBoolean("glow", false);
        String permission = sec.getString("permission");
        Sound clickSound = parseSound(sec.getString("click-sound"), guiId, log);

        // Parse actions
        ClickActions defaultAction = parseActions(sec, null);
        Map<String, ClickActions> onClickMap = new LinkedHashMap<>();
        ConfigurationSection onClickSec = sec.getConfigurationSection("on-click");
        if (onClickSec != null) {
            for (String key : onClickSec.getKeys(false)) {
                ConfigurationSection clickBranch = onClickSec.getConfigurationSection(key);
                if (clickBranch != null) {
                    onClickMap.put(key.toLowerCase(), parseActions(clickBranch, null));
                }
            }
        }

        // Pre-build cached ItemStack (no player placeholders)
        ItemStack stack = buildStack(material, name, lore, amount, glow, null);

        return new ItemConfig(material, name, lore, amount, glow, permission, clickSound,
                defaultAction, onClickMap.isEmpty() ? null : onClickMap, stack);
    }

    /** Build an ItemStack from config values, optionally with a custom display name override. */
    public static ItemStack buildStack(Material material, String name, List<String> lore,
                                       int amount, boolean glow, String nameOverride) {
        ItemStack stack = new ItemStack(material, amount);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String displayName = nameOverride != null ? nameOverride : name;
            if (!displayName.isEmpty()) meta.setDisplayName(displayName);
            if (!lore.isEmpty()) meta.setLore(lore);
            if (glow) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ClickActions parseActions(ConfigurationSection sec, String ignored) {
        String command = sec.getString("command");
        String console = sec.getString("console-command");
        String open = sec.getString("open");
        String message = sec.getString("message");
        if (command == null && console == null && open == null && message == null) {
            return ClickActions.NONE;
        }
        return new ClickActions(command, console, open, message);
    }

    private static Sound parseSound(String name, String guiId, Logger log) {
        if (name == null || name.isEmpty()) return null;
        // Accept either legacy enum form (UI_BUTTON_CLICK) or modern key form (ui.button.click).
        String normalized = name.toLowerCase().replace('_', '.');
        Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(normalized));
        if (sound != null) return sound;
        log.warning("GUI '" + guiId + "' unknown sound '" + name + "'. Ignoring.");
        return null;
    }

    /** Return all perimeter slot indices for an inventory of the given size. */
    private static List<Integer> perimeterSlots(int size) {
        int rows = size / 9;
        List<Integer> slots = new ArrayList<>();
        for (int col = 0; col < 9; col++) {
            slots.add(col);                    // top row
            slots.add((rows - 1) * 9 + col);  // bottom row
        }
        for (int row = 1; row < rows - 1; row++) {
            slots.add(row * 9);        // left column
            slots.add(row * 9 + 8);   // right column
        }
        return slots;
    }
}
