package nl.wantedchef.empirewand.framework.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import nl.wantedchef.empirewand.api.ServiceHealth;
import nl.wantedchef.empirewand.api.spell.SpellMetadata;
import nl.wantedchef.empirewand.api.spell.SpellRegistry;
import nl.wantedchef.empirewand.api.Version;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.toggle.aura.Aura;
import nl.wantedchef.empirewand.spell.toggle.aura.EmpireAura;
import nl.wantedchef.empirewand.spell.control.Confuse;
import nl.wantedchef.empirewand.spell.control.Polymorph;
import nl.wantedchef.empirewand.spell.dark.*;
import nl.wantedchef.empirewand.spell.earth.EarthQuake;
import nl.wantedchef.empirewand.spell.earth.GraspingVines;
import nl.wantedchef.empirewand.spell.earth.Lightwall;
import nl.wantedchef.empirewand.spell.fire.*;
import nl.wantedchef.empirewand.spell.heal.Heal;
import nl.wantedchef.empirewand.spell.heal.RadiantBeacon;
import nl.wantedchef.empirewand.spell.ice.FrostNova;
import nl.wantedchef.empirewand.spell.ice.GlacialSpike;
import nl.wantedchef.empirewand.spell.life.*;
import nl.wantedchef.empirewand.spell.lightning.*;
import nl.wantedchef.empirewand.spell.misc.EmpireLaunch;
import nl.wantedchef.empirewand.spell.misc.EmpireLevitate;
import nl.wantedchef.empirewand.spell.misc.EtherealForm;
import nl.wantedchef.empirewand.spell.misc.ExplosionWave;
import nl.wantedchef.empirewand.spell.movement.*;
import nl.wantedchef.empirewand.spell.poison.CrimsonChains;
import nl.wantedchef.empirewand.spell.poison.MephidicReap;
import nl.wantedchef.empirewand.spell.poison.PoisonWave;
import nl.wantedchef.empirewand.spell.poison.SoulSever;
import nl.wantedchef.empirewand.spell.projectile.ArcaneOrb;
import nl.wantedchef.empirewand.spell.projectile.MagicMissile;
import nl.wantedchef.empirewand.spell.weather.Gust;
import nl.wantedchef.empirewand.spell.weather.Tornado;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The primary implementation of the {@link SpellRegistry}.
 * This class is responsible for registering, managing, and providing access to
 * all available spells.
 */
public class SpellRegistryImpl implements SpellRegistry {

    private final Map<String, Spell<?>> spells = new ConcurrentHashMap<>();
    private final ConfigService configService;

    /**
     * Constructs a new SpellRegistryImpl.
     *
     * @param configService The configuration service.
     */
    public SpellRegistryImpl(ConfigService configService) {
        this.configService = configService;
        registerAllSpells();
    }

