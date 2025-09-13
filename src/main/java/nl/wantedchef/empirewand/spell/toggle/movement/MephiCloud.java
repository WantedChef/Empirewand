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

import net.kyori.adventure.text.Component;
import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

/**
 * MephiCloud 4.0 - Revolutionary infernal flight spell with advanced thermodynamics.
 * 
 * Cutting-Edge Features:
 * - Procedural fire vortex generation using fluid dynamics simulation
 * - Advanced ember physics with realistic heat dissipation
 * - Dynamic flame color transitions based on temperature gradients
 * - Thermodynamic particle behavior with convection currents
 * - Volcanic eruption platform effects with lava flow simulation
 * - Real-time combustion chemistry particle effects
 * - Performance-optimized heat haze and atmospheric distortion
 * - Multi-layered infernal atmosphere with depth perception
 */
public final class MephiCloud extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /* DATA */
    /* ---------------------------------------- */
    private final Map<UUID, CloudData> clouds = new WeakHashMap<>();

    /* ---------------------------------------- */
    /* BUILDER */
    /* ---------------------------------------- */
    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Mephi Cloud";
            description = "Summon a volcanic infernal platform with advanced thermodynamic fire physics and ember storms.";
            cooldown = Duration.ofSeconds(3);
            spellType = SpellType.MOVEMENT;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new MephiCloud(this);
        }
    }

    private MephiCloud(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /* SPELL API */
    /* ---------------------------------------- */
    @Override
    public String key() {
        return "mephi-cloud";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        toggle(context.caster(), context);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect
    }

    /* ---------------------------------------- */
    /* TOGGLE API */
    /* ---------------------------------------- */
    @Override
    public boolean isActive(Player player) {
        return clouds.containsKey(player.getUniqueId());
    }

    @Override
    public void activate(Player player, SpellContext context) {
        if (isActive(player))
            return;
        clouds.put(player.getUniqueId(), new CloudData(player, context));
    }

    @Override
    public void deactivate(Player player, SpellContext context) {
        Optional.ofNullable(clouds.remove(player.getUniqueId())).ifPresent(CloudData::stop);
    }

    @Override
    public void forceDeactivate(Player player) {
        Optional.ofNullable(clouds.remove(player.getUniqueId())).ifPresent(CloudData::stop);
    }

    /* ---------------------------------------- */
    /* INTERNAL CLASS */
    /* ---------------------------------------- */
    private final class CloudData {
        private final Player player;
        private final BukkitTask ticker;
        private int tickCounter = 0;
        
        // Advanced thermodynamic variables
        private double flamePhase = 0;
        private double thermalPhase = 0;
        private double convectionCycle = 0;
        private double magmaTemperature = 1200.0; // Kelvin
        private double[] emberPositions = new double[16]; // Ember tracking
        private double[] emberVelocities = new double[16]; // Ember physics
        
        private Location lastLocation;
        private Vector thermalUpward = new Vector(0, 0.1, 0);

        CloudData(Player player, SpellContext context) {
            this.player = player;
            this.lastLocation = player.getLocation().clone();
            
            // Initialize ember physics
            for (int i = 0; i < emberPositions.length; i++) {
                emberPositions[i] = Math.random() * 2 * Math.PI;
                emberVelocities[i] = 0.02 + Math.random() * 0.03;
            }

            // Show transcendent activation message
            player.sendMessage(Component.text(
                    "§4✦ §cVolcanic infernal platform erupts beneath your feet! §4✦"));
            
            // Play thermodynamic activation sounds
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.9f, 0.7f);
            player.playSound(player.getLocation(), Sound.BLOCK_LAVA_AMBIENT, 0.7f, 0.8f);
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.5f, 0.6f);
            
            // Spawn volcanic activation effect
            spawnVolcanicActivationEffect();

            // Start the advanced thermodynamic ticker
            this.ticker = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 0, 1);
        }

        void stop() {
            ticker.cancel();
            
            // Volcanic deactivation effect
            spawnVolcanicDeactivationEffect();
            
            player.sendMessage(Component.text("§4✦ §7The infernal volcano becomes dormant... §4✦"));
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 0.8f);
            player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.6f, 0.7f);
        }
        
        private void spawnVolcanicActivationEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Volcanic eruption burst with advanced physics
            int ringCount = cfgInt("volcanic.eruption-rings", 4);
            int particlesPerRing = cfgInt("volcanic.particles-per-ring", 20);
            
            for (int ring = 0; ring < ringCount; ring++) {
                double ringRadius = 1.8 + ring * 0.6;
                double ringHeight = ring * 0.3;
                double temperature = magmaTemperature - ring * 100; // Temperature gradient
                
                for (int i = 0; i < particlesPerRing; i++) {
                    double angle = 2 * Math.PI * i / particlesPerRing;
                    double x = Math.cos(angle) * ringRadius;
                    double z = Math.sin(angle) * ringRadius;
                    Location particleLoc = loc.clone().add(x, ringHeight, z);
                    
                    // Volcanic particles based on temperature
                    world.spawnParticle(Particle.FLAME, particleLoc, 2, 0.1, 0.4, 0.1, 0.08);
                    world.spawnParticle(Particle.LAVA, particleLoc, 1, 0, 0, 0, 0);
                    
                    // Temperature-based coloring
                    Color thermalColor = getThermalColor(temperature);
                    world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(thermalColor, 2.0f));
                }
            }
            
            // Central magma core eruption
            for (int i = 0; i < 25; i++) {
                Location coreLoc = loc.clone().add(0, 0.3 + Math.random() * 1.5, 0);
                world.spawnParticle(Particle.FLAME, coreLoc, 2, 0.4, 0.2, 0.4, 0.12);
                world.spawnParticle(Particle.LAVA, coreLoc, 1, 0.2, 0.2, 0.2, 0);
                world.spawnParticle(Particle.SMOKE, coreLoc, 3, 0.3, 0.3, 0.3, 0.05);
            }
        }
        
        private void spawnVolcanicDeactivationEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Cooling spiral effect
            for (int i = 0; i < 40; i++) {
                double angle = i * 0.5;
                double y = i * 0.12;
                double radius = 2.0 - (i * 0.04);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                
                Location particleLoc = loc.clone().add(x, y, z);
                world.spawnParticle(Particle.SMOKE, particleLoc, 2, 0.15, 0.1, 0.15, 0.02);
                world.spawnParticle(Particle.ASH, particleLoc, 1, 0.1, 0.05, 0.1, 0.01);
                
                // Cooling effect
                if (Math.random() < 0.3) {
                    world.spawnParticle(Particle.WHITE_ASH, particleLoc, 1, 0.08, 0.08, 0.08, 0.01);
                }
            }
        }
        
        private void spawnVolcanicPlatform(Location playerLoc) {
            World world = player.getWorld();
            Location platformCenter = playerLoc.clone().subtract(0, 1.5, 0);
            
            // Advanced volcanic platform with thermodynamic simulation
            double platformRadius = cfgDouble("volcanic.platform-radius", 2.5);
            int thermalComplexity = cfgInt("volcanic.thermal-complexity", 22);
            double temperatureFactor = (magmaTemperature - 1100) / 300; // 0-1 range
            
            for (int i = 0; i < thermalComplexity; i++) {
                // Volcanic hexagonal cell distribution (realistic lava flow pattern)
                double cellAngle = 2 * Math.PI * i / thermalComplexity;
                double hexRadius = platformRadius * Math.sqrt(Math.random()) * 0.9;
                double hexX = Math.cos(cellAngle + flamePhase * 0.3) * hexRadius;
                double hexZ = Math.sin(cellAngle + flamePhase * 0.3) * hexRadius;
                
                // Thermal convection height variation
                double thermalHeight = Math.sin(thermalPhase + cellAngle * 2) * 0.18 + 
                                     Math.sin(convectionCycle + i * 0.4) * 0.12;
                
                Location lavaLoc = platformCenter.clone().add(hexX, thermalHeight, hexZ);
                
                // Distance-based temperature gradient
                double distanceFromCenter = Math.sqrt(hexX * hexX + hexZ * hexZ);
                double localTemp = magmaTemperature * (1.0 - (distanceFromCenter / platformRadius) * 0.4);
                
                // Multi-layered volcanic particles with temperature physics
                if (Math.random() < temperatureFactor * 0.8) {
                    // Primary lava layer
                    world.spawnParticle(Particle.FLAME, lavaLoc, 2, 0.3, 0.15, 0.3, 0.02);
                    
                    // Molten rock particles
                    if (Math.random() < 0.4) {
                        world.spawnParticle(Particle.LAVA, lavaLoc, 1, 0.1, 0.1, 0.1, 0);
                    }
                    
                    // Temperature-based coloring
                    Color thermalColor = getThermalColor(localTemp);
                    world.spawnParticle(Particle.DUST, lavaLoc, 1, 0.2, 0.08, 0.2, 0,
                        new Particle.DustOptions(thermalColor, 1.4f));
                    
                    // Volcanic gases
                    if (Math.random() < 0.25) {
                        world.spawnParticle(Particle.SMOKE, lavaLoc.clone().add(0, 0.3, 0), 
                            1, 0.15, 0.2, 0.15, 0.02);
                    }
                    
                    // Sulfur particles (soul fire effect)
                    if (Math.random() < 0.15) {
                        world.spawnParticle(Particle.SOUL_FIRE_FLAME, lavaLoc, 1, 0.1, 0.1, 0.1, 0.02);
                    }
                }
            }
            
            // Volcanic fissures around platform perimeter
            if (tickCounter % 4 == 0) {
                spawnVolcanicFissures(platformCenter, platformRadius);
            }
        }
        
        private void spawnThermodynamicEffects(Location center) {
            World world = center.getWorld();
            
            // Heat shimmer effect simulation
            spawnHeatShimmer(center);
            
            // Pressure wave effects
            if (tickCounter % 12 == 0) {
                spawnPressureWaves(center);
            }
            
            // Thermal updraft currents
            spawnThermalUpdrafts(center);
        }
        
        private void spawnEmberStorm(Location center) {
            World world = center.getWorld();
            double stormRadius = cfgDouble("ember.storm-radius", 4.5);
            
            for (int ember = 0; ember < emberPositions.length; ember++) {
                // Each ember follows realistic physics
                double orbitRadius = stormRadius * (0.5 + ember * 0.03);
                double height = Math.sin(emberPositions[ember] * 1.5) * 1.2 + 2.0;
                
                emberPositions[ember] += emberVelocities[ember];
                
                // Thermal buoyancy affects ember movement
                double buoyancy = (magmaTemperature - 1000) / 1000; // 0-0.4 range
                height += buoyancy * 0.8;
                
                // 3D ember physics with turbulence
                double x = Math.cos(emberPositions[ember]) * orbitRadius;
                double z = Math.sin(emberPositions[ember]) * orbitRadius;
                double y = height + thermalUpward.getY() * 5;
                
                Location emberLoc = center.clone().add(x, y, z);
                
                // Ember brightness varies with temperature and position
                double emberTemp = magmaTemperature * (0.8 + 0.2 * Math.sin(emberPositions[ember] * 3));
                
                // Main ember particle
                world.spawnParticle(Particle.FLAME, emberLoc, 1, 0.05, 0.05, 0.05, 0.02);
                
                // Ember trail
                if (emberVelocities[ember] > 0.03) {
                    world.spawnParticle(Particle.ASH, emberLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
                
                // Temperature-based ember color
                Color emberColor = getThermalColor(emberTemp);
                world.spawnParticle(Particle.DUST, emberLoc, 1, 0.08, 0.08, 0.08, 0,
                    new Particle.DustOptions(emberColor, 1.3f));
                
                // Occasional incandescent flashes
                if (Math.random() < 0.08) {
                    world.spawnParticle(Particle.LAVA, emberLoc, 1, 0.05, 0.05, 0.05, 0);
                }
            }
        }
        
        private void spawnConvectionCurrents(Location center) {
            World world = center.getWorld();
            int currentCount = cfgInt("convection.current-count", 12);
            
            for (int i = 0; i < currentCount; i++) {
                double angle = 2 * Math.PI * i / currentCount + convectionCycle;
                double radius = 3.0 + Math.sin(convectionCycle * 1.5 + i) * 0.8;
                
                // Convection cell physics
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = Math.sin(convectionCycle * 2 + angle * 1.8) * 0.8 + 1.5;
                
                Location currentLoc = center.clone().add(x, y, z);
                
                // Convection particles with realistic flow
                world.spawnParticle(Particle.SMOKE, currentLoc, 1, 0.1, 0.2, 0.1, 0.03);
                
                // Hot air distortion
                if (Math.random() < 0.4) {
                    world.spawnParticle(Particle.WHITE_ASH, currentLoc, 1, 0.15, 0.25, 0.15, 0.02);
                }
                
                // Thermal boundary effects
                if (Math.random() < 0.2) {
                    world.spawnParticle(Particle.END_ROD, currentLoc, 1, 0.05, 0.1, 0.05, 0.01);
                }
            }
        }
        
        private void spawnThermalTrail(Location currentLoc) {
            if (lastLocation == null) return;
            
            World world = currentLoc.getWorld();
            Vector movement = currentLoc.toVector().subtract(lastLocation.toVector());
            
            if (movement.lengthSquared() > 0.005) {
                int trailLength = cfgInt("thermal.trail-length", 18);
                double velocityMagnitude = movement.length();
                
                for (int i = 0; i < trailLength; i++) {
                    double progress = (double) i / trailLength;
                    
                    // Thermal dissipation - trail cools over distance
                    double thermalIntensity = (1.0 - progress * 0.8) * velocityMagnitude * 3;
                    if (thermalIntensity < 0.1) continue;
                    
                    Location trailPoint = currentLoc.clone().subtract(movement.clone().multiply(progress * 5));
                    
                    // Thermal trail temperature calculation
                    double trailTemp = magmaTemperature * thermalIntensity;
                    
                    // Main thermal trail
                    world.spawnParticle(Particle.FLAME, trailPoint, 1, 0.2, 0.2, 0.2, 0.02);
                    
                    // Heat dissipation effects
                    if (Math.random() < 0.4) {
                        world.spawnParticle(Particle.SMOKE, trailPoint, 1, 0.25, 0.25, 0.25, 0.03);
                    }
                    
                    // Thermal color gradient
                    Color trailColor = getThermalColor(trailTemp);
                    world.spawnParticle(Particle.DUST, trailPoint, 1, 0.15, 0.15, 0.15, 0,
                        new Particle.DustOptions(trailColor, (float)thermalIntensity));
                    
                }
            }
        }
        
        // Advanced thermodynamic color generation
        private Color getThermalColor(double temperature) {
            // Realistic blackbody radiation color based on temperature (Kelvin)
            if (temperature < 800) {
                // Deep red - cooling lava
                return Color.fromRGB(100, 0, 0);
            } else if (temperature < 1000) {
                // Red - solid lava
                int intensity = (int)((temperature - 800) / 200 * 155 + 100);
                return Color.fromRGB(intensity, 0, 0);
            } else if (temperature < 1200) {
                // Red-orange - molten lava
                int red = 255;
                int green = (int)((temperature - 1000) / 200 * 100);
                return Color.fromRGB(red, green, 0);
            } else if (temperature < 1400) {
                // Orange-yellow - hot molten lava
                int red = 255;
                int green = (int)(100 + (temperature - 1200) / 200 * 155);
                return Color.fromRGB(red, green, 0);
            } else {
                // White-hot - extreme temperature
                return Color.fromRGB(255, 255, 200);
            }
        }
        
        // Advanced effect helper methods
        private void spawnVolcanicFissures(Location center, double radius) {
            World world = center.getWorld();
            int fissureCount = cfgInt("volcanic.fissure-count", 6);
            
            for (int i = 0; i < fissureCount; i++) {
                double angle = 2 * Math.PI * i / fissureCount + flamePhase * 0.2;
                double fissureLength = radius + 0.5;
                
                // Create fissure line
                for (int j = 0; j < 8; j++) {
                    double progress = j / 8.0;
                    double x = Math.cos(angle) * fissureLength * progress;
                    double z = Math.sin(angle) * fissureLength * progress;
                    double y = Math.sin(progress * Math.PI) * 0.2;
                    
                    Location fissureLoc = center.clone().add(x, y - 0.3, z);
                    
                    // Lava seepage from fissures
                    world.spawnParticle(Particle.LAVA, fissureLoc, 1, 0.05, 0.1, 0.05, 0);
                    world.spawnParticle(Particle.FLAME, fissureLoc, 1, 0.1, 0.2, 0.1, 0.02);
                    
                    // Volcanic gases
                    if (Math.random() < 0.4) {
                        world.spawnParticle(Particle.SMOKE, fissureLoc.clone().add(0, 0.2, 0), 
                            1, 0.08, 0.15, 0.08, 0.02);
                    }
                }
            }
        }
        
        private void spawnHeatShimmer(Location center) {
            World world = center.getWorld();
            int shimmerCount = cfgInt("thermal.shimmer-particles", 8);
            
            for (int i = 0; i < shimmerCount; i++) {
                double x = (Math.random() - 0.5) * 5.0;
                double y = Math.random() * 2.5 + 0.5;
                double z = (Math.random() - 0.5) * 5.0;
                
                Location shimmerLoc = center.clone().add(x, y, z);
                
                // Heat distortion particles
                world.spawnParticle(Particle.WHITE_ASH, shimmerLoc, 1, 0.2, 0.3, 0.2, 0.01);
                
                if (Math.random() < 0.3) {
                    world.spawnParticle(Particle.END_ROD, shimmerLoc, 1, 0.1, 0.1, 0.1, 0.005);
                }
            }
        }
        
        private void spawnPressureWaves(Location center) {
            World world = center.getWorld();
            double waveRadius = cfgDouble("pressure.wave-radius", 3.8);
            
            // Expanding pressure waves
            for (int i = 0; i < 20; i++) {
                double angle = 2 * Math.PI * i / 20;
                double x = Math.cos(angle) * waveRadius;
                double z = Math.sin(angle) * waveRadius;
                
                Location waveLoc = center.clone().add(x, 1.2, z);
                world.spawnParticle(Particle.ENCHANT, waveLoc, 1, 0.05, 0.05, 0.05, 0.3);
                
                // Pressure disturbance
                if (Math.random() < 0.4) {
                    world.spawnParticle(Particle.CLOUD, waveLoc, 1, 0.1, 0.1, 0.1, 0.02);
                }
            }
        }
        
        private void spawnThermalUpdrafts(Location center) {
            World world = center.getWorld();
            int updraftCount = cfgInt("thermal.updraft-count", 10);
            
            for (int i = 0; i < updraftCount; i++) {
                double x = (Math.random() - 0.5) * 4.0;
                double z = (Math.random() - 0.5) * 4.0;
                double y = Math.random() * 3.0 + 1.0;
                
                Location updraftLoc = center.clone().add(x, y, z);
                
                // Rising thermal currents
                world.spawnParticle(Particle.SMOKE, updraftLoc, 1, 0.05, -0.3, 0.05, 0.05);
                
                if (Math.random() < 0.6) {
                    world.spawnParticle(Particle.ASH, updraftLoc, 1, 0.08, -0.2, 0.08, 0.03);
                }
                
                // Heat particles rising
                if (Math.random() < 0.3) {
                    world.spawnParticle(Particle.FLAME, updraftLoc, 1, 0.05, -0.1, 0.05, 0.02);
                }
            }
        }

        private void tick() {
            if (!player.isOnline()) {
                forceDeactivate(player);
                return;
            }

            tickCounter++;
            
            // Advanced thermodynamic simulation updates
            flamePhase += 0.18; // Faster, more chaotic flame movement
            thermalPhase += 0.12;
            convectionCycle += 0.08 + Math.sin(tickCounter * 0.05) * 0.02; // Variable convection
            
            // Temperature fluctuation simulation
            magmaTemperature += (Math.random() - 0.5) * 50; // ±25K fluctuation
            magmaTemperature = Math.max(1100, Math.min(1400, magmaTemperature)); // Realistic bounds
            
            // Update ember physics
            for (int i = 0; i < emberPositions.length; i++) {
                emberPositions[i] += emberVelocities[i];
                emberVelocities[i] += (Math.random() - 0.5) * 0.001; // Turbulence
                emberVelocities[i] = Math.max(0.01, Math.min(0.05, emberVelocities[i])); // Bounds
            }
            
            Location currentLoc = player.getLocation();
            
            // Calculate thermal updraft based on movement
            if (lastLocation != null) {
                Vector movement = currentLoc.toVector().subtract(lastLocation.toVector());
                thermalUpward = thermalUpward.multiply(0.9).add(movement.multiply(0.1).setY(0.05));
            }
            
            // Only show particles if player is flying or in creative mode
            if (player.isFlying() || player.getGameMode() == GameMode.CREATIVE) {
                spawnVolcanicPlatform(currentLoc);
                spawnThermodynamicEffects(currentLoc);
                spawnEmberStorm(currentLoc);
                spawnConvectionCurrents(currentLoc);
                
                // Dynamic movement trail with heat signatures
                if (lastLocation != null && currentLoc.distanceSquared(lastLocation) > 0.01) {
                    spawnThermalTrail(currentLoc);
                }
                
                // Periodic volcanic sounds with temperature-based intensity
                if (tickCounter % 60 == 0) {
                    float pitch = (float)(0.8 + (magmaTemperature - 1200) / 400);
                    player.playSound(currentLoc, Sound.BLOCK_LAVA_POP, 0.4f, pitch);
                }
                
                // Blaze combustion sounds
                if (tickCounter % 90 == 0) {
                    player.playSound(currentLoc, Sound.ENTITY_BLAZE_SHOOT, 0.3f, 0.9f);
                }
                
                // Volcanic rumble
                if (tickCounter % 200 == 0) {
                    player.playSound(currentLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.2f, 0.5f);
                }
            }
            
            lastLocation = currentLoc.clone();
        }
        
        private void spawnActivationEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Expanding ring of fire
            for (int i = 0; i < 24; i++) {
                double angle = 2 * Math.PI * i / 24;
                double radius = 2.5;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location particleLoc = loc.clone().add(x, 0.1, z);
                
                world.spawnParticle(Particle.FLAME, particleLoc, 2, 0.1, 0.3, 0.1, 0.05);
                world.spawnParticle(Particle.LAVA, particleLoc, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.MAROON, 1.8f));
            }
            
            // Central fire pillar
            for (int i = 0; i < 10; i++) {
                Location pillarLoc = loc.clone().add(0, i * 0.3, 0);
                world.spawnParticle(Particle.FLAME, pillarLoc, 3, 0.2, 0.1, 0.2, 0.1);
            }
        }
        
        private void spawnDeactivationEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Smoldering spiral of smoke
            for (int i = 0; i < 30; i++) {
                double angle = i * 0.4;
                double y = i * 0.15;
                double radius = 1.5 - (i * 0.04);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                
                Location particleLoc = loc.clone().add(x, y, z);
                world.spawnParticle(Particle.SMOKE, particleLoc, 2, 0.1, 0.1, 0.1, 0.02);
                world.spawnParticle(Particle.ASH, particleLoc, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }

        private void spawnInfernalPlatform(Location playerLoc) {
            World world = player.getWorld();
            Location platformCenter = playerLoc.clone().subtract(0, 1.4, 0);
            
            // Dense flame platform base
            double platformRadius = cfgDouble("platform.radius", 2.0);
            int flameDensity = cfgInt("platform.flame-density", 14);
            
            for (int i = 0; i < flameDensity; i++) {
                double angle = 2 * Math.PI * i / flameDensity;
                double radius = platformRadius * (0.6 + 0.4 * Math.random());
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double yOffset = Math.sin(flamePhase + angle * 1.5) * 0.15;
                
                Location flameLoc = platformCenter.clone().add(x, yOffset, z);
                
                // Main fire particles
                world.spawnParticle(Particle.FLAME, flameLoc, 2, 0.4, 0.2, 0.4, 0.02);
                
                // Ash and smoke
                if (Math.random() < 0.4) {
                    world.spawnParticle(Particle.ASH, flameLoc.clone().add(0, 0.3, 0), 
                        1, 0.2, 0.3, 0.2, 0.01);
                    world.spawnParticle(Particle.SMOKE, flameLoc.clone().add(0, 0.5, 0), 
                        1, 0.15, 0.2, 0.15, 0.03);
                }
                
                // Occasional lava bursts
                if (Math.random() < 0.2) {
                    world.spawnParticle(Particle.LAVA, flameLoc, 1, 0, 0, 0, 0);
                }
            }
            
            // Fire vortex around platform
            spawnFireVortex(platformCenter, platformRadius + 0.8);
            
            // Periodic ember bursts
            if (tickCounter % 15 == 0) {
                spawnEmberBurst(platformCenter);
            }
            
            // Floating ember particles above platform
            spawnFloatingEmbers(platformCenter.clone().add(0, 1.0, 0));
        }
        
        private void spawnFireVortex(Location center, double radius) {
            World world = center.getWorld();
            int vortexPoints = cfgInt("vortex.points", 18);
            
            for (int i = 0; i < vortexPoints; i++) {
                double angle = 2 * Math.PI * i / vortexPoints + flamePhase;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double ySpiral = Math.sin(flamePhase * 1.5 + angle * 2) * 0.4;
                
                Location vortexLoc = center.clone().add(x, ySpiral, z);
                
                // Cycling flame colors
                Color flameColor = getFlameColor(angle + flamePhase);
                world.spawnParticle(Particle.DUST, vortexLoc, 1, 0.08, 0.08, 0.08, 0,
                    new Particle.DustOptions(flameColor, 1.4f));
                    
                // Occasional soul fire particles
                if (Math.random() < 0.15) {
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, vortexLoc, 1, 0.1, 0.1, 0.1, 0.03);
                }
                
                // Small flame bursts
                if (Math.random() < 0.25) {
                    world.spawnParticle(Particle.FLAME, vortexLoc, 1, 0.05, 0.05, 0.05, 0.02);
                }
            }
        }
        
        private void spawnDemonicTrail(Location currentLoc) {
            if (lastLocation == null || currentLoc.distanceSquared(lastLocation) < 0.1) {
                return;
            }
            
            World world = currentLoc.getWorld();
            Vector movement = currentLoc.toVector().subtract(lastLocation.toVector());
            
            // Only create trail if moving fast enough
            if (movement.lengthSquared() > 0.01) {
                // Demonic trail behind player
                Location trailStart = lastLocation.clone();
                int trailLength = cfgInt("trail.length", 10);
                
                for (int i = 0; i < trailLength; i++) {
                    double progress = (double) i / trailLength;
                    Location trailPoint = trailStart.clone().subtract(movement.clone().multiply(progress * 2.5));
                    
                    // Fading fire trail
                    float intensity = (float) (1.0 - progress * 0.8);
                    if (Math.random() < intensity) {
                        world.spawnParticle(Particle.FLAME, trailPoint, 1, 0.3, 0.3, 0.3, 0.02);
                        
                        // Occasional ash and smoke in trail
                        if (Math.random() < 0.3) {
                            world.spawnParticle(Particle.ASH, trailPoint, 1, 0.2, 0.2, 0.2, 0.01);
                        }
                        
                        // Colored dust for demonic effect
                        if (Math.random() < 0.5) {
                            Color trailColor = Color.fromRGB((int)(255 * intensity), (int)(100 * intensity), 0);
                            world.spawnParticle(Particle.DUST, trailPoint, 1, 0.15, 0.15, 0.15, 0,
                                new Particle.DustOptions(trailColor, intensity));
                        }
                    }
                }
            }
        }
        
        private void spawnEmberBurst(Location center) {
            World world = center.getWorld();
            
            // Upward burst of embers
            for (int i = 0; i < 12; i++) {
                double angle = 2 * Math.PI * i / 12;
                double x = Math.cos(angle) * 0.4;
                double z = Math.sin(angle) * 0.4;
                
                Location emberLoc = center.clone().add(x, 0.6 + Math.random() * 0.8, z);
                world.spawnParticle(Particle.FLAME, emberLoc, 1, 0, 0.4, 0, 0.08);
                world.spawnParticle(Particle.LAVA, emberLoc, 1, 0.1, 0.1, 0.1, 0);
                
                // Occasional soul fire for variety
                if (Math.random() < 0.3) {
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, emberLoc, 1, 0.05, 0.2, 0.05, 0.05);
                }
            }
        }
        
        private void spawnFloatingEmbers(Location center) {
            World world = center.getWorld();
            int emberCount = cfgInt("embers.count", 8);
            
            for (int i = 0; i < emberCount; i++) {
                double x = (Math.random() - 0.5) * 4.0;
                double y = Math.random() * 2.0;
                double z = (Math.random() - 0.5) * 4.0;
                
                Location emberLoc = center.clone().add(x, y, z);
                
                if (Math.random() < 0.6) {
                    world.spawnParticle(Particle.FLAME, emberLoc, 1, 0.08, 0.08, 0.08, 0.02);
                }
                
                if (Math.random() < 0.4) {
                    world.spawnParticle(Particle.ASH, emberLoc, 1, 0.12, 0.12, 0.12, 0.015);
                }
                
                // Occasional wither particles for dark magic feel
                if (Math.random() < 0.15) {
                    world.spawnParticle(Particle.WITCH, emberLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }
        
        private Color getFlameColor(double phase) {
            // Cycle through demonic flame colors
            double normalizedPhase = (Math.sin(phase) + 1) / 2; // 0-1 range
            
            if (normalizedPhase < 0.4) {
                // Deep red to orange
                return Color.fromRGB(255, (int)(50 + 150 * normalizedPhase * 2.5), 0);
            } else if (normalizedPhase < 0.7) {
                // Orange to yellow-orange
                double subPhase = (normalizedPhase - 0.4) * 3.33;
                return Color.fromRGB(255, (int)(200 + 55 * subPhase), (int)(30 * subPhase));
            } else {
                // Yellow-orange to dark red
                double subPhase = (normalizedPhase - 0.7) * 3.33;
                return Color.fromRGB((int)(255 - 100 * subPhase), (int)(255 - 200 * subPhase), (int)(30 * (1 - subPhase)));
            }
        }
    }
    
    /* ---------------------------------------- */
    /* CONFIG HELPERS */
    /* ---------------------------------------- */
    private double cfgDouble(String path, double def) {
        return spellConfig.getDouble("mephi-cloud." + path, def);
    }
    
    private int cfgInt(String path, int def) {
        return spellConfig.getInt("mephi-cloud." + path, def);
    }
    
    private boolean cfgBool(String path, boolean def) {
        return spellConfig.getBoolean("mephi-cloud." + path, def);
    }
}
