package com.basti20999.customGUIs.config;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public record ItemConfig(
    Material material,
    String name,            // already color-translated (no placeholders yet)
    List<String> lore,      // already color-translated (no placeholders yet)
    int amount,
    boolean glow,
    String permission,      // null = no permission required
    Sound clickSound,       // null = no sound
    ClickActions defaultAction,            // used when no on-click block present
    Map<String, ClickActions> onClickMap,  // keyed by click type: left, right, shift-left, etc.
    ItemStack cachedStack   // pre-built stack (without player-specific placeholders)
) {
    /**
     * Resolve the correct ClickActions for the given Bukkit click type name.
     * Falls back to defaultAction if the map is empty or the type is not mapped.
     */
    public ClickActions actionsFor(String clickTypeName) {
        if (onClickMap != null && !onClickMap.isEmpty()) {
            ClickActions specific = onClickMap.get(clickTypeName);
            if (specific != null) return specific;
        }
        return defaultAction != null ? defaultAction : ClickActions.NONE;
    }
}
