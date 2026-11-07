package com.bearnation.bearCosmetics.utils;

import com.bearnation.bearCosmetics.cache.CosmeticCache;
import com.bearnation.bearCosmetics.cache.CosmeticInfo;
import com.bearnation.bearCosmetics.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;

public class UserUtil {

    public static int getCoins(UUID uuid) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT coins FROM users WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("coins");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean tryPurchase(Player player, int cost) {
        UUID uuid = player.getUniqueId();
        int coins = getCoins(uuid);

        if (coins < cost) return false;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE users SET coins = coins - ? WHERE uuid = ?")) {
            ps.setInt(1, cost);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int getCosmeticCost(String internalName) {
        CosmeticInfo info = CosmeticCache.get(internalName);
        return info != null ? info.cost() : 9999;
    }

    public static String getUserUuid(UUID uuid) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM users WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("uuid");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static boolean hasCosmetic(UUID uuid, String internalName) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM user_cosmetics uc JOIN cosmetics c ON uc.cosmetic_id = c.id WHERE uc.uuid = ? AND c.internal_name = ?"
             )) {
            ps.setString(1, uuid.toString());
            ps.setString(2, internalName);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getTypeFromInternalName(String internalName) {
        CosmeticInfo info = CosmeticCache.get(internalName);
        return info != null ? info.typeName() : null;
    }

    public static boolean isEquipped(UUID uuid, String internalName) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
             SELECT 1 FROM user_equipped ue
             JOIN cosmetics c ON ue.cosmetic_id = c.id
             WHERE ue.user_id = ? AND c.internal_name = ?
         """)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, internalName);
            ResultSet rs = ps.executeQuery();
            Bukkit.getLogger().info("Equipping cosmetic: " + internalName);
            CosmeticInfo info = CosmeticCache.get(internalName);
            if (info == null) {
                Bukkit.getLogger().warning("CosmeticInfo not found for: " + internalName);
            }
            return rs.next();


        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }




    public static Material getCosmeticMaterial(UUID uuid, String internalName) {
        CosmeticInfo info = CosmeticCache.get(internalName);
        if (info == null) return Material.BARRIER;

        boolean unlocked = hasCosmetic(uuid, internalName);
        return unlocked ? info.materialIfUnlocked() : info.materialIfLocked();
    }

    public static String getCosmeticDisplayName(String internalName, boolean equipped) {
        CosmeticInfo info = CosmeticCache.get(internalName);
        if (info == null) return "§cUnknown Cosmetic";
        return equipped ? "§a§l" + info.displayName() : "§b" + info.displayName();
    }

    public static List<String> getCosmeticLore(UUID uuid, String internalName, boolean equipped, String typeName, int cost) {
        List<String> lore = new ArrayList<>();
        CosmeticInfo info = CosmeticCache.get(internalName);
        if (info == null) return lore;

        String desc = info.description();
        if (desc != null && !desc.isBlank()) {
            for (String line : desc.split("\\n")) {
                lore.add("§7" + line);
            }
        }

        if (hasCosmetic(uuid, internalName)) {
            if (equipped) {
                lore.add("§a§lEQUIPPED");
                lore.add("§eClick to unequip");
            } else {
                lore.add("§eClick to equip this " + typeName);
            }
        } else {
            lore.add("§fCost: §6" + cost + " Coins");
            lore.add("§eClick to purchase this " + typeName);
        }

        return lore;
    }
}
