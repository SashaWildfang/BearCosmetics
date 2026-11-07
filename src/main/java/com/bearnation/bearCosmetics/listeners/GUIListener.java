package com.bearnation.bearCosmetics.listeners;

import com.bearnation.bearCosmetics.BearCosmetics;
import com.bearnation.bearCosmetics.database.DatabaseManager;
import com.bearnation.bearCosmetics.gui.ConfirmPurchaseMenu;
import com.bearnation.bearCosmetics.gui.CosmeticCategoryMenu;
import com.bearnation.bearCosmetics.gui.CosmeticsMainMenu;
import com.bearnation.bearCosmetics.gui.CosmeticsMenu;
import com.bearnation.bearCosmetics.utils.CosmeticUtil;
import com.bearnation.bearCosmetics.utils.GuiBuilder;
import com.bearnation.bearCosmetics.utils.UserUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GUIListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null || e.getCurrentItem() == null) return;

        ItemStack item = e.getCurrentItem();
        String title = e.getView().getTitle();

        // MAIN MENU: Lobby / CTF Selector
        if (title.equalsIgnoreCase("Select Cosmetic Type")) {
            e.setCancelled(true);
            if (item.getType() == Material.BEACON) {
                CosmeticsMainMenu.open(player, "Lobby");
            } else if (item.getType() == Material.BLUE_BANNER) {
                CosmeticsMainMenu.open(player, "CTF");
            }
            return;
        }

        // CONTEXT MENU: Lobby Cosmetics, CTF Cosmetics, etc.
        if (title.endsWith("Cosmetics")) {
            e.setCancelled(true);
            String context = title.replace(" Cosmetics", "");

            switch (item.getType()) {
                case BLAZE_POWDER -> CosmeticsMenu.open(player, context, "Aura Effects", 1);
                case GOLD_BLOCK -> CosmeticsMenu.open(player, context, "Hats", 1);
                case PRISMARINE_CRYSTALS -> CosmeticsMenu.open(player, context, "Pets", 1);
                case END_ROD -> CosmeticsMenu.open(player, context, "Trails", 1);
                case BARRIER -> CosmeticCategoryMenu.open(player);
            }
            return;
        }

        // COSMETICS MENU PAGE (e.g. Lobby - Trails (Page 2))
        if (title.matches(".* - .*?( \\(Page ?\\d+\\))?")) {
            e.setCancelled(true);

            String context = title.split(" - ")[0].trim();
            String typeName = parseTypeNameFromTitle(title);
            int currentPage = parsePageFromTitle(title);

            if (typeName == null) return;

            // Navigation arrows
            if (item.getType() == Material.ARROW) {
                String name = item.getItemMeta().getDisplayName();
                if (name.contains("Next")) {
                    CosmeticsMenu.open(player, context, typeName, currentPage + 1);
                } else if (name.contains("Previous")) {
                    CosmeticsMenu.open(player, context, typeName, currentPage - 1);
                }
                return;
            }

            if (item.getType() == Material.BARRIER) {
                CosmeticCategoryMenu.open(player);
                return;
            }

            // Cosmetic item click
            String internal = GuiBuilder.getInternalName(item);
            if (internal != null) {
                Bukkit.getScheduler().runTaskAsynchronously(BearCosmetics.getInstance(), () -> {
                    boolean unlocked = UserUtil.hasCosmetic(player.getUniqueId(), internal);

                    if (unlocked) {
                        boolean equipped = UserUtil.isEquipped(player.getUniqueId(), internal);

                        if (equipped) {
                            // Manual unequip → show message
                            CosmeticUtil.unequipCosmetic(player, typeName, false);
                        } else {
                            // Switch cosmetic → silent unequip
                            CosmeticUtil.unequipCosmetic(player, typeName, true);
                            CosmeticUtil.equipCosmetic(player, typeName, internal, false);
                        }

                        Bukkit.getScheduler().runTask(BearCosmetics.getInstance(), () -> {
                            GuiBuilder.suppressNextClose(player); // ✅ suppress InventoryCloseEvent handler
                            player.closeInventory();
                            player.playSound(player.getLocation(), "minecraft:ui.button.click", 1f, 1f);
                        });                        //Bukkit.getScheduler().runTask(BearCosmetics.getInstance(), () -> {
                        //    CosmeticsMenu.open(player, context, typeName, currentPage);
                        //    player.playSound(player.getLocation(), "minecraft:ui.button.click", 1f, 1f);
                        //});
                    } else {
                        Bukkit.getScheduler().runTask(BearCosmetics.getInstance(), () -> {
                            int cost = UserUtil.getCosmeticCost(internal);
                            Material icon = item.getType();
                            String display = item.getItemMeta().getDisplayName();
                            ConfirmPurchaseMenu.open(player, internal, display, cost, icon);
                        });
                    }
                });
            }
            return;
        }

        // CONFIRM PURCHASE MENU
        if (title.equalsIgnoreCase("Confirm Purchase")) {
            e.setCancelled(true);
            String internalName = GuiBuilder.getInternalName(item);
            if (internalName == null) return;

            if (item.getType() == Material.EMERALD_BLOCK) {
                int cost = UserUtil.getCosmeticCost(internalName);
                if (UserUtil.tryPurchase(player, cost)) {
                    try (Connection conn = DatabaseManager.getConnection()) {
                        int cosmeticId = getCosmeticIdFromInternalName(conn, internalName);
                        if (cosmeticId == -1) {
                            player.sendMessage("§cCosmetic not found.");
                            return;
                        }

                        PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO user_cosmetics (uuid, cosmetic_id, unlocked_at) " +
                                        "VALUES (?, ?, NOW()) " +
                                        "ON DUPLICATE KEY UPDATE unlocked_at = unlocked_at"
                        );
                        ps.setString(1, player.getUniqueId().toString());
                        ps.setInt(2, cosmeticId);
                        ps.executeUpdate();
                    } catch (SQLException ex) {
                        player.sendMessage("§cAn error occurred while processing your purchase.");
                        ex.printStackTrace();
                        return;
                    }

                    String typeName = getTypeNameFromInternal(internalName);
                    CosmeticUtil.equipCosmetic(player, typeName, internalName, false);
                    player.sendMessage("§aSuccessfully purchased §f" + internalName + "§a!");
                    player.closeInventory();
                } else {
                    player.sendMessage("§cYou don’t have enough coins.");
                    player.closeInventory();
                }
            } else if (item.getType() == Material.REDSTONE_BLOCK) {
                player.closeInventory();
            }
        }
    }

    private int getCosmeticIdFromInternalName(Connection conn, String internalName) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT id FROM cosmetics WHERE internal_name = ?");
        ps.setString(1, internalName);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("id");
        }
        return -1;
    }

    // Parses "Lobby - Trails (Page 2)" → "Trails"
    private String parseTypeNameFromTitle(String title) {
        if (!title.contains(" - ")) return null;
        String remainder = title.split(" - ", 2)[1];

        if (remainder.contains("(Page")) {
            return remainder.substring(0, remainder.indexOf("(Page")).trim();
        }
        return remainder.trim();
    }

    // Parses "Lobby - Trails (Page 2)" → 2
    private int parsePageFromTitle(String title) {
        if (title.contains("(Page")) {
            try {
                String pageStr = title.substring(title.indexOf("(Page") + 6).replace(")", "").trim();
                return Integer.parseInt(pageStr);
            } catch (Exception e) {
                return 1;
            }
        }
        return 1;
    }

    // TODO: You may want to cache this mapping in memory
    private String getTypeNameFromInternal(String internalName) {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("""
                SELECT ct.name FROM cosmetics c
                JOIN cosmetic_types ct ON c.type_id = ct.id
                WHERE c.internal_name = ?
            """);
            ps.setString(1, internalName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("name");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Unknown";
    }
}
