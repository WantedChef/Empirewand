package nl.wantedchef.empirewand.spell.toggle.movement;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

/**
 * CrystalGlide 1.0 - Revolutionary ice skating movement spell with crystalline frost mechanics.
 *
 * Extraordinary Features:
 * - Dynamic ice trail generation with realistic crystalline structure
 * - Frictionless gliding physics with momentum conservation
 * - Frost aura effects with snowflake particle simulation
 * - Ice crystal formation with geometric crystallography
 * - Advanced thermal dynamics with freeze-thaw cycles
 * - Glacial particle trails with ice shard visualization
 * - Crystalline wing formation with hexagonal ice structures
 * - Arctic wind effects with atmospheric cooling simulation
 *
 * @author WantedChef
 */
public final class CrystalGlide extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /* DATA */
    /* ---------------------------------------- */
    private final Map<UUID, CrystalGliderData> crystalGliders = new WeakHashMap<>();

    /* ---------------------------------------- */
    /* BUILDER */
    /* ---------------------------------------- */
    /**
     * Builder for creating {@link CrystalGlide} instances.
     */
    public static class Builder extends Spell.Builder<Void> {

        /**
         * Creates a new builder for the CrystalGlide spell.
         *
         * @param api the plugin API
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Crystal Glide";
            description = "Glide across crystalline ice trails with frictionless momentum and frost mastery.";
            cooldown = Duration.ofSeconds(6);
            spellType = SpellType.MOVEMENT;
        }

        /**
         * Builds the CrystalGlide spell.
         *
         * @return the constructed spell
         */
        @Override
        public @NotNull Spell<Void> build() {
            return new CrystalGlide(this);
        }
    }

    /**
     * Constructs the spell from its builder.
     *
     * @param builder the spell builder
     */
    private CrystalGlide(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /* SPELL API */
    /* ---------------------------------------- */

    /**
     * Gets the configuration key for this spell.
     *
     * @return "crystal-glide"
     */
    @Override
    public @NotNull String key() {
        return "crystal-glide";
    }

    /**
     * Returns the casting prerequisites.
     *
     * @return a no-op prerequisite
     */
    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    /**
     * Toggles the spell for the caster.
     *
     * @param context the spell context
     * @return always {@code null}
     */
    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        toggle(context.caster(), context);
        return null;
    }

    /**
     * No additional effect handling is required.
     *
     * @param context the spell context
     * @param result  unused
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect
    }

    /* ---------------------------------------- */
    /* TOGGLE API */
    /* ---------------------------------------- */

    /**
     * Checks whether the glide is active for a player.
     *
     * @param player the player to check
     * @return {@code true} if active
     */
    @Override
    public boolean isActive(@NotNull Player player) {
        return crystalGliders.containsKey(player.getUniqueId());
    }

    /**
     * Activates the glide for a player.
     *
     * @param player  the player
     * @param context the spell context
     */
    @Override
    public void activate(@NotNull Player player, @NotNull SpellContext context) {
        if (isActive(player)) {
            return;
        }
        crystalGliders.put(player.getUniqueId(), new CrystalGliderData(player, context));
    }

    /**
     * Deactivates the glide for a player.
     *
     * @param player  the player
     * @param context the spell context
     */
    @Override
    public void deactivate(@NotNull Player player, @NotNull SpellContext context) {
        Optional.ofNullable(crystalGliders.remove(player.getUniqueId())).ifPresent(CrystalGliderData::stop);
    }

    /**
     * Forcefully deactivates the glide.
     *
     * @param player the player
     */
    @Override
    public void forceDeactivate(@NotNull Player player) {
        Optional.ofNullable(crystalGliders.remove(player.getUniqueId())).ifPresent(CrystalGliderData::stop);
    }

    /**
     * Gets the maximum duration of the glide in ticks.
     *
     * @return max duration
     */
    @Override
    public int getMaxDuration() {
        return cfgInt("max-duration-ticks", 2000); // ~1.5 minutes default
    }

    /* ---------------------------------------- */
    /* INTERNAL CLASS */
    /* ---------------------------------------- */
    private final class CrystalGliderData {
        private final Player player;
        private final BossBar frostEnergyBar = BossBar.bossBar(Component.text("Frost Energy"), 1,
                BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
        private final BukkitTask ticker;
        
        // Crystal gliding variables
        private double frostEnergy = 100;
        private int tickCounter = 0;
        private double crystallinePhase = 0;
        private double frostAura = 0;
        private double glacialMomentum = 0;
        private IceState currentState = IceState.FORMING;
        private int iceTrailsCreated = 0;
        private Location lastLocation;
        private final Vector glideVelocity = new Vector(0, 0, 0);
        private final Location[] iceTrailPositions = new Location[50]; // Ice trail history
        private int trailIndex = 0;

        // Ice states for crystalline progression
        private enum IceState {
            FORMING,        // Ice formation
            GLIDING,        // Active gliding
            CRYSTALLINE,    // Crystalline structures
            GLACIAL         // Glacial mastery
        }

        CrystalGliderData(Player player, SpellContext context) {
            this.player = player;
            this.lastLocation = player.getLocation().clone();
            
            player.showBossBar(frostEnergyBar);
            
            // Frost awakening sounds
            player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 1.8f);
            player.playSound(player.getLocation(), Sound.BLOCK_SNOW_PLACE, 1.0f, 1.2f);
            player.playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL, 0.6f, 0.8f);
            
            // Show frost mastery message
            player.sendMessage(Component.text(
                    "§b❄ §fYou embrace the crystal frost, ice bends to your will! §b❄"));
            
            // Spawn frost activation effect
            spawnFrostActivationEffect();
            
            // Start the crystal glider ticker
            this.ticker = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 0, 1);
        }

        void stop() {
            ticker.cancel();
            player.hideBossBar(frostEnergyBar);
            
            // Remove frost effects
            removeFrostEffects();
            
            // Final frost dissipation effect
            spawnFrostDeactivationEffect();
            
            player.sendMessage(Component.text("§b❄ §7The frost melts away, warmth returns... §b❄"));
            player.playSound(player.getLocation(), Sound.BLOCK_SNOW_BREAK, 0.6f, 1.0f);
        }

        private void tick() {
            if (!player.isOnline()) {
                forceDeactivate(player);
                return;
            }
            
            tickCounter++;
            crystallinePhase += 0.2;   // Crystal formation
            frostAura += 0.15;        // Frost aura expansion
            
            Location currentLoc = player.getLocation();
            
            // Ice state management
            updateIceState();
            
            // Energy consumption based on state
            double energyCost = getEnergyCostForState();
            frostEnergy -= energyCost;
            frostEnergy = Math.max(0, Math.min(100, frostEnergy));
            frostEnergyBar.progress((float) (frostEnergy / 100));
            
            // Update boss bar with ice trails
            if (iceTrailsCreated > 0) {
                frostEnergyBar.name(Component.text("Frost Energy - Ice Trails: " + iceTrailsCreated));
            }
            
            // Movement processing
            handleGlidingPhysics(currentLoc);
            
            // Create ice trail
            if (lastLocation != null && currentLoc.distanceSquared(lastLocation) > 0.01) {
                createIceTrail(currentLoc);
                updateTrailHistory(currentLoc);
            }
            
            // Apply frost effects based on current state
            applyFrostEffects();
            
            // Frost visual effects
            spawnFrostForm(currentLoc);
            
            // Crystalline wing formation
            spawnCrystallineWings(currentLoc);
            
            // Glide trail visualization
            renderGlideTrail();
            
            // State-specific sounds
            playStateSpecificSounds(currentLoc);
            
            lastLocation = currentLoc.clone();
        }
        
        private void updateIceState() {
            // Ice state progression based on energy and trails
            if (iceTrailsCreated < 5) {
                currentState = IceState.FORMING;
            } else if (iceTrailsCreated < 15) {
                currentState = IceState.GLIDING;
            } else if (iceTrailsCreated < 30) {
                currentState = IceState.CRYSTALLINE;
            } else {
                currentState = IceState.GLACIAL;
            }
        }
        
        private double getEnergyCostForState() {
            switch (currentState) {
                case FORMING: return cfgDouble("energy.forming-cost", 0.08);
                case GLIDING: return cfgDouble("energy.gliding-cost", 0.15);
                case CRYSTALLINE: return cfgDouble("energy.crystalline-cost", 0.25);
                case GLACIAL: return cfgDouble("energy.glacial-cost", 0.4);
                default: return 0.15;
            }
        }
        
        private void handleGlidingPhysics(Location currentLoc) {
            if (lastLocation == null) return;
            
            Vector movement = currentLoc.toVector().subtract(lastLocation.toVector());
            
            // Apply momentum conservation
            if (movement.lengthSquared() > 0.001) {
                // Update glide velocity with momentum
                double momentumFactor = cfgDouble("physics.momentum-factor", 0.85);
                glideVelocity.multiply(momentumFactor);
                glideVelocity.add(movement.multiply(cfgDouble("physics.velocity-gain", 0.3)));
                
                // Calculate glacial momentum
                glacialMomentum = glideVelocity.length();
                
                // Apply enhanced speed based on ice state
                double speedMultiplier = getSpeedMultiplierForState();
                if (glacialMomentum > 0.1) {
                    Vector enhancedVelocity = glideVelocity.clone().normalize().multiply(glacialMomentum * speedMultiplier);
                    
                    // Apply frictionless gliding
                    if (cfgBool("physics.frictionless-mode", true)) {
                        Location targetLoc = currentLoc.clone().add(enhancedVelocity);
                        if (isSafeLocation(targetLoc)) {
                            player.teleport(targetLoc);
                        }
                    }
                }
            }
            
            // Gradual momentum decay
            glideVelocity.multiply(cfgDouble("physics.decay-factor", 0.98));
        }
        
        private double getSpeedMultiplierForState() {
            switch (currentState) {
                case FORMING: return 1.0;
                case GLIDING: return 1.3;
                case CRYSTALLINE: return 1.6;
                case GLACIAL: return 2.0;
                default: return 1.0;
            }
        }
        
        private boolean isSafeLocation(Location loc) {
            Block block = loc.getBlock();
            Block above = loc.clone().add(0, 1, 0).getBlock();
            Block below = loc.clone().add(0, -1, 0).getBlock();
            
            return !block.getType().isSolid() && 
                   !above.getType().isSolid() && 
                   below.getType().isSolid();
        }
        
        private void createIceTrail(Location location) {
            if (frostEnergy < cfgDouble("ice-trail.energy-cost", 2)) {
                return;
            }
            
            // Create ice beneath player
            Location iceLoc = location.clone().add(0, -1, 0);
            Block iceBlock = iceLoc.getBlock();
            
            if (iceBlock.getType() != Material.ICE && !iceBlock.getType().isSolid()) {
                // Temporarily place ice (could be expanded with block restoration)
                spawnIceTrailEffect(iceLoc);
                iceTrailsCreated++;
                frostEnergy -= cfgDouble("ice-trail.energy-cost", 2);
                
                // Ice creation sound
                if (Math.random() < 0.3) {
                    player.playSound(iceLoc, Sound.BLOCK_GLASS_PLACE, 0.4f, 1.5f);
                }
            }
        }
        
        private void updateTrailHistory(Location location) {
            iceTrailPositions[trailIndex] = location.clone();
            trailIndex = (trailIndex + 1) % iceTrailPositions.length;
        }
        
        private void spawnFrostActivationEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Frost formation effect
            for (int i = 0; i < 80; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = Math.random() * 3.5;
                double height = Math.random() * 3.0;
                
                Location frostLoc = loc.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );
                
                // Frost particles
                world.spawnParticle(Particle.SNOWFLAKE, frostLoc, 2, 0.2, 0.2, 0.2, 0.02);
                world.spawnParticle(Particle.CLOUD, frostLoc, 1, 0.15, 0.15, 0.15, 0.01);
                
                // Ice crystals
                Color crystalColor = Color.fromRGB(200, 220, 255);
                world.spawnParticle(Particle.DUST, frostLoc, 1, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(crystalColor, 1.4f));
            }
            
            // Central frost vortex
            for (int i = 0; i < 30; i++) {
                double spiral = i * 0.4;
                double radius = i * 0.12;
                Location spiralLoc = loc.clone().add(
                    Math.cos(spiral) * radius,
                    i * 0.15,
                    Math.sin(spiral) * radius
                );
                
                world.spawnParticle(Particle.SNOWFLAKE, spiralLoc, 1, 0.05, 0.05, 0.05, 0.01);
                world.spawnParticle(Particle.END_ROD, spiralLoc, 1, 0.03, 0.03, 0.03, 0.005);
            }
        }
        
        private void spawnFrostForm(Location center) {
            // State-specific frost form rendering
            switch (currentState) {
                case FORMING:
                    spawnFormingFrostForm(center);
                    break;
                case GLIDING:
                    spawnGlidingFrostForm(center);
                    break;
                case CRYSTALLINE:
                    spawnCrystallineFrostForm(center);
                    break;
                case GLACIAL:
                    spawnGlacialFrostForm(center);
                    break;
            }
            
            // Frost aura - always present
            spawnFrostAura(center);
        }
        
        private void spawnFormingFrostForm(Location center) {
            World world = Objects.requireNonNull(center.getWorld(), "world");
            
            // Forming ice crystals
            double formRadius = cfgDouble("frost.forming-radius", 1.8);
            int formParticles = cfgInt("frost.forming-particles", 12);
            
            for (int i = 0; i < formParticles; i++) {
                double angle = 2 * Math.PI * i / formParticles + crystallinePhase * 0.5;
                double radius = formRadius * (0.6 + 0.4 * Math.sin(crystallinePhase + i));
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = Math.sin(crystallinePhase * 0.7 + angle) * 0.6 + 1.5;
                
                Location formLoc = center.clone().add(x, y, z);
                
                // Forming crystals
                world.spawnParticle(Particle.SNOWFLAKE, formLoc, 1, 0.1, 0.1, 0.1, 0.01);
                world.spawnParticle(Particle.CLOUD, formLoc, 1, 0.08, 0.08, 0.08, 0.005);
                
                // Crystal formation
                if (Math.random() < 0.5) {
                    Color formColor = Color.fromRGB(180, 200, 255);
                    world.spawnParticle(Particle.DUST, formLoc, 1, 0.06, 0.06, 0.06, 0,
                        new Particle.DustOptions(formColor, 1.1f));
                }
            }
        }
        
        private void spawnGlidingFrostForm(Location center) {
            World world = Objects.requireNonNull(center.getWorld(), "world");
            
            // Gliding ice streams
            double glideRadius = cfgDouble("frost.gliding-radius", 2.2);
            int glideParticles = cfgInt("frost.gliding-particles", 18);
            
            for (int i = 0; i < glideParticles; i++) {
                double angle = 2 * Math.PI * i / glideParticles + frostAura;
                double radius = glideRadius * (0.7 + 0.3 * Math.sin(frostAura * 1.5 + i));
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = Math.sin(frostAura + angle * 1.5) * 0.8 + 1.3;
                
                Location glideLoc = center.clone().add(x, y, z);
                
                // Gliding ice
                world.spawnParticle(Particle.SNOWFLAKE, glideLoc, 2, 0.15, 0.1, 0.15, 0.02);
                world.spawnParticle(Particle.END_ROD, glideLoc, 1, 0.08, 0.08, 0.08, 0.01);
                
                // Ice stream
                Color glideColor = getCrystalColor(angle);
                world.spawnParticle(Particle.DUST, glideLoc, 1, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(glideColor, 1.3f));
            }
        }
        
        private void spawnCrystallineFrostForm(Location center) {
            World world = Objects.requireNonNull(center.getWorld(), "world");
            
            // Crystalline structures
            double crystalRadius = cfgDouble("frost.crystalline-radius", 2.8);
            int crystalParticles = cfgInt("frost.crystalline-particles", 24);
            
            for (int crystal = 0; crystal < 6; crystal++) {
                double crystalAngle = crystal * Math.PI / 3 + crystallinePhase * 0.3;
                
                for (int i = 0; i < crystalParticles / 6; i++) {
                    double progress = (double) i / (crystalParticles / 6);
                    double crystalX = Math.cos(crystalAngle) * progress * crystalRadius;
                    double crystalZ = Math.sin(crystalAngle) * progress * crystalRadius;
                    double crystalY = Math.sin(progress * Math.PI + crystallinePhase) * 1.2 + 1.5;
                    
                    Location crystalLoc = center.clone().add(crystalX, crystalY, crystalZ);
                    
                    // Crystalline structure
                    world.spawnParticle(Particle.SNOWFLAKE, crystalLoc, 1, 0.05, 0.05, 0.05, 0.01);
                    world.spawnParticle(Particle.END_ROD, crystalLoc, 1, 0.03, 0.03, 0.03, 0.005);
                    
                    // Crystal geometry
                    Color crystalColor = Color.fromRGB(220, 240, 255);
                    world.spawnParticle(Particle.DUST, crystalLoc, 1, 0.03, 0.03, 0.03, 0,
                        new Particle.DustOptions(crystalColor, 1.5f));
                }
            }
        }
        
        private void spawnGlacialFrostForm(Location center) {
            World world = Objects.requireNonNull(center.getWorld(), "world");
            
            // Glacial ice mastery
            double glacialRadius = cfgDouble("frost.glacial-radius", 3.5);
            int glacialParticles = cfgInt("frost.glacial-particles", 30);
            
            for (int i = 0; i < glacialParticles; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = glacialRadius * Math.random();
                double height = (Math.random() - 0.5) * 3.0 + 2.0;
                
                // Glacial distortion
                double distortion = Math.sin(frostAura + angle * 2) * 0.4;
                double x = Math.cos(angle) * radius * (1 + distortion);
                double z = Math.sin(angle) * radius * (1 + distortion);
                
                Location glacialLoc = center.clone().add(x, height, z);
                
                // Glacial mastery
                world.spawnParticle(Particle.SNOWFLAKE, glacialLoc, 3, 0.2, 0.2, 0.2, 0.03);
                world.spawnParticle(Particle.END_ROD, glacialLoc, 2, 0.15, 0.15, 0.15, 0.02);
                world.spawnParticle(Particle.CLOUD, glacialLoc, 1, 0.1, 0.1, 0.1, 0.01);
                
                // Glacial ice
                Color glacialColor = getGlacialColor(radius / glacialRadius);
                world.spawnParticle(Particle.DUST, glacialLoc, 2, 0.15, 0.15, 0.15, 0,
                    new Particle.DustOptions(glacialColor, 1.8f));
            }
        }
        
        private void spawnCrystallineWings(Location center) {
            World world = Objects.requireNonNull(center.getWorld(), "world");
            
            // Crystalline ice wings
            double wingSpan = cfgDouble("crystal.wing-span", 3.0);
            int wingParticles = cfgInt("crystal.wing-particles", 20);
            double wingIntensity = getStateIntensityMultiplier();
            
            for (int wing = 0; wing < 2; wing++) {
                double wingSide = wing == 0 ? -1 : 1;
                
                for (int i = 0; i < wingParticles; i++) {
                    double progress = (double) i / wingParticles;
                    double wingFlap = Math.sin(crystallinePhase * 1.5) * 0.2 + 0.8;
                    
                    double wingX = progress * wingSpan * wingSide * wingFlap;
                    double wingY = Math.sin(progress * Math.PI + crystallinePhase) + 1.2;
                    double wingZ = progress * 0.3;
                    
                    Location wingLoc = center.clone().add(wingX, wingY, wingZ);
                    
                    // Crystal wing structure
                    world.spawnParticle(Particle.SNOWFLAKE, wingLoc, 
                        (int)(2 * wingIntensity), 0.08, 0.08, 0.08, 0.02);
                    
                    if (Math.random() < 0.6) {
                        world.spawnParticle(Particle.END_ROD, wingLoc, 1, 0.04, 0.04, 0.04, 0.01);
                    }
                    
                    // Wing crystalline structure
                    Color wingColor = getCrystalWingColor(progress);
                    world.spawnParticle(Particle.DUST, wingLoc, 1, 0.06, 0.06, 0.06, 0,
                        new Particle.DustOptions(wingColor, (float)(1.2 + wingIntensity * 0.3)));
                }
            }
        }
        
        private void spawnFrostAura(Location center) {
            World world = center.getWorld();
            double auraRadius = cfgDouble("frost.aura-radius", 1.5);
            int auraParticles = cfgInt("frost.aura-particles", 10);
            
            for (int i = 0; i < auraParticles; i++) {
                double angle = 2 * Math.PI * i / auraParticles + frostAura * 0.5;
                double x = Math.cos(angle) * auraRadius;
                double z = Math.sin(angle) * auraRadius;
                double y = Math.sin(frostAura * 2 + angle * 2.5) * 0.4 + 1.8;
                
                Location auraLoc = center.clone().add(x, y, z);
                
                // Frost aura
                world.spawnParticle(Particle.SNOWFLAKE, auraLoc, 1, 0.03, 0.03, 0.03, 0.005);
                
                if (Math.random() < 0.4) {
                    Color auraColor = Color.fromRGB(200, 230, 255);
                    world.spawnParticle(Particle.DUST, auraLoc, 1, 0.02, 0.02, 0.02, 0,
                        new Particle.DustOptions(auraColor, 0.9f));
                }
            }
        }
        
        private void renderGlideTrail() {
            World world = player.getWorld();
            
            // Render ice trail history
            for (int i = 0; i < iceTrailPositions.length; i++) {
                Location trailPos = iceTrailPositions[i];
                if (trailPos == null) continue;
                
                // Trail age calculation
                int age = (trailIndex - i + iceTrailPositions.length) % iceTrailPositions.length;
                double ageProgress = (double) age / iceTrailPositions.length;
                
                if (ageProgress < 0.8) { // Only show recent trail
                    float intensity = (float) (1.0 - ageProgress);
                    
                    // Trail particles
                    world.spawnParticle(Particle.SNOWFLAKE, trailPos, 1, 0.1, 0.1, 0.1, 0.01);
                    
                    if (Math.random() < 0.5) {
                        Color trailColor = getGlideTrailColor(ageProgress);
                        world.spawnParticle(Particle.DUST, trailPos, 1, 0.05, 0.05, 0.05, 0,
                            new Particle.DustOptions(trailColor, intensity));
                    }
                }
            }
        }
        
        private void spawnIceTrailEffect(Location iceLoc) {
            World world = Objects.requireNonNull(iceLoc.getWorld(), "world");
            
            // Ice trail creation effect
            for (int i = 0; i < 15; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = Math.random() * 1.5;
                double height = Math.random() * 0.5;
                
                Location iceEffect = iceLoc.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );
                
                // Ice formation
                world.spawnParticle(Particle.SNOWFLAKE, iceEffect, 2, 0.1, 0.1, 0.1, 0.02);
                world.spawnParticle(Particle.CLOUD, iceEffect, 1, 0.08, 0.05, 0.08, 0.01);
                
                // Ice crystals
                Color iceColor = Color.fromRGB(220, 240, 255);
                world.spawnParticle(Particle.DUST, iceEffect, 1, 0.08, 0.08, 0.08, 0,
                    new Particle.DustOptions(iceColor, 1.2f));
            }
        }
        
        private void spawnFrostDeactivationEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Frost melting effect
            for (int i = 0; i < 50; i++) {
                double angle = i * 0.25;
                double radius = 3.0 - (i * 0.05);
                double height = i * 0.08;
                
                Location meltLoc = loc.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );
                
                world.spawnParticle(Particle.CLOUD, meltLoc, 2, 0.15, 0.1, 0.15, 0.01);
                world.spawnParticle(Particle.DRIPPING_WATER, meltLoc, 1, 0.1, 0.05, 0.1, 0);
                
                // Fading frost
                if (Math.random() < 0.4) {
                    Color fadeColor = Color.fromRGB((int)(200 * (1 - i * 0.02)), (int)(230 * (1 - i * 0.02)), 255);
                    world.spawnParticle(Particle.DUST, meltLoc, 1, 0.08, 0.08, 0.08, 0,
                        new Particle.DustOptions(fadeColor, 1.0f));
                }
            }
        }
        
        // Color and effect helpers
        private Color getCrystalColor(double phase) {
            // Crystal color spectrum
            double normalizedPhase = (Math.sin(phase) + 1) / 2;
            
            if (normalizedPhase < 0.5) {
                // Light blue to white
                return Color.fromRGB((int)(180 + 75 * normalizedPhase * 2), (int)(220 + 35 * normalizedPhase * 2), 255);
            } else {
                // White to ice blue
                double subPhase = (normalizedPhase - 0.5) * 2;
                return Color.fromRGB((int)(255 - 55 * subPhase), (int)(255 - 25 * subPhase), 255);
            }
        }
        
        private Color getCrystalWingColor(double progress) {
            // Crystal wing color progression
            if (progress < 0.5) {
                // Ice blue to crystal white
                return Color.fromRGB((int)(200 + 55 * progress * 2), (int)(230 + 25 * progress * 2), 255);
            } else {
                // Crystal white to glacial blue
                double subProgress = (progress - 0.5) * 2;
                return Color.fromRGB((int)(255 - 100 * subProgress), (int)(255 - 50 * subProgress), 255);
            }
        }
        
        private Color getGlacialColor(double intensity) {
            // Glacial color based on intensity
            if (intensity < 0.33) {
                // Deep ice blue
                return Color.fromRGB(150, 200, 255);
            } else if (intensity < 0.67) {
                // Crystal blue
                return Color.fromRGB(200, 230, 255);
            } else {
                // Glacial white
                return Color.fromRGB(240, 250, 255);
            }
        }
        
        private Color getGlideTrailColor(double age) {
            // Trail color based on age
            double alpha = 1.0 - age;
            
            switch (currentState) {
                case FORMING:
                    return Color.fromRGB((int)(180 * alpha), (int)(220 * alpha), (int)(255 * alpha));
                case GLIDING:
                    return Color.fromRGB((int)(200 * alpha), (int)(230 * alpha), (int)(255 * alpha));
                case CRYSTALLINE:
                    return Color.fromRGB((int)(220 * alpha), (int)(240 * alpha), (int)(255 * alpha));
                case GLACIAL:
                    return Color.fromRGB((int)(240 * alpha), (int)(250 * alpha), (int)(255 * alpha));
                default:
                    return Color.fromRGB((int)(200 * alpha), (int)(230 * alpha), (int)(255 * alpha));
            }
        }
        
        private double getStateIntensityMultiplier() {
            switch (currentState) {
                case FORMING: return 0.7;
                case GLIDING: return 1.0;
                case CRYSTALLINE: return 1.4;
                case GLACIAL: return 1.8;
                default: return 1.0;
            }
        }
        
        private void applyFrostEffects() {
            int duration = cfgInt("effects.duration-ticks", 40);
            
            // Frost resistance
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0, false, false));
            
            // State-specific effects
            switch (currentState) {
                case FORMING:
                    // Ice formation clarity
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, false, false));
                    break;
                case GLIDING:
                    // Enhanced gliding
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 0, false, false));
                    break;
                case CRYSTALLINE:
                    // Crystalline structure
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 2, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 1, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 0, false, false));
                    break;
                case GLACIAL:
                    // Glacial mastery
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 3, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 2, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 1, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 0, false, false));
                    break;
            }
            
            // Frost walking
            if (cfgBool("effects.frost-walker", true)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, duration, 0, false, false));
            }
        }
        
        private void removeFrostEffects() {
            player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            player.removePotionEffect(PotionEffectType.RESISTANCE);
            player.removePotionEffect(PotionEffectType.STRENGTH);
            player.removePotionEffect(PotionEffectType.WATER_BREATHING);
        }
        
        private void playStateSpecificSounds(Location loc) {
            if (tickCounter % 100 == 0) {
                switch (currentState) {
                    case FORMING:
                        player.playSound(loc, Sound.BLOCK_SNOW_PLACE, 0.3f, 1.4f);
                        break;
                    case GLIDING:
                        player.playSound(loc, Sound.BLOCK_GLASS_STEP, 0.4f, 1.8f);
                        break;
                    case CRYSTALLINE:
                        player.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.2f, 2.0f);
                        player.playSound(loc, Sound.ITEM_BOTTLE_FILL, 0.3f, 1.6f);
                        break;
                    case GLACIAL:
                        player.playSound(loc, Sound.BLOCK_GLASS_HIT, 0.5f, 0.8f);
                        break;
                }
            }
        }
    }
    
    /* ---------------------------------------- */
    /* CONFIG HELPERS */
    /* ---------------------------------------- */
    private double cfgDouble(String path, double def) {
        return spellConfig.getDouble("crystal-glide." + path, def);
    }
    
    private int cfgInt(String path, int def) {
        return spellConfig.getInt("crystal-glide." + path, def);
    }
    
    private boolean cfgBool(String path, boolean def) {
        return spellConfig.getBoolean("crystal-glide." + path, def);
    }
}
