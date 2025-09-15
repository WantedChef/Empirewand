package nl.wantedchef.empirewand.framework.service;

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
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

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
import nl.wantedchef.empirewand.spell.dark.Pain;
import nl.wantedchef.empirewand.spell.dark.RitualOfUnmaking;
import nl.wantedchef.empirewand.spell.dark.ShadowStep;
import nl.wantedchef.empirewand.spell.dark.Soulburn;
import nl.wantedchef.empirewand.spell.dark.Torture;
import nl.wantedchef.empirewand.spell.dark.Vengeance;
import nl.wantedchef.empirewand.spell.dark.VoidSwap;
import nl.wantedchef.empirewand.spell.dark.Wither;
import nl.wantedchef.empirewand.spell.defensive.Absorb;
import nl.wantedchef.empirewand.spell.defensive.Invincible;
import nl.wantedchef.empirewand.spell.defensive.Reflect;
import nl.wantedchef.empirewand.spell.defensive.Shell;
import nl.wantedchef.empirewand.spell.defensive.Silence;
import nl.wantedchef.empirewand.spell.earth.EarthQuake;
import nl.wantedchef.empirewand.spell.earth.GraspingVines;
import nl.wantedchef.empirewand.spell.earth.Lightwall;
import nl.wantedchef.empirewand.spell.earth.Platform;
import nl.wantedchef.empirewand.spell.earth.Sandblast;
import nl.wantedchef.empirewand.spell.earth.Stash;
import nl.wantedchef.empirewand.spell.earth.Tremor;
import nl.wantedchef.empirewand.spell.earth.Wall;
import nl.wantedchef.empirewand.spell.fire.basic.BlazeLaunch;
import nl.wantedchef.empirewand.spell.fire.basic.Burn;
import nl.wantedchef.empirewand.spell.fire.advanced.CometShower;
import nl.wantedchef.empirewand.spell.fire.advanced.EmpireComet;
import nl.wantedchef.empirewand.spell.fire.effects.ExplosionTrail;
import nl.wantedchef.empirewand.spell.fire.basic.Explosive;
import nl.wantedchef.empirewand.spell.fire.advanced.FlameWave;
import nl.wantedchef.empirewand.spell.fire.advanced.Flamewalk;
import nl.wantedchef.empirewand.spell.fire.advanced.Inferno;
import nl.wantedchef.empirewand.spell.fire.basic.Lava;
import nl.wantedchef.empirewand.spell.fire.advanced.MagmaWave;
import nl.wantedchef.empirewand.spell.heal.DivineHeal;
import nl.wantedchef.empirewand.spell.heal.Heal;
import nl.wantedchef.empirewand.spell.heal.Prayer;
import nl.wantedchef.empirewand.spell.heal.RadiantBeacon;
import nl.wantedchef.empirewand.spell.heal.Repair;
import nl.wantedchef.empirewand.spell.heal.Restoration;
import nl.wantedchef.empirewand.spell.heal.SuperHeal;
import nl.wantedchef.empirewand.spell.ice.ArcticBlast;
import nl.wantedchef.empirewand.spell.ice.FreezeRay;
import nl.wantedchef.empirewand.spell.ice.FrostNova;
import nl.wantedchef.empirewand.spell.ice.Frostwalk;
import nl.wantedchef.empirewand.spell.ice.GlacialSpike;
import nl.wantedchef.empirewand.spell.ice.IceWall;
import nl.wantedchef.empirewand.spell.ice.IceWave;
import nl.wantedchef.empirewand.spell.ice.Petrify;
import nl.wantedchef.empirewand.spell.life.BloodBarrier;
import nl.wantedchef.empirewand.spell.life.BloodBlock;
import nl.wantedchef.empirewand.spell.life.BloodNova;
import nl.wantedchef.empirewand.spell.life.BloodSpam;
import nl.wantedchef.empirewand.spell.life.BloodTap;
import nl.wantedchef.empirewand.spell.life.Hemorrhage;
import nl.wantedchef.empirewand.spell.life.LifeReap;
import nl.wantedchef.empirewand.spell.life.LifeSteal;
import nl.wantedchef.empirewand.spell.life.Lifewalk;
import nl.wantedchef.empirewand.spell.life.NatureGrowth;
import nl.wantedchef.empirewand.spell.life.Pollinate;
import nl.wantedchef.empirewand.spell.life.Regenerate;
import nl.wantedchef.empirewand.spell.life.Tame;
import nl.wantedchef.empirewand.spell.lightning.basic.ChainLightning;
import nl.wantedchef.empirewand.spell.lightning.advanced.LightningArrow;
import nl.wantedchef.empirewand.spell.lightning.basic.LightningBolt;
import nl.wantedchef.empirewand.spell.lightning.basic.LittleSpark;
import nl.wantedchef.empirewand.spell.lightning.advanced.SolarLance;
import nl.wantedchef.empirewand.spell.lightning.basic.Spark;
import nl.wantedchef.empirewand.spell.lightning.basic.ThunderBlast;
import nl.wantedchef.empirewand.spell.misc.EmpireLaunch;
import nl.wantedchef.empirewand.spell.misc.EmpireLevitate;
import nl.wantedchef.empirewand.spell.misc.Empower;
import nl.wantedchef.empirewand.spell.misc.EtherealForm;
import nl.wantedchef.empirewand.spell.misc.Gate;
import nl.wantedchef.empirewand.spell.misc.Invulnerability;
import nl.wantedchef.empirewand.spell.misc.MagicTorch;
import nl.wantedchef.empirewand.spell.misc.Mana;
import nl.wantedchef.empirewand.spell.movement.teleport.BlinkStrike;
import nl.wantedchef.empirewand.spell.movement.teleport.EmpireEscape;
import nl.wantedchef.empirewand.spell.movement.flight.Leap;
import nl.wantedchef.empirewand.spell.movement.flight.Levitate;
import nl.wantedchef.empirewand.spell.movement.flight.Lift;
import nl.wantedchef.empirewand.spell.movement.teleport.Phase;
import nl.wantedchef.empirewand.spell.movement.teleport.Recall;
import nl.wantedchef.empirewand.spell.movement.flight.Rocket;
import nl.wantedchef.empirewand.spell.movement.flight.SunburstStep;
import nl.wantedchef.empirewand.spell.poison.CrimsonChains;
import nl.wantedchef.empirewand.spell.poison.MephidicReap;
import nl.wantedchef.empirewand.spell.poison.PoisonWave;
import nl.wantedchef.empirewand.spell.poison.SoulSever;
import nl.wantedchef.empirewand.spell.projectile.ArcaneOrb;
import nl.wantedchef.empirewand.spell.projectile.MagicMissile;
import nl.wantedchef.empirewand.spell.toggle.aura.Aura;
import nl.wantedchef.empirewand.spell.toggle.aura.EmpireAura;
import nl.wantedchef.empirewand.spell.toggle.godmode.Elementosgod;
import nl.wantedchef.empirewand.spell.toggle.movement.AngelWings;
import nl.wantedchef.empirewand.spell.toggle.movement.CrystalGlide;
import nl.wantedchef.empirewand.spell.toggle.movement.DragonFury;
import nl.wantedchef.empirewand.spell.toggle.movement.KajCloud;
import nl.wantedchef.empirewand.spell.toggle.movement.MephiCloud;
import nl.wantedchef.empirewand.spell.toggle.movement.PhoenixCloak;
import nl.wantedchef.empirewand.spell.toggle.movement.ShadowCloak;
import nl.wantedchef.empirewand.spell.toggle.movement.StormRider;
import nl.wantedchef.empirewand.spell.toggle.movement.VoidWalk;
import nl.wantedchef.empirewand.spell.weather.ClearSkies;
import nl.wantedchef.empirewand.spell.weather.Gust;
import nl.wantedchef.empirewand.spell.weather.HailStorm;
import nl.wantedchef.empirewand.spell.weather.RainDance;
import nl.wantedchef.empirewand.spell.weather.Tornado;

