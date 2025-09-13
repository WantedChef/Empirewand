package nl.wantedchef.empirewand.framework.command.util;


import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * High-performance caching system for command operations including permission checks,
 * tab completion results, and frequent lookups. Uses memory-efficient data structures
 * and LRU eviction policies.
 */
public class CommandCache {
    
    // Cache for permission checks (player name -> permission -> result)
    private final Map<String, Map<String, Boolean>> permissionCache = new ConcurrentHashMap<>();
    
    // Cache for tab completion results (input hash -> results)
    private final Map<Integer, List<String>> tabCompletionCache = new ConcurrentHashMap<>();
    
    // Cache for spell name lookups and filtering
    private final Map<String, List<String>> spellFilterCache = new ConcurrentHashMap<>();
    
    // Cache for formatted command help
    private final Map<String, String> helpCache = new ConcurrentHashMap<>();
    
    // LRU tracking for eviction
    private final Queue<String> permissionLRU = new ConcurrentLinkedQueue<>();
    private final Queue<Integer> tabCompletionLRU = new ConcurrentLinkedQueue<>();
    
    // Cache size limits
    private static final int MAX_PERMISSION_CACHE_SIZE = 1000;
    private static final int MAX_TAB_COMPLETION_CACHE_SIZE = 500;
    private static final int MAX_SPELL_FILTER_CACHE_SIZE = 100;
    
    // Cache hit/miss statistics
    private long permissionCacheHits = 0;
    private long permissionCacheMisses = 0;
    private long tabCompletionCacheHits = 0;
    private long tabCompletionCacheMisses = 0;
    
    /**
     * Checks if a permission result is cached for the given sender and permission.
     * 
     * @param sender The command sender
     * @param permission The permission to check
     * @return Cached permission result or null if not cached
     */
    @Nullable
    public Boolean getCachedPermission(@NotNull CommandSender sender, @NotNull String permission) {
        String senderKey = sender.getName();
        Map<String, Boolean> senderPermissions = permissionCache.get(senderKey);
        
        if (senderPermissions != null) {
            Boolean result = senderPermissions.get(permission);
            if (result != null) {
                permissionCacheHits++;
                // Move to end of LRU queue
                permissionLRU.remove(senderKey + ":" + permission);
                permissionLRU.offer(senderKey + ":" + permission);
                return result;
            }
        }
        
        permissionCacheMisses++;
        return null;
    }
    
    /**
     * Caches a permission check result.
     * 
     * @param sender The command sender
     * @param permission The permission that was checked
     * @param result The permission check result
     */
    public void cachePermission(@NotNull CommandSender sender, @NotNull String permission, boolean result) {
        String senderKey = sender.getName();
        String cacheKey = senderKey + ":" + permission;
        
        permissionCache.computeIfAbsent(senderKey, k -> new ConcurrentHashMap<>()).put(permission, result);
        permissionLRU.offer(cacheKey);
        
        // Evict old entries if cache is too large
        if (permissionLRU.size() > MAX_PERMISSION_CACHE_SIZE) {
            evictOldestPermission();
        }
    }
    
    /**
     * Gets cached tab completion results for the given input.
     * 
     * @param input The input string for tab completion
     * @param args The command arguments for context
     * @return Cached completion results or null if not cached
     */
    @Nullable
    public List<String> getCachedTabCompletion(@NotNull String input, @NotNull String[] args) {
        int cacheKey = Objects.hash(input.toLowerCase(), Arrays.toString(args));
        List<String> result = tabCompletionCache.get(cacheKey);
        
        if (result != null) {
            tabCompletionCacheHits++;
            // Move to end of LRU queue
            tabCompletionLRU.remove(cacheKey);
            tabCompletionLRU.offer(cacheKey);
            return new ArrayList<>(result); // Return defensive copy
        }
        
        tabCompletionCacheMisses++;
        return null;
    }
    
    /**
     * Caches tab completion results.
     * 
     * @param input The input string for tab completion
     * @param args The command arguments for context
     * @param results The completion results to cache
     */
    public void cacheTabCompletion(@NotNull String input, @NotNull String[] args, @NotNull List<String> results) {
        int cacheKey = Objects.hash(input.toLowerCase(), Arrays.toString(args));
        tabCompletionCache.put(cacheKey, new ArrayList<>(results)); // Store defensive copy
        tabCompletionLRU.offer(cacheKey);
        
        // Evict old entries if cache is too large
        if (tabCompletionLRU.size() > MAX_TAB_COMPLETION_CACHE_SIZE) {
            evictOldestTabCompletion();
        }
    }
    
    /**
     * Gets cached spell filter results.
     * 
     * @param filterKey The filter criteria key
     * @return Cached spell list or null if not cached
     */
    @Nullable
    public List<String> getCachedSpellFilter(@NotNull String filterKey) {
        List<String> result = spellFilterCache.get(filterKey.toLowerCase());
        return result != null ? new ArrayList<>(result) : null;
    }
    
