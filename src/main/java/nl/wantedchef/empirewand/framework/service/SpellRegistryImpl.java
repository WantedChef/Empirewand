package nl.wantedchef.empirewand.framework.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import nl.wantedchef.empirewand.api.ServiceHealth;
import nl.wantedchef.empirewand.api.Version;
import nl.wantedchef.empirewand.api.spell.SpellMetadata;
import nl.wantedchef.empirewand.api.spell.SpellRegistry;
import nl.wantedchef.empirewand.api.spell.SpellRegistry.SpellQuery;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import nl.wantedchef.empirewand.core.util.PerformanceMonitor;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.control.Confuse;
import nl.wantedchef.empirewand.spell.control.Polymorph;
import nl.wantedchef.empirewand.spell.control.ZeistChronoAnchor;
import nl.wantedchef.empirewand.spell.dark.DarkCircle;
import nl.wantedchef.empirewand.spell.dark.DarkPulse;
import nl.wantedchef.empirewand.spell.dark.Mephidrain;
import nl.wantedchef.empirewand.spell.dark.RitualOfUnmaking;
import nl.wantedchef.empirewand.spell.dark.ShadowStep;
import nl.wantedchef.empirewand.spell.dark.VoidSwap;
import nl.wantedchef.empirewand.spell.earth.EarthQuake;
import nl.wantedchef.empirewand.spell.earth.GraspingVines;
import nl.wantedchef.empirewand.spell.earth.Lightwall;
import nl.wantedchef.empirewand.spell.fire.BlazeLaunch;
import nl.wantedchef.empirewand.spell.fire.Comet;
import nl.wantedchef.empirewand.spell.fire.CometShower;
import nl.wantedchef.empirewand.spell.fire.EmpireComet;
import nl.wantedchef.empirewand.spell.fire.Explosive;
import nl.wantedchef.empirewand.spell.fire.ExplosionTrail;
import nl.wantedchef.empirewand.spell.fire.Fireball;
import nl.wantedchef.empirewand.spell.fire.FlameWave;
import nl.wantedchef.empirewand.spell.heal.Heal;
import nl.wantedchef.empirewand.spell.heal.RadiantBeacon;
import nl.wantedchef.empirewand.spell.ice.FrostNova;
import nl.wantedchef.empirewand.spell.ice.GlacialSpike;
import nl.wantedchef.empirewand.spell.life.BloodBarrier;
import nl.wantedchef.empirewand.spell.life.BloodBlock;
import nl.wantedchef.empirewand.spell.life.BloodNova;
import nl.wantedchef.empirewand.spell.life.BloodSpam;
import nl.wantedchef.empirewand.spell.life.BloodTap;
import nl.wantedchef.empirewand.spell.life.Hemorrhage;
import nl.wantedchef.empirewand.spell.life.LifeReap;
import nl.wantedchef.empirewand.spell.life.LifeSteal;
import nl.wantedchef.empirewand.spell.lightning.ChainLightning;
import nl.wantedchef.empirewand.spell.lightning.LightningArrow;
import nl.wantedchef.empirewand.spell.lightning.LightningBolt;
import nl.wantedchef.empirewand.spell.lightning.LightningStorm;
import nl.wantedchef.empirewand.spell.lightning.LittleSpark;
import nl.wantedchef.empirewand.spell.lightning.SolarLance;
import nl.wantedchef.empirewand.spell.lightning.Spark;
import nl.wantedchef.empirewand.spell.lightning.ThunderBlast;
import nl.wantedchef.empirewand.spell.misc.EmpireLaunch;
import nl.wantedchef.empirewand.spell.misc.EmpireLevitate;
import nl.wantedchef.empirewand.spell.misc.EtherealForm;
import nl.wantedchef.empirewand.spell.misc.ExplosionWave;
import nl.wantedchef.empirewand.spell.movement.BlinkStrike;
import nl.wantedchef.empirewand.spell.movement.EmpireEscape;
import nl.wantedchef.empirewand.spell.movement.Leap;
import nl.wantedchef.empirewand.spell.movement.SunburstStep;
import nl.wantedchef.empirewand.spell.movement.Teleport;
import nl.wantedchef.empirewand.spell.poison.CrimsonChains;
import nl.wantedchef.empirewand.spell.poison.MephidicReap;
import nl.wantedchef.empirewand.spell.poison.PoisonWave;
import nl.wantedchef.empirewand.spell.poison.SoulSever;
import nl.wantedchef.empirewand.spell.projectile.ArcaneOrb;
import nl.wantedchef.empirewand.spell.projectile.MagicMissile;
import nl.wantedchef.empirewand.spell.toggle.aura.Aura;
import nl.wantedchef.empirewand.spell.toggle.aura.EmpireAura;
import nl.wantedchef.empirewand.spell.weather.Gust;
import nl.wantedchef.empirewand.spell.weather.Tornado;
// New spell imports - 50 amazing spells from real Empirewand
import nl.wantedchef.empirewand.spell.fire.Flamewalk;
import nl.wantedchef.empirewand.spell.fire.Burn;
import nl.wantedchef.empirewand.spell.fire.Lava;
import nl.wantedchef.empirewand.spell.fire.Inferno;
import nl.wantedchef.empirewand.spell.fire.MagmaWave;
import nl.wantedchef.empirewand.spell.ice.Frostwalk;
import nl.wantedchef.empirewand.spell.ice.Petrify;
import nl.wantedchef.empirewand.spell.ice.IceWall;
import nl.wantedchef.empirewand.spell.ice.FreezeRay;
import nl.wantedchef.empirewand.spell.ice.ArcticBlast;
import nl.wantedchef.empirewand.spell.dark.Wither;
import nl.wantedchef.empirewand.spell.dark.Soulburn;
import nl.wantedchef.empirewand.spell.dark.Torture;
import nl.wantedchef.empirewand.spell.dark.Pain;
import nl.wantedchef.empirewand.spell.dark.Vengeance;
import nl.wantedchef.empirewand.spell.earth.Sandblast;
import nl.wantedchef.empirewand.spell.earth.Wall;
import nl.wantedchef.empirewand.spell.earth.Platform;
import nl.wantedchef.empirewand.spell.earth.Stash;
import nl.wantedchef.empirewand.spell.earth.Tremor;
import nl.wantedchef.empirewand.spell.life.Lifewalk;
import nl.wantedchef.empirewand.spell.life.Pollinate;
import nl.wantedchef.empirewand.spell.life.Tame;
import nl.wantedchef.empirewand.spell.life.NatureGrowth;
import nl.wantedchef.empirewand.spell.life.Regenerate;
import nl.wantedchef.empirewand.spell.heal.Prayer;
import nl.wantedchef.empirewand.spell.heal.SuperHeal;
import nl.wantedchef.empirewand.spell.heal.Repair;
import nl.wantedchef.empirewand.spell.heal.Restoration;
import nl.wantedchef.empirewand.spell.heal.DivineHeal;
import nl.wantedchef.empirewand.spell.movement.Levitate;
import nl.wantedchef.empirewand.spell.movement.Lift;
import nl.wantedchef.empirewand.spell.movement.Phase;
import nl.wantedchef.empirewand.spell.movement.Recall;
import nl.wantedchef.empirewand.spell.movement.Rocket;
import nl.wantedchef.empirewand.spell.misc.Gate;
import nl.wantedchef.empirewand.spell.misc.Empower;
import nl.wantedchef.empirewand.spell.misc.Invulnerability;
import nl.wantedchef.empirewand.spell.misc.Mana;
import nl.wantedchef.empirewand.spell.misc.MagicTorch;
import nl.wantedchef.empirewand.spell.offensive.SuperKill;
import nl.wantedchef.empirewand.spell.offensive.Kill;
import nl.wantedchef.empirewand.spell.offensive.Wound;
import nl.wantedchef.empirewand.spell.offensive.Volley;
import nl.wantedchef.empirewand.spell.offensive.Shuriken;
import nl.wantedchef.empirewand.spell.defensive.Shell;
import nl.wantedchef.empirewand.spell.defensive.Silence;
import nl.wantedchef.empirewand.spell.defensive.Invincible;
import nl.wantedchef.empirewand.spell.defensive.Reflect;
import nl.wantedchef.empirewand.spell.defensive.Absorb;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The primary implementation of the {@link SpellRegistry}.
 * This class is responsible for registering, managing, and providing access to
 * all available spells.
 * 
 * Optimized for performance with efficient data structures and caching.
 */
