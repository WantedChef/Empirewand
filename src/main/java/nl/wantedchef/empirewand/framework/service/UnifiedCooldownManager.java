package nl.wantedchef.empirewand.framework.service;

import nl.wantedchef.empirewand.core.util.PerformanceMonitor;
import nl.wantedchef.empirewand.framework.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;

/**
 * Unified cooldown management system for both spells and commands.
 * 
 * <p>This service handles:
 * <ul>
 * <li>Spell cooldowns (tick-based)</li>
 * <li>Command cooldowns (millisecond-based)</li>
 * <li>Per-player and global cooldowns</li>
 * <li>Wand-specific cooldown disabling</li>
 * <li>Automatic cleanup of expired cooldowns</li>
 * <li>Performance monitoring and metrics</li>
 * </ul>
 * 
 * <p>Thread-safe implementation using concurrent collections for high-performance access.
 * Includes automatic cleanup to prevent memory leaks.
 * 
 * @since 2.0.0
 */
public class UnifiedCooldownManager {
    
    private static final long CLEANUP_INTERVAL_TICKS = 1200L; // 60 seconds
    private static final int MAX_CACHE_SIZE = 1000;
    
    // Core data structures
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>> playerSpellCooldowns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>> playerCommandCooldowns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> globalCommandCooldowns = new ConcurrentHashMap<>();

    // Expiry indexes (buckets) for O(1) cleanup
    private final ConcurrentSkipListMap<Long, ConcurrentLinkedQueue<Runnable>> spellExpiryIndex = new ConcurrentSkipListMap<>(); // key: tick expiry
    private final ConcurrentSkipListMap<Long, ConcurrentLinkedQueue<Runnable>> commandExpiryIndex = new ConcurrentSkipListMap<>(); // key: ms expiry
    private final ConcurrentSkipListMap<Long, ConcurrentLinkedQueue<Runnable>> globalCommandExpiryIndex = new ConcurrentSkipListMap<>(); // key: ms expiry

    // Metrics
    private volatile long lastCleanupDurationNanos = 0L;
    private volatile int spellBucketCount = 0;
    private volatile int commandBucketCount = 0;
    private volatile int globalBucketCount = 0;
    private final ConcurrentHashMap<String, Boolean> disabledCooldowns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> wandIdCache = new ConcurrentHashMap<>();
    
    // Dependencies
    private final Logger logger;
    private final PerformanceMonitor performanceMonitor;
    private final BukkitRunnable cleanupTask;
    