    /**
     * Caches spell filter results.
     * 
     * @param filterKey The filter criteria key
     * @param results The filtered spell list
     */
    public void cacheSpellFilter(@NotNull String filterKey, @NotNull List<String> results) {
        spellFilterCache.put(filterKey.toLowerCase(), new ArrayList<>(results));
        
        // Simple eviction for spell filter cache
        if (spellFilterCache.size() > MAX_SPELL_FILTER_CACHE_SIZE) {
            String firstKey = spellFilterCache.keySet().iterator().next();
            spellFilterCache.remove(firstKey);
        }
    }
    
    /**
     * Gets cached help text for a command.
     * 
     * @param commandKey The command identifier
     * @return Cached help text or null if not cached
     */
    @Nullable
    public String getCachedHelp(@NotNull String commandKey) {
        return helpCache.get(commandKey.toLowerCase());
    }
    
    /**
     * Caches help text for a command.
     * 
     * @param commandKey The command identifier
     * @param helpText The help text to cache
     */
    public void cacheHelp(@NotNull String commandKey, @NotNull String helpText) {
        helpCache.put(commandKey.toLowerCase(), helpText);
        
        // Keep help cache reasonable size
        if (helpCache.size() > 50) {
            String firstKey = helpCache.keySet().iterator().next();
            helpCache.remove(firstKey);
        }
    }
    
    /**
     * Invalidates all cached permissions for a specific sender.
     * Useful when permissions are reloaded or changed.
     * 
     * @param sender The command sender
     */
    public void invalidatePermissions(@NotNull CommandSender sender) {
        String senderKey = sender.getName();
        permissionCache.remove(senderKey);
        
        // Remove from LRU queue
        permissionLRU.removeIf(key -> key.startsWith(senderKey + ":"));
    }
    
    /**
     * Invalidates all caches. Useful for plugin reloads.
     */
    public void invalidateAll() {
        permissionCache.clear();
        tabCompletionCache.clear();
        spellFilterCache.clear();
        helpCache.clear();
        permissionLRU.clear();
        tabCompletionLRU.clear();
    }
    
    /**
     * Gets cache statistics for monitoring and debugging.
     * 
     * @return Cache statistics information
     */
    @NotNull
    public CacheStats getStats() {
        long totalPermissionRequests = permissionCacheHits + permissionCacheMisses;
        long totalTabCompletionRequests = tabCompletionCacheHits + tabCompletionCacheMisses;
        
        double permissionHitRate = totalPermissionRequests > 0 ? 
            (double) permissionCacheHits / totalPermissionRequests : 0.0;
        double tabCompletionHitRate = totalTabCompletionRequests > 0 ? 
            (double) tabCompletionCacheHits / totalTabCompletionRequests : 0.0;
        
        return new CacheStats(
            permissionCache.size(),
            tabCompletionCache.size(),
            spellFilterCache.size(),
            helpCache.size(),
            permissionHitRate,
            tabCompletionHitRate,
            permissionCacheHits,
            permissionCacheMisses,
            tabCompletionCacheHits,
            tabCompletionCacheMisses
        );
    }
    
    private void evictOldestPermission() {
        String oldestKey = permissionLRU.poll();
        if (oldestKey != null) {
            String[] parts = oldestKey.split(":", 2);
            if (parts.length == 2) {
                Map<String, Boolean> senderPermissions = permissionCache.get(parts[0]);
                if (senderPermissions != null) {
                    senderPermissions.remove(parts[1]);
                    if (senderPermissions.isEmpty()) {
                        permissionCache.remove(parts[0]);
                    }
                }
            }
        }
    }
    
    private void evictOldestTabCompletion() {
        Integer oldestKey = tabCompletionLRU.poll();
        if (oldestKey != null) {
            tabCompletionCache.remove(oldestKey);
        }
    }
    
    /**
     * Cache statistics data class.
     */
    public static class CacheStats {
        public final int permissionCacheSize;
        public final int tabCompletionCacheSize;
        public final int spellFilterCacheSize;
        public final int helpCacheSize;
        public final double permissionHitRate;
        public final double tabCompletionHitRate;
        public final long permissionCacheHits;
        public final long permissionCacheMisses;
        public final long tabCompletionCacheHits;
        public final long tabCompletionCacheMisses;
        
        CacheStats(int permissionCacheSize, int tabCompletionCacheSize, int spellFilterCacheSize,
                  int helpCacheSize, double permissionHitRate, double tabCompletionHitRate,
                  long permissionCacheHits, long permissionCacheMisses,
                  long tabCompletionCacheHits, long tabCompletionCacheMisses) {
            this.permissionCacheSize = permissionCacheSize;
            this.tabCompletionCacheSize = tabCompletionCacheSize;
            this.spellFilterCacheSize = spellFilterCacheSize;
            this.helpCacheSize = helpCacheSize;
            this.permissionHitRate = permissionHitRate;
            this.tabCompletionHitRate = tabCompletionHitRate;
            this.permissionCacheHits = permissionCacheHits;
            this.permissionCacheMisses = permissionCacheMisses;
            this.tabCompletionCacheHits = tabCompletionCacheHits;
            this.tabCompletionCacheMisses = tabCompletionCacheMisses;
        }
    }
}