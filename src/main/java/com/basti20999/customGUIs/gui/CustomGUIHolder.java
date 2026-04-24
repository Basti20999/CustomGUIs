package com.basti20999.customGUIs.gui;

import com.basti20999.customGUIs.config.GUIConfig;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Carries a GUIConfig so listeners can identify CustomGUI inventories without title matching. */
public class CustomGUIHolder implements InventoryHolder {

    private final GUIConfig config;
    private Inventory inventory;

    public CustomGUIHolder(GUIConfig config) {
        this.config = config;
    }

    public GUIConfig getConfig() {
        return config;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
