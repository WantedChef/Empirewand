package com.example.empirewand.core.services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Skeleton cooldown tracker per player + spell key.
 * Now supports disabling cooldowns per player-wand combination.
 */
public class CooldownService {
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<String, Boolean> disabledCooldowns = new HashMap<>();

    public boolean isOnCooldown(UUID playerId, String key, long nowTicks) {
        var map = cooldowns.get(playerId);
        if (map == null)
            return false;
        var until = map.getOrDefault(key, 0L);
        return nowTicks < until;
    }

    /**
     * Checks if a player-wand-spell combination is on cooldown, considering
     * cooldown disables.
     */
    public boolean isOnCooldown(UUID playerId, String key, long nowTicks, ItemStack wand) {
        // Check if cooldowns are disabled for this player-wand combination
        if (isCooldownDisabled(playerId, wand)) {
            return false;
        }

        return isOnCooldown(playerId, key, nowTicks);
    }

    public long remaining(UUID playerId, String key, long nowTicks) {
        var map = cooldowns.get(playerId);
        if (map == null)
            return 0L;
        var until = map.getOrDefault(key, 0L);
        return Math.max(0L, until - nowTicks);
    }

    /**
     * Gets remaining cooldown time considering cooldown disables.
     */
    public long remaining(UUID playerId, String key, long nowTicks, ItemStack wand) {
        // If cooldowns are disabled, always return 0
        if (isCooldownDisabled(playerId, wand)) {
            return 0L;
        }

        return remaining(playerId, key, nowTicks);
    }

    public void set(UUID playerId, String key, long untilTicks) {
        cooldowns.computeIfAbsent(playerId, k -> new HashMap<>()).put(key, untilTicks);
    }

    public void clearAll(UUID playerId) {
        cooldowns.remove(playerId);
        // Also remove any cooldown disables for this player
        disabledCooldowns.entrySet().removeIf(entry -> entry.getKey().startsWith(playerId.toString()));
    }

    /**
     * Sets whether cooldowns are disabled for a specific player and wand.
     * Uses the wand's unique identifier to track per-wand cooldown state.
     */
    public void setCooldownDisabled(UUID playerId, ItemStack wand, boolean disabled) {
        if (wand == null)
            return;

        String wandId = getWandIdentifier(wand);
        String disableKey = playerId.toString() + ":" + wandId;

        if (disabled) {
            disabledCooldowns.put(disableKey, true);
        } else {
            disabledCooldowns.remove(disableKey);
        }
    }

    /**
     * Checks if cooldowns are disabled for a specific player and wand.
     */
    public boolean isCooldownDisabled(UUID playerId, ItemStack wand) {
        if (wand == null)
            return false;

        String wandId = getWandIdentifier(wand);
        String disableKey = playerId.toString() + ":" + wandId;

        return disabledCooldowns.getOrDefault(disableKey, false);
    }

    /**
     * Generates a unique identifier for a wand based on its metadata.
     * This could be enhanced to use custom NBT data for truly unique
     * identification.
     */
    private String getWandIdentifier(ItemStack wand) {
        if (wand == null)
            return "unknown";

        ItemMeta meta = wand.getItemMeta();
        if (meta == null)
            return "default";

        // Use display name + material as identifier for now
        // In a more sophisticated implementation, you might use custom NBT tags
        String displayName = meta.hasDisplayName() && meta.displayName() != null
                ? meta.displayName().toString()
                : "";
        return wand.getType().toString() + ":" + displayName.hashCode();
    }
}