    /**
     * Registers all the default spells.
     */
    private void registerAllSpells() {
        // During migration, builders no longer require a non-null API instance.
        final nl.wantedchef.empirewand.api.EmpireWandAPI api = null;

        // Je ReadableConfig kan niet direct subsecties geven zoals Bukkit
        // ConfigurationSection.
        // We proberen defensief te casten; zo niet, dan slaan we het laden van
        // per-spell config over.
        final var readable = configService.getSpellsConfig();
        final ReadableConfig bukkitSpells = readable;

        Stream.<Supplier<Spell.Builder<?>>>of(
                // Aura
                () -> new Aura.Builder(api),
                () -> new EmpireAura.Builder(api),
                // Control
                () -> new Confuse.Builder(api),
                () -> new Polymorph.Builder(api),
                // Dark
                () -> new DarkCircle.Builder(api),
                () -> new DarkPulse.Builder(api),
                () -> new RitualOfUnmaking.Builder(api),
                () -> new ShadowStep.Builder(api),
                () -> new VoidSwap.Builder(api),
                // Earth
                () -> new EarthQuake.Builder(api),
                () -> new GraspingVines.Builder(api),
                () -> new Lightwall.Builder(api),
                // Fire
                () -> new BlazeLaunch.Builder(api),
                () -> new Comet.Builder(api),
                () -> new CometShower.Builder(api),
                () -> new EmpireComet.Builder(api),
                () -> new Explosive.Builder(api),
                () -> new ExplosionTrail.Builder(api),
                () -> new Fireball.Builder(api),
                () -> new FlameWave.Builder(api),
                // Heal
                () -> new Heal.Builder(api),
                () -> new RadiantBeacon.Builder(api),
                // Ice
                () -> new FrostNova.Builder(api),
                () -> new GlacialSpike.Builder(api),
                // Life
                () -> new BloodBarrier.Builder(api),
                () -> new BloodBlock.Builder(api),
                () -> new BloodNova.Builder(api),
                () -> new BloodSpam.Builder(api),
                () -> new BloodTap.Builder(api),
                () -> new Hemorrhage.Builder(api),
                () -> new LifeReap.Builder(api),
                () -> new LifeSteal.Builder(api),
                // Lightning
                () -> new ChainLightning.Builder(api),
                () -> new LightningArrow.Builder(api),
                () -> new LightningBolt.Builder(api),
                () -> new LightningStorm.Builder(api),
                () -> new LittleSpark.Builder(api),
                () -> new SolarLance.Builder(api),
                () -> new Spark.Builder(api),
                () -> new ThunderBlast.Builder(api),
                // Misc
                () -> new EmpireLaunch.Builder(api),
                () -> new EmpireLevitate.Builder(api),
                () -> new EtherealForm.Builder(api),
                () -> new ExplosionWave.Builder(api),
                // Movement
                () -> new BlinkStrike.Builder(api),
                () -> new EmpireEscape.Builder(api),
                () -> new Leap.Builder(api),
                () -> new SunburstStep.Builder(api),
                () -> new Teleport.Builder(api),
                // Poison
                () -> new CrimsonChains.Builder(api),
                () -> new MephidicReap.Builder(api),
                () -> new PoisonWave.Builder(api),
                () -> new SoulSever.Builder(api),
                // Projectile
                () -> new ArcaneOrb.Builder(api),
                () -> new MagicMissile.Builder(api),
                // Weather
                () -> new Gust.Builder(api),
                () -> new Tornado.Builder(api)).forEach(builderSupplier -> {
                    Spell.Builder<?> builder = builderSupplier.get();
                    Spell<?> spell = builder.build();

                    ReadableConfig spellCfg = bukkitSpells.getConfigurationSection(spell.key());
                    if (spellCfg != null) {
                        spell.loadConfig(spellCfg);
                    }

                    spells.put(spell.key(), spell);
                });
    }

    // ===== SpellRegistry API =====

    @Override
    public @NotNull Optional<Spell<?>> getSpell(@NotNull String key) {
        return Optional.ofNullable(spells.get(key));
    }

    @Override
    public @NotNull Map<String, Spell<?>> getAllSpells() {
        return Collections.unmodifiableMap(spells);
    }

    @Override
    public @NotNull Set<String> getSpellKeys() {
        return Collections.unmodifiableSet(spells.keySet());
    }

    @Override
    public boolean isSpellRegistered(@NotNull String key) {
        return spells.containsKey(key);
    }

    @Override
    public @NotNull String getSpellDisplayName(@NotNull String key) {
        return getSpell(key)
                .map(s -> PlainTextComponentSerializer.plainText().serialize(s.displayName()))
                .orElse(key);
    }

    @Override
    public @NotNull SpellBuilder createSpell(@NotNull String key) {
        throw new UnsupportedOperationException("Custom spell creation is not supported.");
    }

    @Override
    public boolean registerSpell(@NotNull Spell<?> spell) {
        return spells.putIfAbsent(spell.key(), spell) == null;
    }

    @Override
    public boolean unregisterSpell(@NotNull String key) {
        return spells.remove(key) != null;
    }

