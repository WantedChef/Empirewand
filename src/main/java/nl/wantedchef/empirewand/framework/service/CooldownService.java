package nl.wantedchef.empirewand.framework.service;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Manages spell cooldowns for players.
 * <p>
 * This service tracks the last time a player cast a specific spell and can determine if they are still on cooldown.
 * It also supports disabling cooldowns for specific player-wand combinations.
 */
public class CooldownService {
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Map<String, Boolean> disabledCooldowns = new ConcurrentHashMap<>();

    /**
     * Checks if a player is on cooldown for a specific spell.
     *
     * @param playerId The UUID of the player.
     * @param key      The key of the spell.
     * @param nowTicks The current server tick.
     * @return true if the player is on cooldown, false otherwise.
     */
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
     * Checks if a player is on cooldown for a specific spell, considering wand-specific cooldown disables.
     *
     * @param playerId The UUID of the player.
     * @param key      The key of the spell.
     * @param nowTicks The current server tick.
     * @param wand     The wand being used.
     * @return true if the player is on cooldown, false otherwise.
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

    /**
     * Gets the remaining cooldown time for a player and spell.
     *
     * @param playerId The UUID of the player.
     * @param key      The key of the spell.
     * @param nowTicks The current server tick.
     * @return The remaining cooldown in ticks, or 0 if not on cooldown.
     */
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
     * Gets the remaining cooldown time for a player and spell, considering wand-specific cooldown disables.
     *
     * @param playerId The UUID of the player.
     * @param key      The key of the spell.
     * @param nowTicks The current server tick.
     * @param wand     The wand being used.
     * @return The remaining cooldown in ticks, or 0 if not on cooldown.
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

    /**
     * Sets a cooldown for a player and spell.
     *
     * @param playerId   The UUID of the player.
     * @param key        The key of the spell.
     * @param untilTicks The server tick until which the cooldown is active.
     */
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

    /**
     * Clears all cooldowns for a specific player.
     *
     * @param playerId The UUID of the player.
     */
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
     *
     * @param playerId The UUID of the player.
     * @param wand     The wand item.
     * @param disabled true to disable cooldowns, false to enable them.
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
     *
     * @param playerId The UUID of the player.
     * @param wand     The wand item.
     * @return true if cooldowns are disabled, false otherwise.
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
     * This is used to track per-wand cooldown disable states.
     *
     * @param wand The wand item.
     * @return A string identifier for the wand.
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
                    ? Objects.toString(meta.displayName(), "default")
                    : "default";
            return wand.getType().toString() + ":" + (displayName.isEmpty() ? "empty" : displayName).hashCode();
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * Shuts down the cooldown service and clears all cooldown data.
     */
    public void shutdown() {
        cooldowns.clear();
        disabledCooldowns.clear();
    }
}





