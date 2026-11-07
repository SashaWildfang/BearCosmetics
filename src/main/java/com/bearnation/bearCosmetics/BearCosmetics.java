package com.bearnation.bearCosmetics;

import com.bearnation.bearCosmetics.cache.CosmeticCache;
import com.bearnation.bearCosmetics.commands.CosmeticsCommand;
import com.bearnation.bearCosmetics.cosmetics.AuraManager;
import com.bearnation.bearCosmetics.cosmetics.TrailManager;
import com.bearnation.bearCosmetics.database.DatabaseManager;
import com.bearnation.bearCosmetics.listeners.CosmeticJoinListener;
import com.bearnation.bearCosmetics.listeners.GUIListener;
import com.bearnation.bearCosmetics.listeners.InventoryListener;
import com.bearnation.bearCosmetics.listeners.ItemInteractListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class BearCosmetics extends JavaPlugin {

    private static BearCosmetics instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Initialize database
        DatabaseManager.initialize();

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemInteractListener(), this);
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(new CosmeticJoinListener(this), this);

        // Register commands
        getCommand("cosmetics").setExecutor(new CosmeticsCommand());


        CosmeticCache.loadAll();
        TrailManager.start(this);
        AuraManager.start(this);

        getLogger().info("BearCosmetics enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BearCosmetics disabled.");
    }

    public static BearCosmetics getInstance() {
        return instance;
    }
}