    /**
     * Creates a new unified cooldown manager.
     * 
     * @param plugin the plugin instance
     */
    public UnifiedCooldownManager(@NotNull Plugin plugin) {
        this.logger = plugin.getLogger();
        this.performanceMonitor = new PerformanceMonitor(logger);
        
        // Start periodic cleanup task
        this.cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredCooldowns();
            }
        };
        try {
            if (plugin.getServer() != null) {
                cleanupTask.runTaskTimer(plugin, CLEANUP_INTERVAL_TICKS, CLEANUP_INTERVAL_TICKS);
            } else {
                logger.fine("UnifiedCooldownManager cleanup scheduler not started (no server in test context)");
            }
        } catch (Throwable ignored) {
            logger.fine("UnifiedCooldownManager cleanup scheduler not started due to environment");
        }
        
        logger.info("UnifiedCooldownManager initialized with automatic cleanup");
    }
    
    // ============================================
    // SPELL COOLDOWN METHODS (tick-based)
    // ============================================
    
    /**
     * Checks if a player is on cooldown for a specific spell.
     * 
     * @param playerId the player UUID
     * @param spellKey the spell key
     * @param nowTicks the current server tick
     * @return true if on cooldown
     */
    public boolean isSpellOnCooldown(@NotNull UUID playerId, @NotNull String spellKey, long nowTicks) {
        // Only measure performance occasionally to reduce overhead
        var timing = (nowTicks % 20 == 0) ? performanceMonitor.startTiming("UnifiedCooldownManager.isSpellOnCooldown", 5) : null;
        try {
            
            if (playerId == null || spellKey == null) {
                return false;
            }
            
            var playerMap = playerSpellCooldowns.get(playerId);
            if (playerMap == null) {
                return false;
            }
            
            long cooldownEnd = playerMap.getOrDefault(spellKey, 0L);
            return nowTicks < cooldownEnd;
        } finally {
            if (timing != null) {
                timing.observe();
            }
        }
    }
    
    /**
     * Checks if a player is on cooldown for a specific spell with wand consideration.
     * 
     * @param playerId the player UUID
     * @param spellKey the spell key
     * @param nowTicks the current server tick
     * @param wand the wand being used
     * @return true if on cooldown
     */
    public boolean isSpellOnCooldown(@NotNull UUID playerId, @NotNull String spellKey, 
                                   long nowTicks, @Nullable ItemStack wand) {
        try (var timing = performanceMonitor.startTiming("UnifiedCooldownManager.isSpellOnCooldownWithWand", 5)) {
            timing.observe();
            
            if (playerId == null || spellKey == null) {
                return false;
            }
            
            // Check if cooldowns are disabled for this wand
            if (wand != null && isCooldownDisabled(playerId, wand)) {
                return false;
            }
            
            return isSpellOnCooldown(playerId, spellKey, nowTicks);
        }
    }
    
    /**
     * Gets remaining spell cooldown time.
     * 
     * @param playerId the player UUID
     * @param spellKey the spell key
     * @param nowTicks the current server tick
     * @return remaining ticks, 0 if not on cooldown
     */
    public long getSpellCooldownRemaining(@NotNull UUID playerId, @NotNull String spellKey, long nowTicks) {
        try (var timing = performanceMonitor.startTiming("UnifiedCooldownManager.getSpellCooldownRemaining", 5)) {
            timing.observe();
            
            if (playerId == null || spellKey == null) {
                return 0L;
            }
            
            var playerMap = playerSpellCooldowns.get(playerId);
            if (playerMap == null) {
                return 0L;
            }
            
            long cooldownEnd = playerMap.getOrDefault(spellKey, 0L);
            return Math.max(0L, cooldownEnd - nowTicks);
        }
    }
    
    /**
     * Gets remaining spell cooldown time with wand consideration.
     * 
     * @param playerId the player UUID
     * @param spellKey the spell key
     * @param nowTicks the current server tick
     * @param wand the wand being used
     * @return remaining ticks, 0 if not on cooldown or disabled
     */
    public long getSpellCooldownRemaining(@NotNull UUID playerId, @NotNull String spellKey, 
                                        long nowTicks, @Nullable ItemStack wand) {
        try (var timing = performanceMonitor.startTiming("UnifiedCooldownManager.getSpellCooldownRemainingWithWand", 5)) {
            timing.observe();
            
            if (playerId == null || spellKey == null) {
                return 0L;
            }
            
            // Check if cooldowns are disabled for this wand
            if (wand != null && isCooldownDisabled(playerId, wand)) {
                return 0L;
            }
            
            return getSpellCooldownRemaining(playerId, spellKey, nowTicks);
        }
    }
    
    /**
     * Sets a spell cooldown.
     * 
     * @param playerId the player UUID
     * @param spellKey the spell key
     * @param untilTicks the tick when cooldown ends
     */
    public void setSpellCooldown(@NotNull UUID playerId, @NotNull String spellKey, long untilTicks) {
        try (var timing = performanceMonitor.startTiming("UnifiedCooldownManager.setSpellCooldown", 5)) {
            timing.observe();
            
            if (playerId == null || spellKey == null || untilTicks < 0) {
                return;
            }
            
            var map = playerSpellCooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>(4));
            map.put(spellKey, untilTicks);

            // Index this expiry for O(1) cleanup
            spellExpiryIndex
                .computeIfAbsent(untilTicks, k -> new ConcurrentLinkedQueue<>())
                .add(() -> {
                    var playerMap = playerSpellCooldowns.get(playerId);
                    if (playerMap != null) {
                        Long current = playerMap.get(spellKey);
                        if (current != null && current == untilTicks) {
                            playerMap.remove(spellKey);
                            if (playerMap.isEmpty()) {
                                playerSpellCooldowns.remove(playerId);
                            }
                        }
                    }
                });
        }
    }
    
    // ============================================
    // COMMAND COOLDOWN METHODS (millisecond-based)
    // ============================================
    
    /**
     * Checks if a command is on cooldown and throws exception if it is.
     * 
     * @param sender the command sender
     * @param commandName the command name
     * @param cooldownSeconds the cooldown duration in seconds
     * @throws CommandException if the command is on cooldown
     */
    public void checkCommandCooldown(@NotNull CommandSender sender, @NotNull String commandName,
                                   int cooldownSeconds) throws CommandException {
        checkCommandCooldown(sender, commandName, commandName, cooldownSeconds);
    }
    
    /**
     * Checks if a command is on cooldown with custom key and throws exception if it is.
     * 
     * @param sender the command sender
     * @param commandName the command name (for display)
     * @param cooldownKey the cooldown key (for differentiation)
     * @param cooldownSeconds the cooldown duration in seconds
     * @throws CommandException if the command is on cooldown
     */
    public void checkCommandCooldown(@NotNull CommandSender sender, @NotNull String commandName,
                                   @NotNull String cooldownKey, int cooldownSeconds) throws CommandException {
        try (var timing = performanceMonitor.startTiming("UnifiedCooldownManager.checkCommandCooldown", 5)) {
            timing.observe();
            
            if (cooldownSeconds <= 0) {
                return; // No cooldown
            }
            
            long now = System.currentTimeMillis();
            long cooldownMs = cooldownSeconds * 1000L;
            String fullCooldownKey = (commandName + "." + cooldownKey).toLowerCase();
            
            if (sender instanceof Player player) {
                UUID playerId = player.getUniqueId();
                var playerCooldownsMap = playerCommandCooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
                
                Long cooldownEnd = playerCooldownsMap.get(fullCooldownKey);
                if (cooldownEnd != null && now < cooldownEnd) {
                    long remainingSeconds = (cooldownEnd - now) / 1000;
                    throw new CommandException(
                            String.format("This command is on cooldown. Please wait %d second%s.",
                                    remainingSeconds, remainingSeconds != 1 ? "s" : ""),
                            "COMMAND_COOLDOWN", commandName, remainingSeconds);
                }
                
                // Set new cooldown
                long untilMs = now + cooldownMs;
                playerCooldownsMap.put(fullCooldownKey, untilMs);

                // Index for O(1) cleanup
                commandExpiryIndex
                    .computeIfAbsent(untilMs, k -> new ConcurrentLinkedQueue<>())
                    .add(() -> {
                        var map = playerCommandCooldowns.get(playerId);
                        if (map != null) {
                            Long current = map.get(fullCooldownKey);
                            if (current != null && current == untilMs) {
                                map.remove(fullCooldownKey);
                                if (map.isEmpty()) {
                                    playerCommandCooldowns.remove(playerId);
                                }
                            }
                        }
                    });
            } else {
                // Global cooldown for console commands
                Long cooldownEnd = globalCommandCooldowns.get(fullCooldownKey);
                if (cooldownEnd != null && now < cooldownEnd) {
                    long remainingSeconds = (cooldownEnd - now) / 1000;
                    throw new CommandException(
                            String.format("This command is on cooldown. Please wait %d second%s.",
                                    remainingSeconds, remainingSeconds != 1 ? "s" : ""),
                            "COMMAND_COOLDOWN", commandName, remainingSeconds);
                }
                
                // Set new cooldown
                long untilMs = now + cooldownMs;
                globalCommandCooldowns.put(fullCooldownKey, untilMs);

                // Index for O(1) cleanup
                globalCommandExpiryIndex
                    .computeIfAbsent(untilMs, k -> new ConcurrentLinkedQueue<>())
                    .add(() -> {
                        Long current = globalCommandCooldowns.get(fullCooldownKey);
                        if (current != null && current == untilMs) {
                            globalCommandCooldowns.remove(fullCooldownKey);
                        }
                    });
            }
        }
    }
    
    /**
     * Gets remaining command cooldown time in seconds.
     * 
     * @param sender the command sender
     * @param commandName the command name
     * @return remaining seconds, 0 if not on cooldown
     */
    public long getCommandCooldownRemaining(@NotNull CommandSender sender, @NotNull String commandName) {
        return getCommandCooldownRemaining(sender, commandName, commandName);
    }
    
    /**
     * Gets remaining command cooldown time in seconds with custom key.
     * 
     * @param sender the command sender
     * @param commandName the command name
     * @param cooldownKey the cooldown key
     * @return remaining seconds, 0 if not on cooldown
     */
    public long getCommandCooldownRemaining(@NotNull CommandSender sender, @NotNull String commandName,
                                          @NotNull String cooldownKey) {
        try (var timing = performanceMonitor.startTiming("UnifiedCooldownManager.getCommandCooldownRemaining", 5)) {
            timing.observe();
            
            long now = System.currentTimeMillis();
            String fullCooldownKey = (commandName + "." + cooldownKey).toLowerCase();
            
            if (sender instanceof Player player) {
                UUID playerId = player.getUniqueId();
                var playerCooldownsMap = playerCommandCooldowns.get(playerId);
                if (playerCooldownsMap == null) {
                    return 0L;
                }
                
                Long cooldownEnd = playerCooldownsMap.get(fullCooldownKey);
                if (cooldownEnd == null || now >= cooldownEnd) {
                    return 0L;
                }
                
                return (cooldownEnd - now) / 1000;
            } else {
                Long cooldownEnd = globalCommandCooldowns.get(fullCooldownKey);
                if (cooldownEnd == null || now >= cooldownEnd) {
                    return 0L;
                }
                
                return (cooldownEnd - now) / 1000;
            }
        }
    }
    
    // ============================================
    // MANAGEMENT METHODS
    // ============================================
    
    /**
     * Clears all cooldowns for a player.
     * 
     * @param playerId the player UUID
     */
    public void clearPlayerCooldowns(@NotNull UUID playerId) {
        try (var timing = performanceMonitor.startTiming("UnifiedCooldownManager.clearPlayerCooldowns", 5)) {
            timing.observe();
            
            if (playerId == null) {
                return;
            }
            
            playerSpellCooldowns.remove(playerId);
            playerCommandCooldowns.remove(playerId);
            
            // Also remove any cooldown disables for this player
            disabledCooldowns.entrySet().removeIf(entry -> entry.getKey().startsWith(playerId.toString()));
        }
    }
    
    /**
     * Clears a specific spell cooldown for a player.
     * 
     * @param playerId the player UUID
     * @param spellKey the spell key
     */
    public void clearPlayerSpellCooldown(@NotNull UUID playerId, @NotNull String spellKey) {
        if (playerId == null || spellKey == null) {
            return;
        }
        
        var playerMap = playerSpellCooldowns.get(playerId);
        if (playerMap != null) {
            playerMap.remove(spellKey);
            if (playerMap.isEmpty()) {
                playerSpellCooldowns.remove(playerId);
            }
        }
    }
    
    /**
     * Clears a specific command cooldown for a player.
     * 
     * @param playerId the player UUID
     * @param commandName the command name
     */
    public void clearPlayerCommandCooldown(@NotNull UUID playerId, @NotNull String commandName) {
        clearPlayerCommandCooldown(playerId, commandName, commandName);
    }
    
    /**
     * Clears a specific command cooldown for a player with custom key.
     * 
     * @param playerId the player UUID
     * @param commandName the command name
     * @param cooldownKey the cooldown key
     */
    public void clearPlayerCommandCooldown(@NotNull UUID playerId, @NotNull String commandName,
                                         @NotNull String cooldownKey) {
        if (playerId == null || commandName == null || cooldownKey == null) {
            return;
        }
        
        String fullCooldownKey = (commandName + "." + cooldownKey).toLowerCase();
        var playerMap = playerCommandCooldowns.get(playerId);
        if (playerMap != null) {
            playerMap.remove(fullCooldownKey);
            if (playerMap.isEmpty()) {
                playerCommandCooldowns.remove(playerId);
            }
        }
    }
    
    // ============================================
    // WAND COOLDOWN DISABLE METHODS
    // ============================================
    
    /**
     * Sets whether cooldowns are disabled for a specific player and wand.
     * 
     * @param playerId the player UUID
     * @param wand the wand item
     * @param disabled true to disable cooldowns, false to enable them
     */
    public void setCooldownDisabled(@NotNull UUID playerId, @NotNull ItemStack wand, boolean disabled) {
        try (var timing = performanceMonitor.startTiming("UnifiedCooldownManager.setCooldownDisabled", 5)) {
            timing.observe();
            
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
    }
    
    /**
     * Checks if cooldowns are disabled for a specific player and wand.
     * 
     * @param playerId the player UUID
     * @param wand the wand item
     * @return true if cooldowns are disabled, false otherwise
     */
    public boolean isCooldownDisabled(@NotNull UUID playerId, @NotNull ItemStack wand) {
        try (var timing = performanceMonitor.startTiming("UnifiedCooldownManager.isCooldownDisabled", 5)) {
            timing.observe();
            
            if (playerId == null || wand == null) {
                return false;
            }
            
            String wandId = getWandIdentifier(wand);
            String disableKey = playerId.toString() + ":" + wandId;
            return disabledCooldowns.getOrDefault(disableKey, false);
        }
    }
    
    /**
     * Generates a unique identifier for a wand based on its metadata.
     * 
     * @param wand the wand item
     * @return a string identifier for the wand
     */
    private String getWandIdentifier(@NotNull ItemStack wand) {
        if (wand == null) {
            return "unknown";
        }
        
        // Check cache first
        String cacheKey = wand.toString();
        String cachedId = wandIdCache.get(cacheKey);
        if (cachedId != null) {
            return cachedId;
        }
        
        // Generate new identifier
        ItemMeta meta = wand.getItemMeta();
        if (meta == null) {
            return "default";
        }
        
        String displayName = meta.hasDisplayName() && meta.displayName() != null
                ? Objects.toString(meta.displayName(), "default")
                : "default";
        String wandId = wand.getType().toString() + ":" + (displayName.isEmpty() ? "empty" : displayName).hashCode();
        
        // Cache with size limit
        if (wandIdCache.size() < MAX_CACHE_SIZE) {
            wandIdCache.put(cacheKey, wandId);
        }
        
        return wandId;
    }
    
    // ============================================
    // METRICS AND MAINTENANCE
    // ============================================
    
    /**
     * Gets performance metrics for monitoring the state of the cooldown manager.
     * 
     * @return a snapshot of the current cooldown metrics
     */
    public CooldownMetrics getMetrics() {
        int spellPlayers = playerSpellCooldowns.size();
        int commandPlayers = playerCommandCooldowns.size();
        int totalSpellCooldowns = playerSpellCooldowns.values().stream()
                .mapToInt(ConcurrentHashMap::size)
                .sum();
        int totalCommandCooldowns = playerCommandCooldowns.values().stream()
                .mapToInt(ConcurrentHashMap::size)
                .sum();
        int globalCommandCooldowns = this.globalCommandCooldowns.size();
        int disabledCooldowns = this.disabledCooldowns.size();
        int cacheSize = wandIdCache.size();
        
        return new CooldownMetrics(spellPlayers, commandPlayers, totalSpellCooldowns, 
                totalCommandCooldowns, globalCommandCooldowns, disabledCooldowns, cacheSize);
    }
    
    /**
     * Periodically cleans up expired cooldowns to prevent memory leaks.
     */
    private void cleanupExpiredCooldowns() {
        // Only measure cleanup performance occasionally to reduce overhead
        var timing = (System.currentTimeMillis() % 10000 == 0) ? // Every 10 seconds
            performanceMonitor.startTiming("UnifiedCooldownManager.cleanup", 50) : null;

        long start = System.nanoTime();
        try {
            long currentTicks = System.currentTimeMillis() / 50;
            long currentMs = System.currentTimeMillis();

            // Drain spell expiry buckets up to current tick
            var expiredSpellBuckets = spellExpiryIndex.headMap(currentTicks, true);
            if (!expiredSpellBuckets.isEmpty()) {
                for (var entry : expiredSpellBuckets.entrySet()) {
                    ConcurrentLinkedQueue<Runnable> queue = entry.getValue();
                    Runnable r;
                    while (queue != null && (r = queue.poll()) != null) {
                        try { r.run(); } catch (Throwable ignored) {}
                    }
                }
                expiredSpellBuckets.clear();
            }
            spellBucketCount = spellExpiryIndex.size();

            // Drain player command expiry buckets up to now
            var expiredCommandBuckets = commandExpiryIndex.headMap(currentMs, true);
            if (!expiredCommandBuckets.isEmpty()) {
                for (var entry : expiredCommandBuckets.entrySet()) {
                    ConcurrentLinkedQueue<Runnable> queue = entry.getValue();
                    Runnable r;
                    while (queue != null && (r = queue.poll()) != null) {
                        try { r.run(); } catch (Throwable ignored) {}
                    }
                }
                expiredCommandBuckets.clear();
            }
            commandBucketCount = commandExpiryIndex.size();

            // Drain global command expiry buckets up to now
            var expiredGlobalBuckets = globalCommandExpiryIndex.headMap(currentMs, true);
            if (!expiredGlobalBuckets.isEmpty()) {
                for (var entry : expiredGlobalBuckets.entrySet()) {
                    ConcurrentLinkedQueue<Runnable> queue = entry.getValue();
                    Runnable r;
                    while (queue != null && (r = queue.poll()) != null) {
                        try { r.run(); } catch (Throwable ignored) {}
                    }
                }
                expiredGlobalBuckets.clear();
            }
            globalBucketCount = globalCommandExpiryIndex.size();

            // Clean up caches if they grow too large
            if (wandIdCache.size() > MAX_CACHE_SIZE) {
                wandIdCache.clear();
                logger.fine("Cleared wand identifier cache");
            }
            if (disabledCooldowns.size() > 1000) {
                disabledCooldowns.clear();
                logger.fine("Cleared disabled cooldowns cache");
            }

            if (timing != null) {
                CooldownMetrics metrics = getMetrics();
                logger.fine(String.format("Cooldown cleanup - Spell players: %d (%d cooldowns), " +
                        "Command players: %d (%d cooldowns), Global commands: %d, Disabled: %d, Cache: %d",
                        metrics.spellPlayers(), metrics.totalSpellCooldowns(),
                        metrics.commandPlayers(), metrics.totalCommandCooldowns(),
                        metrics.globalCommandCooldowns(), metrics.disabledCooldowns(), metrics.cacheSize()));
            }
        } finally {
            lastCleanupDurationNanos = System.nanoTime() - start;
            if (timing != null) {
                timing.observe();
            }
        }
    }
    
    /**
     * Shuts down the cooldown manager, stopping the cleanup task and clearing all data.
     */
    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        
        playerSpellCooldowns.clear();
        playerCommandCooldowns.clear();
        globalCommandCooldowns.clear();
        disabledCooldowns.clear();
        wandIdCache.clear();
        
        logger.info("UnifiedCooldownManager shut down");
    }
    
    /**
     * A data class holding a snapshot of cooldown manager metrics.
     * 
     * @param spellPlayers number of players with spell cooldowns
     * @param commandPlayers number of players with command cooldowns
     * @param totalSpellCooldowns total number of spell cooldowns
     * @param totalCommandCooldowns total number of command cooldowns
     * @param globalCommandCooldowns number of global command cooldowns
     * @param disabledCooldowns number of disabled cooldown entries
     * @param cacheSize size of the wand identifier cache
     */
    public record CooldownMetrics(
            int spellPlayers,
            int commandPlayers,
            int totalSpellCooldowns,
            int totalCommandCooldowns,
            int globalCommandCooldowns,
            int disabledCooldowns,
            int cacheSize) {
        public long lastCleanupDurationNanos(@NotNull UnifiedCooldownManager m) {
            return m.lastCleanupDurationNanos;
        }
        public int spellBucketCount(@NotNull UnifiedCooldownManager m) { return m.spellBucketCount; }
        public int commandBucketCount(@NotNull UnifiedCooldownManager m) { return m.commandBucketCount; }
        public int globalBucketCount(@NotNull UnifiedCooldownManager m) { return m.globalBucketCount; }
    }

    // Direct metric accessors
    public long getLastCleanupDurationNanos() { return lastCleanupDurationNanos; }
    public int getSpellBucketCount() { return spellBucketCount; }
    public int getCommandBucketCount() { return commandBucketCount; }
    public int getGlobalBucketCount() { return globalBucketCount; }
}
