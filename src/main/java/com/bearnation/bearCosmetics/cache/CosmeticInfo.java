package com.bearnation.bearCosmetics.cache;

import org.bukkit.Material;

public record CosmeticInfo(
        String internalName,
        String displayName,
        String description,
        String typeName,
        int cost,
        Material materialIfUnlocked,
        Material materialIfLocked
) {}
