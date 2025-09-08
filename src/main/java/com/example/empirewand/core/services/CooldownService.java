package com.example.empirewand.core.services;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Skeleton cooldown tracker per player + spell key.
 * Now supports disabling cooldowns per player-wand combination.
 */
public class CooldownService {
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Map<String, Boolean> disabledCooldowns = new ConcurrentHashMap<>();

    public boolean isOnCooldown(UUID playerId, String key, long nowTicks) {
        if (playerId == null || key == null) {
            return false;
        }
        try {
            var map = cooldowns.get(playerId);
            if (map == null)
                return false;
            var until = map.getOrDefault(key, 0L);
            return nowTicks < until;
        } catch (Exception e) {
            // Log error but don't crash - cooldown check should never break functionality
            return false;
        }
    }

    /**
     * Checks if a player-wand-spell combination is on cooldown, considering
     * cooldown disables.
     */
    public boolean isOnCooldown(UUID playerId, String key, long nowTicks, ItemStack wand) {
        if (playerId == null || key == null) {
            return false;
        }
        try {
            // Check if cooldowns are disabled for this player-wand combination
            if (isCooldownDisabled(playerId, wand)) {
                return false;
            }
            return isOnCooldown(playerId, key, nowTicks);
        } catch (Exception e) {
            return false;
        }
    }

    public long remaining(UUID playerId, String key, long nowTicks) {
        if (playerId == null || key == null) {
            return 0L;
        }
        try {
            var map = cooldowns.get(playerId);
            if (map == null)
                return 0L;
            var until = map.getOrDefault(key, 0L);
            return Math.max(0L, until - nowTicks);
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Gets remaining cooldown time considering cooldown disables.
     */
    public long remaining(UUID playerId, String key, long nowTicks, ItemStack wand) {
        if (playerId == null || key == null) {
            return 0L;
        }
        try {
            // If cooldowns are disabled, always return 0
            if (isCooldownDisabled(playerId, wand)) {
                return 0L;
            }
            return remaining(playerId, key, nowTicks);
        } catch (Exception e) {
            return 0L;
        }
    }

    public void set(UUID playerId, String key, long untilTicks) {
        if (playerId == null || key == null || untilTicks < 0) {
            return;
        }
        try {
            cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(key, untilTicks);
        } catch (Exception e) {
            // Log error but don't crash
        }
    }

    public void clearAll(UUID playerId) {
        if (playerId == null) {
            return;
        }
        try {
            cooldowns.remove(playerId);
            // Also remove any cooldown disables for this player
            disabledCooldowns.entrySet().removeIf(entry -> entry.getKey().startsWith(playerId.toString()));
        } catch (Exception e) {
            // Log error but don't crash
        }
    }

    /**
     * Sets whether cooldowns are disabled for a specific player and wand.
     * Uses the wand's unique identifier to track per-wand cooldown state.
     */
    public void setCooldownDisabled(UUID playerId, ItemStack wand, boolean disabled) {
        if (playerId == null || wand == null) {
            return;
        }
        try {
            String wandId = getWandIdentifier(wand);
            String disableKey = playerId.toString() + ":" + wandId;

            if (disabled) {
                disabledCooldowns.put(disableKey, true);
            } else {
                disabledCooldowns.remove(disableKey);
            }
        } catch (Exception e) {
            // Log error but don't crash
        }
    }

    /**
     * Checks if cooldowns are disabled for a specific player and wand.
     */
    public boolean isCooldownDisabled(UUID playerId, ItemStack wand) {
        if (playerId == null || wand == null) {
            return false;
        }
        try {
            String wandId = getWandIdentifier(wand);
            String disableKey = playerId.toString() + ":" + wandId;
            return disabledCooldowns.getOrDefault(disableKey, false);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generates a unique identifier for a wand based on its metadata.
     * This could be enhanced to use custom NBT data for truly unique
     * identification.
     */
    private String getWandIdentifier(ItemStack wand) {
        if (wand == null) {
            return "unknown";
        }
        try {
            ItemMeta meta = wand.getItemMeta();
            if (meta == null) {
                return "default";
            }

            // Use display name + material as identifier for now
            // In a more sophisticated implementation, you might use custom NBT tags
            String displayName = meta.hasDisplayName() && meta.displayName() != null
                    ? meta.displayName().toString()
                    : "";
            return wand.getType().toString() + ":" + displayName.hashCode();
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * Shuts down the cooldown service and cleans up all data
     */
    public void shutdown() {
        cooldowns.clear();
        disabledCooldowns.clear();
    }
}
