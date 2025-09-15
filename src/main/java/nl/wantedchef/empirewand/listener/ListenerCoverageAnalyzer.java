package nl.wantedchef.empirewand.listener;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.core.config.ReadableConfig;


import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Analyzes listener coverage for spells and provides recommendations for missing listeners.
 * Used for development and testing to ensure comprehensive spell effect handling.
 */
public class ListenerCoverageAnalyzer {
    private final EmpireWandPlugin plugin;
    
    // Map spell types to their required listener categories
    private static final Map<SpellType, Set<ListenerCategory>> SPELL_TYPE_REQUIREMENTS = Map.of(
        SpellType.LIGHTNING, EnumSet.of(ListenerCategory.LIGHTNING, ListenerCategory.STATUS_EFFECT, ListenerCategory.PERFORMANCE),
        SpellType.FIRE, EnumSet.of(ListenerCategory.ENVIRONMENTAL, ListenerCategory.STATUS_EFFECT, ListenerCategory.WAVE, ListenerCategory.PERFORMANCE),
        SpellType.ICE, EnumSet.of(ListenerCategory.ENVIRONMENTAL, ListenerCategory.STATUS_EFFECT, ListenerCategory.WAVE, ListenerCategory.PERFORMANCE),
        SpellType.MOVEMENT, EnumSet.of(ListenerCategory.MOVEMENT, ListenerCategory.STATUS_EFFECT, ListenerCategory.PERFORMANCE),
        SpellType.PROJECTILE, EnumSet.of(ListenerCategory.PROJECTILE, ListenerCategory.WAVE, ListenerCategory.PERFORMANCE),
        SpellType.AURA, EnumSet.of(ListenerCategory.AURA, ListenerCategory.STATUS_EFFECT, ListenerCategory.PERFORMANCE),
        SpellType.DARK, EnumSet.of(ListenerCategory.STATUS_EFFECT, ListenerCategory.AURA, ListenerCategory.WAVE, ListenerCategory.PERFORMANCE),
        SpellType.LIFE, EnumSet.of(ListenerCategory.STATUS_EFFECT, ListenerCategory.AURA, ListenerCategory.WAVE, ListenerCategory.PERFORMANCE),
        SpellType.EARTH, EnumSet.of(ListenerCategory.ENVIRONMENTAL, ListenerCategory.STATUS_EFFECT, ListenerCategory.PERFORMANCE),
        SpellType.WEATHER, EnumSet.of(ListenerCategory.ENVIRONMENTAL, ListenerCategory.PERFORMANCE)
    );
    
    // Track which listeners are currently active
    private final Set<ListenerCategory> activeListeners = EnumSet.noneOf(ListenerCategory.class);
    
    public ListenerCoverageAnalyzer(EmpireWandPlugin plugin) {
        this.plugin = plugin;
        initializeActiveListeners();
    }
    
