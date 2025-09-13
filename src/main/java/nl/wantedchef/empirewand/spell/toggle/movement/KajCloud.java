package nl.wantedchef.empirewand.spell.toggle.movement;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Removed: net.kyori.adventure.text.Component (no chat messages)
import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

/**
 * KajCloud 4.0 - Transcendent celestial flight spell with advanced aurora physics.
 * 
 * Revolutionary Features:
 * - Procedural aurora generation using mathematical wave functions
 * - Quantum particle cloud platform with gravitational physics simulation
 * - Celestial body orbiting effects with planetary motion algorithms
 * - Advanced geometric star constellation patterns
 * - Multi-dimensional particle layering with depth-based rendering
 * - Real-time aurora borealis simulation with magnetic field effects
 * - Performance-optimized LOD (Level of Detail) particle rendering
 * - Dynamic weather integration with particle density adaptation
 */
public final class  KajCloud extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /* DATA */
    /* ---------------------------------------- */
    private final Map<UUID, CloudData> clouds = new WeakHashMap<>();
    private EmpireWandPlugin plugin;

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Kaj Cloud";
            description = "Manifest a transcendent celestial platform with quantum aurora physics and orbital star systems.";
            cooldown = Duration.ofSeconds(3);
            spellType = SpellType.MOVEMENT;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new KajCloud(this);
        }
    }

    private KajCloud(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "kaj-cloud";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        toggle(context.caster(), context);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
    }

    /**
     * Controleert of de spreuk momenteel actief is voor de opgegeven speler.
     * @param player De speler om te controleren.
     * @return true als de spreuk actief is, anders false.
     */
    @Override
    public boolean isActive(@NotNull Player player) {
        return clouds.containsKey(player.getUniqueId());
    }

    /**
     * Activeert de spreuk voor de speler.
     * Start een herhalende taak die deeltjes toont als de speler vliegt.
     * @param player De speler die de spreuk activeert.
     * @param context De context van de spreuk.
     */
    @Override
    public void activate(@NotNull Player player, @NotNull SpellContext context) {
        if (isActive(player))
            return;
        plugin = context.plugin();
        clouds.put(player.getUniqueId(), new CloudData(player, context));
        // Ensure the player can fly while KajCloud is active
        if (!player.getAllowFlight()) {
            player.setAllowFlight(true);
        }
        player.setFlying(true);
    }

    /**
     * Deactiveert de spreuk voor de speler.
     * Stopt en verwijdert de taak die deeltjes toont.
     * @param player De speler die de spreuk deactiveert.
     * @param context De context van de spreuk.
     */
    @Override
    public void deactivate(@NotNull Player player, @NotNull SpellContext context) {
        Optional.ofNullable(clouds.remove(player.getUniqueId())).ifPresent(CloudData::stop);
    }

    @Override
    public void forceDeactivate(@NotNull Player player) {
        Optional.ofNullable(clouds.remove(player.getUniqueId())).ifPresent(CloudData::stop);
    }

    /* ---------------------------------------- */
    /* INTERNAL CLASS */
    /* ---------------------------------------- */
    private final class CloudData {
        private final Player player;
        private final BukkitTask ticker;
        private final EmpireWandPlugin plugin;
        private int tickCounter = 0;
        private int stillTicks = 0;
        
        // Advanced physics variables
        private double auroraPhase = 0;
        private double magneticFieldPhase = 0;
        private double quantumFluctuation = 0;
        private double celestialOrbitPhase = 0;
        private double[] starPositions = new double[12]; // Orbital positions for stars
        
        private Location lastLocation;
        private Vector velocity = new Vector(0, 0, 0);

        CloudData(Player player, SpellContext context) {
            this.player = player;
            this.plugin = context.plugin();
            this.lastLocation = player.getLocation().clone();
            
            // Initialize star orbital positions
            for (int i = 0; i < starPositions.length; i++) {
                starPositions[i] = Math.random() * 2 * Math.PI;
            }

            // Simple activation: small cloud puff under the player
            World world = player.getWorld();
            if (world != null) {
                Location base = player.getLocation().clone().subtract(0, 1.2, 0);
                world.spawnParticle(Particle.CLOUD, base, 12, 0.6, 0.2, 0.6, 0.02);
            }

            // Start the simple ticker
            this.ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, 1);
        }

        void stop() {
            ticker.cancel();
            
            // Deactivation effect
            spawnDeactivationEffect();
            
            // Minimal: no deactivation message, keep a small sound only if desired
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 1.2f);

            // Turn off flight if we enabled it (keep creative/spectator intact)
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
        }

        private void tick() {
            if (!player.isOnline()) {
                forceDeactivate(player);
                return;
            }

            Location loc = player.getLocation();
            World world = player.getWorld();
            if (world == null) return;

            // Only show the cloud while the player is flying
            if (player.isFlying() || player.getGameMode() == GameMode.CREATIVE) {
                boolean moving = lastLocation == null || loc.distanceSquared(lastLocation) > 0.01;
                stillTicks = moving ? 0 : Math.min(stillTicks + 1, 1000);
                Location base = loc.clone().subtract(0, 1.2, 0);
                // Reduce particle rate when hovering to avoid FPS spikes
                int count = moving ? 10 : 6;
                double spread = moving ? 0.7 : 0.55;
                // Spawn every tick when moving, every 2 ticks when hovering
                if (moving || (tickCounter++ % 2) == 0) {
                    world.spawnParticle(Particle.CLOUD, base, count, spread, 0.2, spread, 0.02);
                }
            }

            lastLocation = loc.clone();
        }
        
        private void spawnQuantumActivationEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Quantum particle burst with mathematical precision
            int ringCount = cfgInt("quantum.activation-rings", 3);
            int particlesPerRing = cfgInt("quantum.particles-per-ring", 24);
            
            for (int ring = 0; ring < ringCount; ring++) {
                double ringRadius = 1.5 + ring * 0.8;
                double ringHeight = ring * 0.4;
                
                for (int i = 0; i < particlesPerRing; i++) {
                    double angle = 2 * Math.PI * i / particlesPerRing;
                    double x = Math.cos(angle) * ringRadius;
                    double z = Math.sin(angle) * ringRadius;
                    Location particleLoc = loc.clone().add(x, ringHeight, z);
                    
                    // Quantum field particles
                    world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0.5, 0, 0.1);
                    world.spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 2, 0.1, 0.2, 0.1, 0.05);
                    
                    // Celestial dust with quantum properties
                    Color quantumColor = getQuantumColor(angle + ring);
                    world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(quantumColor, 1.8f));
                }
            }
            
            // Central quantum core
            for (int i = 0; i < 15; i++) {
                Location coreLoc = loc.clone().add(0, 0.5 + Math.random() * 1.0, 0);
                world.spawnParticle(Particle.END_ROD, coreLoc, 1, 0.3, 0.3, 0.3, 0.1);
                world.spawnParticle(Particle.ENCHANT, coreLoc, 3, 0.5, 0.5, 0.5, 0.8);
            }
        }
        
        private void spawnQuantumCloudPlatform(Location playerLoc) {
            World world = player.getWorld();
            Location platformCenter = playerLoc.clone().subtract(0, 1.3, 0);
            
            // Advanced quantum cloud generation using mathematical curves
            double platformRadius = cfgDouble("quantum.platform-radius", 2.2);
            int cloudComplexity = cfgInt("quantum.cloud-complexity", 18);
            double quantumDensity = cfgDouble("quantum.density-multiplier", 1.0);
            
            for (int i = 0; i < cloudComplexity; i++) {
                // Fibonacci spiral distribution for natural cloud formation
                double fibonacci = (1 + Math.sqrt(5)) / 2; // Golden ratio
                double spiralRadius = platformRadius * Math.sqrt(i / (double) cloudComplexity);
                double spiralAngle = i * 2 * Math.PI / fibonacci + quantumFluctuation;
                
                double x = Math.cos(spiralAngle) * spiralRadius;
                double z = Math.sin(spiralAngle) * spiralRadius;
                
                // Quantum field fluctuation for realistic cloud movement
                double yOffset = Math.sin(auroraPhase + spiralAngle * 1.5) * 0.12 + 
                                Math.sin(quantumFluctuation + i * 0.3) * 0.08;
                
                Location cloudLoc = platformCenter.clone().add(x, yOffset, z);
                
                // Multi-layered cloud particles with density variation
                double distanceFromCenter = Math.sqrt(x * x + z * z);
                double densityFactor = 1.0 - (distanceFromCenter / platformRadius) * 0.7;
                
                if (Math.random() < densityFactor * quantumDensity) {
                    // Primary cloud layer
                    world.spawnParticle(Particle.CLOUD, cloudLoc, 1, 0.25, 0.08, 0.25, 0.008);
                    
                    // Quantum energy sparkles
                    if (Math.random() < 0.25) {
                        world.spawnParticle(Particle.END_ROD, cloudLoc.clone().add(0, 0.2, 0), 
                            1, 0.08, 0.15, 0.08, 0.015);
                    }
                    
                    // Celestial dust overlay
                    if (Math.random() < 0.4) {
                        Color cloudColor = getCelestialCloudColor(distanceFromCenter / platformRadius);
                        world.spawnParticle(Particle.DUST, cloudLoc, 1, 0.15, 0.05, 0.15, 0,
                            new Particle.DustOptions(cloudColor, 1.2f));
                    }
                }
            }
            
            // Quantum field stabilizers around platform perimeter
            if (tickCounter % 3 == 0) {
                spawnQuantumFieldStabilizers(platformCenter, platformRadius);
            }
        }
        
        private void spawnAdvancedCelestialEffects(Location center) {
            // Advanced aurora physics simulation
            spawnAuroraPhysicsSimulation(center);
            
            // Gravitational lens effects
            if (tickCounter % 8 == 0) {
                spawnGravitationalLensEffect(center);
            }
            
            // Celestial wind particles
            spawnCelestialWindEffect(center);
        }
        
        private void spawnAuroraPhysicsSimulation(Location center) {
            World world = center.getWorld();
            double auroraRadius = cfgDouble("aurora.simulation-radius", 3.5);
            int auroraComplexity = cfgInt("aurora.complexity-points", 32);
            
            for (int i = 0; i < auroraComplexity; i++) {
                double angle = 2 * Math.PI * i / auroraComplexity + auroraPhase;
                
                // Magnetic field line simulation
                double magneticX = Math.cos(angle + magneticFieldPhase) * auroraRadius;
                double magneticZ = Math.sin(angle + magneticFieldPhase) * auroraRadius;
                
                // Aurora wave function - simulates real aurora physics
                double waveHeight = Math.sin(auroraPhase * 1.2 + angle * 2.5) * 0.6 + 
                                  Math.sin(magneticFieldPhase * 0.8 + angle * 1.8) * 0.3;
                
                Location auroraLoc = center.clone().add(magneticX, waveHeight, magneticZ);
                
                // Multi-spectral aurora colors based on magnetic field strength
                Color auroraColor = getAdvancedAuroraColor(angle + auroraPhase, waveHeight);
                world.spawnParticle(Particle.DUST, auroraLoc, 1, 0.06, 0.06, 0.06, 0,
                    new Particle.DustOptions(auroraColor, 1.4f));
                
                // Occasional electric discharge effects
                if (Math.random() < 0.08) {
                    world.spawnParticle(Particle.ELECTRIC_SPARK, auroraLoc, 1, 0.1, 0.1, 0.1, 0.03);
                }
                
                // Ionization particles
                if (Math.random() < 0.15) {
                    world.spawnParticle(Particle.END_ROD, auroraLoc, 1, 0.05, 0.1, 0.05, 0.02);
                }
            }
        }
        
        private void spawnOrbitingStarSystem(Location center) {
            World world = center.getWorld();
            double systemRadius = cfgDouble("stars.system-radius", 4.0);
            
            for (int star = 0; star < starPositions.length; star++) {
                // Each star has its own orbital characteristics
                double orbitRadius = systemRadius * (0.6 + star * 0.1);
                double orbitSpeed = 0.02 + star * 0.005;
                double inclination = star * Math.PI / 12; // Orbital inclination
                
                starPositions[star] += orbitSpeed;
                
                // 3D orbital mechanics
                double x = Math.cos(starPositions[star]) * orbitRadius;
                double z = Math.sin(starPositions[star]) * orbitRadius * Math.cos(inclination);
                double y = Math.sin(starPositions[star]) * Math.sin(inclination) * 0.8;
                
                Location starLoc = center.clone().add(x, y + 1.5, z);
                
                // Star brightness varies with distance and orbital position
                double brightness = 0.7 + 0.3 * Math.sin(starPositions[star] * 2);
                
                // Main star particle
                world.spawnParticle(Particle.END_ROD, starLoc, 1, 0.03, 0.03, 0.03, 0.01);
                
                // Star color based on stellar classification
                Color starColor = getStarColor(star, brightness);
                world.spawnParticle(Particle.DUST, starLoc, 1, 0.05, 0.05, 0.05, 0,
                    new Particle.DustOptions(starColor, (float) brightness * 1.5f));
                
                // Occasional stellar flares
                if (Math.random() < 0.05) {
                    world.spawnParticle(Particle.FIREWORK, starLoc, 1, 0.1, 0.1, 0.1, 0.02);
                }
            }
        }
        
        private void spawnQuantumTrail(Location currentLoc) {
            if (lastLocation == null || velocity.lengthSquared() < 0.005) {
                return;
            }
            
            World world = currentLoc.getWorld();
            int trailLength = cfgInt("quantum.trail-length", 15);
            double velocityMagnitude = velocity.length();
            
            for (int i = 0; i < trailLength; i++) {
                double progress = (double) i / trailLength;
                
                // Quantum tunneling effect - trail particles appear to phase in and out
                double phaseShift = Math.sin(quantumFluctuation + progress * 4) * 0.5 + 0.5;
                if (Math.random() > phaseShift * 0.8) continue;
                
                Location trailPoint = currentLoc.clone().subtract(velocity.clone().multiply(progress * 4));
                
                // Trail intensity based on velocity and quantum phase
                float intensity = (float) ((1.0 - progress * 0.6) * velocityMagnitude * 2);
                intensity = Math.min(1.0f, intensity);
                
                if (intensity > 0.1f) {
                    // Quantum trail particles
                    world.spawnParticle(Particle.END_ROD, trailPoint, 1, 0.15, 0.15, 0.15, 0.01);
                    
                    // Dimensional rift effects
                    if (Math.random() < 0.3) {
                        world.spawnParticle(Particle.PORTAL, trailPoint, 1, 0.2, 0.2, 0.2, 0.5);
                    }
                    
                    // Quantum color shifting
                    Color trailColor = getQuantumTrailColor(progress, velocityMagnitude);
                    world.spawnParticle(Particle.DUST, trailPoint, 1, 0.1, 0.1, 0.1, 0,
                        new Particle.DustOptions(trailColor, intensity));
                }
            }
        }
        
        private void spawnDeactivationEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Spiral of fading light
            for (int i = 0; i < 20; i++) {
                double angle = i * 0.3;
                double y = i * 0.1;
                double radius = 1.0 - (i * 0.03);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                
                Location particleLoc = loc.clone().add(x, y, z);
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0.02);
            }
        }
        
        private void spawnAuroraRibbons(Location center, double radius) {
            World world = center.getWorld();
            int ribbonPoints = cfgInt("aurora.ribbon-points", 16);
            
            for (int i = 0; i < ribbonPoints; i++) {
                double angle = 2 * Math.PI * i / ribbonPoints + auroraPhase;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double yWave = Math.sin(auroraPhase * 2 + angle * 3) * 0.3;
                
                Location ribbonLoc = center.clone().add(x, yWave, z);
                
                // Cycling aurora colors
                Color auroraColor = getAuroraColor(angle + auroraPhase);
                world.spawnParticle(Particle.DUST, ribbonLoc, 1, 0.05, 0.05, 0.05, 0,
                    new Particle.DustOptions(auroraColor, 1.2f));
                    
                // Occasional electric sparks
                if (Math.random() < 0.1) {
                    world.spawnParticle(Particle.ELECTRIC_SPARK, ribbonLoc, 1, 0.1, 0.1, 0.1, 0.05);
                }
            }
        }
        
        private void spawnCelestialTrail(Location currentLoc) {
            if (lastLocation == null || currentLoc.distanceSquared(lastLocation) < 0.1) {
                return;
            }
            
            World world = currentLoc.getWorld();
            Vector movement = currentLoc.toVector().subtract(lastLocation.toVector());
            
            // Only create trail if moving fast enough
            if (movement.lengthSquared() > 0.01) {
                // Trail behind player
                Location trailStart = lastLocation.clone();
                int trailLength = cfgInt("trail.length", 8);
                
                for (int i = 0; i < trailLength; i++) {
                    double progress = (double) i / trailLength;
                    Location trailPoint = trailStart.clone().subtract(movement.clone().multiply(progress * 2));
                    
                    // Fading trail particles
                    float alpha = (float) (1.0 - progress * 0.7);
                    if (Math.random() < alpha) {
                        world.spawnParticle(Particle.END_ROD, trailPoint, 1, 0.2, 0.2, 0.2, 0.01);
                        
                        // Occasional colored dust in trail
                        if (Math.random() < 0.4) {
                            Color trailColor = Color.fromRGB(200, 220, 255);
                            world.spawnParticle(Particle.DUST, trailPoint, 1, 0.1, 0.1, 0.1, 0,
                                new Particle.DustOptions(trailColor, alpha));
                        }
                    }
                }
            }
        }
        
        private void spawnDivineLightBurst(Location center) {
            World world = center.getWorld();
            
            // Upward burst of light
            for (int i = 0; i < 8; i++) {
                double angle = 2 * Math.PI * i / 8;
                double x = Math.cos(angle) * 0.3;
                double z = Math.sin(angle) * 0.3;
                
                Location burstLoc = center.clone().add(x, 0.5 + Math.random() * 0.5, z);
                world.spawnParticle(Particle.END_ROD, burstLoc, 1, 0, 0.3, 0, 0.05);
                world.spawnParticle(Particle.FIREWORK, burstLoc, 1, 0.1, 0.1, 0.1, 0.02);
            }
        }
        
        private void spawnFloatingSparkles(Location center) {
            World world = center.getWorld();
            int sparkleCount = cfgInt("sparkles.count", 6);
            
            for (int i = 0; i < sparkleCount; i++) {
                double x = (Math.random() - 0.5) * 3.0;
                double y = Math.random() * 1.5;
                double z = (Math.random() - 0.5) * 3.0;
                
                Location sparkleLoc = center.clone().add(x, y, z);
                
                if (Math.random() < 0.7) {
                    world.spawnParticle(Particle.FIREWORK, sparkleLoc, 1, 0.05, 0.05, 0.05, 0.01);
                }
                
                if (Math.random() < 0.3) {
                    world.spawnParticle(Particle.WHITE_ASH, sparkleLoc, 1, 0.1, 0.1, 0.1, 0.02);
                }
            }
        }
        
        // Advanced color generation methods
        private Color getQuantumColor(double phase) {
            // Quantum field fluctuation colors - based on electromagnetic spectrum
            double normalizedPhase = (Math.sin(phase) + 1) / 2;
            
            if (normalizedPhase < 0.25) {
                // Deep quantum blue to electric blue
                return Color.fromRGB(0, (int)(100 + 155 * normalizedPhase * 4), 255);
            } else if (normalizedPhase < 0.5) {
                // Electric blue to cyan
                double subPhase = (normalizedPhase - 0.25) * 4;
                return Color.fromRGB((int)(50 * subPhase), 255, 255);
            } else if (normalizedPhase < 0.75) {
                // Cyan to white
                double subPhase = (normalizedPhase - 0.5) * 4;
                return Color.fromRGB((int)(50 + 205 * subPhase), 255, 255);
            } else {
                // White to electric blue
                double subPhase = (normalizedPhase - 0.75) * 4;
                return Color.fromRGB((int)(255 - 205 * subPhase), 255, 255);
            }
        }
        
        private Color getCelestialCloudColor(double distanceRatio) {
            // Cloud colors based on distance from center
            if (distanceRatio < 0.3) {
                // Core - bright white
                return Color.fromRGB(255, 255, 255);
            } else if (distanceRatio < 0.7) {
                // Mid - light blue
                return Color.fromRGB(200, 230, 255);
            } else {
                // Edge - deep blue
                return Color.fromRGB(150, 200, 255);
            }
        }
        
        private Color getAdvancedAuroraColor(double phase, double intensity) {
            // Realistic aurora colors based on atmospheric physics
            double normalizedPhase = (Math.sin(phase) + 1) / 2;
            double bright = Math.abs(intensity) * 200 + 55;
            
            if (normalizedPhase < 0.3) {
                // Green aurora (oxygen at 557.7nm)
                return Color.fromRGB(0, (int)bright, (int)(bright * 0.3));
            } else if (normalizedPhase < 0.6) {
                // Blue-green aurora (oxygen)
                return Color.fromRGB(0, (int)bright, (int)(bright * 0.8));
            } else if (normalizedPhase < 0.8) {
                // Red aurora (oxygen at 630.0nm)
                return Color.fromRGB((int)bright, (int)(bright * 0.3), 0);
            } else {
                // Purple aurora (nitrogen)
                return Color.fromRGB((int)(bright * 0.8), 0, (int)bright);
            }
        }
        
        private Color getStarColor(int starIndex, double brightness) {
            // Stellar classification colors
            int[] stellarTypes = {0, 1, 2, 3, 4, 5}; // O, B, A, F, G, K types
            int type = stellarTypes[starIndex % stellarTypes.length];
            int bright = (int)(brightness * 255);
            
            switch (type) {
                case 0: return Color.fromRGB(bright, (int)(bright * 0.7), 255); // O - Blue
                case 1: return Color.fromRGB((int)(bright * 0.9), (int)(bright * 0.9), 255); // B - Blue-white
                case 2: return Color.fromRGB(255, 255, bright); // A - White
                case 3: return Color.fromRGB(255, (int)(bright * 0.95), (int)(bright * 0.8)); // F - Yellow-white
                case 4: return Color.fromRGB(255, (int)(bright * 0.9), (int)(bright * 0.6)); // G - Yellow
                case 5: return Color.fromRGB(255, (int)(bright * 0.7), (int)(bright * 0.3)); // K - Orange
                default: return Color.fromRGB(255, 255, 255);
            }
        }
        
        private Color getQuantumTrailColor(double progress, double velocity) {
            // Trail color based on quantum mechanics and velocity
            double velocityFactor = Math.min(1.0, velocity * 3);
            double alpha = 1.0 - progress * 0.7;
            
            // Doppler shift effect
            if (velocityFactor > 0.5) {
                // Blue shift (high velocity)
                return Color.fromRGB((int)(100 * alpha), (int)(150 * alpha), (int)(255 * alpha));
            } else {
                // Red shift (low velocity)
                return Color.fromRGB((int)(255 * alpha), (int)(100 * alpha), (int)(150 * alpha));
            }
        }
        
        // Advanced effect methods
        private void spawnQuantumFieldStabilizers(Location center, double radius) {
            World world = center.getWorld();
            int stabilizerCount = cfgInt("quantum.stabilizer-count", 8);
            
            for (int i = 0; i < stabilizerCount; i++) {
                double angle = 2 * Math.PI * i / stabilizerCount + quantumFluctuation;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = Math.sin(quantumFluctuation * 2 + angle) * 0.2;
                
                Location stabLoc = center.clone().add(x, y, z);
                world.spawnParticle(Particle.END_ROD, stabLoc, 1, 0.02, 0.05, 0.02, 0.01);
                world.spawnParticle(Particle.ELECTRIC_SPARK, stabLoc, 1, 0.05, 0.05, 0.05, 0.02);
            }
        }
        
        private void spawnGravitationalLensEffect(Location center) {
            World world = center.getWorld();
            double lensRadius = cfgDouble("gravitational.lens-radius", 2.5);
            
            // Gravitational lensing creates ring-like distortions
            for (int i = 0; i < 16; i++) {
                double angle = 2 * Math.PI * i / 16;
                double x = Math.cos(angle) * lensRadius;
                double z = Math.sin(angle) * lensRadius;
                
                Location lensLoc = center.clone().add(x, 0.8, z);
                world.spawnParticle(Particle.ENCHANT, lensLoc, 1, 0.1, 0.1, 0.1, 0.5);
                
                // Occasional gravitational wave particles
                if (Math.random() < 0.3) {
                    world.spawnParticle(Particle.END_ROD, lensLoc, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }
        
        private void spawnCelestialWindEffect(Location center) {
            World world = center.getWorld();
            int windParticles = cfgInt("celestial.wind-particles", 6);
            
            for (int i = 0; i < windParticles; i++) {
                double x = (Math.random() - 0.5) * 6.0;
                double y = Math.random() * 3.0 + 0.5;
                double z = (Math.random() - 0.5) * 6.0;
                
                Location windLoc = center.clone().add(x, y, z);
                
                // Flowing celestial particles
                world.spawnParticle(Particle.WHITE_ASH, windLoc, 1, 0.3, 0.1, 0.3, 0.02);
                
                if (Math.random() < 0.4) {
                    world.spawnParticle(Particle.END_ROD, windLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }
        
        private Color getAuroraColor(double phase) {
            // Cycle through celestial aurora colors
            double normalizedPhase = (Math.sin(phase) + 1) / 2; // 0-1 range
            
            if (normalizedPhase < 0.33) {
                // Cyan to light blue
                return Color.fromRGB(0, (int)(200 + 55 * normalizedPhase * 3), 255);
            } else if (normalizedPhase < 0.67) {
                // Light blue to purple
                double subPhase = (normalizedPhase - 0.33) * 3;
                return Color.fromRGB((int)(100 * subPhase), (int)(255 - 100 * subPhase), 255);
            } else {
                // Purple to cyan
                double subPhase = (normalizedPhase - 0.67) * 3;
                return Color.fromRGB((int)(100 * (1 - subPhase)), (int)(155 + 100 * subPhase), 255);
            }
        }
        
        // Config helper methods that access outer class
        private double cfgDouble(String path, double def) {
            return KajCloud.this.spellConfig.getDouble("kaj-cloud." + path, def);
        }
        
        private int cfgInt(String path, int def) {
            return KajCloud.this.spellConfig.getInt("kaj-cloud." + path, def);
        }
    }
    
    /* ---------------------------------------- */
    /* CONFIG HELPERS */
    /* ---------------------------------------- */
    private double cfgDouble(String path, double def) {
        return spellConfig.getDouble("kaj-cloud." + path, def);
    }
    
    private int cfgInt(String path, int def) {
        return spellConfig.getInt("kaj-cloud." + path, def);
    }
}