/**
 * The primary implementation of the {@link SpellRegistry}. This class is
 * responsible for registering, managing, and providing access to all available
 * spells.
 *
 * Optimized for performance with efficient data structures and caching.
 */
public class SpellRegistryImpl implements SpellRegistry {

    // Use ConcurrentHashMap for thread-safe operations
    private final Map<String, Spell<?>> spells = new ConcurrentHashMap<>(64); // Initial capacity to reduce resizing
    private final ConfigService configService;
    private final PerformanceMonitor performanceMonitor;
    private final Logger logger;
    private final Map<String, Set<String>> categorySpellCache = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> toggleableSpellCache = new ConcurrentHashMap<>();
    private volatile Set<String> cachedCategories = null;
    private final Map<String, SpellMetadata> metadataCache = new ConcurrentHashMap<>();
    private final Map<String, String> displayNameCache = new ConcurrentHashMap<>();

    /**
     * Constructs a new SpellRegistryImpl.
     *
     * @param configService The configuration service.
     */
    public SpellRegistryImpl(ConfigService configService) {
        this.configService = configService;
        this.logger = java.util.logging.Logger.getLogger("SpellRegistryImpl");
        this.performanceMonitor = new PerformanceMonitor(this.logger);
        registerAllSpells();
    }

    /**
     * Registers all the default spells.
     */
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "REC_CATCH_EXCEPTION",
        justification = "Intentional broad catch to ensure server stability; falls back to reduced spell set on any exception during registration.")
    private void registerAllSpells() {
        try (var timing = performanceMonitor.startTiming("SpellRegistryImpl.registerAllSpells", 100)) {
            timing.observe();

            // Validate spell configurations
            validateSpellConfigurations();

            final nl.wantedchef.empirewand.api.EmpireWandAPI api = null;
            final var readable = configService.getSpellsConfig();
            final ReadableConfig bukkitSpells = readable.getConfigurationSection("spells");
            @SuppressWarnings("unchecked")
            Supplier<Spell.Builder<?>>[] spellBuilders = new Supplier[] {
                // Aura
                () -> new Aura.Builder(api),
                () -> new EmpireAura.Builder(api),
                () -> new Elementosgod.Builder(api),
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
                // Fire - Standard versions
                () -> new BlazeLaunch.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.fire.basic.Comet.Builder(api),
                () -> new CometShower.Builder(api),
                () -> new EmpireComet.Builder(api),
                () -> new Explosive.Builder(api),
                () -> new ExplosionTrail.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.fire.basic.Fireball.Builder(api),
                () -> new FlameWave.Builder(api),
                // Fire - Enhanced versions
                () -> new nl.wantedchef.empirewand.spell.fire.basic.Comet.Builder(api)
                        .enhancementLevel(nl.wantedchef.empirewand.spell.fire.basic.Comet.EnhancementLevel.ENHANCED),
                () -> new nl.wantedchef.empirewand.spell.fire.basic.Fireball.Builder(api)
                        .enhancementLevel(nl.wantedchef.empirewand.spell.fire.basic.Fireball.EnhancementLevel.ENHANCED),
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
                () -> new nl.wantedchef.empirewand.spell.life.Bloodwave.Builder(api),
                () -> new LifeReap.Builder(api),
                () -> new LifeSteal.Builder(api),
                // Lightning
                () -> new ChainLightning.Builder(api),
                () -> new LightningArrow.Builder(api),
                () -> new LightningBolt.Builder(api),
                () -> new LittleSpark.Builder(api),
                () -> new SolarLance.Builder(api),
                () -> new Spark.Builder(api),
                () -> new ThunderBlast.Builder(api),
                // Misc
                () -> new EmpireLaunch.Builder(api),
                () -> new EmpireLevitate.Builder(api),
                () -> new EtherealForm.Builder(api),
                // Movement
                () -> new BlinkStrike.Builder(api),
                () -> new EmpireEscape.Builder(api),
                () -> new Leap.Builder(api),
                () -> new KajCloud.Builder(api),
                () -> new MephiCloud.Builder(api),
                () -> new ShadowCloak.Builder(api),
                () -> new AngelWings.Builder(api),
                () -> new CrystalGlide.Builder(api),
                () -> new DragonFury.Builder(api),
                () -> new PhoenixCloak.Builder(api),
                () -> new StormRider.Builder(api),
                () -> new VoidWalk.Builder(api),
                () -> new SunburstStep.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.movement.teleport.Teleport.Builder(api),
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
                () -> new ClearSkies.Builder(api),
                () -> new HailStorm.Builder(api),
                () -> new RainDance.Builder(api),
                // Enhanced Spells
                () -> new nl.wantedchef.empirewand.spell.enhanced.cosmic.TemporalStasis.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.destructive.LightningStorm.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.defensive.StoneFortress.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.cosmic.VoidZone.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.defensive.DivineAura.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.cosmic.HomingRockets.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.cosmic.GravityWell.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.cosmic.BlackHole.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.cosmic.TimeDilation.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.defensive.EnergyShield.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.enhanced.destructive.DragonsBreath.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.swarns.SummonSwarm.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.swarns.SummonSwarmFlame.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.swarns.SummonSwarmToxic.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.swarns.SummonSwarmArcane.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.swarns.SummonSwarmRadiant.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.swarns.SummonSwarmVoid.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.swarns.SummonSwarmWind.Builder(api),
                () -> new nl.wantedchef.empirewand.spell.swarns.SummonSwarmStone.Builder(api),

                // ========== 50 NEW AMAZING SPELLS FROM REAL EMPIREWAND ==========
                // Fire Spells (5 new)
                () -> new Flamewalk.Builder(api),
                () -> new Burn.Builder(api),
                () -> new Lava.Builder(api),
                () -> new Inferno.Builder(api),
                () -> new MagmaWave.Builder(api),

                // Ice Spells (6 new)
                () -> new Frostwalk.Builder(api),
                () -> new Petrify.Builder(api),
                () -> new IceWall.Builder(api),
                () -> new IceWave.Builder(api),
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

                // Defensive Spells (5 new)
                () -> new Shell.Builder(api),
                () -> new Silence.Builder(api),
                () -> new Invincible.Builder(api),
                () -> new Reflect.Builder(api),
                () -> new Absorb.Builder(api)
            };
            for (Supplier<Spell.Builder<?>> builderSupplier : spellBuilders) {
                Spell.Builder<?> builder = builderSupplier.get();
                Spell<?> spell = builder.build();
                if (bukkitSpells != null) {
                    ReadableConfig spellCfg = bukkitSpells.getConfigurationSection(spell.key());
                    if (spellCfg != null) {
                        spell.loadConfig(spellCfg);
                    }
                }
                spells.put(spell.key(), spell);
            }
            invalidateCaches();
        } catch (Exception e) {
            // Log the error before falling back to reduced spell set
            if (e instanceof ClassNotFoundException) {
                logger.warning("Failed to dynamically load spells due to missing classes. Using fallback spell set.");
                logger.fine("Missing class details: " + e.getMessage());
            } else if (e instanceof InstantiationException || e instanceof IllegalAccessException) {
                logger.warning("Failed to instantiate spell classes. Using fallback spell set.");
                logger.fine("Instantiation error: " + e.getMessage());
            } else {
                logger.severe("Unexpected error during spell registration. Using fallback spell set: " + e.getMessage());
                if (logger.isLoggable(java.util.logging.Level.FINE)) {
                    e.printStackTrace();
                }
            }

            // Fall back to reduced spell set
            try {
                for (Supplier<Spell.Builder<?>> builderSupplier : getFallbackSpellBuilders()) {
                    Spell.Builder<?> builder = builderSupplier.get();
                    Spell<?> spell = builder.build();
                    spells.put(spell.key(), spell);
                }
                invalidateCaches();
                logger.info("Fallback spell registration completed. " + spells.size() + " spells loaded.");
            } catch (Exception fallbackError) {
                logger.severe("Critical error: Even fallback spell registration failed: " + fallbackError.getMessage());
                fallbackError.printStackTrace();
            }
        }
    }

    /**
     * Fallback method for spell builder registration when reflection fails.
     */
    @SuppressWarnings("unchecked")
    private Supplier<Spell.Builder<?>>[] getFallbackSpellBuilders() {
        final nl.wantedchef.empirewand.api.EmpireWandAPI api = null;
        Supplier<Spell.Builder<?>>[] builders = new Supplier[] {
            // Aura
            () -> new Aura.Builder(api),
            () -> new EmpireAura.Builder(api),
            () -> new Elementosgod.Builder(api),
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
            // Fire - Standard versions
            () -> new BlazeLaunch.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.fire.basic.Comet.Builder(api),
            () -> new CometShower.Builder(api),
            () -> new EmpireComet.Builder(api),
            () -> new Explosive.Builder(api),
            () -> new ExplosionTrail.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.fire.basic.Fireball.Builder(api),
            () -> new FlameWave.Builder(api),
            // Fire - Enhanced versions
            () -> new nl.wantedchef.empirewand.spell.fire.basic.Comet.Builder(api)
                    .enhancementLevel(nl.wantedchef.empirewand.spell.fire.basic.Comet.EnhancementLevel.ENHANCED),
            () -> new nl.wantedchef.empirewand.spell.fire.basic.Fireball.Builder(api)
                    .enhancementLevel(nl.wantedchef.empirewand.spell.fire.basic.Fireball.EnhancementLevel.ENHANCED),
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
            () -> new nl.wantedchef.empirewand.spell.life.Bloodwave.Builder(api),
            () -> new LifeReap.Builder(api),
            () -> new LifeSteal.Builder(api),
            // Lightning
            () -> new ChainLightning.Builder(api),
            () -> new LightningArrow.Builder(api),
            () -> new LightningBolt.Builder(api),
            () -> new LittleSpark.Builder(api),
            () -> new SolarLance.Builder(api),
            () -> new Spark.Builder(api),
            () -> new ThunderBlast.Builder(api),
            // Misc
            () -> new EmpireLaunch.Builder(api),
            () -> new EmpireLevitate.Builder(api),
            () -> new EtherealForm.Builder(api),
            // Movement
            () -> new BlinkStrike.Builder(api),
            () -> new EmpireEscape.Builder(api),
            () -> new Leap.Builder(api),
            () -> new KajCloud.Builder(api),
            () -> new MephiCloud.Builder(api),
            () -> new ShadowCloak.Builder(api),
            () -> new AngelWings.Builder(api),
            () -> new CrystalGlide.Builder(api),
            () -> new DragonFury.Builder(api),
            () -> new PhoenixCloak.Builder(api),
            () -> new StormRider.Builder(api),
            () -> new VoidWalk.Builder(api),
            () -> new SunburstStep.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.movement.teleport.Teleport.Builder(api),
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
            () -> new ClearSkies.Builder(api),
            () -> new HailStorm.Builder(api),
            () -> new RainDance.Builder(api),
            // Enhanced Spells
            () -> new nl.wantedchef.empirewand.spell.enhanced.cosmic.TemporalStasis.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.enhanced.destructive.LightningStorm.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.enhanced.defensive.StoneFortress.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.enhanced.cosmic.VoidZone.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.enhanced.defensive.DivineAura.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.enhanced.cosmic.HomingRockets.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.enhanced.cosmic.GravityWell.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.enhanced.cosmic.BlackHole.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.enhanced.cosmic.TimeDilation.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.enhanced.defensive.EnergyShield.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.enhanced.destructive.DragonsBreath.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.swarns.SummonSwarm.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.swarns.SummonSwarmFlame.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.swarns.SummonSwarmToxic.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.swarns.SummonSwarmArcane.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.swarns.SummonSwarmRadiant.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.swarns.SummonSwarmVoid.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.swarns.SummonSwarmWind.Builder(api),
            () -> new nl.wantedchef.empirewand.spell.swarns.SummonSwarmStone.Builder(api),

            // ========== 50 NEW AMAZING SPELLS FROM REAL EMPIREWAND ==========
            // Fire Spells (5 new)
            () -> new Flamewalk.Builder(api),
            () -> new Burn.Builder(api),
            () -> new Lava.Builder(api),
            () -> new Inferno.Builder(api),
            () -> new MagmaWave.Builder(api),

            // Ice Spells (6 new)
            () -> new Frostwalk.Builder(api),
            () -> new Petrify.Builder(api),
            () -> new IceWall.Builder(api),
            () -> new IceWave.Builder(api),
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

            // Defensive Spells (5 new)
            () -> new Shell.Builder(api),
            () -> new Silence.Builder(api),
            () -> new Invincible.Builder(api),
            () -> new Reflect.Builder(api),
            () -> new Absorb.Builder(api)
        };
        return builders;
    }

    /**
     * Invalidates all cached data to force recalculation on next access.
     */
    private synchronized void invalidateCaches() {
        categorySpellCache.clear();
        toggleableSpellCache.clear();
        metadataCache.clear();
        displayNameCache.clear();
        cachedCategories = null;
    }

    // ===== SpellRegistry API =====
    @Override
    public @NotNull
    Optional<Spell<?>> getSpell(@NotNull String key) {
        try (var timing = performanceMonitor.startTiming("SpellRegistryImpl.getSpell", 5)) {
            timing.observe();
            return Optional.ofNullable(spells.get(key));
        }
    }

    @Override
    public @NotNull
    Map<String, Spell<?>> getAllSpells() {
        return Collections.unmodifiableMap(spells);
    }

    @Override
    public @NotNull
    Set<String> getSpellKeys() {
        return Collections.unmodifiableSet(spells.keySet());
    }

    @Override
    public boolean isSpellRegistered(@NotNull String key) {
        return spells.containsKey(key);
    }

    @Override
    public @NotNull
    String getSpellDisplayName(@NotNull String key) {
        // Check cache first
        String cached = displayNameCache.get(key);
        if (cached != null) {
            return cached;
        }

        try (var timing = performanceMonitor.startTiming("SpellRegistryImpl.getSpellDisplayName", 5)) {
            timing.observe();
            Spell<?> spell = spells.get(key);
            if (spell == null) {
                return key;
            }
            String displayName = PlainTextComponentSerializer.plainText().serialize(spell.displayName());
            // Cache the result
            displayNameCache.put(key, displayName);
            return displayName;
        }
    }

    @Override
    public @NotNull
    SpellRegistry.SpellBuilder createSpell(@NotNull String key) {
        throw new UnsupportedOperationException("Custom spell creation is not supported.");
    }

    @Override
    public boolean registerSpell(@NotNull Spell<?> spell) {
        try (var timing = performanceMonitor.startTiming("SpellRegistryImpl.registerSpell", 10)) {
            timing.observe();
            boolean result = spells.putIfAbsent(spell.key(), spell) == null;
            if (result) {
                // Invalidate caches when a new spell is registered
                invalidateCaches();
            }
            return result;
        }
    }

    @Override
    public boolean unregisterSpell(@NotNull String key) {
        try (var timing = performanceMonitor.startTiming("SpellRegistryImpl.unregisterSpell", 10)) {
            timing.observe();
            boolean result = spells.remove(key) != null;
            if (result) {
                // Invalidate caches when a spell is unregistered
                invalidateCaches();
            }
            return result;
        }
    }

    @Override
    public @NotNull
    Optional<SpellMetadata> getSpellMetadata(@NotNull String key) {
        // Check cache first
        SpellMetadata cached = metadataCache.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }

        try (var timing = performanceMonitor.startTiming("SpellRegistryImpl.getSpellMetadata", 5)) {
            timing.observe();
            Spell<?> spell = spells.get(key);
            if (spell == null) {
                return Optional.empty();
            }
            SpellMetadata metadata = new BasicSpellMetadata(spell);
            // Cache the result
            metadataCache.put(key, metadata);
            return Optional.of(metadata);
        }
    }

    @Override
    public boolean updateSpellMetadata(@NotNull String key, @NotNull SpellMetadata metadata) {
        return false; // Not supported
    }

    @Override
    public @NotNull
    Set<String> getSpellCategories() {
        // Use cached categories if available (double-checked locking pattern)
        Set<String> categories = cachedCategories;
        if (categories != null) {
            return categories;
        }

        synchronized (this) {
            // Check again inside synchronized block
            categories = cachedCategories;
            if (categories != null) {
                return categories;
            }

            try (var timing = performanceMonitor.startTiming("SpellRegistryImpl.getSpellCategories", 10)) {
                timing.observe();
                // Calculate and cache categories more efficiently
                Set<String> result = ConcurrentHashMap.newKeySet();
                for (Spell<?> spell : spells.values()) {
                    result.add(spell.type().name());
                }

                cachedCategories = Collections.unmodifiableSet(result);
                return cachedCategories;
            }
        }
    }

    @Override
    public @NotNull
    Set<String> getSpellsByCategory(@NotNull String category) {
        // Check cache first
        Set<String> cached = categorySpellCache.get(category);
        if (cached != null) {
            return cached;
        }

        try (var timing = performanceMonitor.startTiming("SpellRegistryImpl.getSpellsByCategory", 15)) {
            timing.observe();
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
        }
    }

    @Override
    public @NotNull
    Set<String> getSpellsByTag(@NotNull String tag) {
        return Set.of();
    }

    @Override
    public @NotNull
    Set<String> getSpellTags() {
        return Set.of();
    }

    @Override
    public @NotNull
    List<Spell<?>> findSpells(@NotNull SpellQuery query) {
        try (var timing = performanceMonitor.startTiming("SpellRegistryImpl.findSpells", 25)) {
            timing.observe();
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
                    case COOLDOWN ->
                        comparator = Comparator.comparingLong(s -> s.getCooldown().toMillis());
                    case RANGE ->
                        comparator = Comparator.comparingDouble(s -> 0.0);
                    case LEVEL_REQUIREMENT ->
                        comparator = Comparator.comparingInt(s -> 0);
                    case CATEGORY ->
                        comparator = Comparator.comparing(s -> s.type().name());
                    default ->
                        comparator = Comparator.comparing(Spell::key);
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
        }
    }

    @Override
    public @NotNull
    SpellQuery.Builder createQuery() {
        return new SpellQueryBuilderImpl();
    }

    @Override
    public int getSpellCount() {
        return spells.size();
    }

    @Override
    public int getSpellCountByCategory(@NotNull String category) {
        try (var timing = performanceMonitor.startTiming("SpellRegistryImpl.getSpellCountByCategory", 10)) {
            timing.observe();
            return (int) spells.values().stream()
                    .filter(s -> s.type().name().equalsIgnoreCase(category))
                    .count();
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
        try (var timing = performanceMonitor.startTiming("SpellRegistryImpl.reload", 200)) {
            timing.observe();
            spells.clear();
            invalidateCaches();
            registerAllSpells();
        }
    }

    /**
     * Shuts down the spell registry and cleans up resources. This method should
     * be called during plugin shutdown to prevent memory leaks.
     */
    public void shutdown() {
        try (var timing = performanceMonitor.startTiming("SpellRegistryImpl.shutdown", 50)) {
            timing.observe();
            spells.clear();
            invalidateCaches();
        }
    }

    /**
     * Gets performance metrics for this service.
     *
     * @return A string containing performance metrics.
     */
    public String getPerformanceMetrics() {
        return "Metrics not available.";
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
        try (var timing = performanceMonitor.startTiming("SpellRegistryImpl.getToggleableSpell", 5)) {
            timing.observe();
            Spell<?> spell = spells.get(key);
            if (spell instanceof ToggleableSpell toggleableSpell) {
                return Optional.of(toggleableSpell);
            }
            return Optional.empty();
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
                if (spell instanceof ToggleableSpell toggleableSpell) {
                    result.put(key, toggleableSpell);
                }
            }
            return Collections.unmodifiableMap(result);
        }

        try (var timing = performanceMonitor.startTiming("SpellRegistryImpl.getAllToggleableSpells", 15)) {
            timing.observe();
            // Calculate and cache
            Map<String, ToggleableSpell> toggleableSpells = new HashMap<>();
            Set<String> toggleableKeys = new HashSet<>();

            for (Map.Entry<String, Spell<?>> entry : spells.entrySet()) {
                if (entry.getValue() instanceof ToggleableSpell toggleableSpell) {
                    toggleableSpells.put(entry.getKey(), toggleableSpell);
                    toggleableKeys.add(entry.getKey());
                }
            }

            toggleableSpellCache.put("all", toggleableKeys);
            return Collections.unmodifiableMap(toggleableSpells);
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

        try (var timing = performanceMonitor.startTiming("SpellRegistryImpl.getToggleableSpellKeys", 15)) {
            timing.observe();
            // Calculate and cache
            Set<String> result = spells.entrySet().stream()
                    .filter(entry -> entry.getValue() instanceof ToggleableSpell)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            toggleableSpellCache.put("keys", result);
            return result;
        }
    }

    @Override
    public boolean isToggleableSpell(@NotNull String key) {
        try (var timing = performanceMonitor.startTiming("SpellRegistryImpl.isToggleableSpell", 5)) {
            timing.observe();
            Spell<?> spell = spells.get(key);
            return spell instanceof ToggleableSpell;
        }
    }

    @Override
    public int getToggleableSpellCount() {
        try (var timing = performanceMonitor.startTiming("SpellRegistryImpl.getToggleableSpellCount", 10)) {
            timing.observe();
            return (int) spells.values().stream()
                    .filter(spell -> spell instanceof ToggleableSpell)
                    .count();
        }
    }

    // ===== Metadata wrapper =====
    /**
     * A basic implementation of {@link SpellMetadata} that wraps a
     * {@link Spell} instance.
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

    /**
     * Validates spell configurations to ensure they have required fields.
     * Logs warnings for any configuration issues but doesn't prevent startup.
     */
    private void validateSpellConfigurations() {
        try {
            var spellsConfig = configService.getSpellsConfig();
            if (spellsConfig == null) {
                logger.warning("Spells configuration is null - spells may not work correctly");
                return;
            }

            var spellsSection = spellsConfig.getConfigurationSection("spells");
            if (spellsSection == null) {
                logger.warning("No 'spells' section found in spells.yml - no spells will be configured");
                return;
            }

            // Check for some critical spells that should exist
            String[] criticalSpells = {"empire-launch", "lightning-arrow", "summon-swarm", "aura"};
            for (String spellKey : criticalSpells) {
                var spellConfig = spellsSection.getConfigurationSection(spellKey);
                if (spellConfig == null) {
                    logger.warning("Critical spell '" + spellKey + "' not found in configuration");
                } else {
                    // Validate required fields
                    if (spellConfig.get("type") == null) {
                        logger.warning("Spell '" + spellKey + "' missing required 'type' field");
                    }
                    if (spellConfig.get("cooldown") == null) {
                        logger.warning("Spell '" + spellKey + "' missing required 'cooldown' field");
                    }
                }
            }

            logger.fine("Spell configuration validation completed");
        } catch (Exception e) {
            logger.warning("Error during spell configuration validation: " + e.getMessage());
        }
    }
}