    @Override
    public @NotNull Optional<SpellMetadata> getSpellMetadata(@NotNull String key) {
        return getSpell(key).map(BasicSpellMetadata::new);
    }

    @Override
    public boolean updateSpellMetadata(@NotNull String key, @NotNull SpellMetadata metadata) {
        return false; // Not supported
    }

    @Override
    public @NotNull Set<String> getSpellCategories() {
        return spells.values().stream()
                .map(s -> s.type().name())
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public @NotNull Set<String> getSpellsByCategory(@NotNull String category) {
        return spells.values().stream()
                .filter(s -> s.type().name().equalsIgnoreCase(category))
                .map(Spell::key)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public @NotNull Set<String> getSpellsByTag(@NotNull String tag) {
        return Set.of();
    }

    @Override
    public @NotNull Set<String> getSpellTags() {
        return Set.of();
    }

    @Override
    public @NotNull List<Spell<?>> findSpells(@NotNull SpellQuery query) {
        Stream<Spell<?>> stream = spells.values().stream();

        if (query.getCategory() != null) {
            stream = stream.filter(s -> s.type().name().equalsIgnoreCase(query.getCategory()));
        }
        final String nameContains = query.getNameContains();
        if (nameContains != null && !nameContains.isBlank()) {
            final String q = nameContains.toLowerCase();
            stream = stream.filter(
                    s -> PlainTextComponentSerializer.plainText().serialize(s.displayName())
                            .toLowerCase().contains(q));
        }
        if (query.getMaxCooldown() >= 0) {
            stream = stream.filter(s -> s.getCooldown().toMillis() / 50 <= query.getMaxCooldown());
        }
        // (Tags/range/level/enabled/filtering kun je hier later toevoegen)

        // Sorteren
        var sortField = query.getSortField();
        if (sortField != null) {
            java.util.Comparator<Spell<?>> cmp;
            switch (sortField) {
                case NAME -> cmp = java.util.Comparator
                        .comparing(s -> PlainTextComponentSerializer.plainText().serialize(s.displayName()));
                case COOLDOWN -> cmp = java.util.Comparator.comparingLong(s -> s.getCooldown().toMillis());
                case RANGE -> cmp = java.util.Comparator.comparingDouble(s -> 0.0);
                case LEVEL_REQUIREMENT -> cmp = java.util.Comparator.comparingInt(s -> 0);
                case CATEGORY -> cmp = java.util.Comparator.comparing(s -> s.type().name());
                default -> cmp = java.util.Comparator.comparing(s -> s.key());
            }
            if (query.getSortOrder() == SpellQuery.SortOrder.DESCENDING) {
                cmp = cmp.reversed();
            }
            stream = stream.sorted(cmp);
        }

        var list = stream.toList();
        if (query.getLimit() > 0 && query.getLimit() < list.size()) {
            return list.subList(0, query.getLimit());
        }
        return list;
    }

    @Override
    public @NotNull SpellQuery.Builder createQuery() {
        return new SpellQueryBuilderImpl();
    }

    @Override
    public int getSpellCount() {
        return spells.size();
    }

    @Override
    public int getSpellCountByCategory(@NotNull String category) {
        return (int) spells.values().stream()
                .filter(s -> s.type().name().equalsIgnoreCase(category))
                .count();
    }

    @Override
    public int getEnabledSpellCount() {
        return spells.size(); // Assuming all enabled
    }

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
        return true;
    }

    @Override
    public ServiceHealth getHealth() {
        return ServiceHealth.HEALTHY;
    }

    @Override
    public void reload() {
        spells.clear();
        registerAllSpells();
    }

    /**
     * Shuts down the spell registry and cleans up resources
     */
    public void shutdown() {
        spells.clear();
    }

    // ===== Toggleable Spell Methods =====

    @Override
    @NotNull
    public Optional<ToggleableSpell> getToggleableSpell(@NotNull String key) {
        Spell<?> spell = spells.get(key);
        if (spell instanceof ToggleableSpell) {
            return Optional.of((ToggleableSpell) spell);
        }
        return Optional.empty();
    }

    @Override
    @NotNull
    public Map<String, ToggleableSpell> getAllToggleableSpells() {
        Map<String, ToggleableSpell> toggleableSpells = new HashMap<>();
        for (Map.Entry<String, Spell<?>> entry : spells.entrySet()) {
            if (entry.getValue() instanceof ToggleableSpell) {
                toggleableSpells.put(entry.getKey(), (ToggleableSpell) entry.getValue());
            }
        }
        return Collections.unmodifiableMap(toggleableSpells);
    }

    @Override
    @NotNull
    public Set<String> getToggleableSpellKeys() {
        return spells.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof ToggleableSpell)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isToggleableSpell(@NotNull String key) {
        Spell<?> spell = spells.get(key);
        return spell instanceof ToggleableSpell;
    }

    @Override
    public int getToggleableSpellCount() {
        return (int) spells.values().stream()
                .filter(spell -> spell instanceof ToggleableSpell)
                .count();
    }

    // ===== Metadata wrapper =====

    /**
     * A basic implementation of {@link SpellMetadata} that wraps a {@link Spell}
     * instance.
     */
    private static class BasicSpellMetadata implements SpellMetadata {
        private final Spell<?> spell;

        public BasicSpellMetadata(Spell<?> spell) {
            this.spell = spell;
        }

        @Override
        public String getKey() {
            return spell.key();
        }

        @Override
        public Component getDisplayName() {
            return spell.displayName();
        }

        @Override
        public Component getDescription() {
            return Component.text(spell.getDescription());
        }

        @Override
        public String getCategory() {
            return spell.type().name();
        }

        @Override
        public Set<String> getTags() {
            return Set.of();
        }

        @Override
        public long getCooldownTicks() {
            return spell.getCooldown().toMillis() / 50;
        }

        @Override
        public double getRange() {
            return 0;
        }

        @Override
        public int getLevelRequirement() {
            return 0;
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
    }

    // ===== Query builder & impl =====

    /**
     * A builder for creating {@link SpellQuery} instances.
     */
    private class SpellQueryBuilderImpl implements SpellQuery.Builder {
        private String category;
        private String tag;
        private String nameContains;
        private long maxCooldown = -1;
        private double minRange = -1;
        private double maxRange = -1;
        private int maxLevelRequirement = -1;
        private Boolean enabled;
        private SpellQuery.SortField sortField;
        private SpellQuery.SortOrder sortOrder = SpellQuery.SortOrder.ASCENDING;
        private int limit = -1;

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
            return new SpellQueryImpl(this, SpellRegistryImpl.this);
        }
    }

    /**
     * An implementation of {@link SpellQuery} that holds the query parameters.
     */
    private record SpellQueryImpl(SpellQueryBuilderImpl builder, SpellRegistryImpl registry) implements SpellQuery {
        @Override
        public String getCategory() {
            return builder.category;
        }

        @Override
        public String getTag() {
            return builder.tag;
        }

        @Override
        public String getNameContains() {
            return builder.nameContains;
        }

        @Override
        public long getMaxCooldown() {
            return builder.maxCooldown;
        }

        @Override
        public double getMinRange() {
            return builder.minRange;
        }

        @Override
        public double getMaxRange() {
            return builder.maxRange;
        }

        @Override
        public int getMaxLevelRequirement() {
            return builder.maxLevelRequirement;
        }

        @Override
        public Boolean isEnabled() {
            return builder.enabled;
        }

        @Override
        public SortField getSortField() {
            return builder.sortField;
        }

        @Override
        public SortOrder getSortOrder() {
            return builder.sortOrder;
        }

        @Override
        public int getLimit() {
            return builder.limit;
        }

        @Override
        public List<Spell<?>> execute() {
            return registry.findSpells(this);
        }
    }
}
