package com.bearnation.bearCosmetics.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class GuiBuilder {
    private static final Set<UUID> suppressClose = new HashSet<>();
    public static final NamespacedKey KEY = new NamespacedKey(
            JavaPlugin.getProvidingPlugin(GuiBuilder.class), "internal_name");

    private static final Set<UUID> emptyMenuUsers = new HashSet<>();
    private static final Map<UUID, String> tempEmptyMenuContext = new HashMap<>();
    private static final Map<UUID, String> lastMenuContext = new HashMap<>();
    private static final Map<UUID, Deque<Runnable>> guiHistory = new HashMap<>();
    private static final Set<UUID> lastOpenedWasCosmeticGUI = new HashSet<>();

    public static void markAsTemporaryEmptyMenu(Player player, String context) {
        UUID uuid = player.getUniqueId();
        emptyMenuUsers.add(uuid);
        tempEmptyMenuContext.put(uuid, context);
        lastMenuContext.put(uuid, context);
    }

    public static boolean wasTemporaryEmptyMenu(Player player) {
        return emptyMenuUsers.contains(player.getUniqueId());
    }

    public static void clearTemporaryEmptyMenu(Player player) {
        UUID uuid = player.getUniqueId();
        emptyMenuUsers.remove(uuid);
        tempEmptyMenuContext.remove(uuid);
    }

    public static String getTemporaryMenuContext(Player player) {
        return tempEmptyMenuContext.getOrDefault(player.getUniqueId(), "Lobby");
    }

    public static String getLastMenuContext(Player player) {
        return lastMenuContext.getOrDefault(player.getUniqueId(), "Lobby");
    }

    public static void updateLastMenuContext(Player player, String context) {
        lastMenuContext.put(player.getUniqueId(), context);
    }

    public static void pushHistory(Player player, Runnable backAction) {
        guiHistory.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>()).push(backAction);
    }

    public static void popAndRunHistory(Player player) {
        Runnable back = popHistory(player);
        if (back != null) back.run();
    }

    public static void clearHistory(Player player) {
        guiHistory.remove(player.getUniqueId());
    }

    public static void suppressNextClose(Player player) {
        suppressClose.add(player.getUniqueId());
    }

    public static boolean shouldSuppressClose(Player player) {
        return suppressClose.contains(player.getUniqueId());
    }

    public static void clearSuppressFlag(Player player) {
        suppressClose.remove(player.getUniqueId());
    }

    public static ItemStack makeBackButton() {
        return make(Material.BARRIER, "\u00a7cBack", "\u00a77Return to the previous menu");
    }

    public static ItemStack make(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack setInternalName(ItemStack item, String internal) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, internal);
        item.setItemMeta(meta);
        return item;
    }

    public static String getInternalName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
    }

    public static ItemStack createCosmeticItem(Material material, String displayName, String internalName, String... lore) {
        return setInternalName(make(material, displayName, lore), internalName);
    }

    public static boolean hasHistory(Player player) {
        Deque<Runnable> stack = guiHistory.get(player.getUniqueId());
        return stack != null && !stack.isEmpty();
    }

    public static Runnable popHistory(Player player) {
        Deque<Runnable> stack = guiHistory.get(player.getUniqueId());
        if (stack != null && !stack.isEmpty()) {
            return stack.pop();
        }
        return null;
    }
}