public class SpellRegistryImpl implements SpellRegistry {

    // Use ConcurrentHashMap for thread-safe operations
    private final Map<String, Spell<?>> spells = new ConcurrentHashMap<>(64); // Initial capacity to reduce resizing
    private final ConfigService configService;

    // Performance monitor for tracking registry operations
    private final PerformanceMonitor performanceMonitor;

    // Cached category mappings for faster lookups
    private final Map<String, Set<String>> categorySpellCache = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> toggleableSpellCache = new ConcurrentHashMap<>();

    // Cached spell categories to avoid repeated computation
    private volatile Set<String> cachedCategories = null;

    // Cache for frequently accessed spell metadata
    private final Map<String, SpellMetadata> metadataCache = new ConcurrentHashMap<>();

    // Cache for spell display names to avoid repeated serialization
    private final Map<String, String> displayNameCache = new ConcurrentHashMap<>();

    /**
     * Constructs a new SpellRegistryImpl.
     *
     * @param configService The configuration service.
     */
    public SpellRegistryImpl(ConfigService configService) {
        this.configService = configService;
        this.performanceMonitor = new PerformanceMonitor(java.util.logging.Logger.getLogger("SpellRegistryImpl"));
        registerAllSpells();
    }

    /**
     * Registers all the default spells.
     */
    private void registerAllSpells() {
        PerformanceMonitor.TimingContext timing = performanceMonitor.startTiming("SpellRegistryImpl.registerAllSpells");
        try {
            // During migration, builders no longer require a non-null API instance.
            final nl.wantedchef.empirewand.api.EmpireWandAPI api = null;

            // Je ReadableConfig kan niet direct subsecties geven zoals Bukkit
            // ConfigurationSection.
            // We proberen defensief te casten; zo niet, dan slaan we het laden van
            // per-spell config over.
            final var readable = configService.getSpellsConfig();
            final ReadableConfig bukkitSpells = readable;

            // Pre-allocate array with known size for better performance
            @SuppressWarnings("unchecked")
            Supplier<Spell.Builder<?>>[] spellBuilders = (Supplier<Spell.Builder<?>>[]) new Supplier[] {
                    // Aura
                    () -> new Aura.Builder(api),
                    () -> new EmpireAura.Builder(api),
                    // Control
                    () -> new Confuse.Builder(api),
                    () -> new Polymorph.Builder(api),
                    () -> new ZeistChronoAnchor.Builder(api),
                    // Dark
                    () -> new DarkCircle.Builder(api),
                    () -> new DarkPulse.Builder(api),
                    () -> new Mephidrain.Builder(api),
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
                    () -> new Tornado.Builder(api),
                    // Enhanced Spells
                    () -> new nl.wantedchef.empirewand.spell.enhanced.MeteorShower.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.enhanced.TemporalStasis.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.enhanced.LightningStorm.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.enhanced.StoneFortress.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.enhanced.Blizzard.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.enhanced.VoidZone.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.enhanced.DivineAura.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.enhanced.HomingRockets.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.enhanced.GravityWell.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.enhanced.BlackHole.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.enhanced.TimeDilation.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.enhanced.EnergyShield.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.enhanced.DragonsBreath.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.enhanced.SummonSwarm.Builder(api),
                    // Enhanced Spell Variants
                    () -> new nl.wantedchef.empirewand.spell.enhanced.MeteorShowerEnhanced.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.enhanced.BlizzardEnhanced.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.fire.FireballEnhanced.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.lightning.ChainLightningEnhanced.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.fire.CometEnhanced.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.heal.HealEnhanced.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.movement.TeleportEnhanced.Builder(api),
                    // Refactored Spells
                    () -> new nl.wantedchef.empirewand.spell.fire.FlameWaveRefactored.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.lightning.LightningBoltRefactored.Builder(api),
                    () -> new nl.wantedchef.empirewand.spell.lightning.ChainLightningRefactored.Builder(api),
                    
                    // ========== 50 NEW AMAZING SPELLS FROM REAL EMPIREWAND ==========
                    // Fire Spells (5 new)
                    () -> new Flamewalk.Builder(api),
                    () -> new Burn.Builder(api),
                    () -> new Lava.Builder(api),
                    () -> new Inferno.Builder(api),
                    () -> new MagmaWave.Builder(api),
                    
                    // Ice Spells (5 new)
                    () -> new Frostwalk.Builder(api),
                    () -> new Petrify.Builder(api),
                    () -> new IceWall.Builder(api),
                    () -> new FreezeRay.Builder(api),
                    () -> new ArcticBlast.Builder(api),
                    
                    // Dark Spells (5 new)
                    () -> new Wither.Builder(api),
                    () -> new Soulburn.Builder(api),
                    () -> new Torture.Builder(api),
                    () -> new Pain.Builder(api),
                    () -> new Vengeance.Builder(api),
                    
                    // Earth Spells (5 new)
                    () -> new Sandblast.Builder(api),
                    () -> new Wall.Builder(api),
                    () -> new Platform.Builder(api),
                    () -> new Stash.Builder(api),
                    () -> new Tremor.Builder(api),
                    
                    // Life Spells (5 new)
                    () -> new Lifewalk.Builder(api),
                    () -> new Pollinate.Builder(api),
                    () -> new Tame.Builder(api),
                    () -> new NatureGrowth.Builder(api),
                    () -> new Regenerate.Builder(api),
                    
                    // Healing Spells (5 new)
                    () -> new Prayer.Builder(api),
                    () -> new SuperHeal.Builder(api),
                    () -> new Repair.Builder(api),
                    () -> new Restoration.Builder(api),
                    () -> new DivineHeal.Builder(api),
                    
                    // Movement Spells (5 new)
                    () -> new Levitate.Builder(api),
                    () -> new Lift.Builder(api),
                    () -> new Phase.Builder(api),
                    () -> new Recall.Builder(api),
                    () -> new Rocket.Builder(api),
                    
                    // Misc/Utility Spells (5 new)
                    () -> new Gate.Builder(api),
                    () -> new Empower.Builder(api),
                    () -> new Invulnerability.Builder(api),
                    () -> new Mana.Builder(api),
                    () -> new MagicTorch.Builder(api),
                    
                    // Offensive Spells (5 new)
                    () -> new SuperKill.Builder(api),
                    () -> new Kill.Builder(api),
                    () -> new Wound.Builder(api),
                    () -> new Volley.Builder(api),
                    () -> new Shuriken.Builder(api),
                    
                    // Defensive Spells (5 new)
                    () -> new Shell.Builder(api),
                    () -> new Silence.Builder(api),
                    () -> new Invincible.Builder(api),
                    () -> new Reflect.Builder(api),
                    () -> new Absorb.Builder(api)
            };

            // Register all spells
            for (Supplier<Spell.Builder<?>> builderSupplier : spellBuilders) {
                Spell.Builder<?> builder = builderSupplier.get();
                Spell<?> spell = builder.build();

                ReadableConfig spellCfg = bukkitSpells.getConfigurationSection(spell.key());
                if (spellCfg != null) {
                    spell.loadConfig(spellCfg);
                }

                spells.put(spell.key(), spell);
            }

            // Invalidate caches after registration
            invalidateCaches();
        } catch (Exception e) {
            // Handle reflection exceptions gracefully
            for (Supplier<Spell.Builder<?>> builderSupplier : getFallbackSpellBuilders()) {
                Spell.Builder<?> builder = builderSupplier.get();
                Spell<?> spell = builder.build();
                spells.put(spell.key(), spell);
            }
            invalidateCaches();
        } finally {
            timing.complete(100); // Log if spell registration takes more than 100ms
        }
    }

