package com.example.empirewand.core.services.metrics;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.example.empirewand.api.SpellRegistry;
import com.example.empirewand.core.services.ConfigService;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.plugin.Plugin;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellType;
import com.example.empirewand.spell.SpellTypes;

/**
 * Handles bStats metrics collection for EmpireWand.
 * Provides opt-in analytics about plugin usage and performance.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = { "EI_EXPOSE_REP2",
        "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE" }, justification = "Plugin reference is an injected Bukkit singleton needed for metrics; logger retrieval may flag redundant null check but defensive for test harness environments.")
public class MetricsService {

    private final Plugin plugin;
    private final ConfigService config;
    private final SpellRegistry spellRegistry;
    private Metrics metrics;
    private boolean enabled = false;
    private final AtomicInteger spellsCast = new AtomicInteger(0);
    private final AtomicInteger wandsCreated = new AtomicInteger(0);
    private final DebugMetricsService debugMetrics;

    public MetricsService(Plugin plugin, ConfigService config, SpellRegistry spellRegistry,
            DebugMetricsService debugMetrics) {
        this.plugin = plugin;
        this.config = config;
        this.spellRegistry = spellRegistry;
        this.debugMetrics = debugMetrics;
    }

    /**
     * Initializes bStats metrics if enabled in configuration.
     */
    public void initialize() {
        this.enabled = config.getConfig().getBoolean("metrics.enabled", true);
        Logger logger = getLoggerSafely();
        if (logger == null) {
            // Fallback to a generic logger if plugin returns null
            logger = Logger.getLogger("EmpireWand");
        }
        if (!enabled) {
            logger.info("bStats metrics disabled by configuration");
            return;
        }

        try {
            // Get plugin ID from config, fallback to a default
            // Note: Register your plugin at bStats.org to get a real plugin ID
            int pluginId = config.getConfig().getInt("metrics.plugin-id", 12345);
            if (pluginId <= 0) {
                logger.warning("Invalid bStats plugin ID in config; metrics disabled.");
                return;
            }

            this.metrics = new Metrics((org.bukkit.plugin.java.JavaPlugin) plugin, pluginId);

            // Add custom charts
            addCustomCharts();

            logger.info("bStats metrics enabled");
        } catch (Throwable t) {
            // In test or non-server environments, Metrics may fail to initialize
            logger.info("bStats metrics initialization skipped (non-runtime environment)");
            this.metrics = null; // Leave disabled but do not fail
        }
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
        if (metrics != null && config.getConfig().getBoolean("metrics.enabled", true)) {
            Optional<Spell> spellOpt = spellRegistry.getSpell(spellKey);
            if (spellOpt.isPresent()) {
                SpellType type = SpellTypes.resolveTypeFromKey(spellKey);
                metrics.addCustomChart(new SimplePie("spell_type_casts", () -> type.name().toLowerCase()));
            }
            metrics.addCustomChart(new SimplePie("spell_casts", () -> spellKey));
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
        if (metrics == null)
            return;

        // Chart for server software
        metrics.addCustomChart(new SimplePie("server_software", () -> {
            String version = plugin.getServer().getVersion();
            if (version.contains("Paper"))
                return "Paper";
            if (version.contains("Spigot"))
                return "Spigot";
            if (version.contains("Bukkit"))
                return "Bukkit";
            return "Other";
        }));

        // Chart for Java version
        metrics.addCustomChart(new SimplePie("java_version", () -> System.getProperty("java.version", "Unknown")));

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
        return enabled;
    }

    private Logger getLoggerSafely() {
        try {
            return plugin.getLogger(); // Bukkit guarantees non-null
        } catch (Throwable t) {
            return Logger.getLogger("EmpireWand");
        }
    }
}
