package com.bearnation.bearCosmetics.gui;

import com.bearnation.bearCosmetics.BearCosmetics;
import com.bearnation.bearCosmetics.database.DatabaseManager;
import com.bearnation.bearCosmetics.utils.GuiBuilder;
import com.bearnation.bearCosmetics.utils.ItemBuilder;
import com.bearnation.bearCosmetics.utils.UserUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

public class ConfirmPurchaseMenu {

    public static void open(Player player, String internalName, String displayName, int cost, Material icon) {
        UUID uuid = player.getUniqueId();

        new BukkitRunnable() {
            @Override
            public void run() {
                int coins = UserUtil.getCoins(uuid);
                boolean ownsCosmetic = UserUtil.hasCosmetic(uuid, internalName);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (coins < cost) {
                            int diff = cost - coins;
                            player.sendMessage("§cYou need §6" + format(diff) + " more coins §cto purchase §f" + displayName + "§c.");
                            GuiBuilder.suppressNextClose(player);
                            player.closeInventory();
                            return;
                        }

                        if (ownsCosmetic) {
                            player.sendMessage("§eYou already own §f" + displayName + "§e!");
                            GuiBuilder.suppressNextClose(player);
                            player.closeInventory();
                            return;
                        }

                        Inventory gui = Bukkit.createInventory(null, 27, "Confirm Purchase");

                        ItemStack confirm = GuiBuilder.setInternalName(
                                new ItemBuilder(Material.EMERALD_BLOCK)
                                        .name("§aConfirm Purchase")
                                        .lore("§7Buy §f" + displayName + " §7for §6" + format(cost) + " coins?")
                                        .build(),
                                internalName
                        );

                        ItemStack cancel = new ItemBuilder(Material.REDSTONE_BLOCK)
                                .name("§cCancel")
                                .build();

                        ItemStack preview = new ItemBuilder(icon)
                                .name("§b" + displayName)
                                .lore("§7Cost: §6" + format(cost))
                                .build();

                        gui.setItem(11, confirm);
                        gui.setItem(13, preview);
                        gui.setItem(15, cancel);

                        GuiBuilder.pushHistory(player, () -> {
                            String context = GuiBuilder.getLastMenuContext(player);
                            String type = UserUtil.getTypeFromInternalName(internalName);
                            CosmeticsMenu.open(player, context, type, 1);
                        });

                        GuiBuilder.updateLastMenuContext(player, GuiBuilder.getLastMenuContext(player));
                        player.openInventory(gui);
                    }
                }.runTask(BearCosmetics.getInstance());
            }
        }.runTaskAsynchronously(BearCosmetics.getInstance());
    }

    public static void finalizePurchase(Player player, String internalName, Runnable onSuccess, Runnable onFailure) {
        UUID uuid = player.getUniqueId();

        new BukkitRunnable() {
            @Override
            public void run() {
                int cost = UserUtil.getCosmeticCost(internalName);
                int coins = UserUtil.getCoins(uuid);

                if (coins < cost || !UserUtil.tryPurchase(player, cost)) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            onFailure.run();
                        }
                    }.runTask(BearCosmetics.getInstance());
                    return;
                }

                try (Connection conn = DatabaseManager.getConnection()) {
                    int cosmeticId = getCosmeticIdFromInternalName(conn, internalName);
                    if (cosmeticId == -1) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                onFailure.run();
                            }
                        }.runTask(BearCosmetics.getInstance());
                        return;
                    }

                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO user_cosmetics (uuid, cosmetic_id, unlocked_at) VALUES (?, ?, NOW())")) {
                        ps.setString(1, uuid.toString());
                        ps.setInt(2, cosmeticId);
                        ps.executeUpdate();
                    }

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            onSuccess.run();
                        }
                    }.runTask(BearCosmetics.getInstance());

                } catch (SQLException e) {
                    e.printStackTrace();
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            onFailure.run();
                        }
                    }.runTask(BearCosmetics.getInstance());
                }
            }
        }.runTaskAsynchronously(BearCosmetics.getInstance());
    }

    private static int getCosmeticIdFromInternalName(Connection conn, String internalName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM cosmetics WHERE internal_name = ?")) {
            ps.setString(1, internalName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return -1;
    }

    private static String format(int num) {
        return NumberFormat.getNumberInstance(Locale.US).format(num);
    }
}