    /**
     * Analyzes current spell configuration and provides coverage report
     */
    public CoverageReport analyzeSpellCoverage() {
        Map<String, SpellAnalysis> spellAnalyses = new HashMap<>();
        Set<SpellType> uncoveredTypes = new HashSet<>();
        Set<ListenerCategory> missingListeners = new HashSet<>();
        
        ReadableConfig spellsConfig = plugin.getConfigService().getSpellsConfig();
        if (spellsConfig == null) {
            return new CoverageReport(spellAnalyses, uncoveredTypes, missingListeners, 0.0);
        }
        
        // Get spells section as a map or use the top-level keys
        Object spellsData = spellsConfig.get("spells");
        if (!(spellsData instanceof Map)) {
            return new CoverageReport(spellAnalyses, uncoveredTypes, missingListeners, 0.0);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> spellsSection = (Map<String, Object>) spellsData;
        
        for (String spellKey : spellsSection.keySet()) {
            SpellAnalysis analysis = analyzeSpell(spellKey);
            spellAnalyses.put(spellKey, analysis);
            
            if (!analysis.isFullyCovered()) {
                uncoveredTypes.add(analysis.spellType());
                missingListeners.addAll(analysis.missingListeners());
            }
        }
        
        double coveragePercentage = calculateCoveragePercentage(spellAnalyses);
        
        return new CoverageReport(spellAnalyses, uncoveredTypes, missingListeners, coveragePercentage);
    }
    
    /**
     * Analyzes a specific spell's listener coverage
     */
    public SpellAnalysis analyzeSpell(String spellKey) {
        ReadableConfig spellsConfig = plugin.getConfigService().getSpellsConfig();
        if (spellsConfig == null) {
            return new SpellAnalysis(spellKey, SpellType.MISC, Set.of(), Set.of(), false);
        }
        
        // Get the specific spell configuration
        Object spellConfigObj = spellsConfig.get("spells." + spellKey);
        if (!(spellConfigObj instanceof Map)) {
            return new SpellAnalysis(spellKey, SpellType.MISC, Set.of(), Set.of(), false);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> spellConfig = (Map<String, Object>) spellConfigObj;
        
        String typeString = (String) spellConfig.getOrDefault("type", "MISC");
        SpellType spellType;
        try {
            spellType = SpellType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            spellType = SpellType.MISC;
        }
        
        Set<ListenerCategory> requiredListeners = SPELL_TYPE_REQUIREMENTS.getOrDefault(spellType, Set.of());
        Set<ListenerCategory> coveredListeners = requiredListeners.stream()
                .filter(activeListeners::contains)
                .collect(Collectors.toSet());
        
        Set<ListenerCategory> missingListeners = new HashSet<>(requiredListeners);
        missingListeners.removeAll(coveredListeners);
        
        boolean fullyCovered = missingListeners.isEmpty();
        
        return new SpellAnalysis(spellKey, spellType, coveredListeners, missingListeners, fullyCovered);
    }
    
    /**
     * Provides recommendations for improving listener coverage
     */
    public List<String> getRecommendations() {
        CoverageReport report = analyzeSpellCoverage();
        List<String> recommendations = new ArrayList<>();

        double coverage = report.coveragePercentage();
        if (!Double.isNaN(coverage) && !Double.isInfinite(coverage) && coverage < 90.0) {
            recommendations.add(String.format("Overall coverage is %.1f%% - aim for 90%% coverage",
                    coverage));
        }
        
        if (!report.missingListeners().isEmpty()) {
            recommendations.add("Missing listener categories: " + 
                    report.missingListeners().stream()
                            .map(ListenerCategory::getDisplayName)
                            .collect(Collectors.joining(", ")));
        }
        
        if (!report.uncoveredSpellTypes().isEmpty()) {
            recommendations.add("Spell types with coverage gaps: " + 
                    report.uncoveredSpellTypes().stream()
                            .map(SpellType::name)
                            .collect(Collectors.joining(", ")));
        }
        
        // Specific recommendations based on missing listeners
        for (ListenerCategory missing : report.missingListeners()) {
            switch (missing) {
                case LIGHTNING -> recommendations.add("Add LightningEffectListener for proper lightning spell handling");
                case ENVIRONMENTAL -> recommendations.add("Add EnvironmentalSpellListener for block manipulation and weather effects");
                case MOVEMENT -> recommendations.add("Add MovementSpellListener for teleportation safety and movement effects");
                case AURA -> recommendations.add("Add AuraSpellListener for passive aura effects");
                case WAVE -> recommendations.add("Add WaveSpellListener for multi-projectile and wave spell mechanics");
                case STATUS_EFFECT -> recommendations.add("Add StatusEffectListener for potion effect management");
                case PLAYER -> recommendations.add("Ensure PlayerJoinQuitListener covers all necessary player state management");
                case WAND -> recommendations.add("Review Wand interaction listeners for comprehensive wand handling");
                case PERFORMANCE -> recommendations.add("Implement PerformanceMonitoringListener for spell performance tracking");
                case COMBAT -> recommendations.add("Add or verify combat listeners for damage and PvP interactions");
                case PROJECTILE -> recommendations.add("Ensure ProjectileHitListener is correctly handling all custom projectiles");
            }
        }
        
        return recommendations;
    }
    
    /**
     * Prints a detailed coverage report to the console
     */
    public void printCoverageReport() {
        CoverageReport report = analyzeSpellCoverage();

        plugin.getLogger().info("=== Spell Listener Coverage Analysis ===");
        double coverage = report.coveragePercentage();
        if (!Double.isNaN(coverage) && !Double.isInfinite(coverage)) {
            plugin.getLogger().info(String.format("Overall Coverage: %.1f%%", coverage));
        } else {
            plugin.getLogger().info("Overall Coverage: 0.0%");
        }
        plugin.getLogger().info(String.format("Total Spells Analyzed: %d", report.spellAnalyses().size()));
        
        if (!report.uncoveredSpellTypes().isEmpty()) {
            plugin.getLogger().warning("Spell types with coverage gaps:");
            for (SpellType type : report.uncoveredSpellTypes()) {
                long count = report.spellAnalyses().values().stream()
                        .filter(analysis -> analysis.spellType() == type && !analysis.isFullyCovered())
                        .count();
                plugin.getLogger().warning(String.format("  %s: %d spells", type.name(), count));
            }
        }
        
        if (!report.missingListeners().isEmpty()) {
            plugin.getLogger().warning("Missing listener categories:");
            for (ListenerCategory category : report.missingListeners()) {
                plugin.getLogger().warning("  " + category.getDisplayName());
            }
        }
        
        List<String> recommendations = getRecommendations();
        if (!recommendations.isEmpty()) {
            plugin.getLogger().info("Recommendations:");
            for (String recommendation : recommendations) {
                plugin.getLogger().info("  - " + recommendation);
            }
        }
        
        if (!report.spellAnalyses().isEmpty()) {
            double coverageCheck = report.coveragePercentage();
            if (!Double.isNaN(coverageCheck) && !Double.isInfinite(coverageCheck) && coverageCheck >= 90.0) {
                plugin.getLogger().info("Excellent listener coverage! All critical spell types are handled.");
            }
        }
    }
    
    private void initializeActiveListeners() {
        // Mark listeners as active based on current implementation
        activeListeners.add(ListenerCategory.PROJECTILE);      // ProjectileHitListener
        activeListeners.add(ListenerCategory.COMBAT);          // Various combat listeners
        activeListeners.add(ListenerCategory.PLAYER);          // Player join/quit listeners
        activeListeners.add(ListenerCategory.WAND);            // Wand interaction listeners
        
        // Enhanced spell listeners (newly added)
        activeListeners.add(ListenerCategory.STATUS_EFFECT);   // StatusEffectListener
        activeListeners.add(ListenerCategory.LIGHTNING);       // LightningEffectListener
        activeListeners.add(ListenerCategory.MOVEMENT);        // MovementSpellListener
        activeListeners.add(ListenerCategory.ENVIRONMENTAL);   // EnvironmentalSpellListener
        activeListeners.add(ListenerCategory.WAVE);            // WaveSpellListener
        activeListeners.add(ListenerCategory.AURA);            // AuraSpellListener
        activeListeners.add(ListenerCategory.PERFORMANCE);     // PerformanceMonitoringListener
    }
    
    private double calculateCoveragePercentage(Map<String, SpellAnalysis> spellAnalyses) {
        if (spellAnalyses.isEmpty()) {
            return 100.0;
        }
        
        long fullyCoveredCount = spellAnalyses.values().stream()
                .mapToLong(analysis -> analysis.isFullyCovered() ? 1 : 0)
                .sum();
        
        return (double) fullyCoveredCount / spellAnalyses.size() * 100.0;
    }
    
    /**
     * Listener category enumeration
     */
    public enum ListenerCategory {
        PROJECTILE("Projectile Handling"),
        COMBAT("Combat Effects"),
        PLAYER("Player Management"),
        WAND("Wand Interactions"),
        STATUS_EFFECT("Status Effect Management"),
        LIGHTNING("Lightning Effects"),
        MOVEMENT("Movement & Teleportation"),
        ENVIRONMENTAL("Environmental & Blocks"),
        WAVE("Wave & Multi-hit Spells"),
        AURA("Aura & Passive Effects"),
        PERFORMANCE("Performance Monitoring");
        
        private final String displayName;
        
        ListenerCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Analysis result for a specific spell
     */
    public record SpellAnalysis(
            String spellKey,
            SpellType spellType,
            Set<ListenerCategory> coveredListeners,
            Set<ListenerCategory> missingListeners,
            boolean isFullyCovered
    ) {}
    
    /**
     * Comprehensive coverage report
     */
    public record CoverageReport(
            Map<String, SpellAnalysis> spellAnalyses,
            Set<SpellType> uncoveredSpellTypes,
            Set<ListenerCategory> missingListeners,
            double coveragePercentage
    ) {}
}