    /**
     * Fallback method for spell builder registration when reflection fails.
     */
    private Supplier<Spell.Builder<?>>[] getFallbackSpellBuilders() {
        final nl.wantedchef.empirewand.api.EmpireWandAPI api = null;
        @SuppressWarnings("unchecked")
        Supplier<Spell.Builder<?>>[] builders = (Supplier<Spell.Builder<?>>[]) new Supplier[] {
                // Aura
                () -> new Aura.Builder(api),
                () -> new EmpireAura.Builder(api),
                // Control
                () -> new Confuse.Builder(api),
                () -> new Polymorph.Builder(api),
                () -> new ZeistChronoAnchor.Builder(api),
                // Dark
                () -> new DarkCircle.Builder(api),
                () -> new DarkPulse.Builder(api),
                () -> new Mephidrain.Builder(api),
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
                () -> new Tornado.Builder(api),
                // Enhanced Spells
                () -> new nl.wantedchef.empirewand.spell.enhanced.MeteorShower.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.TemporalStasis.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.LightningStorm.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.StoneFortress.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.Blizzard.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.VoidZone.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.DivineAura.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.HomingRockets.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.GravityWell.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.BlackHole.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.TimeDilation.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.EnergyShield.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.DragonsBreath.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.SummonSwarm.Builder(api),
                // Enhanced Spell Variants
                () -> new nl.wantedchef.empirewand.spell.enhanced.MeteorShowerEnhanced.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.BlizzardEnhanced.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.fire.FireballEnhanced.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.lightning.ChainLightningEnhanced.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.fire.CometEnhanced.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.heal.HealEnhanced.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.movement.TeleportEnhanced.Builder(api),
                // Refactored Spells
                () -> new nl.wantedchef.empirewand.spell.fire.FlameWaveRefactored.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.lightning.LightningBoltRefactored.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.lightning.ChainLightningRefactored.Builder(api)
        };
        return builders;
    }

