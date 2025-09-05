package com.example.empirewand.core;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles bStats metrics collection for EmpireWand.
 * Provides opt-in analytics about plugin usage and performance.
 */
public class MetricsService {

    private final Plugin plugin;
    private final ConfigService config;
    private Metrics metrics;
    private final AtomicInteger spellsCast = new AtomicInteger(0);
    private final AtomicInteger wandsCreated = new AtomicInteger(0);
    private final DebugMetricsService debugMetrics;

    public MetricsService(Plugin plugin, ConfigService config) {
        this.plugin = plugin;
        this.config = config;
        this.debugMetrics = new DebugMetricsService(1000); // Keep last 1000 samples
    }

    /**
     * Initializes bStats metrics if enabled in configuration.
     */
    public void initialize() {
        if (!config.getConfig().getBoolean("metrics.enabled", true)) {
            plugin.getLogger().info("bStats metrics disabled by configuration");
            return;
        }

        // Initialize bStats with plugin ID (use a unique ID for your plugin)
        // Note: You should get an actual plugin ID from bStats when you submit your plugin
        this.metrics = new Metrics((org.bukkit.plugin.java.JavaPlugin) plugin, 12345); // Replace with actual bStats plugin ID

        // Add custom charts
        addCustomCharts();

        plugin.getLogger().info("bStats metrics enabled");
    }

    /**
     * Shuts down metrics collection.
     */
    public void shutdown() {
        if (metrics != null) {
            metrics.shutdown();
        }
    }

    /**
     * Records a spell cast event for metrics.
     */
    public void recordSpellCast(String spellKey) {
        if (metrics != null) {
            spellsCast.incrementAndGet();
        }
    }

    /**
     * Records a spell cast with timing for debug metrics.
     */
    public void recordSpellCast(String spellKey, long durationMs) {
        recordSpellCast(spellKey);
        if (config.getConfig().getBoolean("metrics.debug", false)) {
            debugMetrics.recordSpellCast(durationMs);
        }
    }

    /**
     * Records a failed spell cast.
     */
    public void recordFailedCast() {
        if (config.getConfig().getBoolean("metrics.debug", false)) {
            debugMetrics.recordFailedCast();
        }
    }

    /**
     * Records event processing timing.
     */
    public void recordEventProcessing(long durationMs) {
        if (config.getConfig().getBoolean("metrics.debug", false)) {
            debugMetrics.recordEventProcessing(durationMs);
        }
    }

    /**
     * Gets debug metrics information.
     */
    public String getDebugInfo() {
        if (config.getConfig().getBoolean("metrics.debug", false)) {
            return debugMetrics.getDebugInfo();
        }
        return "Debug metrics disabled in configuration";
    }

    /**
     * Records a wand creation event for metrics.
     */
    public void recordWandCreated() {
        if (metrics != null) {
            wandsCreated.incrementAndGet();
        }
    }

    /**
     * Adds custom charts to bStats.
     */
    private void addCustomCharts() {
        if (metrics == null) return;

        // Chart for server software
        metrics.addCustomChart(new SimplePie("server_software", () -> {
            String version = plugin.getServer().getVersion();
            if (version.contains("Paper")) return "Paper";
            if (version.contains("Spigot")) return "Spigot";
            if (version.contains("Bukkit")) return "Bukkit";
            return "Other";
        }));

        // Chart for Java version
        metrics.addCustomChart(new SimplePie("java_version", () ->
            System.getProperty("java.version", "Unknown")
        ));

        // Chart for spells cast (debug mode only)
        if (config.getConfig().getBoolean("metrics.debug", false)) {
            metrics.addCustomChart(new SingleLineChart("spells_cast", spellsCast::get));
        }

        // Chart for wands created
        metrics.addCustomChart(new SingleLineChart("wands_created", wandsCreated::get));
    }

    /**
     * Checks if metrics are enabled.
     */
    public boolean isEnabled() {
        return metrics != null;
    }
}