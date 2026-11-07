package com.bearnation.bearCosmetics.utils;

import com.bearnation.bearCosmetics.cosmetics.AuraManager;
import com.bearnation.bearCosmetics.cosmetics.TrailManager;
import com.bearnation.bearCosmetics.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class CosmeticUtil {

    public static void equipCosmetic(Player player, String typeName, String internalName, boolean silent) {

        UUID uuid = player.getUniqueId();

        // Validate inputs
        if (typeName == null || internalName == null) {
            Bukkit.getLogger().warning("equipCosmetic called with null arguments.");
            return;
        }

        // Step 0: Unequip currently equipped cosmetic of that type
        unequipCosmetic(player, typeName, true);

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement getType = conn.prepareStatement("SELECT id FROM cosmetic_types WHERE name = ?");
            getType.setString(1, typeName);
            ResultSet rsType = getType.executeQuery();

            if (!rsType.next()) {
                Bukkit.getLogger().warning("Could not equip cosmetic: unknown type '" + typeName + "'");
                return;
            }

            int typeId = rsType.getInt("id");

            PreparedStatement ps = conn.prepareStatement("""
                REPLACE INTO user_equipped (user_id, type_id, cosmetic_id)
                VALUES (?, ?, (SELECT id FROM cosmetics WHERE internal_name = ?))
            """);
            ps.setString(1, uuid.toString());
            ps.setInt(2, typeId);
            ps.setString(3, internalName);
            ps.executeUpdate();

            PreparedStatement itemInfo = conn.prepareStatement("""
                SELECT c.type_id, c.material_if_unlocked, c.display_name
                FROM cosmetics c
                WHERE c.internal_name = ?
            """);
            itemInfo.setString(1, internalName);
            ResultSet rs = itemInfo.executeQuery();

            if (rs.next()) {
                int cosmeticTypeId = rs.getInt("type_id");
                String matName = rs.getString("material_if_unlocked");
                String displayName = rs.getString("display_name");

                if (cosmeticTypeId == 8) {
                    Material mat = Material.getMaterial(matName.toUpperCase());
                    if (mat != null) {
                        ItemStack gadgetItem = new ItemBuilder(mat)
                                .name("§b" + displayName)
                                .lore("§7Equipped Gadget")
                                .build();
                        player.getInventory().setItem(6, gadgetItem);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Apply effects
        switch (typeName.toLowerCase()) {
            case "trails" -> {
                var particle = TrailManager.fromInternalName(internalName);
                if (particle != null) TrailManager.setTrail(player, particle);
            }
            case "auras" -> {
                var aura = AuraManager.fromInternalName(internalName);
                if (aura != null) AuraManager.setAura(player, aura);
            }
            case "gadgets" -> {} // already handled
        }

        if (!silent) {
            player.sendMessage("§aEquipped cosmetic: §f" + internalName);
        }
    }

    public static void unequipCosmetic(Player player, String typeName, boolean silent) {
        UUID uuid = player.getUniqueId();
        if (typeName == null) {
            Bukkit.getLogger().warning("Tried to unequip cosmetic with null typeName!");
            return;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
            DELETE FROM user_equipped
            WHERE user_id = ? AND type_id = (
                SELECT id FROM cosmetic_types WHERE name = ?
            )
        """)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, typeName);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        switch (typeName.toLowerCase()) {
            case "trails" -> TrailManager.removeTrail(player);
            case "auras" -> AuraManager.removeAura(player);
            case "gadgets" -> player.getInventory().clear(6);
        }

        if (!silent) {
            player.sendMessage("§cUnequipped cosmetic type '" + typeName + "'");
        }
    }


    public static void applyEquippedCosmetics(Player player, boolean silent) {
        UUID uuid = player.getUniqueId();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                SELECT ct.name AS type_name, c.internal_name
                FROM user_equipped ue
                JOIN cosmetic_types ct ON ue.type_id = ct.id
                JOIN cosmetics c ON ue.cosmetic_id = c.id
                WHERE ue.user_id = ?
            """)) {

            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String typeName = rs.getString("type_name");
                String internalName = rs.getString("internal_name");
                equipCosmetic(player, typeName, internalName, true);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getUnlockedCountByCategory(UUID uuid, int categoryId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("""
                SELECT COUNT(*) AS count
                FROM user_cosmetics uc
                JOIN cosmetics c ON uc.cosmetic_id = c.id
                JOIN cosmetic_types ct ON c.type_id = ct.id
                WHERE uc.uuid = ? AND ct.category_id = ?
            """);
            ps.setString(1, uuid.toString());
            ps.setInt(2, categoryId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("count");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getTotalCosmeticsByCategory(int categoryId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("""
                SELECT COUNT(*) AS count
                FROM cosmetics c
                JOIN cosmetic_types ct ON c.type_id = ct.id
                WHERE ct.category_id = ? AND c.enabled = 1
            """);
            ps.setInt(1, categoryId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("count");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getUnlockedCountByCategoryAndType(UUID uuid, int categoryId, int typeId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("""
                SELECT COUNT(*) AS count
                FROM user_cosmetics uc
                JOIN cosmetics c ON uc.cosmetic_id = c.id
                JOIN cosmetic_types ct ON c.type_id = ct.id
                WHERE uc.uuid = ? AND ct.category_id = ? AND ct.id = ?
            """);
            ps.setString(1, uuid.toString());
            ps.setInt(2, categoryId);
            ps.setInt(3, typeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("count");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getTotalCosmeticsByCategoryAndType(int categoryId, int typeId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("""
                SELECT COUNT(*) AS count
                FROM cosmetics c
                JOIN cosmetic_types ct ON c.type_id = ct.id
                WHERE ct.category_id = ? AND ct.id = ? AND c.enabled = 1
            """);
            ps.setInt(1, categoryId);
            ps.setInt(2, typeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("count");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Map<Integer, Integer> getUnlockedCountsByType(UUID uuid, int categoryId) {
        Map<Integer, Integer> unlockedMap = new HashMap<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                SELECT ct.id AS type_id, COUNT(*) AS count
                FROM user_cosmetics uc
                JOIN cosmetics c ON uc.cosmetic_id = c.id
                JOIN cosmetic_types ct ON c.type_id = ct.id
                WHERE uc.uuid = ? AND ct.category_id = ?
                GROUP BY ct.id
            """)) {

            ps.setString(1, uuid.toString());
            ps.setInt(2, categoryId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                unlockedMap.put(rs.getInt("type_id"), rs.getInt("count"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return unlockedMap;
    }

    public static Map<Integer, Integer> getTotalCountsByType(int categoryId) {
        Map<Integer, Integer> totalMap = new HashMap<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                SELECT ct.id AS type_id, COUNT(*) AS count
                FROM cosmetics c
                JOIN cosmetic_types ct ON c.type_id = ct.id
                WHERE ct.category_id = ? AND c.enabled = 1
                GROUP BY ct.id
            """)) {

            ps.setInt(1, categoryId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                totalMap.put(rs.getInt("type_id"), rs.getInt("count"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return totalMap;
    }
}
