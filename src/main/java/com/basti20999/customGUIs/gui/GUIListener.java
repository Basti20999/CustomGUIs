package com.basti20999.customGUIs.gui;

import com.basti20999.customGUIs.config.ClickActions;
import com.basti20999.customGUIs.config.GUIConfig;
import com.basti20999.customGUIs.config.ItemConfig;
import com.basti20999.customGUIs.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class GUIListener implements Listener {

    private final GUIManager manager;

    public GUIListener(GUIManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof CustomGUIHolder guiHolder)) return;

        GUIConfig gui = guiHolder.getConfig();

        if (gui.readonly()) event.setCancelled(true);

        // Ignore clicks outside the GUI top inventory
        if (event.getRawSlot() >= gui.size()) return;

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        ItemConfig item = gui.items().get(event.getRawSlot());
        if (item == null) return;

        // Determine click-type key
        String clickKey = switch (event.getClick()) {
            case LEFT -> "left";
            case RIGHT -> "right";
            case SHIFT_LEFT -> "shift-left";
            case SHIFT_RIGHT -> "shift-right";
            case MIDDLE -> "middle";
            default -> "left";
        };

        ClickActions actions = item.actionsFor(clickKey);
        if (actions.isEmpty()) return;

        Player player = (Player) event.getWhoClicked();

        if (item.clickSound() != null) {
            player.playSound(player.getLocation(), item.clickSound(), 1f, 1f);
        }

        if (gui.closeOnClick()) player.closeInventory();

        runActions(actions, player);
    }

    private void runActions(ClickActions actions, Player player) {
        if (actions.command() != null) {
            player.performCommand(ColorUtil.placeholders(actions.command(), player));
        }
        if (actions.consoleCommand() != null) {
            String cmd = ColorUtil.placeholders(actions.consoleCommand(), player);
            player.getServer().dispatchCommand(player.getServer().getConsoleSender(), cmd);
        }
        if (actions.openGui() != null) {
            manager.openGUI(actions.openGui(), player);
        }
        if (actions.message() != null) {
            player.sendMessage(ColorUtil.translate(actions.message(), player));
        }
    }
}
