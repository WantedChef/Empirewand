package com.example.empirewand.core.services;

import com.example.empirewand.api.ServiceHealth;
import com.example.empirewand.api.SpellMetadata;
import com.example.empirewand.api.SpellQuery;
import com.example.empirewand.api.SpellRegistry;
import com.example.empirewand.api.Version;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.implementation.aura.*;
import com.example.empirewand.spell.implementation.control.*;
import com.example.empirewand.spell.implementation.dark.*;
import com.example.empirewand.spell.implementation.earth.*;
import com.example.empirewand.spell.implementation.fire.*;
import com.example.empirewand.spell.implementation.heal.*;
import com.example.empirewand.spell.implementation.ice.*;
import com.example.empirewand.spell.implementation.life.*;
import com.example.empirewand.spell.implementation.lightning.*;
import com.example.empirewand.spell.implementation.misc.*;
import com.example.empirewand.spell.implementation.movement.*;
import com.example.empirewand.spell.implementation.poison.*;
import com.example.empirewand.spell.implementation.projectile.*;
import com.example.empirewand.spell.implementation.weather.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SpellRegistryImpl implements SpellRegistry {
    private final Map<String, Spell> spells = new HashMap<>();
    private final Map<String, SpellMetadata> spellMetadata = new HashMap<>();
    private final Map<String, Set<String>> spellCategories = new HashMap<>();
    private final Map<String, Set<String>> spellTags = new HashMap<>();
    private boolean enabled = true;

    public SpellRegistryImpl() {
        registerAllSpells();
        initializeMetadata();
    }

    private void registerAllSpells() {
        registerAuraSpells();
        registerControlSpells();
        registerDarkSpells();
        registerEarthSpells();
        registerFireSpells();
        registerHealSpells();
        registerIceSpells();
        registerLifeSpells();
        registerLightningSpells();
        registerMiscSpells();
        registerMovementSpells();
        registerPoisonSpells();
        registerProjectileSpells();
        registerWeatherSpells();
    }

    private void initializeMetadata() {
        // Initialize basic metadata for all registered spells
        for (Spell spell : spells.values()) {
            String key = spell.key();
            if (!spellMetadata.containsKey(key)) {
                // Create basic metadata - in a real implementation, this would load from config
                SpellMetadata metadata = createBasicMetadata(spell);
                spellMetadata.put(key, metadata);

                // Add to categories and tags
                String category = metadata.getCategory();
                spellCategories.computeIfAbsent(category, k -> new HashSet<>()).add(key);

                for (String tag : metadata.getTags()) {
                    spellTags.computeIfAbsent(tag, k -> new HashSet<>()).add(key);
                }
            }
        }
    }

    private SpellMetadata createBasicMetadata(Spell spell) {
        // This is a simplified implementation - in practice, you'd load this from
        // config files
        return new SpellMetadata() {
            @Override
            public String getKey() {
                return spell.key();
            }

            @Override
            public net.kyori.adventure.text.Component getDisplayName() {
                return spell.displayName();
            }

            @Override
            public net.kyori.adventure.text.Component getDescription() {
                return net.kyori.adventure.text.Component.text("A magical spell");
            }

            @Override
            public String getCategory() {
                return spell.type() != null ? spell.type().name().toLowerCase() : "misc";
            }

            @Override
            public Set<String> getTags() {
                return Set.of("magic");
            }

            @Override
            public long getCooldownTicks() {
                return 100; // Default cooldown
            }

            @Override
            public double getRange() {
                return 20.0; // Default range
            }

            @Override
            public int getLevelRequirement() {
                return 1; // Default level requirement
            }

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public String getIconMaterial() {
                return "DIAMOND";
            }

            @Override
            public Object getProperty(String key) {
                return null;
            }
        };
    }

    private void register(Spell spell) {
        spells.put(spell.getName(), spell);
    }

    @Override
    public Optional<Spell> getSpell(String key) {
        return Optional.ofNullable(spells.get(key));
    }

    @Override
    @Deprecated(forRemoval = true)
    public Spell getSpellNullable(String key) {
        return spells.get(key);
    }

    @Override
    public Map<String, Spell> getAllSpells() {
        return Collections.unmodifiableMap(spells);
    }

    @Override
    public Set<String> getSpellKeys() {
        return Collections.unmodifiableSet(spells.keySet());
    }

    @Override
    public boolean isSpellRegistered(String key) {
        return spells.containsKey(key);
    }

    @Override
    public String getSpellDisplayName(String key) {
        return getSpell(key)
                .map(spell -> spell.displayName() != null ? spell.displayName().toString() : key)
                .orElse(key);
    }

    // Grouped registration methods for better organization
    private void registerAuraSpells() {
        register(new Aura());
        register(new EmpireAura());
    }

    private void registerControlSpells() {
        register(new Confuse());
        register(new Polymorph());
        register(new StasisField());
    }

    private void registerDarkSpells() {
        register(new DarkCircle());
        register(new DarkPulse());
        register(new RitualOfUnmaking());
        register(new ShadowCloak());
        register(new ShadowStep());
        register(new VoidSwap());
    }

    private void registerEarthSpells() {
        register(new EarthQuake());
        register(new GraspingVines());
        register(new Lightwall());
        register(new Sandstorm());
    }

    private void registerFireSpells() {
        register(new BlazeLaunch());
        register(new Comet());
        register(new CometShower());
        register(new EmpireComet());
        register(new Explosive());
        register(new ExplosionTrail());
        register(new Fireball());
        register(new FlameWave());
    }

    private void registerHealSpells() {
        register(new GodCloud());
        register(new Heal());
        register(new RadiantBeacon());
    }

    private void registerIceSpells() {
        register(new FrostNova());
        register(new GlacialSpike());
    }

    private void registerLifeSpells() {
        register(new BloodBarrier());
        register(new BloodBlock());
        register(new BloodNova());
        register(new BloodSpam());
        register(new BloodTap());
        register(new Hemorrhage());
        register(new LifeReap());
        register(new LifeSteal());
    }

    private void registerLightningSpells() {
        register(new ChainLightning());
        register(new LightningArrow());
        register(new LightningBolt());
        register(new LightningStorm());
        register(new LittleSpark());
        register(new SolarLance());
        register(new Spark());
        register(new ThunderBlast());
    }

    private void registerMiscSpells() {
        register(new EmpireLaunch());
        register(new EmpireLevitate());
        register(new EtherealForm());
        register(new ExplosionWave());
    }

    private void registerMovementSpells() {
        register(new BlinkStrike());
        register(new EmpireEscape());
        register(new Leap());
        register(new SunburstStep());
        register(new Teleport());
    }

    private void registerPoisonSpells() {
        register(new CrimsonChains());
        register(new MephidicReap());
        register(new PoisonWave());
        register(new SoulSever());
    }

    private void registerProjectileSpells() {
        register(new ArcaneOrb());
        register(new MagicMissile());
    }

    private void registerWeatherSpells() {
        register(new Gust());
        register(new Tornado());
    }

    // ===== IMPLEMENTATION OF NEW SPELLREGISTRY METHODS =====

    @Override
    public String getServiceName() {
        return "SpellRegistry";
    }

    @Override
    public Version getServiceVersion() {
        return Version.of(2, 0, 0);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public ServiceHealth getHealth() {
        return enabled ? ServiceHealth.HEALTHY : ServiceHealth.UNHEALTHY;
    }

    @Override
    public void reload() {
        // Reload spell configurations and metadata
        spellMetadata.clear();
        spellCategories.clear();
        spellTags.clear();
        initializeMetadata();
    }

    @Override
    public SpellBuilder createSpell(String key) {
        return new SpellBuilderImpl(key);
    }

    @Override
    public boolean registerSpell(Spell spell) {
        if (spell == null || spells.containsKey(spell.key())) {
            return false;
        }
        spells.put(spell.key(), spell);

        // Create and store metadata
        SpellMetadata metadata = createBasicMetadata(spell);
        spellMetadata.put(spell.key(), metadata);

        // Update categories and tags
        String category = metadata.getCategory();
        spellCategories.computeIfAbsent(category, k -> new HashSet<>()).add(spell.key());

        for (String tag : metadata.getTags()) {
            spellTags.computeIfAbsent(tag, k -> new HashSet<>()).add(spell.key());
        }

        return true;
    }

    @Override
    public boolean unregisterSpell(String key) {
        if (!spells.containsKey(key)) {
            return false;
        }

        spells.remove(key);
        SpellMetadata metadata = spellMetadata.remove(key);

        if (metadata != null) {
            // Remove from categories and tags
            String category = metadata.getCategory();
            Set<String> categorySpells = spellCategories.get(category);
            if (categorySpells != null) {
                categorySpells.remove(key);
                if (categorySpells.isEmpty()) {
                    spellCategories.remove(category);
                }
            }

            for (String tag : metadata.getTags()) {
                Set<String> tagSpells = spellTags.get(tag);
                if (tagSpells != null) {
                    tagSpells.remove(key);
                    if (tagSpells.isEmpty()) {
                        spellTags.remove(tag);
                    }
                }
            }
        }

        return true;
    }

    @Override
    public Optional<SpellMetadata> getSpellMetadata(String key) {
        return Optional.ofNullable(spellMetadata.get(key));
    }

    @Override
    public boolean updateSpellMetadata(String key, SpellMetadata metadata) {
        if (!spells.containsKey(key) || metadata == null) {
            return false;
        }

        // Remove old metadata from categories and tags
        SpellMetadata oldMetadata = spellMetadata.get(key);
        if (oldMetadata != null) {
            String oldCategory = oldMetadata.getCategory();
            Set<String> oldCategorySpells = spellCategories.get(oldCategory);
            if (oldCategorySpells != null) {
                oldCategorySpells.remove(key);
                if (oldCategorySpells.isEmpty()) {
                    spellCategories.remove(oldCategory);
                }
            }

            for (String tag : oldMetadata.getTags()) {
                Set<String> oldTagSpells = spellTags.get(tag);
                if (oldTagSpells != null) {
                    oldTagSpells.remove(key);
                    if (oldTagSpells.isEmpty()) {
                        spellTags.remove(tag);
                    }
                }
            }
        }

        // Store new metadata
        spellMetadata.put(key, metadata);

        // Add to new categories and tags
        String newCategory = metadata.getCategory();
        spellCategories.computeIfAbsent(newCategory, k -> new HashSet<>()).add(key);

        for (String tag : metadata.getTags()) {
            spellTags.computeIfAbsent(tag, k -> new HashSet<>()).add(key);
        }

        return true;
    }

    @Override
    public Set<String> getSpellCategories() {
        return Collections.unmodifiableSet(spellCategories.keySet());
    }

    @Override
    public Set<String> getSpellsByCategory(String category) {
        Set<String> spellsInCategory = spellCategories.get(category);
        return spellsInCategory != null ? Collections.unmodifiableSet(spellsInCategory) : Collections.emptySet();
    }

    @Override
    public Set<String> getSpellsByTag(String tag) {
        Set<String> spellsWithTag = spellTags.get(tag);
        return spellsWithTag != null ? Collections.unmodifiableSet(spellsWithTag) : Collections.emptySet();
    }

    @Override
    public Set<String> getSpellTags() {
        return Collections.unmodifiableSet(spellTags.keySet());
    }

    @Override
    public List<Spell> findSpells(SpellQuery query) {
        return query.execute();
    }

    @Override
    public SpellQuery.Builder createQuery() {
        return new SpellQueryBuilderImpl();
    }

    @Override
    public int getSpellCount() {
        return spells.size();
    }

    @Override
    public int getSpellCountByCategory(String category) {
        Set<String> spellsInCategory = spellCategories.get(category);
        return spellsInCategory != null ? spellsInCategory.size() : 0;
    }

    @Override
    public int getEnabledSpellCount() {
        return (int) spellMetadata.values().stream()
                .filter(SpellMetadata::isEnabled)
                .count();
    }

    // ===== INNER CLASSES FOR BUILDERS =====

    private class SpellBuilderImpl implements SpellBuilder {
        private final String key;
        private String name;
        private String description = "A magical spell";
        private String category = "misc";
        private Set<String> tags = new HashSet<>();
        private long cooldownTicks = 100;
        private double range = 20.0;
        private int levelRequirement = 1;
        private boolean enabled = true;
        private String iconMaterial = "DIAMOND";

        public SpellBuilderImpl(String key) {
            this.key = key;
            this.name = key;
        }

        @Override
        public SpellBuilder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public SpellBuilder description(String description) {
            this.description = description;
            return this;
        }

        @Override
        public SpellBuilder category(String category) {
            this.category = category;
            return this;
        }

        @Override
        public SpellBuilder tags(String... tags) {
            this.tags = Set.of(tags);
            return this;
        }

        @Override
        public SpellBuilder cooldown(long ticks) {
            this.cooldownTicks = ticks;
            return this;
        }

        @Override
        public SpellBuilder range(double range) {
            this.range = range;
            return this;
        }

        @Override
        public SpellBuilder levelRequirement(int level) {
            this.levelRequirement = level;
            return this;
        }

        @Override
        public SpellBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        @Override
        public SpellBuilder iconMaterial(String material) {
            this.iconMaterial = material;
            return this;
        }

        @Override
        public SpellBuilder executor(SpellExecutor executor) {
            // In a real implementation, you'd store this and use it to create the spell
            return this;
        }

        @Override
        public SpellBuilder validator(SpellValidator validator) {
            // In a real implementation, you'd store this and use it to create the spell
            return this;
        }

        @Override
        public SpellBuilder property(String key, Object value) {
            // In a real implementation, you'd store custom properties
            return this;
        }

        @Override
        public Spell build() {
            // This is a simplified implementation - in practice, you'd create a proper
            // Spell instance
            // For now, return null as this would require a full spell implementation
            throw new UnsupportedOperationException("Custom spell creation not yet implemented");
        }
    }

    private class SpellQueryBuilderImpl implements SpellQuery.Builder {
        private String category;
        private String tag;
        private String nameContains;
        private Long maxCooldown;
        private Double minRange;
        private Double maxRange;
        private Integer maxLevelRequirement;
        private Boolean enabled;
        private SpellQuery.SortField sortField = SpellQuery.SortField.NAME;
        private SpellQuery.SortOrder sortOrder = SpellQuery.SortOrder.ASCENDING;
        private int limit = Integer.MAX_VALUE;

        @Override
        public SpellQuery.Builder category(String category) {
            this.category = category;
            return this;
        }

        @Override
        public SpellQuery.Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        @Override
        public SpellQuery.Builder nameContains(String text) {
            this.nameContains = text;
            return this;
        }

        @Override
        public SpellQuery.Builder cooldown(long maxTicks) {
            this.maxCooldown = maxTicks;
            return this;
        }

        @Override
        public SpellQuery.Builder range(double min, double max) {
            this.minRange = min;
            this.maxRange = max;
            return this;
        }

        @Override
        public SpellQuery.Builder levelRequirement(int maxLevel) {
            this.maxLevelRequirement = maxLevel;
            return this;
        }

        @Override
        public SpellQuery.Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        @Override
        public SpellQuery.Builder sortBy(SpellQuery.SortField field) {
            this.sortField = field;
            return this;
        }

        @Override
        public SpellQuery.Builder sortOrder(SpellQuery.SortOrder order) {
            this.sortOrder = order;
            return this;
        }

        @Override
        public SpellQuery.Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        @Override
        public SpellQuery build() {
            return new SpellQueryImpl(this);
        }
    }

    private class SpellQueryImpl implements SpellQuery {
        private final SpellQueryBuilderImpl builder;

        public SpellQueryImpl(SpellQueryBuilderImpl builder) {
            this.builder = builder;
        }

        @Override
        public List<Spell> execute() {
            List<Spell> results = new ArrayList<>();

            for (Spell spell : spells.values()) {
                if (matches(spell)) {
                    results.add(spell);
                }
            }

            // Apply sorting
            results.sort((s1, s2) -> {
                int comparison = 0;
                switch (builder.sortField) {
                    case NAME:
                        comparison = s1.getName().compareTo(s2.getName());
                        break;
                    case COOLDOWN:
                        SpellMetadata m1_cd = spellMetadata.get(s1.key());
                        SpellMetadata m2_cd = spellMetadata.get(s2.key());
                        if (m1_cd != null && m2_cd != null) {
                            comparison = Long.compare(m1_cd.getCooldownTicks(), m2_cd.getCooldownTicks());
                        }
                        break;
                    case RANGE:
                        SpellMetadata m1_r = spellMetadata.get(s1.key());
                        SpellMetadata m2_r = spellMetadata.get(s2.key());
                        if (m1_r != null && m2_r != null) {
                            comparison = Double.compare(m1_r.getRange(), m2_r.getRange());
                        }
                        break;
                    case LEVEL_REQUIREMENT:
                        SpellMetadata m1_lr = spellMetadata.get(s1.key());
                        SpellMetadata m2_lr = spellMetadata.get(s2.key());
                        if (m1_lr != null && m2_lr != null) {
                            comparison = Integer.compare(m1_lr.getLevelRequirement(), m2_lr.getLevelRequirement());
                        }
                        break;
                    case CATEGORY:
                        SpellMetadata m1_c = spellMetadata.get(s1.key());
                        SpellMetadata m2_c = spellMetadata.get(s2.key());
                        if (m1_c != null && m2_c != null) {
                            comparison = m1_c.getCategory().compareTo(m2_c.getCategory());
                        }
                        break;
                }

                return builder.sortOrder == SpellQuery.SortOrder.DESCENDING ? -comparison : comparison;
            });

            // Apply limit
            if (results.size() > builder.limit) {
                results = results.subList(0, builder.limit);
            }

            return results;
        }

        private boolean matches(Spell spell) {
            SpellMetadata metadata = spellMetadata.get(spell.key());
            if (metadata == null)
                return false;

            // Category filter
            if (builder.category != null && !builder.category.equals(metadata.getCategory())) {
                return false;
            }

            // Tag filter
            if (builder.tag != null && !metadata.getTags().contains(builder.tag)) {
                return false;
            }

            // Name contains filter
            if (builder.nameContains != null &&
                    !spell.getName().toLowerCase().contains(builder.nameContains.toLowerCase())) {
                return false;
            }

            // Cooldown filter
            if (builder.maxCooldown != null && metadata.getCooldownTicks() > builder.maxCooldown) {
                return false;
            }

            // Range filter
            if (builder.minRange != null && metadata.getRange() < builder.minRange) {
                return false;
            }
            if (builder.maxRange != null && metadata.getRange() > builder.maxRange) {
                return false;
            }

            // Level requirement filter
            if (builder.maxLevelRequirement != null &&
                    metadata.getLevelRequirement() > builder.maxLevelRequirement) {
                return false;
            }

            // Enabled filter
            if (builder.enabled != null && metadata.isEnabled() != builder.enabled) {
                return false;
            }

            return true;
        }
    }
}
