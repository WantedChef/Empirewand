package nl.wantedchef.empirewand.framework.service;

import nl.wantedchef.empirewand.core.util.PerformanceMonitor;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Manages spell cooldowns for players.
 * <p>
 * This service tracks the last time a player cast a specific spell and can
 * determine if they are still on cooldown. It also supports disabling cooldowns
 * for specific player-wand combinations.
 *
 * Optimized for performance with efficient data structures and minimal object
 * creation.
 */
public class CooldownService {

    // Use a more memory-efficient data structure for cooldowns
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Map<String, Boolean> disabledCooldowns = new ConcurrentHashMap<>();

    // Performance monitor for tracking cooldown operations
    private final PerformanceMonitor performanceMonitor;

    // Cache for frequently accessed wand identifiers with size limit to prevent memory leaks
    private final Map<String, String> wandIdCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000; // Limit cache size to prevent memory leaks

    /**
     * Constructs a new CooldownService.
     */
    public CooldownService() {
        // Default constructor for backward compatibility
        this.performanceMonitor = new PerformanceMonitor(java.util.logging.Logger.getLogger("CooldownService"));
    }

    /**
     * Constructs a new CooldownService with plugin context.
     *
     * @param plugin The plugin instance for logger access.
     */
    public CooldownService(Plugin plugin) {
        this.performanceMonitor = new PerformanceMonitor(plugin.getLogger());
    }

    /**
     * Checks if a player is on cooldown for a specific spell.
     *
     * @param playerId The UUID of the player.
     * @param key The key of the spell.
     * @param nowTicks The current server tick.
     * @return true if the player is on cooldown, false otherwise.
     */
    public boolean isOnCooldown(UUID playerId, String key, long nowTicks) {
        try (var timing = performanceMonitor.startTiming("CooldownService.isOnCooldown", 5)) {
            timing.observe();
            assert timing != null;
            if (playerId == null || key == null) {
                return false;
            }

            // Direct access without exception handling for better performance
            Map<String, Long> playerCooldowns = cooldowns.get(playerId);
            if (playerCooldowns == null) {
                return false;
            }

            Long until = playerCooldowns.get(key);
            if (until == null) {
                return false;
            }

            return nowTicks < until;
        }
    }

    /**
     * Checks if a player is on cooldown for a specific spell, considering
     * wand-specific cooldown disables.
     *
     * @param playerId The UUID of the player.
     * @param key The key of the spell.
     * @param nowTicks The current server tick.
     * @param wand The wand being used.
     * @return true if the player is on cooldown, false otherwise.
     */
    public boolean isOnCooldown(UUID playerId, String key, long nowTicks, ItemStack wand) {
        try (var timing = performanceMonitor.startTiming("CooldownService.isOnCooldownWithWand", 5)) {
            timing.observe();
            assert timing != null;
            if (playerId == null || key == null) {
                return false;
            }

            // Check if cooldowns are disabled for this player-wand combination first
            // This is a faster check than checking cooldowns
            if (isCooldownDisabled(playerId, wand)) {
                return false;
            }

            return isOnCooldown(playerId, key, nowTicks);
        }
    }

    /**
     * Gets the remaining cooldown time for a player and spell.
     *
     * @param playerId The UUID of the player.
     * @param key The key of the spell.
     * @param nowTicks The current server tick.
     * @return The remaining cooldown in ticks, or 0 if not on cooldown.
     */
    public long remaining(UUID playerId, String key, long nowTicks) {
        try (var timing = performanceMonitor.startTiming("CooldownService.remaining", 5)) {
            timing.observe();
            assert timing != null;
            if (playerId == null || key == null) {
                return 0L;
            }

            Map<String, Long> playerCooldowns = cooldowns.get(playerId);
            if (playerCooldowns == null) {
                return 0L;
            }

            Long until = playerCooldowns.get(key);
            if (until == null) {
                return 0L;
            }

            long remaining = until - nowTicks;
            return remaining > 0 ? remaining : 0L;
        }
    }

    /**
     * Gets the remaining cooldown time for a player and spell, considering
     * wand-specific cooldown disables.
     *
     * @param playerId The UUID of the player.
     * @param key The key of the spell.
     * @param nowTicks The current server tick.
     * @param wand The wand being used.
     * @return The remaining cooldown in ticks, or 0 if not on cooldown.
     */
    public long remaining(UUID playerId, String key, long nowTicks, ItemStack wand) {
        try (var timing = performanceMonitor.startTiming("CooldownService.remainingWithWand", 5)) {
            timing.observe();
            assert timing != null;
            if (playerId == null || key == null) {
                return 0L;
            }

            // If cooldowns are disabled, always return 0
            if (isCooldownDisabled(playerId, wand)) {
                return 0L;
            }

            return remaining(playerId, key, nowTicks);
        }
    }