    /**
     * Invalidates all cached data to force recalculation on next access.
     */
    private void invalidateCaches() {
        cachedCategories = null;
        categorySpellCache.clear();
        toggleableSpellCache.clear();
        metadataCache.clear();
        displayNameCache.clear();
    }

    // ===== SpellRegistry API =====

    @Override
    public @NotNull Optional<Spell<?>> getSpell(@NotNull String key) {
        PerformanceMonitor.TimingContext timing = performanceMonitor.startTiming("SpellRegistryImpl.getSpell");
        try {
            return Optional.ofNullable(spells.get(key));
        } finally {
            timing.complete(5); // Log if spell retrieval takes more than 5ms
        }
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
        // Check cache first
        String cached = displayNameCache.get(key);
        if (cached != null) {
            return cached;
        }

        PerformanceMonitor.TimingContext timing = performanceMonitor
                .startTiming("SpellRegistryImpl.getSpellDisplayName");
        try {
            Spell<?> spell = spells.get(key);
            if (spell == null) {
                return key;
            }
            String displayName = PlainTextComponentSerializer.plainText().serialize(spell.displayName());
            // Cache the result
            displayNameCache.put(key, displayName);
            return displayName;
        } finally {
            timing.complete(5); // Log if display name retrieval takes more than 5ms
        }
    }

