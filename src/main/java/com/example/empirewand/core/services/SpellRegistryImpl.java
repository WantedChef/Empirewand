package com.example.empirewand.core.services;

import com.example.empirewand.api.EmpireWandAPI;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Stream;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class SpellRegistryImpl implements SpellRegistry {

    private static final Logger LOGGER = Logger.getLogger(SpellRegistryImpl.class.getName());
    private final Map<String, Spell<?>> spells = new ConcurrentHashMap<>();
    private final EmpireWandAPI api;
    private final ConfigService configService;

    public SpellRegistryImpl(EmpireWandAPI api, ConfigService configService) {
        this.api = api;
        this.configService = configService;
        registerAllSpells();
    }

    private void registerAllSpells() {
        // Je ReadableConfig kan niet direct subsecties geven zoals Bukkit
        // ConfigurationSection.
        // We proberen defensief te casten; zo niet, dan slaan we het laden van
        // per-spell config over.
        final var readable = configService.getSpellsConfig();
        final ConfigurationSection bukkitSpells = (readable instanceof ConfigurationSection cs) ? cs : null;

        Stream.<Supplier<Spell.Builder<?>>>of(
                // Aura
                () -> new Aura.Builder(null),
                () -> new EmpireAura.Builder(null),
                // Control
                () -> new Confuse.Builder(null),
                () -> new Polymorph.Builder(null),
                () -> new StasisField.Builder(null),
                // Dark
                () -> new DarkCircle.Builder(null),
                () -> new DarkPulse.Builder(null),
                () -> new RitualOfUnmaking.Builder(null),
                () -> new ShadowCloak.Builder(null),
                () -> new ShadowStep.Builder(null),
                () -> new VoidSwap.Builder(null),
                // Earth
                () -> new EarthQuake.Builder(null),
                () -> new GraspingVines.Builder(null),
                () -> new Lightwall.Builder(null),
                // Fire
                () -> new BlazeLaunch.Builder(null),
                () -> new Comet.Builder(null),
                () -> new CometShower.Builder(null),
                () -> new EmpireComet.Builder(null),
                () -> new Explosive.Builder(null),
                () -> new ExplosionTrail.Builder(null),
                () -> new Fireball.Builder(null),
                () -> new FlameWave.Builder(null),
                // Heal
                () -> new GodCloud.Builder(null),
                () -> new Heal.Builder(null),
                () -> new RadiantBeacon.Builder(null),
                // Ice
                () -> new FrostNova.Builder(null),
                () -> new GlacialSpike.Builder(null),
                // Life
                () -> new BloodBarrier.Builder(null),
                () -> new BloodBlock.Builder(null),
                () -> new BloodNova.Builder(null),
                () -> new BloodSpam.Builder(null),
                () -> new BloodTap.Builder(null),
                () -> new Hemorrhage.Builder(null),
                () -> new LifeReap.Builder(null),
                () -> new LifeSteal.Builder(null),
                // Lightning
                () -> new ChainLightning.Builder(null),
                () -> new LightningArrow.Builder(null),
                () -> new LightningBolt.Builder(null),
                () -> new LightningStorm.Builder(null),
                () -> new LittleSpark.Builder(null),
                () -> new SolarLance.Builder(null),
                () -> new Spark.Builder(null),
                () -> new ThunderBlast.Builder(null),
                // Misc
                () -> new EmpireLaunch.Builder(null),
                () -> new EmpireLevitate.Builder(null),
                () -> new EtherealForm.Builder(null),
                () -> new ExplosionWave.Builder(null),
                // Movement
                () -> new BlinkStrike.Builder(null),
                () -> new EmpireEscape.Builder(null),
                () -> new Leap.Builder(null),
                () -> new SunburstStep.Builder(null),
                () -> new Teleport.Builder(null),
                // Poison
                () -> new CrimsonChains.Builder(null),
                () -> new MephidicReap.Builder(null),
                () -> new PoisonWave.Builder(null),
                () -> new SoulSever.Builder(null),
                // Projectile
                () -> new ArcaneOrb.Builder(null),
                () -> new MagicMissile.Builder(null),
                // Weather
                () -> new Gust.Builder(null),
                () -> new Tornado.Builder(null)).forEach(builderSupplier -> {
                    Spell<?> spell = builderSupplier.get().build();

                    if (bukkitSpells != null) {
                        ConfigurationSection spellCfg = bukkitSpells.getConfigurationSection(spell.key());
                        if (spellCfg != null) {
                            spell.loadConfig(spellCfg);
                        }
                    } else {
                        // Geen Bukkit-sectie beschikbaar – netjes melden en doorgaan.
                        LOGGER.fine(() -> "Skipping config load for spell '" + spell.key()
                                + "': spells config is not a Bukkit ConfigurationSection");
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
        return Set.of(); // TODO
    }

    @Override
    public @NotNull Set<String> getSpellsByCategory(@NotNull String category) {
        return Set.of(); // TODO
    }

    @Override
    public @NotNull Set<String> getSpellsByTag(@NotNull String tag) {
        return Set.of(); // TODO
    }

    @Override
    public @NotNull Set<String> getSpellTags() {
        return Set.of(); // TODO
    }

    @Override
    public @NotNull List<Spell<?>> findSpells(@NotNull SpellQuery query) {
        Stream<Spell<?>> stream = spells.values().stream();

        if (query.getCategory() != null) {
            stream = stream.filter(s -> s.type().name().equalsIgnoreCase(query.getCategory()));
        }
        if (query.getNameContains() != null && !query.getNameContains().isBlank()) {
            final String q = query.getNameContains().toLowerCase();
            stream = stream.filter(
                    s -> PlainTextComponentSerializer.plainText().serialize(s.displayName()).toLowerCase().contains(q));
        }
        if (query.getMaxCooldown() >= 0) {
            stream = stream.filter(s -> s.getCooldown().toMillis() / 50 <= query.getMaxCooldown());
        }
        // (Tags/range/level/enabled/filtering kun je hier later toevoegen)

        // Sorteren
        if (query.getSortField() != null) {
            java.util.Comparator<Spell<?>> cmp;
            if (query.getSortField() == SpellQuery.SortField.NAME) {
                cmp = java.util.Comparator
                        .comparing(s -> PlainTextComponentSerializer.plainText().serialize(s.displayName()));
            } else if (query.getSortField() == SpellQuery.SortField.COOLDOWN) {
                cmp = java.util.Comparator.comparingLong(s -> s.getCooldown().toMillis());
            } else if (query.getSortField() == SpellQuery.SortField.RANGE) {
                cmp = java.util.Comparator.comparingDouble(s -> 0.0);
            } else if (query.getSortField() == SpellQuery.SortField.LEVEL_REQUIREMENT) {
                cmp = java.util.Comparator.comparingInt(s -> 0);
            } else if (query.getSortField() == SpellQuery.SortField.CATEGORY) {
                cmp = java.util.Comparator.comparing(s -> s.type().name());
            } else {
                cmp = java.util.Comparator.comparing(s -> s.key());
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

    // ===== Metadata wrapper =====

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
