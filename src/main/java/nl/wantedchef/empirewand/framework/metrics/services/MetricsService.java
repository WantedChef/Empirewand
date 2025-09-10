package nl.wantedchef.empirewand.framework.metrics.services;

import nl.wantedchef.empirewand.api.spell.SpellRegistry;
import nl.wantedchef.empirewand.core.config.ConfigService;
import nl.wantedchef.empirewand.spell.base.Spell;
import nl.wantedchef.empirewand.spell.base.SpellType;
import nl.wantedchef.empirewand.spell.base.SpellTypes;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Verwerkt de verzameling van bStats-statistieken voor EmpireWand.
 * Biedt opt-in analyses over het gebruik en de prestaties van de plugin.
 * Deze service zorgt voor een veilige en efficiënte initialisatie en rapportage
 * aan bStats.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = { "EI_EXPOSE_REP2",
        "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE" }, justification = "Plugin reference is an injected Bukkit singleton needed for metrics; logger retrieval may flag redundant null check but defensive for test harness environments.")
public class MetricsService {

    /** De Bukkit plugin instantie. */
    private final JavaPlugin plugin;
    /** De service voor het beheren van de configuratie. */
    private final ConfigService config;
    /** De registry die alle beschikbare spells bevat. */
    private final SpellRegistry spellRegistry;
    /** De bStats Metrics instantie. */
    private Metrics metrics;
    /** Vlag om aan te geven of de statistiekenverzameling is ingeschakeld. */
    private boolean enabled = false;
    /** Een atomaire teller voor het totale aantal uitgevoerde spells. */
    private final AtomicInteger spellsCast = new AtomicInteger(0);
    /** Een atomaire teller voor het totale aantal gemaakte wands. */
    private final AtomicInteger wandsCreated = new AtomicInteger(0);
    /** De service voor het verzamelen van gedetailleerde debug-statistieken. */
    private final DebugMetricsService debugMetrics;

    /** Houdt het aantal casts per spell-naam bij. */
    private final Map<String, AtomicInteger> spellCastCounts = new ConcurrentHashMap<>();
    /** Houdt het aantal casts per spell-type bij. */
    private final Map<String, AtomicInteger> spellTypeCastCounts = new ConcurrentHashMap<>();

