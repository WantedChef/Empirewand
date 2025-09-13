package nl.wantedchef.empirewand.framework.service;

import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * A memory-optimized and high-performance service for managing spell cooldowns.
 * <p>
 * This implementation uses concurrent collections to ensure thread safety and improve access speed.
 * It features a periodic cleanup task to remove expired cooldowns, preventing long-term memory leaks.
 */
public class OptimizedCooldownService {
    private static final long CLEANUP_INTERVAL_TICKS = 1200L; // 60 seconds

    private final Logger logger;
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>> playerCooldowns = new ConcurrentHashMap<>();
    private final Map<String, Boolean> disabledCooldowns = new ConcurrentHashMap<>();
    private final BukkitRunnable cleanupTask;

    /**
     * Constructs a new OptimizedCooldownService.
     *
     * @param plugin The plugin instance.
     */
    public OptimizedCooldownService(Plugin plugin) {
        this.logger = plugin.getLogger();

        // Start periodic cleanup task
        this.cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredCooldowns();
            }
        };
        cleanupTask.runTaskTimer(plugin, CLEANUP_INTERVAL_TICKS, CLEANUP_INTERVAL_TICKS);

        logger.info("OptimizedCooldownService initialized with concurrent collections");
    }

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

        ConcurrentHashMap<String, Long> playerMap = playerCooldowns.get(playerId);
        if (playerMap == null) {
            return false;
        }

        long cooldownEnd = playerMap.getOrDefault(key, 0L);
        return nowTicks < cooldownEnd;
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

        if (isCooldownDisabled(playerId, wand)) {
            return false;
        }

        return isOnCooldown(playerId, key, nowTicks);
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

        ConcurrentHashMap<String, Long> playerMap = playerCooldowns.get(playerId);
        if (playerMap == null) {
            return 0L;
        }

        long cooldownEnd = playerMap.getOrDefault(key, 0L);
        return Math.max(0L, cooldownEnd - nowTicks);
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

        if (isCooldownDisabled(playerId, wand)) {
            return 0L;
        }

        return remaining(playerId, key, nowTicks);
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

        ConcurrentHashMap<String, Long> playerMap = playerCooldowns.computeIfAbsent(playerId,
                k -> new ConcurrentHashMap<>());
        playerMap.put(key, untilTicks);
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

        playerCooldowns.remove(playerId);
        // Also remove any cooldown disables for this player
        disabledCooldowns.entrySet().removeIf(entry -> entry.getKey().startsWith(playerId.toString()));
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
     *
     * @param playerId The UUID of the player.
     * @param wand     The wand item.
     * @return true if cooldowns are disabled, false otherwise.
     */
    public boolean isCooldownDisabled(UUID playerId, ItemStack wand) {
        if (playerId == null || wand == null) {
            return false;
        }

        String wandId = getWandIdentifier(wand);
        String disableKey = playerId.toString() + ":" + wandId;
        return disabledCooldowns.getOrDefault(disableKey, false);
    }

    /**
     * Generates a unique identifier for a wand based on its metadata.
     *
     * @param wand The wand item.
     * @return A string identifier for the wand.
     */
    private String getWandIdentifier(ItemStack wand) {
        if (wand == null) {
            return "unknown";
        }

        ItemMeta meta = wand.getItemMeta();
        if (meta == null) {
            return "default";
        }

        // Use display name + material as identifier for now
        String displayName = meta.hasDisplayName() && meta.displayName() != null
                ? Objects.toString(meta.displayName(), "default")
                : "default";
        return wand.getType().toString() + ":" + (displayName.isEmpty() ? "empty" : displayName).hashCode();
    }

    /**
     * Gets performance metrics for monitoring the state of the cooldown service.
     *
     * @return A snapshot of the current cooldown metrics.
     */
    public CooldownMetrics getMetrics() {
        int totalPlayers = playerCooldowns.size();
        int totalCooldowns = playerCooldowns.values().stream()
                .mapToInt(ConcurrentHashMap::size)
                .sum();

        return new CooldownMetrics(totalPlayers, totalCooldowns, disabledCooldowns.size());
    }

    /**
     * Periodically cleans up expired cooldowns to prevent memory leaks.
     */
    private void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis() / 50; // Convert to ticks

        // Remove expired cooldowns
        playerCooldowns.values().forEach(map -> {
            map.entrySet().removeIf(entry -> currentTime >= entry.getValue());
        });

        // Remove empty player maps
        playerCooldowns.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        // Clean up old disabled cooldowns (simple periodic cleanup)
        if (disabledCooldowns.size() > 1000) {
            disabledCooldowns.clear();
            logger.info("Cleaned up disabled cooldowns cache");
        }

        CooldownMetrics metrics = getMetrics();
        logger.fine(String.format("Cooldown cleanup - Players: %d, Cooldowns: %d, Disabled: %d",
                metrics.totalPlayers(), metrics.totalCooldowns(), metrics.disabledCooldowns()));
    }

    /**
     * Shuts down the cooldown service, stopping the cleanup task and clearing all data.
     */
    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        playerCooldowns.clear();
        disabledCooldowns.clear();

        logger.info("OptimizedCooldownService shut down");
    }

    /**
     * A data class holding a snapshot of cooldown service metrics.
     *
     * @param totalPlayers      The total number of players with active cooldowns.
     * @param totalCooldowns    The total number of active cooldowns across all players.
     * @param disabledCooldowns The number of player-wand combinations with disabled cooldowns.
     */
    public record CooldownMetrics(
            int totalPlayers,
            int totalCooldowns,
            int disabledCooldowns) {
    }
}





