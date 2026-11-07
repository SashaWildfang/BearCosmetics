package com.bearnation.bearCosmetics.gui;

import com.bearnation.bearCosmetics.BearCosmetics;
import com.bearnation.bearCosmetics.database.DatabaseManager;
import com.bearnation.bearCosmetics.utils.CosmeticUtil;
import com.bearnation.bearCosmetics.utils.GuiBuilder;
import com.bearnation.bearCosmetics.utils.ItemBuilder;
import com.bearnation.bearCosmetics.utils.MutableBoolean;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;

public class CosmeticsMainMenu {

    public static void open(Player player, String context) {
        GuiBuilder.clearTemporaryEmptyMenu(player);
        GuiBuilder.updateLastMenuContext(player, context);


        new BukkitRunnable() {
            @Override
            public void run() {
                UUID uuid = player.getUniqueId();
                int categoryId = context.equalsIgnoreCase("CTF") ? 2 : 1;

                Map<Integer, Integer> unlockedMap = CosmeticUtil.getUnlockedCountsByType(uuid, categoryId);
                Map<Integer, Integer> totalMap = CosmeticUtil.getTotalCountsByType(categoryId);

                Inventory gui = Bukkit.createInventory(null, 27, context + " Cosmetics");
                MutableBoolean hasCosmetics = new MutableBoolean(false);
                int slot = 10;

                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement("""
                        SELECT ct.id, ct.name, ct.display_name, ct.item, ct.lore
                        FROM cosmetic_types ct
                        WHERE ct.category_id = ?
                          AND EXISTS (
                            SELECT 1 FROM cosmetics c
                            WHERE c.type_id = ct.id AND c.enabled = 1
                          )
                        ORDER BY ct.id
                    """)) {

                    ps.setInt(1, categoryId);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            hasCosmetics.set(true);

                            int typeId = rs.getInt("id");
                            String internalName = rs.getString("name");
                            String displayName = rs.getString("display_name");
                            if (displayName == null || displayName.isBlank()) displayName = internalName;
                            String itemName = rs.getString("item");
                            String lore = rs.getString("lore");

                            Material icon = Material.matchMaterial(itemName);
                            if (icon == null) icon = Material.GRAY_DYE;

                            int unlocked = unlockedMap.getOrDefault(typeId, 0);
                            int total = totalMap.getOrDefault(typeId, 0);

                            ItemStack item = new ItemBuilder(icon)
                                    .name("§6" + displayName)
                                    .lore(
                                            "§7" + lore,
                                            "",
                                            "§aUnlocked: §f" + unlocked + "§7/§f" + total,
                                            "",
                                            "§eClick to browse"
                                    )
                                    .build();

                            gui.setItem(slot++, GuiBuilder.setInternalName(item, internalName));
                            if (slot % 9 == 7) slot++;
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!hasCosmetics.get()) {
                            gui.setItem(13, new ItemBuilder(Material.BARRIER)
                                    .name("§cNo Cosmetics Found")
                                    .lore("§7There are no cosmetics", "§7for this category yet.")
                                    .build());
                            GuiBuilder.markAsTemporaryEmptyMenu(player, context);
                        } else {
                            gui.setItem(22, GuiBuilder.makeBackButton());
                        }

                        player.openInventory(gui);
                    }
                }.runTask(BearCosmetics.getInstance());
            }
        }.runTaskAsynchronously(BearCosmetics.getInstance());
    }
}