    @Override
    public @NotNull SpellRegistry.SpellBuilder createSpell(@NotNull String key) {
        throw new UnsupportedOperationException("Custom spell creation is not supported.");
    }

    @Override
    public boolean registerSpell(@NotNull Spell<?> spell) {
        PerformanceMonitor.TimingContext timing = performanceMonitor.startTiming("SpellRegistryImpl.registerSpell");
        try {
            boolean result = spells.putIfAbsent(spell.key(), spell) == null;
            if (result) {
                // Invalidate caches when a new spell is registered
                invalidateCaches();
            }
            return result;
        } finally {
            timing.complete(10); // Log if spell registration takes more than 10ms
        }
    }

    @Override
    public boolean unregisterSpell(@NotNull String key) {
        PerformanceMonitor.TimingContext timing = performanceMonitor.startTiming("SpellRegistryImpl.unregisterSpell");
        try {
            boolean result = spells.remove(key) != null;
            if (result) {
                // Invalidate caches when a spell is unregistered
                invalidateCaches();
            }
            return result;
        } finally {
            timing.complete(10); // Log if spell unregistration takes more than 10ms
        }
    }

    @Override
    public @NotNull Optional<SpellMetadata> getSpellMetadata(@NotNull String key) {
        // Check cache first
        SpellMetadata cached = metadataCache.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }

