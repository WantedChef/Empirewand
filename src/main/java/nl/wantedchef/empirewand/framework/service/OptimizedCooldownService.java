package nl.wantedchef.empirewand.framework.service;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * A memory-optimized and high-performance service for managing spell cooldowns.
 * <p>
 * This implementation uses `fastutil` collections to reduce memory overhead and improve access speed.
 * It features a periodic cleanup task to remove expired cooldowns, preventing long-term memory leaks.
 */
public class OptimizedCooldownService {
    private static final long CLEANUP_INTERVAL_TICKS = 1200L; // 60 seconds

    private final Logger logger;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Object2ObjectOpenHashMap<UUID, Long2LongOpenHashMap> playerCooldowns = new Object2ObjectOpenHashMap<>();
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

        logger.info("OptimizedCooldownService initialized with fastutil collections");
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

        lock.readLock().lock();
        try {
            Long2LongOpenHashMap playerMap = playerCooldowns.get(playerId);
            if (playerMap == null)
                return false;

            long cooldownEnd = playerMap.getOrDefault(key.hashCode(), 0L);
            return nowTicks < cooldownEnd;
        } finally {
            lock.readLock().unlock();
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

        lock.readLock().lock();
        try {
            Long2LongOpenHashMap playerMap = playerCooldowns.get(playerId);
            if (playerMap == null)
                return 0L;

            long cooldownEnd = playerMap.getOrDefault(key.hashCode(), 0L);
            return Math.max(0L, cooldownEnd - nowTicks);
        } finally {
            lock.readLock().unlock();
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

        lock.writeLock().lock();
        try {
            Long2LongOpenHashMap playerMap = playerCooldowns.computeIfAbsent(playerId,
                    k -> new Long2LongOpenHashMap());
            playerMap.put(key.hashCode(), untilTicks);
        } finally {
            lock.writeLock().unlock();
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

        lock.writeLock().lock();
        try {
            playerCooldowns.remove(playerId);
            // Also remove any cooldown disables for this player
            disabledCooldowns.entrySet().removeIf(entry -> entry.getKey().startsWith(playerId.toString()));
        } finally {
            lock.writeLock().unlock();
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
        lock.readLock().lock();
        try {
            int totalPlayers = playerCooldowns.size();
            int totalCooldowns = playerCooldowns.values().stream()
                    .mapToInt(Long2LongOpenHashMap::size)
                    .sum();

            return new CooldownMetrics(totalPlayers, totalCooldowns, disabledCooldowns.size());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Periodically cleans up expired cooldowns to prevent memory leaks.
     */
    private void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis() / 50; // Convert to ticks

        lock.writeLock().lock();
        try {
            // Remove expired cooldowns
            playerCooldowns.values().forEach(map -> {
                map.long2LongEntrySet().removeIf(entry -> currentTime >= entry.getLongValue());
            });

            // Remove empty player maps
            playerCooldowns.entrySet().removeIf(entry -> entry.getValue().isEmpty());

            // Clean up old disabled cooldowns (simple periodic cleanup)
            if (disabledCooldowns.size() > 1000) {
                disabledCooldowns.clear();
                logger.info("Cleaned up disabled cooldowns cache");
            }

        } finally {
            lock.writeLock().unlock();
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

        lock.writeLock().lock();
        try {
            playerCooldowns.clear();
            disabledCooldowns.clear();
        } finally {
            lock.writeLock().unlock();
        }

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





