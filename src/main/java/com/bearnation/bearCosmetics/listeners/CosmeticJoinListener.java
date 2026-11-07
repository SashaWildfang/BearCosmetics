package com.bearnation.bearCosmetics.listeners;

import com.bearnation.bearCosmetics.utils.CosmeticUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class CosmeticJoinListener implements Listener {

    private final JavaPlugin plugin;

    public CosmeticJoinListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Delay to ensure player is fully loaded before applying cosmetics silently
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            CosmeticUtil.applyEquippedCosmetics(player, true);
        }, 20L); // 1 second delay
    }
}