    /**
     * Creëert een nieuwe instantie van de MetricsService.
     *
     * @param plugin        de hoofdplugin-instantie, moet een {@link JavaPlugin}
     *                      zijn.
     * @param config        de configuratieservice.
     * @param spellRegistry de spell-registry.
     * @param debugMetrics  de service voor debug-statistieken.
     * @throws IllegalArgumentException als een van de parameters null is.
     */
    public MetricsService(JavaPlugin plugin, ConfigService config, SpellRegistry spellRegistry,
            DebugMetricsService debugMetrics) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("ConfigService cannot be null");
        }
        if (spellRegistry == null) {
            throw new IllegalArgumentException("SpellRegistry cannot be null");
        }
        if (debugMetrics == null) {
            throw new IllegalArgumentException("DebugMetricsService cannot be null");
        }
        this.plugin = plugin;
        this.config = config;
        this.spellRegistry = spellRegistry;
        this.debugMetrics = debugMetrics;
    }

    /**
     * Initialiseert de bStats-statistieken als dit is ingeschakeld in de
     * configuratie.
     * Stelt de verbinding met bStats in en voegt aangepaste grafieken toe.
     */
    public void initialize() {
        this.enabled = config.getConfig().getBoolean("metrics.enabled", true);
        Logger logger = getLoggerSafely();
        if (logger == null) {
            // Fallback naar een generieke logger als de plugin null retourneert
            logger = Logger.getLogger("EmpireWand");
        }
        if (!enabled) {
            logger.info("bStats metrics disabled by configuration");
            return;
        }

        try {
            // Haal plugin-ID op uit de config, met een fallback naar een standaardwaarde
            // Let op: Registreer je plugin op bStats.org om een echt plugin-ID te krijgen
            int pluginId = config.getConfig().getInt("metrics.plugin-id", 12345);
            if (pluginId <= 0) {
                logger.warning("Invalid bStats plugin ID in config; metrics disabled.");
                return;
            }

            this.metrics = new Metrics(plugin, pluginId);

            // Voeg aangepaste grafieken toe
            addCustomCharts();

            logger.info("bStats metrics enabled");
        } catch (Throwable t) {
            // In test- of niet-serveromgevingen kan Metrics niet initialiseren
            logger.info("bStats metrics initialization skipped (non-runtime environment)");
            this.metrics = null; // Laat uitgeschakeld maar laat de plugin niet falen
        }
    }

    /**
     * Stopt de verzameling van statistieken en sluit de verbinding met bStats.
     */
    public void shutdown() {
        if (metrics != null) {
            metrics.shutdown();
        }
    }

    /**
     * Registreert een uitgevoerde spell voor de statistieken.
     * Verhoogt de teller voor de specifieke spell en het spell-type.
     *
     * @param spellKey De unieke sleutel van de spell die is uitgevoerd.
     */
    public void recordSpellCast(String spellKey) {
        if (spellKey == null || !isEnabled()) {
            return;
        }

        try {
            Optional<Spell<?>> spellOpt = spellRegistry.getSpell(spellKey);
            if (spellOpt.isPresent()) {
                // Verhoog de totale teller voor de tijdlijngrafiek
                spellsCast.incrementAndGet();

                // Verhoog de teller voor de specifieke spell-naam
                spellCastCounts.computeIfAbsent(spellKey, k -> new AtomicInteger(0)).incrementAndGet();

                // Verhoog de teller voor het spell-type
                SpellType type = SpellTypes.resolveTypeFromKey(spellKey);
                if (type != null) {
                    spellTypeCastCounts.computeIfAbsent(type.name().toLowerCase(), k -> new AtomicInteger(0))
                            .incrementAndGet();
                }
            }
        } catch (Exception e) {
            // Log de fout maar laat de plugin niet crashen - statistieken mogen nooit de
            // hoofdfunctie verstoren
            getLoggerSafely().log(Level.WARNING, "Error recording spell cast metrics", e);
        }
    }

    /**
     * Registreert een uitgevoerde spell met de uitvoeringstijd voor
     * debug-statistieken.
     *
     * @param spellKey   De unieke sleutel van de spell.
     * @param durationMs De duur van de spell-uitvoering in milliseconden.
     */
    public void recordSpellCast(String spellKey, long durationMs) {
        if (spellKey == null || durationMs < 0) {
            return;
        }
        try {
            recordSpellCast(spellKey);
            if (config.getConfig().getBoolean("metrics.debug", false)) {
                debugMetrics.recordSpellCast(durationMs);
            }
        } catch (Exception e) {
            getLoggerSafely().log(Level.WARNING, "Error recording spell cast with duration", e);
        }
    }

    /**
     * Registreert een mislukte spell cast voor debug-statistieken.
     */
    public void recordFailedCast() {
        try {
            if (config.getConfig().getBoolean("metrics.debug", false)) {
                debugMetrics.recordFailedCast();
            }
        } catch (Exception e) {
            getLoggerSafely().log(Level.WARNING, "Error recording failed cast", e);
        }
    }

    /**
     * Registreert de verwerkingstijd van een event voor debug-statistieken.
     *
     * @param durationMs De duur van de eventverwerking in milliseconden.
     */
    public void recordEventProcessing(long durationMs) {
        if (durationMs < 0) {
            return;
        }
        try {
            if (config.getConfig().getBoolean("metrics.debug", false)) {
                debugMetrics.recordEventProcessing(durationMs);
            }
        } catch (Exception e) {
            getLoggerSafely().log(Level.WARNING, "Error recording event processing", e);
        }
    }

    /**
     * Vraagt de debug-informatie op van de debug-statistiekenservice.
     *
     * @return Een string met debug-informatie, of een bericht dat debug-modus is
     *         uitgeschakeld.
     */
    public String getDebugInfo() {
        if (config.getConfig().getBoolean("metrics.debug", false)) {
            return debugMetrics.getDebugInfo();
        }
        return "Debug metrics disabled in configuration";
    }

    /**
     * Registreert het creëren van een nieuwe wand voor de statistieken.
     */
    public void recordWandCreated() {
        try {
            if (isEnabled()) {
                wandsCreated.incrementAndGet();
            }
        } catch (Exception e) {
            getLoggerSafely().log(Level.WARNING, "Error recording wand creation", e);
        }
    }

    /**
     * Voegt alle aangepaste grafieken toe aan bStats tijdens de initialisatie.
     */
    private void addCustomCharts() {
        if (metrics == null) {
            return;
        }

        // Grafiek voor serversoftware (Paper, Spigot, etc.)
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

        // Grafiek voor Java-versie
        metrics.addCustomChart(new SimplePie("java_version", () -> System.getProperty("java.version", "Unknown")));

        // Grafiek voor het totale aantal uitgevoerde spells (alleen debug-modus)
        if (config.getConfig().getBoolean("metrics.debug", false)) {
            metrics.addCustomChart(new SingleLineChart("total_spells_cast_timeline", spellsCast::get));
        }

        // Grafiek voor het totale aantal gemaakte wands
        metrics.addCustomChart(new SingleLineChart("wands_created_timeline", wandsCreated::get));

        // Grafiek voor de populariteit van elke spell
        metrics.addCustomChart(new AdvancedPie("spell_popularity", () -> spellCastCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()))));

        // Grafiek voor de populariteit van elk spell-type
        metrics.addCustomChart(new AdvancedPie("spell_type_popularity", () -> spellTypeCastCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()))));
    }

    /**
     * Controleert of de statistiekenverzameling is ingeschakeld.
     *
     * @return {@code true} als statistieken zijn ingeschakeld, anders
     *         {@code false}.
     */
    public boolean isEnabled() {
        return enabled && metrics != null;
    }

    /**
     * Haalt de logger van de plugin op een veilige manier op, met een fallback.
     *
     * @return Een {@link Logger} instantie.
     */
    private Logger getLoggerSafely() {
        try {
            return plugin.getLogger(); // Bukkit garandeert non-null
        } catch (Throwable t) {
            return Logger.getLogger("EmpireWand");
        }
    }
}
