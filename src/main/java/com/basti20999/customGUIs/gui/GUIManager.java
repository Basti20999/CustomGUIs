package com.basti20999.customGUIs.gui;

import com.basti20999.customGUIs.config.ConfigLoader;
import com.basti20999.customGUIs.config.GUIConfig;
import com.basti20999.customGUIs.config.ItemConfig;
import com.basti20999.customGUIs.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GUIManager {

    private Map<String, GUIConfig> guis = Collections.emptyMap();
    private final JavaPlugin plugin;

    public GUIManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        guis = ConfigLoader.load(plugin);
    }

    public Map<String, GUIConfig> getGUIs() {
        return Collections.unmodifiableMap(guis);
    }

    public GUIConfig getGUI(String id) {
        return guis.get(id);
    }

    /** Open a GUI for a player, resolving all player-specific placeholders. */
    public boolean openGUI(String id, Player player) {
        GUIConfig config = guis.get(id);
        if (config == null) return false;

        String title = ColorUtil.translate(config.title(), player);
        CustomGUIHolder holder = new CustomGUIHolder(config);
        Inventory inv = Bukkit.createInventory(holder, config.size(), title);
        holder.setInventory(inv);

        for (Map.Entry<Integer, ItemConfig> entry : config.items().entrySet()) {
            ItemConfig itemCfg = entry.getValue();

            // Skip item if player lacks permission
            if (itemCfg.permission() != null && !player.hasPermission(itemCfg.permission())) continue;

            // Resolve placeholders in name/lore if they contain %player% etc.
            ItemStack stack = resolveStack(itemCfg, player);
            inv.setItem(entry.getKey(), stack);
        }

        player.openInventory(inv);

        if (config.openSound() != null) {
            player.playSound(player.getLocation(), config.openSound(), 1f, 1f);
        }
        return true;
    }

    // -------------------------------------------------------------------------

    private ItemStack resolveStack(ItemConfig cfg, Player player) {
        String name = ColorUtil.translate(cfg.name(), player);
        List<String> lore = new ArrayList<>();
        for (String line : cfg.lore()) {
            lore.add(ColorUtil.translate(line, player));
        }

        // Fast-path: if name/lore contain no %…% placeholders, reuse cached stack clone
        if (!name.equals(cfg.name()) || !lore.equals(cfg.lore())) {
            return ConfigLoader.buildStack(cfg.material(), name, lore, cfg.amount(), cfg.glow(), null);
        }

        ItemStack base = cfg.cachedStack().clone();
        // Re-apply amount (clone preserves it, but be explicit)
        base.setAmount(cfg.amount());
        return base;
    }

    /** Convenience: check whether a raw name/lore string needs placeholder resolution. */
    @SuppressWarnings("unused")
    private boolean hasPlaceholders(String text) {
        return text != null && text.contains("%");
    }
}
