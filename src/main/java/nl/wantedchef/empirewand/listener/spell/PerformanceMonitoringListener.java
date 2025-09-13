package nl.wantedchef.empirewand.listener.spell;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.api.event.SpellCastEvent;
import nl.wantedchef.empirewand.api.event.SpellFailEvent;
import nl.wantedchef.empirewand.framework.service.metrics.MetricsService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitors spell system performance and tracks metrics for optimization.
 * Provides real-time performance data and identifies bottlenecks.
 */
public class PerformanceMonitoringListener implements Listener {
    private final EmpireWandPlugin plugin;
    private final MetricsService metricsService;
    
    // Performance tracking
    private final Map<String, SpellPerformanceData> spellPerformance = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerPerformanceData> playerPerformance = new ConcurrentHashMap<>();
    
    // System-wide metrics
    private final AtomicLong totalSpellsCast = new AtomicLong(0);
    private final AtomicLong totalSpellsFailed = new AtomicLong(0);
    private final AtomicInteger activeListeners = new AtomicInteger(0);
    
    // Performance thresholds (milliseconds)
    private static final long SLOW_SPELL_THRESHOLD = 50L;   // 50ms
    private static final long VERY_SLOW_SPELL_THRESHOLD = 100L; // 100ms
    
    public PerformanceMonitoringListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
        this.metricsService = plugin.getMetricsService();
        
        // Performance reporting task
        plugin.getTaskManager().runTaskTimerAsynchronously(this::reportPerformanceMetrics, 1200L, 1200L); // Every minute
        
        // Cleanup task
        plugin.getTaskManager().runTaskTimerAsynchronously(this::cleanupOldData, 6000L, 6000L); // Every 5 minutes
        
