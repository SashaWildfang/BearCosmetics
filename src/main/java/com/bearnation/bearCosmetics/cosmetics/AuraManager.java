package com.bearnation.bearCosmetics.cosmetics;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AuraManager {

    private static final Map<UUID, Particle> activeAuras = new HashMap<>();
    private static final Map<String, Particle> auraMap = new HashMap<>();
    private static BukkitRunnable task;

    static {
        auraMap.put("aura_fire", Particle.FLAME);
        auraMap.put("aura_soul", Particle.SOUL_FIRE_FLAME);
        auraMap.put("aura_heart", Particle.HEART);
        auraMap.put("aura_notes", Particle.NOTE);
        auraMap.put("aura_rainbow", Particle.RAIN); // Adjust as needed
        // Add more mappings if needed
    }

    public static void start(JavaPlugin plugin) {
        if (task != null) return;

        task = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Particle aura = activeAuras.get(player.getUniqueId());
                    if (aura != null) {
                        Location loc = player.getLocation().add(0, 1, 0);
                        player.getWorld().spawnParticle(aura, loc, 8, 0.5, 0.3, 0.5, 0.01);
                    }
                }
            }
        };
        task.runTaskTimer(plugin, 0L, 10L);
    }

    public static void setAura(Player player, Particle particle) {
        activeAuras.put(player.getUniqueId(), particle);
    }

    public static void removeAura(Player player) {
        activeAuras.remove(player.getUniqueId());
    }

    public static boolean hasAura(Player player) {
        return activeAuras.containsKey(player.getUniqueId());
    }

    public static Particle fromInternalName(String name) {
        return auraMap.getOrDefault(name.toLowerCase(), null);
    }
}
