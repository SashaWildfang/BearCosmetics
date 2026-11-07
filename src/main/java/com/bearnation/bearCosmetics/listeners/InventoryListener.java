package com.bearnation.bearCosmetics.listeners;

import com.bearnation.bearCosmetics.gui.CosmeticCategoryMenu;
import com.bearnation.bearCosmetics.utils.GuiBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class InventoryListener implements Listener {

    private final JavaPlugin plugin;

    public InventoryListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            String title = event.getView().getTitle();

            // Suppress close behavior if flagged
            if (GuiBuilder.shouldSuppressClose(player)) {
                GuiBuilder.clearSuppressFlag(player);
                return;
            }

            // Only react if player is in normal inventory state
            player.getOpenInventory();
            player.getOpenInventory().getTopInventory();
            if (player.getOpenInventory().getTopInventory().getType().name().equals("CRAFTING")) {

                // Special case: menu was temporarily empty (no cosmetics)
                if (GuiBuilder.wasTemporaryEmptyMenu(player)) {
                    GuiBuilder.clearTemporaryEmptyMenu(player);
                    CosmeticCategoryMenu.open(player);
                    return;
                }

                // Back navigation
                if (GuiBuilder.hasHistory(player)) {
                    Runnable back = GuiBuilder.popHistory(player);

                    // Don't reopen CosmeticCategoryMenu if that's what just closed
                    if (back != null && !title.equals("Select Cosmetic Type")) {
                        back.run();
                    }
                }
            }
        }, 2L);
    }
}