        activeListeners.incrementAndGet();
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSpellCast(SpellCastEvent event) {
        long startTime = System.nanoTime();
        
        try {
            String spellKey = event.getSpellKey();
            Player caster = event.getCaster();
            
            // Track spell performance
            updateSpellPerformance(spellKey, true, 0L);
            
            // Track player performance  
            updatePlayerPerformance(caster.getUniqueId(), true);
            
            totalSpellsCast.incrementAndGet();
            
        } finally {
            long executionTime = (System.nanoTime() - startTime) / 1_000_000L; // Convert to milliseconds
            
            // Log slow listener performance
            if (executionTime > 10L) { // 10ms threshold for listener
                plugin.getLogger().warning(String.format(
                    "PerformanceMonitoringListener.onSpellCast took %dms - this is concerning!", 
                    executionTime));
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSpellFail(SpellFailEvent event) {
        long startTime = System.nanoTime();
        
        try {
            String spellKey = event.getSpellKey();
            Player caster = event.getCaster();
            
            // Track spell failure
            updateSpellPerformance(spellKey, false, 0L);
            
            // Track player failure
            updatePlayerPerformance(caster.getUniqueId(), false);
            
            totalSpellsFailed.incrementAndGet();
            
            // Log failure reason for analysis
            plugin.getLogger().fine(String.format("Spell failure: %s by %s - %s", 
                spellKey, caster.getName(), event.getReason()));
            
        } finally {
            long executionTime = (System.nanoTime() - startTime) / 1_000_000L;
            
            if (executionTime > 10L) {
                plugin.getLogger().warning(String.format(
                    "PerformanceMonitoringListener.onSpellFail took %dms", 
                    executionTime));
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        
        // Initialize player performance tracking
        playerPerformance.put(playerId, new PlayerPerformanceData());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        
        // Clean up player performance data
        PlayerPerformanceData data = playerPerformance.remove(playerId);
        
        if (data != null) {
            // Log session summary if player cast spells
            if (data.spellsCast.get() > 0) {
                plugin.getLogger().info(String.format(
                    "Player %s session: %d spells cast, %d failed, %.1f%% success rate",
                    event.getPlayer().getName(),
                    data.spellsCast.get(),
                    data.spellsFailed.get(),
                    data.getSuccessRate()
                ));
            }
        }
    }
    
    /**
     * Records spell execution time for performance analysis
     */
    public void recordSpellExecutionTime(String spellKey, long executionTimeMs) {
        updateSpellPerformance(spellKey, true, executionTimeMs);
        
        // Log slow spells
        if (executionTimeMs > VERY_SLOW_SPELL_THRESHOLD) {
            plugin.getLogger().warning(String.format(
                "Very slow spell execution: %s took %dms", spellKey, executionTimeMs));
        } else if (executionTimeMs > SLOW_SPELL_THRESHOLD) {
            plugin.getLogger().fine(String.format(
                "Slow spell execution: %s took %dms", spellKey, executionTimeMs));
        }
    }
    
    /**
     * Gets performance data for a specific spell
     */
    public SpellPerformanceData getSpellPerformance(String spellKey) {
        return spellPerformance.get(spellKey);
    }
    
    /**
     * Gets overall system performance metrics
     */
    public SystemPerformanceMetrics getSystemMetrics() {
        long totalCasts = totalSpellsCast.get();
        long totalFails = totalSpellsFailed.get();
        double successRate = totalCasts > 0 ? ((double) (totalCasts - totalFails) / totalCasts) * 100.0 : 0.0;
        
        return new SystemPerformanceMetrics(
            totalCasts,
            totalFails,
            successRate,
            spellPerformance.size(),
            playerPerformance.size(),
            activeListeners.get()
        );
    }
    
    /**
     * Gets the top performing spells by cast count
     */
    public Map<String, SpellPerformanceData> getTopSpells(int limit) {
        return spellPerformance.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e2.getValue().totalCasts.get(), e1.getValue().totalCasts.get()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                java.util.LinkedHashMap::new
            ));
    }
    
    /**
     * Gets the slowest spells by average execution time
     */
    public Map<String, SpellPerformanceData> getSlowestSpells(int limit) {
        return spellPerformance.entrySet().stream()
            .filter(entry -> entry.getValue().totalCasts.get() > 0)
            .sorted((e1, e2) -> Double.compare(e2.getValue().getAverageExecutionTime(), e1.getValue().getAverageExecutionTime()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                java.util.LinkedHashMap::new
            ));
    }
    
    private void updateSpellPerformance(String spellKey, boolean success, long executionTimeMs) {
        SpellPerformanceData data = spellPerformance.computeIfAbsent(spellKey, k -> new SpellPerformanceData());
        
        if (success) {
            data.totalCasts.incrementAndGet();
            data.successfulCasts.incrementAndGet();
        } else {
            data.totalCasts.incrementAndGet();
        }
        
        if (executionTimeMs > 0) {
            data.totalExecutionTime.addAndGet(executionTimeMs);
            data.executionCount.incrementAndGet();
            
            // Update min/max execution times
            data.updateMinMaxExecutionTime(executionTimeMs);
        }
        
        data.lastUsed = System.currentTimeMillis();
    }
    
    private void updatePlayerPerformance(UUID playerId, boolean success) {
        PlayerPerformanceData data = playerPerformance.computeIfAbsent(playerId, k -> new PlayerPerformanceData());
        
        data.spellsCast.incrementAndGet();
        if (!success) {
            data.spellsFailed.incrementAndGet();
        }
        
        data.lastActivity = System.currentTimeMillis();
    }
    
    private void reportPerformanceMetrics() {
        SystemPerformanceMetrics metrics = getSystemMetrics();
        
        plugin.getLogger().info(String.format(
            "Performance Report - Total Casts: %d, Failures: %d, Success Rate: %.1f%%, Active Players: %d",
            metrics.totalSpellsCast(),
            metrics.totalSpellsFailed(),
            metrics.successRate(),
            metrics.activePlayers()
        ));
        
        // Report top 5 most used spells
        Map<String, SpellPerformanceData> topSpells = getTopSpells(5);
        if (!topSpells.isEmpty()) {
            plugin.getLogger().info("Top spells by usage:");
            topSpells.forEach((spell, data) -> {
                plugin.getLogger().info(String.format("  %s: %d casts (%.1f%% success, %.1fms avg)",
                    spell, data.totalCasts.get(), data.getSuccessRate(), data.getAverageExecutionTime()));
            });
        }
        
        // Report slowest spells
        Map<String, SpellPerformanceData> slowestSpells = getSlowestSpells(3);
        if (!slowestSpells.isEmpty()) {
            plugin.getLogger().info("Slowest spells by execution time:");
            slowestSpells.forEach((spell, data) -> {
                if (data.getAverageExecutionTime() > SLOW_SPELL_THRESHOLD) {
                    plugin.getLogger().warning(String.format("  %s: %.1fms avg (min: %dms, max: %dms)",
                        spell, data.getAverageExecutionTime(), data.minExecutionTime.get(), data.maxExecutionTime.get()));
                }
            });
        }
        
        // Send metrics to MetricsService
        if (metricsService != null) {
            metricsService.recordSystemMetrics(metrics.totalSpellsCast(), metrics.totalSpellsFailed());
        }
    }
    
    private void cleanupOldData() {
        long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000L); // 24 hours
        
        // Clean up old spell performance data
        spellPerformance.entrySet().removeIf(entry -> 
            entry.getValue().lastUsed < cutoffTime && entry.getValue().totalCasts.get() < 10);
        
        // Clean up old player performance data
        playerPerformance.entrySet().removeIf(entry -> 
            entry.getValue().lastActivity < cutoffTime);
        
        plugin.getLogger().fine(String.format("Cleaned up performance data. Tracking %d spells, %d players",
            spellPerformance.size(), playerPerformance.size()));
    }
    
    /**
     * Shuts down the performance monitoring system
     */
    public void shutdown() {
        activeListeners.decrementAndGet();
        
        // Final performance report
        reportPerformanceMetrics();
        
        plugin.getLogger().info("Performance monitoring listener shut down");
    }
    
    /**
     * Spell performance data tracking
     */
    public static class SpellPerformanceData {
        public final AtomicLong totalCasts = new AtomicLong(0);
        public final AtomicLong successfulCasts = new AtomicLong(0);
        public final AtomicLong totalExecutionTime = new AtomicLong(0);
        public final AtomicLong executionCount = new AtomicLong(0);
        public final AtomicLong minExecutionTime = new AtomicLong(Long.MAX_VALUE);
        public final AtomicLong maxExecutionTime = new AtomicLong(0);
        public volatile long lastUsed = System.currentTimeMillis();
        
        public double getSuccessRate() {
            long total = totalCasts.get();
            return total > 0 ? ((double) successfulCasts.get() / total) * 100.0 : 0.0;
        }
        
        public double getAverageExecutionTime() {
            long count = executionCount.get();
            return count > 0 ? (double) totalExecutionTime.get() / count : 0.0;
        }
        
        public void updateMinMaxExecutionTime(long executionTime) {
            minExecutionTime.updateAndGet(current -> Math.min(current, executionTime));
            maxExecutionTime.updateAndGet(current -> Math.max(current, executionTime));
        }
    }
    
    /**
     * Player performance data tracking
     */
    private static class PlayerPerformanceData {
        public final AtomicLong spellsCast = new AtomicLong(0);
        public final AtomicLong spellsFailed = new AtomicLong(0);
        public volatile long lastActivity = System.currentTimeMillis();
        
        public double getSuccessRate() {
            long total = spellsCast.get();
            return total > 0 ? ((double) (total - spellsFailed.get()) / total) * 100.0 : 0.0;
        }
    }
    
    /**
     * System-wide performance metrics
     */
    public record SystemPerformanceMetrics(
        long totalSpellsCast,
        long totalSpellsFailed,
        double successRate,
        int trackedSpells,
        int activePlayers,
        int activeListeners
    ) {}
}