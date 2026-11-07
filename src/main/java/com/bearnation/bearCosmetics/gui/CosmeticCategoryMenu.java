package com.bearnation.bearCosmetics.gui;

import com.bearnation.bearCosmetics.BearCosmetics;
import com.bearnation.bearCosmetics.utils.GuiBuilder;
import com.bearnation.bearCosmetics.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;

public class CosmeticCategoryMenu {

    public static void open(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Inventory gui = Bukkit.createInventory(null, 9, "Select Cosmetic Type");

                // Fill with gray stained glass panes
                ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("\u00a77").build();
                for (int i = 0; i < 9; i++) {
                    gui.setItem(i, filler);
                }

                // Lobby cosmetics - Beacon
                ItemStack lobby = new ItemBuilder(Material.BEACON)
                        .name("\u00a7bLobby Cosmetics")
                        .lore(
                                "\u00a77Customize your hub appearance,",
                                "\u00a77from particles to pets and effects.",
                                "",
                                "\u00a7eClick to view"
                        )
                        .build();
                gui.setItem(3, lobby);

                // CTF cosmetics - red/blue banner with black stripe
                ItemStack banner = new ItemStack(Material.BLUE_BANNER);
                BannerMeta meta = (BannerMeta) banner.getItemMeta();
                meta.setDisplayName("\u00a7cCTF Cosmetics");
                meta.setLore(Arrays.asList(
                        "\u00a77Unlock cosmetics for in-game battles,",
                        "\u00a77like trails, kill effects, and more.",
                        "",
                        "\u00a7eClick to view"
                ));
                meta.setPatterns(Arrays.asList(
                        new Pattern(DyeColor.RED, PatternType.HALF_VERTICAL)
                ));
                meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                banner.setItemMeta(meta);
                gui.setItem(5, banner);

                // Register for history so it's backable
                GuiBuilder.pushHistory(player, () -> CosmeticCategoryMenu.open(player));

                player.openInventory(gui);
            }
        }.runTask(BearCosmetics.getInstance());
    }
}