    /**
     * Sets a cooldown for a player and spell.
     *
     * @param playerId The UUID of the player.
     * @param key The key of the spell.
     * @param untilTicks The server tick until which the cooldown is active.
     */
    public void set(UUID playerId, String key, long untilTicks) {
        try (var timing = performanceMonitor.startTiming("CooldownService.set", 5)) {
            timing.observe();
            assert timing != null;
            if (playerId == null || key == null || untilTicks < 0) {
                return;
            }

            // Use computeIfAbsent for thread-safe lazy initialization
            cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>(4)) // Initial capacity to reduce resizing
                    .put(key, untilTicks);
        }
    }

    /**
     * Clears all cooldowns for a specific player.
     *
     * @param playerId The UUID of the player.
     */
    public void clearAll(UUID playerId) {
        try (var timing = performanceMonitor.startTiming("CooldownService.clearAll", 10)) {
            timing.observe();
            assert timing != null;
            if (playerId == null) {
                return;
            }

            cooldowns.remove(playerId);

            // More efficient removal of cooldown disables for this player
            String playerPrefix = playerId.toString() + ":";
            disabledCooldowns.entrySet().removeIf(entry -> entry.getKey().startsWith(playerPrefix));
        }
    }

    /**
     * Sets whether cooldowns are disabled for a specific player and wand.
     *
     * @param playerId The UUID of the player.
     * @param wand The wand item.
     * @param disabled true to disable cooldowns, false to enable them.
     */
    public void setCooldownDisabled(UUID playerId, ItemStack wand, boolean disabled) {
        try (var timing = performanceMonitor.startTiming("CooldownService.setCooldownDisabled", 5)) {
            timing.observe();
            assert timing != null;
            if (playerId == null || wand == null) {
                return;
            }

            String wandId = getWandIdentifier(wand);
            String disableKey = playerId.toString() + ":" + wandId;

            if (disabled) {
                disabledCooldowns.put(disableKey, Boolean.TRUE); // Use Boolean.TRUE to avoid autoboxing
            } else {
                disabledCooldowns.remove(disableKey);
            }
        }
    }

    /**
     * Checks if cooldowns are disabled for a specific player and wand.
     *
     * @param playerId The UUID of the player.
     * @param wand The wand item.
     * @return true if cooldowns are disabled, false otherwise.
     */
    public boolean isCooldownDisabled(UUID playerId, ItemStack wand) {
        try (var timing = performanceMonitor.startTiming("CooldownService.isCooldownDisabled", 5)) {
            timing.observe();
            assert timing != null;
            if (playerId == null || wand == null) {
                return false;
            }

            String wandId = getWandIdentifier(wand);
            String disableKey = playerId.toString() + ":" + wandId;
            Boolean disabled = disabledCooldowns.get(disableKey);
            return disabled != null && disabled;
        }
    }

    /**
     * Generates a unique identifier for a wand based on its metadata. This is
     * used to track per-wand cooldown disable states.
     *
     * @param wand The wand item.
     * @return A string identifier for the wand.
     */
    private String getWandIdentifier(ItemStack wand) {
        if (wand == null) {
            return "unknown";
        }

        // Generate a cache key for the wand
        String cacheKey = String.valueOf(wand.hashCode());

        // Check cache first
        String cached = wandIdCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try (var timing = performanceMonitor.startTiming("CooldownService.getWandIdentifier", 5)) {
            timing.observe();
            assert timing != null;
            ItemMeta meta = wand.getItemMeta();
            if (meta == null) {
                return "default";
            }

            // Use display name + material as identifier for now
            // In a more sophisticated implementation, you might use custom NBT tags
            String displayName = meta.hasDisplayName() && meta.displayName() != null
                    ? Objects.toString(meta.displayName(), "default")
                    : "default";
            String result = wand.getType().toString() + ":" + (displayName.isEmpty() ? "empty" : displayName).hashCode();

            // Cache the result with size limit to prevent memory leaks
            if (wandIdCache.size() < MAX_CACHE_SIZE) {
                wandIdCache.put(cacheKey, result);
            } else {
                // Cache is full, clear oldest entries (simple eviction strategy)
                if (wandIdCache.size() >= MAX_CACHE_SIZE * 1.2) { // Only clear when significantly over limit
                    wandIdCache.clear();
                    wandIdCache.put(cacheKey, result);
                }
            }
            return result;
        }
    }

    /**
     * Shuts down the cooldown service and clears all cooldown data. This method
     * should be called during plugin shutdown to prevent memory leaks.
     */
    public void shutdown() {
        try (var timing = performanceMonitor.startTiming("CooldownService.shutdown", 50)) {
            timing.observe();
            assert timing != null;
            cooldowns.clear();
            disabledCooldowns.clear();
            wandIdCache.clear();
        }
    }

    /**
     * Gets performance metrics for this service.
     *
     * @return A string containing performance metrics.
     */
    public String getPerformanceMetrics() {
        return "Metrics not available.";
    }

    /**
     * Clears performance metrics for this service.
     */
    public void clearPerformanceMetrics() {
        performanceMonitor.clearMetrics();
    }
}
