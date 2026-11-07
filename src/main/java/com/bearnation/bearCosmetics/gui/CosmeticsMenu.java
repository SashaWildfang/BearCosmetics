package com.bearnation.bearCosmetics.gui;

import com.bearnation.bearCosmetics.BearCosmetics;
import com.bearnation.bearCosmetics.database.DatabaseManager;
import com.bearnation.bearCosmetics.utils.GuiBuilder;
import com.bearnation.bearCosmetics.utils.ItemBuilder;
import com.bearnation.bearCosmetics.utils.UserUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class CosmeticsMenu {

    private static final int ITEMS_PER_PAGE = 45;

    public static void open(Player player, String context, String type, int page) {
        if (page < 1) page = 1;
        final int safePage = page;
        final String safeContext = context == null ? "Cosmetics" : context;
        final String safeType = type == null ? "" : type;

        new BukkitRunnable() {
            @Override
            public void run() {
                // --- CHANGED: silently ignore unknown types (no player message) ---
                Integer typeId = getTypeIdNullable(safeType);
                if (typeId == null) {
                    // Optional: one-line debug log only
                    BearCosmetics.getInstance().getLogger()
                            .fine("[CosmeticsMenu] Ignoring open() for unknown type '" + safeType + "'");
                    return;
                }

                List<ItemStack> cosmetics = getCosmeticsForTypeId(player, safeContext, typeId, safeType);

                int totalPages = (int) Math.ceil((double) cosmetics.size() / ITEMS_PER_PAGE);
                if (totalPages == 0) totalPages = 1;
                int pageToOpen = Math.min(safePage, totalPages);

                String title = totalPages > 1
                        ? (safeContext + " - " + safeType + " (Page " + pageToOpen + ")")
                        : (safeContext + " - " + safeType);

                Inventory gui = Bukkit.createInventory(null, 54, title);

                int start = (pageToOpen - 1) * ITEMS_PER_PAGE;
                int end = Math.min(start + ITEMS_PER_PAGE, cosmetics.size());
                for (int i = start; i < end; i++) {
                    gui.setItem(i - start, cosmetics.get(i));
                }

                gui.setItem(45, GuiBuilder.makeBackButton());
                if (totalPages > 1 && pageToOpen > 1) {
                    gui.setItem(48, new ItemBuilder(Material.ARROW)
                            .name("§ePrevious Page")
                            .lore("§7Go to page " + (pageToOpen - 1))
                            .build());
                }
                if (totalPages > 1 && end < cosmetics.size()) {
                    gui.setItem(50, new ItemBuilder(Material.ARROW)
                            .name("§eNext Page")
                            .lore("§7Go to page " + (pageToOpen + 1))
                            .build());
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) return;
                        GuiBuilder.pushHistory(player, () -> CosmeticsMainMenu.open(player, safeContext));
                        GuiBuilder.updateLastMenuContext(player, safeContext);
                        player.openInventory(gui);
                    }
                }.runTask(BearCosmetics.getInstance());
            }
        }.runTaskAsynchronously(BearCosmetics.getInstance());
    }

    private static Integer getTypeIdNullable(String typeName) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM cosmetic_types WHERE name = ?")) {
            ps.setString(1, typeName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (Exception e) {
            BearCosmetics.getInstance().getLogger()
                    .fine("[CosmeticsMenu] Failed to resolve type '" + typeName + "': " + e.getMessage());
        }
        return null;
    }

    private static List<ItemStack> getCosmeticsForTypeId(Player player, String context, int typeId, String typeName) {
        List<ItemStack> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            UUID uuid = player.getUniqueId();
            String query = """
                SELECT c.*, (uc.uuid IS NOT NULL) AS unlocked
                FROM cosmetics c
                LEFT JOIN user_cosmetics uc ON uc.cosmetic_id = c.id AND uc.uuid = ?
                WHERE c.type_id = ? AND c.context = ? AND c.enabled = 1
                """;
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, typeId);
                ps.setString(3, context);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        boolean unlocked = rs.getBoolean("unlocked");
                        String name = rs.getString("display_name");
                        String desc = rs.getString("description");
                        String internal = rs.getString("internal_name");
                        int cost = rs.getInt("cost");

                        Material icon;
                        try {
                            icon = Material.valueOf(unlocked
                                    ? rs.getString("material_if_unlocked")
                                    : rs.getString("material_if_locked"));
                        } catch (IllegalArgumentException ex) {
                            icon = Material.BARRIER;
                        }

                        String displayName = unlocked ? "§a" + name : "§c" + name + " - Locked";
                        List<String> lore = new ArrayList<>();
                        lore.add("§7" + (desc == null ? "" : desc));
                        lore.add("");

                        ItemBuilder builder = new ItemBuilder(icon).name(displayName);

                        if (unlocked) {
                            boolean equipped = UserUtil.isEquipped(uuid, internal);
                            if (equipped) {
                                displayName = "§a§l" + name;
                                lore.add("§a§lEQUIPPED");
                                lore.add("§eClick to unequip");
                                builder.name(displayName)
                                        .enchant(Enchantment.UNBREAKING, 1)
                                        .flag(ItemFlag.HIDE_ENCHANTS);
                            } else {
                                lore.add("§eClick to equip this " + typeName);
                            }
                        } else {
                            lore.add("§fCost: §6" + formatCost(cost) + " Coins");
                            lore.add("§eClick to purchase this " + typeName);
                        }

                        ItemStack item = builder.lore(lore.toArray(new String[0])).build();
                        list.add(GuiBuilder.setInternalName(item, internal));
                    }
                }
            }
        } catch (Exception e) {
            BearCosmetics.getInstance().getLogger()
                    .warning("Error loading cosmetics for typeId " + typeId + " (" + typeName + "): " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    private static String formatCost(int value) {
        if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000.0);
        if (value >= 1_000) return String.format("%.1fk", value / 1_000.0);
        return String.valueOf(value);
    }
}