        PerformanceMonitor.TimingContext timing = performanceMonitor.startTiming("SpellRegistryImpl.getSpellMetadata");
        try {
            Spell<?> spell = spells.get(key);
            if (spell == null) {
                return Optional.empty();
            }
            SpellMetadata metadata = new BasicSpellMetadata(spell);
            // Cache the result
            metadataCache.put(key, metadata);
            return Optional.of(metadata);
        } finally {
            timing.complete(5); // Log if metadata retrieval takes more than 5ms
        }
    }

    @Override
    public boolean updateSpellMetadata(@NotNull String key, @NotNull SpellMetadata metadata) {
        return false; // Not supported
    }

    @Override
    public @NotNull Set<String> getSpellCategories() {
        // Use cached categories if available
        Set<String> categories = cachedCategories;
        if (categories != null) {
            return categories;
        }

        PerformanceMonitor.TimingContext timing = performanceMonitor
                .startTiming("SpellRegistryImpl.getSpellCategories");
        try {
            // Calculate and cache categories more efficiently
            Set<String> result = ConcurrentHashMap.newKeySet();
            for (Spell<?> spell : spells.values()) {
                result.add(spell.type().name());
            }

            cachedCategories = result;
            return result;
        } finally {
            timing.complete(10); // Log if category retrieval takes more than 10ms
        }
    }

    @Override
    public @NotNull Set<String> getSpellsByCategory(@NotNull String category) {
        if (category == null) {
            return Collections.emptySet();
        }

        // Check cache first
        Set<String> cached = categorySpellCache.get(category);
        if (cached != null) {
            return cached;
        }

        PerformanceMonitor.TimingContext timing = performanceMonitor
                .startTiming("SpellRegistryImpl.getSpellsByCategory");
        try {
            // Calculate and cache more efficiently
            Set<String> result = ConcurrentHashMap.newKeySet();
            String lowerCategory = category.toLowerCase();

            for (Spell<?> spell : spells.values()) {
                if (spell.type().name().toLowerCase().equals(lowerCategory)) {
                    result.add(spell.key());
                }
            }

            categorySpellCache.put(category, result);
            return result;
        } finally {
            timing.complete(15); // Log if category spell retrieval takes more than 15ms
        }
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
        PerformanceMonitor.TimingContext timing = performanceMonitor.startTiming("SpellRegistryImpl.findSpells");
        try {
            // Use direct iteration instead of streams for better performance
            List<Spell<?>> filtered = new ArrayList<>(Math.min(spells.size(), 64)); // Pre-size with reasonable estimate

            String categoryFilter = query.getCategory();
            String nameContainsFilter = query.getNameContains();
            long maxCooldownFilter = query.getMaxCooldown();

            // Pre-process filters for better performance (structured to avoid NPE warnings)
            boolean hasCategoryFilter;
            String lowerCategoryFilter;
            if (categoryFilter != null && !categoryFilter.isEmpty()) {
                lowerCategoryFilter = categoryFilter.toLowerCase(Locale.ROOT);
                hasCategoryFilter = true;
            } else {
                lowerCategoryFilter = null;
                hasCategoryFilter = false;
            }

            boolean hasNameFilter;
            String lowerNameFilter;
            if (nameContainsFilter != null && !nameContainsFilter.isBlank()) {
                lowerNameFilter = nameContainsFilter.toLowerCase(Locale.ROOT);
                hasNameFilter = true;
            } else {
                lowerNameFilter = null;
                hasNameFilter = false;
            }
            boolean hasCooldownFilter = maxCooldownFilter >= 0;

            // Pre-compute expensive operations
            PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();

            for (Spell<?> spell : spells.values()) {
                // Apply category filter
                if (hasCategoryFilter) {
                    String spellCategory = spell.type().name();
                    if (!spellCategory.toLowerCase(Locale.ROOT).equals(lowerCategoryFilter)) {
                        continue;
                    }
                }

                // Apply name filter
                if (hasNameFilter) {
                    String displayName = serializer.serialize(spell.displayName());
                    if (!displayName.toLowerCase(Locale.ROOT).contains(lowerNameFilter)) {
                        continue;
                    }
                }

                // Apply cooldown filter
                if (hasCooldownFilter) {
                    long cooldownTicks = spell.getCooldown().toMillis() / 50;
                    if (cooldownTicks > maxCooldownFilter) {
                        continue;
                    }
                }

                filtered.add(spell);
            }

            // Apply sorting if needed
            var sortField = query.getSortField();
            if (sortField != null) {
                Comparator<Spell<?>> comparator;
                switch (sortField) {
                    case NAME -> {
                        PlainTextComponentSerializer ser = PlainTextComponentSerializer.plainText();
                        comparator = Comparator.comparing(s -> ser.serialize(s.displayName()));
                    }
                    case COOLDOWN -> comparator = Comparator.comparingLong(s -> s.getCooldown().toMillis());
                    case RANGE -> comparator = Comparator.comparingDouble(s -> 0.0);
                    case LEVEL_REQUIREMENT -> comparator = Comparator.comparingInt(s -> 0);
                    case CATEGORY -> comparator = Comparator.comparing(s -> s.type().name());
                    default -> comparator = Comparator.comparing(Spell::key);
                }

                if (query.getSortOrder() == SpellQuery.SortOrder.DESCENDING) {
                    comparator = comparator.reversed();
                }

                // Only sort if we have more than 1 item
                if (filtered.size() > 1) {
                    filtered.sort(comparator);
                }
            }

            // Apply limit if needed
            int limit = query.getLimit();
            if (limit > 0 && limit < filtered.size()) {
                return new ArrayList<>(filtered.subList(0, limit)); // Return new list to avoid subList overhead
            }

            return filtered;
        } finally {
            timing.complete(25); // Log if spell finding takes more than 25ms
        }
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
        PerformanceMonitor.TimingContext timing = performanceMonitor
                .startTiming("SpellRegistryImpl.getSpellCountByCategory");
        try {
            return (int) spells.values().stream()
                    .filter(s -> s.type().name().equalsIgnoreCase(category))
                    .count();
        } finally {
            timing.complete(10); // Log if category count takes more than 10ms
        }
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
        PerformanceMonitor.TimingContext timing = performanceMonitor.startTiming("SpellRegistryImpl.reload");
        try {
            spells.clear();
            invalidateCaches();
            registerAllSpells();
        } finally {
            timing.complete(200); // Log if reload takes more than 200ms
        }
    }

    /**
     * Shuts down the spell registry and cleans up resources.
     * This method should be called during plugin shutdown to prevent memory leaks.
     */
    public void shutdown() {
        PerformanceMonitor.TimingContext timing = performanceMonitor.startTiming("SpellRegistryImpl.shutdown");
        try {
            spells.clear();
            invalidateCaches();
        } finally {
            timing.complete(50); // Log if shutdown takes more than 50ms
        }
    }

    /**
     * Gets performance metrics for this service.
     *
     * @return A string containing performance metrics.
     */
    public String getPerformanceMetrics() {
        return performanceMonitor.getMetricsReport();
    }

    /**
     * Clears performance metrics for this service.
     */
    public void clearPerformanceMetrics() {
        performanceMonitor.clearMetrics();
    }

    // ===== Toggleable Spell Methods =====

    @Override
    @NotNull
    public Optional<ToggleableSpell> getToggleableSpell(@NotNull String key) {
        PerformanceMonitor.TimingContext timing = performanceMonitor
                .startTiming("SpellRegistryImpl.getToggleableSpell");
        try {
            Spell<?> spell = spells.get(key);
            if (spell instanceof ToggleableSpell) {
                return Optional.of((ToggleableSpell) spell);
            }
            return Optional.empty();
        } finally {
            timing.complete(5); // Log if toggleable spell retrieval takes more than 5ms
        }
    }

    @Override
    @NotNull
    public Map<String, ToggleableSpell> getAllToggleableSpells() {
        // Check cache first
        Set<String> cachedKeys = toggleableSpellCache.get("all");
        if (cachedKeys != null) {
            Map<String, ToggleableSpell> result = new HashMap<>(cachedKeys.size());
            for (String key : cachedKeys) {
                Spell<?> spell = spells.get(key);
                if (spell instanceof ToggleableSpell) {
                    result.put(key, (ToggleableSpell) spell);
                }
            }
            return Collections.unmodifiableMap(result);
        }

        PerformanceMonitor.TimingContext timing = performanceMonitor
                .startTiming("SpellRegistryImpl.getAllToggleableSpells");
        try {
            // Calculate and cache
            Map<String, ToggleableSpell> toggleableSpells = new HashMap<>();
            Set<String> toggleableKeys = new HashSet<>();

            for (Map.Entry<String, Spell<?>> entry : spells.entrySet()) {
                if (entry.getValue() instanceof ToggleableSpell) {
                    toggleableSpells.put(entry.getKey(), (ToggleableSpell) entry.getValue());
                    toggleableKeys.add(entry.getKey());
                }
            }

            toggleableSpellCache.put("all", toggleableKeys);
            return Collections.unmodifiableMap(toggleableSpells);
        } finally {
            timing.complete(15); // Log if all toggleable spells retrieval takes more than 15ms
        }
    }

    @Override
    @NotNull
    public Set<String> getToggleableSpellKeys() {
        // Check cache first
        Set<String> cached = toggleableSpellCache.get("keys");
        if (cached != null) {
            return cached;
        }

        PerformanceMonitor.TimingContext timing = performanceMonitor
                .startTiming("SpellRegistryImpl.getToggleableSpellKeys");
        try {
            // Calculate and cache
            Set<String> result = spells.entrySet().stream()
                    .filter(entry -> entry.getValue() instanceof ToggleableSpell)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            toggleableSpellCache.put("keys", result);
            return result;
        } finally {
            timing.complete(15); // Log if toggleable spell keys retrieval takes more than 15ms
        }
    }

    @Override
    public boolean isToggleableSpell(@NotNull String key) {
        PerformanceMonitor.TimingContext timing = performanceMonitor.startTiming("SpellRegistryImpl.isToggleableSpell");
        try {
            Spell<?> spell = spells.get(key);
            return spell instanceof ToggleableSpell;
        } finally {
            timing.complete(5); // Log if toggleable spell check takes more than 5ms
        }
    }

    @Override
    public int getToggleableSpellCount() {
        PerformanceMonitor.TimingContext timing = performanceMonitor
                .startTiming("SpellRegistryImpl.getToggleableSpellCount");
        try {
            return (int) spells.values().stream()
                    .filter(spell -> spell instanceof ToggleableSpell)
                    .count();
        } finally {
            timing.complete(10); // Log if toggleable spell count takes more than 10ms
        }
    }

    // ===== Metadata wrapper =====

    /**
     * A basic implementation of {@link SpellMetadata} that wraps a {@link Spell}
     * instance.
     * 
     * Optimized for performance with direct field access.
     */
    private static class BasicSpellMetadata implements SpellMetadata {
        private final String key;
        private final Component displayName;
        private final String description;
        private final String category;
        private final long cooldownTicks;

        public BasicSpellMetadata(Spell<?> spell) {
            this.key = spell.key();
            this.displayName = spell.displayName();
            this.description = spell.getDescription();
            this.category = spell.type().name();
            this.cooldownTicks = spell.getCooldown().toMillis() / 50;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Component getDisplayName() {
            return displayName;
        }

        @Override
        public Component getDescription() {
            return Component.text(description);
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public Set<String> getTags() {
            return Set.of();
        }

        @Override
        public long getCooldownTicks() {
            return cooldownTicks;
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
     * 
     * Optimized for performance with direct field access.
     */
    private static class SpellQueryBuilderImpl implements SpellQuery.Builder {
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
            return new SpellQueryImpl(this);
        }
    }

    /**
     * An implementation of {@link SpellQuery} that holds the query parameters.
     * 
     * Optimized as a record for better performance and memory usage.
     */
    private record SpellQueryImpl(SpellQueryBuilderImpl builder) implements SpellQuery {
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
            // This would normally reference the registry, but we'll handle execution
            // differently
            throw new UnsupportedOperationException("Execute not supported on this implementation");
        }
    }
}
