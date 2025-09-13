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
 * DragonFury 1.0 - Revolutionary draconic flight spell with dragon transformation mechanics.
 * 
 * Extraordinary Features:
 * - Dragon scale armor manifestation with metallic particle rendering
 * - Flame breath trails with realistic combustion physics
 * - Draconic wing formation with aerodynamic lift simulation
 * - Ancient dragon roar effects with sonic wave propagation
 * - Dragonfire aura with thermal radiation visualization
 * - Scale armor protection with damage absorption mechanics
 * - Flame breath attacks with projectile fire trails
 * - Draconic rage mode with enhanced combat capabilities
 */
public final class DragonFury extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /* DATA */
    /* ---------------------------------------- */
    private final Map<UUID, DragonRiderData> dragonRiders = new WeakHashMap<>();
    private static final String DRAGON_REGENERATION_PATH = "effects.dragon-regeneration";
    private static final boolean DRAGON_REGENERATION_DEFAULT = true;

    /* ---------------------------------------- */
    /* BUILDER */
    /* ---------------------------------------- */
    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Dragon Fury";
            description = "Transform into a mighty dragon with scale armor, flame breath, and devastating fury.";
            cooldown = Duration.ofSeconds(15);
            spellType = SpellType.MOVEMENT;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new DragonFury(this);
        }
    }

    private DragonFury(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /* SPELL API */
    /* ---------------------------------------- */
    @Override
    public @NotNull String key() {
        return "dragon-fury";
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
        // Instant effect
    }

    /* ---------------------------------------- */
    /* TOGGLE API */
    /* ---------------------------------------- */
    @Override
    public boolean isActive(@NotNull Player player) {
        return dragonRiders.containsKey(player.getUniqueId());
    }

    @Override
    public void activate(@NotNull Player player, @NotNull SpellContext context) {
        if (isActive(player))
            return;
        dragonRiders.put(player.getUniqueId(), new DragonRiderData(player, context));
    }

    @Override
    public void deactivate(@NotNull Player player, @NotNull SpellContext context) {
        Optional.ofNullable(dragonRiders.remove(player.getUniqueId())).ifPresent(DragonRiderData::stop);
    }

    @Override
    public void forceDeactivate(@NotNull Player player) {
        Optional.ofNullable(dragonRiders.remove(player.getUniqueId())).ifPresent(DragonRiderData::stop);
    }

    @Override
    public int getMaxDuration() {
        return cfgInt("max-duration-ticks", 1500); // 1.25 minutes default
    }

    /* ---------------------------------------- */
    /* INTERNAL CLASS */
    /* ---------------------------------------- */
    private final class DragonRiderData {
        private final Player player;
        private final BossBar dragonFuryBar = BossBar.bossBar(Component.text("Dragon Fury"), 1,
                BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        private final BukkitTask ticker;
        
        // Dragon riding variables
        private double dragonFury = 100;
        private int tickCounter = 0;
        private double dragonPhase = 0;
        private double scaleArmor = 0;
        private double flameBreath = 0;
        private DragonState currentState = DragonState.AWAKENING;
        private int flameBreaths = 0;
        private int dragonRoars = 0;
        private Location lastLocation;
        private final Vector[] scalePositions = new Vector[24]; // Dragon scale positions
        private boolean isRaging = false;
        private int rageDuration = 0;

        // Dragon states for transformation progression (kept for compatibility)
        private enum DragonState {
            AWAKENING,
            SCALED,
            BREATHING,
            RAGING
        }

        DragonRiderData(Player player, SpellContext context) {
            this.player = player;
            this.lastLocation = player.getLocation().clone();
            
            // Initialize dragon scale positions
            initializeDragonScales();
            
            // Dragon awakening sounds
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.6f);
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.6f, 1.2f);
            
            // Minimal: no activation chat message
            
            // Grant flight if not already flying
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
                player.setFlying(true);
            }
            
            // Start the dragon rider ticker
            this.ticker = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 0, 1);
        }

        void stop() {
            ticker.cancel();
            
            // Remove dragon effects
            removeDragonEffects();
            
            // Final dragon departure effect
            spawnDragonDeactivationEffect();
            
            // Minimal: no deactivation chat message
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 0.6f, 1.0f);
            
            // Remove flight if granted by spell
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }

        private void tick() {
            if (!player.isOnline()) {
                forceDeactivate(player);
                return;
            }

            // Minimal fixed effects (no progression)
            int duration = 60;
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 0, false, false));

            // Simple dark wings behind the player (dark blue/purple/black)
            Location base = player.getLocation();
            World world = base.getWorld();
            if (world != null) {
                Vector dir = base.getDirection().normalize();
                Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
                Vector up = new Vector(0, 1, 0);
                Vector back = dir.clone().multiply(-1);

                // Wing parameters
                int points = 16;
                double span = 1.9;
                double backOffset = 0.6;

                // Color palette
                Color darkBlue = Color.fromRGB(20, 30, 90);
                Color purple = Color.fromRGB(80, 0, 120);
                Color black = Color.fromRGB(8, 8, 8);

                for (int side = -1; side <= 1; side += 2) {
                    Vector sideVec = right.clone().multiply(side);
                    for (int i = 0; i < points; i++) {
                        double t = i / (double) (points - 1);
                        double outward = span * t;
                        double arch = 0.6 + Math.sin(t * Math.PI) * 1.0; // wing arch
                        double backZ = backOffset + t * 0.25;

                        Location p = base.clone()
                                .add(sideVec.clone().multiply(outward))
                                .add(up.clone().multiply(arch))
                                .add(back.clone().multiply(backZ));

                        // Inner fill with dark dust (rotate colors gently)
                        Color c = (i % 3 == 0) ? darkBlue : (i % 3 == 1 ? purple : black);
                        world.spawnParticle(Particle.DUST, p, 1, 0.03, 0.03, 0.03, 0,
                                new Particle.DustOptions(c, 1.0f));

                        // Subtle white-ish edge highlight every few points
                        if (i % 2 == 0) {
                            Location edge = p.clone().add(sideVec.clone().multiply(0.05)).add(up.clone().multiply(0.05));
                            world.spawnParticle(Particle.END_ROD, edge, 1, 0.01, 0.01, 0.01, 0.003);
                        }
                    }
                }
            }

            lastLocation = base.clone();
        }
        
        private void initializeDragonScales() {
            // Initialize dragon scale armor positions
            for (int i = 0; i < scalePositions.length; i++) {
                double angle = 2 * Math.PI * i / scalePositions.length;
                double radius = 1.0 + (i % 3) * 0.3; // Layered scales
                scalePositions[i] = new Vector(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            }
        }
        
        private void updateDragonState() {
            // Dragon state progression based on fury and activity
            if (isRaging) {
                currentState = DragonState.RAGING;
            } else if (flameBreaths > 3) {
                currentState = DragonState.BREATHING;
            } else if (tickCounter > 100) {
                currentState = DragonState.SCALED;
            } else {
                currentState = DragonState.AWAKENING;
            }
        }
        
        private double getEnergyCostForState() {
            return switch (currentState) {
                case AWAKENING -> cfgDouble("energy.awakening-cost", 0.1);
                case SCALED -> cfgDouble("energy.scaled-cost", 0.2);
                case BREATHING -> cfgDouble("energy.breathing-cost", 0.35);
                case RAGING -> cfgDouble("energy.raging-cost", 0.6);
            };
        }
        
        private void updateDragonFuryDisplay() {
            String displayText = "Dragon Fury";
            if (flameBreaths > 0) {
                displayText += " - Flames: " + flameBreaths;
            }
            if (dragonRoars > 0) {
                displayText += " - Roars: " + dragonRoars;
            }
            if (isRaging) {
                displayText += " - RAGING!";
            }
            dragonFuryBar.name(Component.text(displayText));
        }
        
        private void enterRageMode() {
            isRaging = true;
            rageDuration = cfgInt("rage.duration-ticks", 200);
            dragonFury = Math.min(100, dragonFury + cfgDouble("rage.fury-boost", 30));
            
            // Rage activation effect
            spawnRageActivationEffect();
            
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.5f);
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.0f);
        }
        
        private void exitRageMode() {
            isRaging = false;
            rageDuration = 0;
            
            // Rage exit effect
            spawnRageExitEffect();
            
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, 0.8f, 1.2f);
        }
        
        private void triggerFlameBreath() {
            if (dragonFury < cfgDouble("flame-breath.energy-cost", 15)) {
                return;
            }
            
            flameBreaths++;
            dragonFury -= cfgDouble("flame-breath.energy-cost", 15);
            
            // Flame breath effect
            spawnFlameBreath();
            
            // Flame breath sounds
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.8f, 1.2f);
        }
        
        private void triggerDragonRoar() {
            dragonRoars++;
            
            // Dragon roar effect
            spawnDragonRoarEffect();
            
            // Dragon roar sounds
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.7f);
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.8f);
            
            // Sonic wave effect
            for (Player nearbyPlayer : player.getWorld().getPlayers()) {
                if (nearbyPlayer != player && nearbyPlayer.getLocation().distance(player.getLocation()) < 20) {
                    nearbyPlayer.playSound(nearbyPlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 0.9f);
                }
            }
        }
        
        private void spawnDragonAwakeningEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Dragon awakening transformation
            for (int i = 0; i < 30; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = Math.random() * 2.0;
                double height = Math.random() * 1.8;
                
                Location dragonLoc = loc.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );
                
                // Dragon awakening particles
                world.spawnParticle(Particle.FLAME, dragonLoc, 1, 0.15, 0.15, 0.15, 0.01);
                
                // Dragon energy
                Color dragonColor = Color.fromRGB(180, 50, 50);
                world.spawnParticle(Particle.DUST, dragonLoc, 1, 0.05, 0.05, 0.05, 0,
                    new Particle.DustOptions(dragonColor, 1.0f));
            }
            
            // Central dragon emergence
            for (int i = 0; i < 16; i++) {
                double spiral = i * 0.4;
                double radius = i * 0.08;
                Location spiralLoc = loc.clone().add(
                    Math.cos(spiral) * radius,
                    i * 0.15,
                    Math.sin(spiral) * radius
                );
                
                world.spawnParticle(Particle.FLAME, spiralLoc, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }
        
        private void spawnDragonForm(Location center) {
            // Simplified: single subtle form only
            spawnAwakeningDragonForm(center);
        }
        
        private void spawnAwakeningDragonForm(Location center) {
            World world = center.getWorld();
            
            // Awakening dragon essence
            double awakeRadius = cfgDouble("dragon.awakening-radius", 1.4);
            int awakeParticles = cfgInt("dragon.awakening-particles", 8);
            
            for (int i = 0; i < awakeParticles; i++) {
                double angle = 2 * Math.PI * i / awakeParticles + dragonPhase * 0.5;
                double radius = awakeRadius * (0.7 + 0.3 * Math.sin(dragonPhase + i));
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = Math.sin(dragonPhase * 0.8 + angle) * 0.8 + 1.8;
                
                Location awakeLoc = center.clone().add(x, y, z);
                
                // Subtle awakening flames
                world.spawnParticle(Particle.FLAME, awakeLoc, 1, 0.08, 0.08, 0.08, 0.01);
                
                if (Math.random() < 0.5) {
                    Color awakeColor = Color.fromRGB(150, 50, 50);
                    world.spawnParticle(Particle.DUST, awakeLoc, 1, 0.04, 0.04, 0.04, 0,
                        new Particle.DustOptions(awakeColor, 0.9f));
                }
            }
        }

        private void spawnDragonTrail(Location currentLoc) {
            if (lastLocation == null) return;
            
            World world = currentLoc.getWorld();
            Vector movement = currentLoc.toVector().subtract(lastLocation.toVector());
            
            if (movement.lengthSquared() > 0.005) {
                int trailLength = cfgInt("dragon.trail-length", 8);
                double velocityMagnitude = movement.length();
                
                for (int i = 0; i < trailLength; i++) {
                    double progress = (double) i / trailLength;
                    Location trailPoint = currentLoc.clone().subtract(movement.clone().multiply(progress * 6));
                    
                    // Trail intensity based on velocity and state
                    float intensity = (float) ((1.0 - progress * 0.8) * velocityMagnitude * 4);
                    intensity *= getStateIntensityMultiplier();
                    
                    if (intensity > 0.1f) {
                        // Dragon trail
                        world.spawnParticle(Particle.FLAME, trailPoint, 1, 0.06, 0.06, 0.06, 0.01);
                        
                        // State-specific trail effects
                        if (currentState == DragonState.RAGING) {
                            // No lava in normal mode
                        }
                        
                        // Dragon trail color
                        Color trailColor = getDragonTrailColor(progress);
                        world.spawnParticle(Particle.DUST, trailPoint, 1, 0.04, 0.04, 0.04, 0,
                            new Particle.DustOptions(trailColor, Math.min(1.0f, intensity * 0.4f)));
                    }
                }
            }
        }
        
        private void spawnFlameBreath() {
            Location eyeLoc = player.getEyeLocation();
            Vector direction = eyeLoc.getDirection();
            World world = eyeLoc.getWorld();
            double breathDistance = cfgDouble("flame-breath.distance", 6.0);
            
            // Flame breath stream
            for (int i = 0; i < 12; i++) {
                double progress = (double) i / 25;
                Location breathLoc = eyeLoc.clone().add(direction.clone().multiply(progress * breathDistance));
                
                // Flame breath cone expansion
                double coneRadius = progress * cfgDouble("flame-breath.cone-radius", 1.2);
                
                for (int cone = 0; cone < 5; cone++) {
                    double coneAngle = 2 * Math.PI * cone / 8;
                    double coneX = Math.cos(coneAngle) * coneRadius * Math.random();
                    double coneZ = Math.sin(coneAngle) * coneRadius * Math.random();
                    
                    Location flameLoc = breathLoc.clone().add(coneX, Math.random() * coneRadius - coneRadius/2, coneZ);
                    
                    // Flame breath particles
                    world.spawnParticle(Particle.FLAME, flameLoc, 1, 0.06, 0.06, 0.06, 0.01);
                    
                    if (Math.random() < 0.6) {
                        Color flameColor = Color.fromRGB(255, (int)(100 + 155 * (1 - progress)), 0);
                        world.spawnParticle(Particle.DUST, flameLoc, 1, 0.05, 0.05, 0.05, 0,
                            new Particle.DustOptions(flameColor, 1.0f));
                    }
                }
            }
        }
        
        private void spawnDragonRoarEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            double roarRadius = cfgDouble("roar.radius", 4.0);
            
            // Sonic wave circles
            for (int wave = 1; wave <= 3; wave++) {
                double waveRadius = roarRadius * wave / 5;
                int waveParticles = 10 * wave;
                
                for (int i = 0; i < waveParticles; i++) {
                    double angle = 2 * Math.PI * i / waveParticles;
                    double x = Math.cos(angle) * waveRadius;
                    double z = Math.sin(angle) * waveRadius;
                    double y = Math.sin(angle * 4) * 0.5 + 1.5;
                    
                    Location waveLoc = loc.clone().add(x, y, z);
                    
                    // Sonic wave
                    world.spawnParticle(Particle.CLOUD, waveLoc, 1, 0.05, 0.05, 0.05, 0.01);
                    
                    // Sound wave visualization
                    Color waveColor = Color.fromRGB(150, 150, 150);
                    world.spawnParticle(Particle.DUST, waveLoc, 1, 0.03, 0.03, 0.03, 0,
                        new Particle.DustOptions(waveColor, 0.9f));
                }
            }
        }
        
        private void spawnRageActivationEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Rage explosion
            for (int i = 0; i < 24; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = Math.random() * 2.5;
                double height = Math.random() * 2.0;
                
                Location rageLoc = loc.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );
                
                // Rage particles
                world.spawnParticle(Particle.FLAME, rageLoc, 1, 0.1, 0.1, 0.1, 0.02);
                
                // Rage energy
                Color rageColor = Color.fromRGB(255, 0, 0);
                world.spawnParticle(Particle.DUST, rageLoc, 3, 0.3, 0.3, 0.3, 0,
                    new Particle.DustOptions(rageColor, 2.0f));
            }
        }
        
        private void spawnRageExitEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Rage subsiding
            for (int i = 0; i < 16; i++) {
                double angle = i * 0.3;
                double radius = 3.0 - (i * 0.1);
                double height = i * 0.08;
                
                Location exitLoc = loc.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );
                
                world.spawnParticle(Particle.SMOKE, exitLoc, 1, 0.08, 0.08, 0.08, 0.01);
                
                // Cooling down
                if (Math.random() < 0.4) {
                    Color coolColor = Color.fromRGB((int)(200 * (1 - i * 0.06)), (int)(100 * (1 - i * 0.06)), 0);
                    world.spawnParticle(Particle.DUST, exitLoc, 1, 0.03, 0.03, 0.03, 0,
                        new Particle.DustOptions(coolColor, 0.8f));
                }
            }
        }
        
        private void spawnDragonDeactivationEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Dragon departure
            for (int i = 0; i < 20; i++) {
                double angle = i * 0.4;
                double radius = 3.0 - (i * 0.09);
                double height = i * 0.08;
                
                Location departureLoc = loc.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );
                
                world.spawnParticle(Particle.SMOKE, departureLoc, 1, 0.08, 0.08, 0.08, 0.01);
                
                // Fading dragon energy
                // Keep deactivation subtle; no extra flames
            }
        }
        
        // Color and effect helpers
        private Color getDragonScaleColor(double phase) {
            // Dragon scale metallic colors
            double normalizedPhase = (Math.sin(phase) + 1) / 2;
            
            if (normalizedPhase < 0.33) {
                // Dark red to red
                return Color.fromRGB((int)(80 + 175 * normalizedPhase * 3), 20, 20);
            } else if (normalizedPhase < 0.67) {
                // Red to orange
                double subPhase = (normalizedPhase - 0.33) * 3;
                return Color.fromRGB(255, (int)(20 + 80 * subPhase), 0);
            } else {
                // Orange to gold
                double subPhase = (normalizedPhase - 0.67) * 3;
                return Color.fromRGB(255, (int)(100 + 155 * subPhase), (int)(100 * subPhase));
            }
        }
        
        private Color getDragonWingColor(double progress) {
            // Dragon wing membrane colors
            if (progress < 0.5) {
                // Dark red to bright red
                return Color.fromRGB((int)(100 + 155 * progress * 2), (int)(30 * progress * 2), 0);
            } else {
                // Bright red to flame orange
                double subProgress = (progress - 0.5) * 2;
                return Color.fromRGB(255, (int)(30 + 70 * subProgress), 0);
            }
        }
        
        private Color getFlameColor(double intensity) {
            // Flame heat colors
            if (intensity < 0.33) {
                // Red flames
                return Color.fromRGB(255, (int)(50 * intensity * 3), 0);
            } else if (intensity < 0.67) {
                // Orange flames
                double subIntensity = (intensity - 0.33) * 3;
                return Color.fromRGB(255, (int)(50 + 150 * subIntensity), 0);
            } else {
                // White-hot flames
                double subIntensity = (intensity - 0.67) * 3;
                return Color.fromRGB(255, (int)(200 + 55 * subIntensity), (int)(200 * subIntensity));
            }
        }
        
        private Color getRageColor(double intensity) {
            // Rage intensity colors
            if (intensity < 0.5) {
                // Deep red rage
                return Color.fromRGB(200, 0, 0);
            } else {
                // Blazing rage
                return Color.fromRGB(255, (int)(100 * (intensity - 0.5) * 2), 0);
            }
        }
        
        private Color getDragonTrailColor(double age) {
            // Trail color based on age and state
            double alpha = 1.0 - age;
            
            return switch (currentState) {
                case AWAKENING -> Color.fromRGB((int)(150 * alpha), (int)(50 * alpha), (int)(50 * alpha));
                case SCALED -> Color.fromRGB((int)(200 * alpha), (int)(80 * alpha), (int)(20 * alpha));
                case BREATHING -> Color.fromRGB((int)(255 * alpha), (int)(150 * alpha), 0);
                case RAGING -> Color.fromRGB((int)(255 * alpha), (int)(50 * alpha), 0);
            };
        }
        
        private double getStateIntensityMultiplier() {
            return switch (currentState) {
                case AWAKENING -> 0.8;
                case SCALED -> 1.2;
                case BREATHING -> 1.6;
                case RAGING -> 2.5;
            };
        }
        
        private void applyDragonEffects() {
            int duration = cfgInt("effects.duration-ticks", 40);
            
            // Dragon flight
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, false));
            
            // Fire immunity
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0, false, false));
            
            // State-specific effects
            switch (currentState) {
                case AWAKENING -> {
                    // Dragon sight
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, false, false));
                }
                case SCALED -> {
                    // Scale armor protection
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 0, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 0, false, false));
                }
                case BREATHING -> {
                    // Flame breath power
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 1, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 1, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0, false, false));
                }
                case RAGING -> {
                    // Dragon fury
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 2, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 2, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 2, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 1, false, false));
                }
            }
            
            // Dragon regeneration
            if (cfgBool(DRAGON_REGENERATION_PATH, DRAGON_REGENERATION_DEFAULT)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 0, false, false));
            }
        }
        
        private void removeDragonEffects() {
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
            player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.removePotionEffect(PotionEffectType.RESISTANCE);
            player.removePotionEffect(PotionEffectType.STRENGTH);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            player.removePotionEffect(PotionEffectType.REGENERATION);
        }
        
        private void playStateSpecificSounds(Location loc) {
            if (tickCounter % 140 == 0) {
                switch (currentState) {
                    case AWAKENING -> player.playSound(loc, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.3f, 1.2f);
                    case SCALED -> {
                        player.playSound(loc, Sound.ENTITY_IRON_GOLEM_STEP, 0.4f, 0.8f);
                        player.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 0.2f, 0.9f);
                    }
                    case BREATHING -> {
                        player.playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.0f);
                        player.playSound(loc, Sound.ENTITY_BLAZE_BURN, 0.3f, 1.2f);
                    }
                    case RAGING -> {
                        player.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 0.7f);
                        player.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 0.4f, 1.0f);
                    }
                }
            }
        }
    }
    
    /* ---------------------------------------- */
    /* CONFIG HELPERS */
    /* ---------------------------------------- */
    private double cfgDouble(String path, double def) {
        return spellConfig.getDouble("dragon-fury." + path, def);
    }
    
    private int cfgInt(String path, int def) {
        return spellConfig.getInt("dragon-fury." + path, def);
    }
    
    private boolean cfgBool(String path, boolean def) {
        return spellConfig.getBoolean("dragon-fury." + path, def);
    }
}
