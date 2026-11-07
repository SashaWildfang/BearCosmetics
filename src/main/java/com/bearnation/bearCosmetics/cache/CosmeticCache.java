package com.bearnation.bearCosmetics.cache;

import com.bearnation.bearCosmetics.database.DatabaseManager;
import org.bukkit.Material;

import java.sql.*;
import java.util.*;

public class CosmeticCache {
    private static final Map<String, CosmeticInfo> CACHE = new HashMap<>();

    public static void loadAll() {
        CACHE.clear();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                 SELECT c.internal_name, c.display_name, c.description, ct.name as type_name,
                        c.cost, c.material_if_unlocked, c.material_if_locked
                 FROM cosmetics c
                 JOIN cosmetic_types ct ON c.type_id = ct.id
             """);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String internalName = rs.getString("internal_name");
                CosmeticInfo info = new CosmeticInfo(
                        internalName,
                        rs.getString("display_name"),
                        rs.getString("description"),
                        rs.getString("type_name"),
                        rs.getInt("cost"),
                        Material.getMaterial(rs.getString("material_if_unlocked")),
                        Material.getMaterial(rs.getString("material_if_locked"))
                );
                CACHE.put(internalName, info);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static CosmeticInfo get(String internalName) {
        return CACHE.get(internalName);
    }

    public static Collection<CosmeticInfo> all() {
        return CACHE.values();
    }
}
