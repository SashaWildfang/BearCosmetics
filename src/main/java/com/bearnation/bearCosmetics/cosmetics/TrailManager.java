package com.bearnation.bearCosmetics.cosmetics;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TrailManager {

    private static final Map<UUID, Particle> activeTrails = new HashMap<>();
    private static final Map<String, Particle> trailMap = new HashMap<>();
    private static final Map<UUID, Location> lastLocations = new HashMap<>();
    private static BukkitRunnable task;

    static {
        trailMap.put("trail_spark", Particle.CRIT);
        trailMap.put("trail_heart", Particle.HEART);
        trailMap.put("trail_bubbles", Particle.DRIPPING_WATER); // best visual trail
        trailMap.put("trail_magic", Particle.END_ROD);
        // Add more if needed
    }

    public static void start(JavaPlugin plugin) {
        if (task != null) return;

        task = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    Particle trail = activeTrails.get(uuid);
                    if (trail == null) continue;

                    Location current = player.getLocation();
                    Location last = lastLocations.get(uuid);

                    // Skip if player hasn't moved (within ~0.05 blocks)
                    if (last != null && current.distanceSquared(last) < 0.0025) {
                        continue;
                    }

                    lastLocations.put(uuid, current.clone());

                    // Spawn particle slightly behind and close to the ground
                    Location loc = current.clone()
                            .subtract(current.getDirection().multiply(0.3));
                    loc.setY(loc.getY() + 0.4); // consistent ground height for all trails

                    player.getWorld().spawnParticle(
                            trail,
                            loc,
                            5,              // count
                            0.2, 0.05, 0.2, // spread
                            0.01            // speed
                    );
                }
            }
        };
        task.runTaskTimer(plugin, 0L, 2L); // every 2 ticks
    }

    public static void setTrail(Player player, Particle particle) {
        activeTrails.put(player.getUniqueId(), particle);
        lastLocations.put(player.getUniqueId(), player.getLocation().clone());
    }

    public static void removeTrail(Player player) {
        UUID uuid = player.getUniqueId();
        activeTrails.remove(uuid);
        lastLocations.remove(uuid);
    }

    public static boolean hasTrail(Player player) {
        return activeTrails.containsKey(player.getUniqueId());
    }

    public static Particle fromInternalName(String name) {
        return trailMap.getOrDefault(name.toLowerCase(), null);
    }
}
