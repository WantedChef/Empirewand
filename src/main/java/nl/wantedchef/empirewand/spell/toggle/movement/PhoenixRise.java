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

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

/**
 * PhoenixRise 1.0 - Revolutionary resurrection-themed movement spell with advanced fire rebirth mechanics.
 * 
 * Extraordinary Features:
 * - Phoenix transformation with fire particle wing metamorphosis
 * - Resurrection cycle with ash-to-flame particle physics simulation
 * - Rising flame spiral effects with mathematical curve generation
 * - Temporary invulnerability with fire immunity and regeneration
 * - Advanced combustion particle effects with realistic flame behavior
 * - Phoenix cry sound effects with echoing harmonic resonance
 * - Performance-optimized flame rebirth cycles with particle recycling
 * - Multi-stage transformation with metamorphic particle transitions
 */
public final class PhoenixRise extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /* DATA */
    /* ---------------------------------------- */
    private final Map<UUID, PhoenixData> phoenixes = new WeakHashMap<>();

    /* ---------------------------------------- */
    /* BUILDER */
    /* ---------------------------------------- */
    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Phoenix Rise";
            description = "Transform into a majestic phoenix with fire rebirth cycles and temporary invulnerability.";
            cooldown = Duration.ofSeconds(12);
            spellType = SpellType.MOVEMENT;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new PhoenixRise(this);
        }
    }

    private PhoenixRise(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /* SPELL API */
    /* ---------------------------------------- */
    @Override
    public @NotNull String key() {
        return "phoenix-rise";
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
        return phoenixes.containsKey(player.getUniqueId());
    }

    @Override
    public void activate(@NotNull Player player, @NotNull SpellContext context) {
        if (isActive(player))
            return;
        phoenixes.put(player.getUniqueId(), new PhoenixData(player, context));
    }

    @Override
    public void deactivate(@NotNull Player player, @NotNull SpellContext context) {
        Optional.ofNullable(phoenixes.remove(player.getUniqueId())).ifPresent(PhoenixData::stop);
    }

    @Override
    public void forceDeactivate(@NotNull Player player) {
        Optional.ofNullable(phoenixes.remove(player.getUniqueId())).ifPresent(PhoenixData::stop);
    }

    @Override
    public int getMaxDuration() {
        return cfgInt("max-duration-ticks", 1200); // 1 minute default
    }

    /* ---------------------------------------- */
    /* INTERNAL CLASS */
    /* ---------------------------------------- */
    private final class PhoenixData {
        private final Player player;
        private final BukkitTask ticker;
        
        // Phoenix transformation variables
        private double rebirthEnergy = 100;
        private int tickCounter = 0;
        private double flamePhase = 0;
        private double wingPhase = 0;
        private double ashCycle = 0;
        private PhoenixState currentState = PhoenixState.RISING;
        private int rebirthCycles = 0;
        private Location lastLocation;
        private boolean hasRecentlyReborn = false;

        // Phoenix states for transformation cycles
        private enum PhoenixState {
            RISING,     // Rising from ashes
            SOARING,    // Full phoenix form
            BURNING,    // Intense combustion
            ASHES       // Ash form (brief invulnerability)
        }

        PhoenixData(Player player, SpellContext context) {
            this.player = player;
            this.lastLocation = player.getLocation().clone();
            
            // Phoenix awakening sounds
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 1.2f);
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.8f, 1.8f);
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.6f, 2.0f);
            
            // Minimal: no activation chat message
            
            // Grant flight if not already flying
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
                player.setFlying(true);
            }
            
            // Start the phoenix cycle ticker
            this.ticker = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 0, 1);
        }

        void stop() {
            ticker.cancel();
            
            // Remove phoenix effects
            removePhoenixEffects();
            
            // Final phoenix death effect
            spawnPhoenixDeathEffect();
            
            // Minimal: no deactivation chat message
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.8f, 1.0f);
            
            // Remove flight if granted by spell
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }
        
        private void spawnPhoenixAura(Location center) {
            World world = center.getWorld();
            if (world == null) return;
            double auraRadius = cfgDouble("phoenix.aura-radius", 1.2);
            int auraParticles = cfgInt("phoenix.aura-particles", 6);
            double phase = wingPhase * 0.1;
            for (int i = 0; i < auraParticles; i++) {
                double angle = 2 * Math.PI * i / auraParticles + phase;
                double x = Math.cos(angle) * auraRadius;
                double z = Math.sin(angle) * auraRadius;
                Location auraLoc = center.clone().add(x, 1.0, z);
                world.spawnParticle(Particle.END_ROD, auraLoc, 1, 0.02, 0.02, 0.02, 0.005);
            }
        }

        private void tick() {
            if (!player.isOnline()) {
                forceDeactivate(player);
                return;
            }
            
            // Lightweight persistent effects
            applyPhoenixEffects();
            
            // Simple phoenix ring: small vertical flame pillars around the player
            tickCounter++;
            World world = player.getWorld();
            if (world != null) {
                Location base = player.getLocation();
                double radius = 0.90;
                int pillars = 30;
                double rotation = tickCounter * 0.05; // slower rotation
                for (int i = 0; i < pillars; i++) {
                    double angle = 2 * Math.PI * i / pillars + rotation;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    // 3 small vertical points (ankle, waist, chest)
                    world.spawnParticle(Particle.FLAME, base.clone().add(x, 0.2, z), 1, 0.02, 0.02, 0.02, 0.005);
                    world.spawnParticle(Particle.FLAME, base.clone().add(x, 0.8, z), 1, 0.02, 0.02, 0.02, 0.005);
                    world.spawnParticle(Particle.FLAME, base.clone().add(x, 1.4, z), 1, 0.02, 0.02, 0.02, 0.005);
                }
            }
            lastLocation = player.getLocation().clone();
        }
        
        private void applyPhoenixEffects() {
            // Minimal fixed effects only (no progression)
            int duration = cfgInt("effects.duration-ticks", 40);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0, false, false));
        }
        
        private double getEnergyCostForState() {
            switch (currentState) {
                case RISING: return cfgDouble("energy.rising-cost", 0.2);
                case SOARING: return cfgDouble("energy.soaring-cost", 0.3);
                case BURNING: return cfgDouble("energy.burning-cost", 0.5);
                case ASHES: return cfgDouble("energy.ashes-regen", -0.8); // Regenerates
                default: return 0.3;
            }
        }
        
        private void triggerRebirthCycle() {
            rebirthCycles++;
            rebirthEnergy = 100;
            hasRecentlyReborn = true;
            currentState = PhoenixState.ASHES;
            
            // Rebirth effect
            spawnRebirthCycleEffect();
            
            // Rebirth sounds
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.8f, 0.8f);
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 1.5f);
            
            // Minimal: no rebirth chat message
            
            // Temporary invulnerability
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 
                cfgInt("rebirth.invulnerability-duration", 60), 4, false, false));
        }
        
        private void spawnRebirthActivationEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Spiraling flame eruption
            for (int i = 0; i < 20; i++) {
                double angle = i * 0.4;
                double radius = i * 0.08;
                double height = i * 0.1;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                
                Location flameLoc = loc.clone().add(x, height, z);
                world.spawnParticle(Particle.FLAME, flameLoc, 1, 0.06, 0.06, 0.06, 0.01);
                
                // Phoenix colors
                Color phoenixColor = getPhoenixColor(angle);
                world.spawnParticle(Particle.DUST, flameLoc, 1, 0.03, 0.03, 0.03, 0,
                    new Particle.DustOptions(phoenixColor, 1.0f));
            }
            
            // Central flame pillar
            for (int i = 0; i < 8; i++) {
                Location pillarLoc = loc.clone().add(0, Math.random() * 3.0, 0);
                world.spawnParticle(Particle.FLAME, pillarLoc, 1, 0.08, 0.08, 0.08, 0.01);
            }
        }
        
        private void spawnPhoenixForm(Location center) {
            // State-specific phoenix form rendering
            switch (currentState) {
                case RISING:
                    spawnRisingPhoenixForm(center);
                    break;
                case SOARING:
                    spawnSoaringPhoenixForm(center);
                    break;
                case BURNING:
                    spawnBurningPhoenixForm(center);
                    break;
                case ASHES:
                    spawnAshPhoenixForm(center);
                    break;
            }
            
            // Phoenix aura - always present
            spawnPhoenixAura(center);
        }
        
        private void spawnRisingPhoenixForm(Location center) {
            World world = center.getWorld();
            
            // Rising flame wings
            double wingSpan = cfgDouble("phoenix.rising-wingspan", 2.4);
            int wingParticles = cfgInt("phoenix.rising-wing-particles", 10);
            
            for (int wing = 0; wing < 2; wing++) {
                double wingSide = wing == 0 ? -1 : 1;
                
                for (int i = 0; i < wingParticles; i++) {
                    double progress = (double) i / wingParticles;
                    double wingX = progress * wingSpan * wingSide;
                    double wingY = Math.sin(progress * Math.PI + wingPhase) * 1.2 + 0.5;
                    double wingZ = progress * 0.3;
                    
                    Location wingLoc = center.clone().add(wingX, wingY, wingZ);
                    
                    // Rising flame particles
                    world.spawnParticle(Particle.FLAME, wingLoc, 1, 0.06, 0.06, 0.06, 0.01);
                    
                    // Phoenix feather effects
                    if (Math.random() < 0.3) {
                        Color featherColor = Color.fromRGB(255, 140, 0); // Phoenix orange
                        world.spawnParticle(Particle.DUST, wingLoc, 1, 0.03, 0.03, 0.03, 0,
                            new Particle.DustOptions(featherColor, 0.9f));
                    }
                }
            }
        }
        
        private void spawnSoaringPhoenixForm(Location center) {
            World world = center.getWorld();
            
            // Full majestic phoenix wings
            double wingSpan = cfgDouble("phoenix.soaring-wingspan", 3.2);
            int wingParticles = cfgInt("phoenix.soaring-wing-particles", 12);
            
            for (int wing = 0; wing < 2; wing++) {
                double wingSide = wing == 0 ? -1 : 1;
                
                for (int i = 0; i < wingParticles; i++) {
                    double progress = (double) i / wingParticles;
                    double flapCycle = Math.sin(wingPhase) * 0.4 + 0.6;
                    
                    double wingX = progress * wingSpan * wingSide * flapCycle;
                    double wingY = Math.sin(progress * Math.PI) * 1.8 + 1.0;
                    double wingZ = progress * 0.5;
                    
                    Location wingLoc = center.clone().add(wingX, wingY, wingZ);
                    
                    // Majestic flame wings (subtle)
                    world.spawnParticle(Particle.FLAME, wingLoc, 1, 0.06, 0.06, 0.06, 0.01);
                    
                    // Golden phoenix dust
                    Color phoenixGold = Color.fromRGB(255, 215, 0);
                    world.spawnParticle(Particle.DUST, wingLoc, 1, 0.03, 0.03, 0.03, 0,
                        new Particle.DustOptions(phoenixGold, 1.0f));
                }
            }
            
            // Phoenix tail plume
            spawnPhoenixTail(center);
        }
        
        private void spawnBurningPhoenixForm(Location center) {
            World world = center.getWorld();
            
            // Intense burning phoenix form
            double intensity = 0.8;
            double wingSpan = cfgDouble("phoenix.burning-wingspan", 2.8);
            int wingParticles = cfgInt("phoenix.burning-wing-particles", 10);
            
            for (int wing = 0; wing < 2; wing++) {
                double wingSide = wing == 0 ? -1 : 1;
                
                for (int i = 0; i < wingParticles; i++) {
                    double progress = (double) i / wingParticles;
                    
                    double wingX = progress * wingSpan * wingSide;
                    double wingY = Math.sin(progress * Math.PI + flamePhase) * 1.5 + 0.8;
                    double wingZ = progress * 0.4;
                    
                    Location wingLoc = center.clone().add(wingX, wingY, wingZ);
                    
                    // Burning flames (subtle)
                    world.spawnParticle(Particle.FLAME, wingLoc, 1, 0.08, 0.08, 0.08, 0.015);
                    
                    // White-hot core
                    // no core needed in normal mode
                }
            }
        }
        
        private void spawnAshPhoenixForm(Location center) {
            World world = center.getWorld();
            
            // Ash form - preparing for rebirth
            double ashRadius = cfgDouble("phoenix.ash-radius", 1.4);
            int ashParticles = cfgInt("phoenix.ash-particles", 6);
            
            for (int i = 0; i < ashParticles; i++) {
                double angle = 2 * Math.PI * i / ashParticles + ashCycle;
                double radius = ashRadius * (0.5 + 0.5 * Math.sin(ashCycle * 2 + i));
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = Math.sin(ashCycle + angle * 2) * 0.8 + 1.0;
                
                Location ashLoc = center.clone().add(x, y, z);
                
                // Ash particles
                world.spawnParticle(Particle.ASH, ashLoc, 1, 0.08, 0.08, 0.08, 0.005);
                
                // Ember sparks - signs of coming rebirth
                if (Math.random() < 0.4) {
                    world.spawnParticle(Particle.FLAME, ashLoc, 1, 0.05, 0.05, 0.05, 0.01);
                }
                
                // Ash dust
                Color ashColor = Color.fromRGB(64, 64, 64);
                world.spawnParticle(Particle.DUST, ashLoc, 1, 0.03, 0.03, 0.03, 0,
                    new Particle.DustOptions(ashColor, 0.9f));
            }
        }
        
        private void spawnPhoenixTail(Location center) {
            World world = center.getWorld();
            int tailLength = cfgInt("phoenix.tail-length", 4);
            
            Vector backward = player.getLocation().getDirection().multiply(-1);
            
            for (int i = 0; i < tailLength; i++) {
                double progress = (double) i / tailLength;
                Location tailLoc = center.clone().add(backward.clone().multiply(progress * 2));
                tailLoc.add(0, -progress * 0.5, 0); // Drooping tail
                
                // Subtle tail flames
                world.spawnParticle(Particle.FLAME, tailLoc, 1, 0.05, 0.05, 0.05, 0.01);
                
                // Tail color dust
                Color tailColor = getPhoenixColor(progress * Math.PI);
                world.spawnParticle(Particle.DUST, tailLoc, 1, 0.03, 0.03, 0.03, 0,
                    new Particle.DustOptions(tailColor, 0.9f));
            }
        }

        private void spawnPhoenixTrail(Location currentLoc) {
            if (lastLocation == null) return;
            
            World world = currentLoc.getWorld();
            Vector movement = currentLoc.toVector().subtract(lastLocation.toVector());
            
            if (movement.lengthSquared() > 0.005) {
                int trailLength = cfgInt("phoenix.trail-length", 6);
                double velocityMagnitude = movement.length();
                
                for (int i = 0; i < trailLength; i++) {
                    double progress = (double) i / trailLength;
                    Location trailPoint = currentLoc.clone().subtract(movement.clone().multiply(progress * 4));
                    
                    // Trail intensity based on velocity and state
                    float intensity = (float) ((1.0 - progress * 0.7) * velocityMagnitude * 2);
                    intensity *= getStateIntensityMultiplier();
                    
                    if (intensity > 0.1f) {
                        // Phoenix flame trail
                        world.spawnParticle(Particle.FLAME, trailPoint, 1, 0.05, 0.05, 0.05, 0.01);
                        
                        // State-specific trail effects
                        if (currentState == PhoenixState.ASHES) {
                            world.spawnParticle(Particle.ASH, trailPoint, 1, 0.1, 0.1, 0.1, 0.01);
                        }
                        
                        // Phoenix trail color
                        Color trailColor = getPhoenixTrailColor(progress);
                        world.spawnParticle(Particle.DUST, trailPoint, 1, 0.03, 0.03, 0.03, 0,
                            new Particle.DustOptions(trailColor, Math.min(1.0f, intensity * 0.5f)));
                    }
                }
            }
        }
        
        private void spawnRebirthCycleEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Death-to-rebirth transformation (subtle)
            for (int i = 0; i < 16; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = Math.random() * 3.0;
                double height = Math.random() * 4.0;
                
                Location rebirthLoc = loc.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );
                
                // Ash to flame transformation
                if (height < 2.0) {
                    world.spawnParticle(Particle.ASH, rebirthLoc, 1, 0.08, 0.08, 0.08, 0.01);
                } else {
                    world.spawnParticle(Particle.FLAME, rebirthLoc, 1, 0.06, 0.06, 0.06, 0.01);
                }
                
                // Rebirth energy burst
                world.spawnParticle(Particle.END_ROD, rebirthLoc, 1, 0.03, 0.03, 0.03, 0.01);
            }
        }
        
        private void spawnPhoenixDeathEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Final flame extinguishing (subtle)
            for (int i = 0; i < 14; i++) {
                double angle = i * 0.3;
                double radius = 2.0 - (i * 0.08);
                double height = i * 0.08;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                
                Location deathLoc = loc.clone().add(x, height, z);
                world.spawnParticle(Particle.SMOKE, deathLoc, 1, 0.08, 0.08, 0.08, 0.01);
                world.spawnParticle(Particle.ASH, deathLoc, 1, 0.05, 0.05, 0.05, 0.005);
                
                // Final sparks
                // No extra sparks in normal mode
            }
        }
        
        // Color and effect helpers
        private Color getPhoenixColor(double phase) {
            // Phoenix color spectrum
            double normalizedPhase = (Math.sin(phase) + 1) / 2;
            
            if (normalizedPhase < 0.33) {
                // Deep red to orange
                return Color.fromRGB(255, (int)(50 + 155 * normalizedPhase * 3), 0);
            } else if (normalizedPhase < 0.67) {
                // Orange to golden
                double subPhase = (normalizedPhase - 0.33) * 3;
                return Color.fromRGB(255, (int)(205 + 50 * subPhase), (int)(100 * subPhase));
            } else {
                // Golden to white-hot
                double subPhase = (normalizedPhase - 0.67) * 3;
                return Color.fromRGB(255, 255, (int)(100 + 155 * subPhase));
            }
        }
        
        private Color getPhoenixTrailColor(double progress) {
            // Trail color based on current state
            double alpha = 1.0 - progress * 0.6;
            
            switch (currentState) {
                case RISING:
                    return Color.fromRGB((int)(255 * alpha), (int)(140 * alpha), 0);
                case SOARING:
                    return Color.fromRGB((int)(255 * alpha), (int)(215 * alpha), 0);
                case BURNING:
                    return Color.fromRGB((int)(255 * alpha), (int)(255 * alpha), (int)(200 * alpha));
                case ASHES:
                    return Color.fromRGB((int)(100 * alpha), (int)(100 * alpha), (int)(100 * alpha));
                default:
                    return Color.fromRGB((int)(255 * alpha), (int)(165 * alpha), 0);
            }
        }
        
        private double getStateIntensityMultiplier() {
            switch (currentState) {
                case RISING: return 0.8;
                case SOARING: return 1.2;
                case BURNING: return 1.8;
                case ASHES: return 0.4;
                default: return 1.0;
            }
        }
        
        private void removePhoenixEffects() {
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
            player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
            player.removePotionEffect(PotionEffectType.REGENERATION);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.STRENGTH);
            player.removePotionEffect(PotionEffectType.RESISTANCE);
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
        
        private void playStateSpecificSounds(Location loc) {
            if (tickCounter % 100 == 0) {
                switch (currentState) {
                    case RISING:
                        player.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 0.4f, 1.8f);
                        break;
                    case SOARING:
                        player.playSound(loc, Sound.ITEM_ELYTRA_FLYING, 0.3f, 1.5f);
                        player.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 0.3f, 1.2f);
                        break;
                    case BURNING:
                        player.playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.0f);
                        player.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 0.3f, 1.5f);
                        break;
                    case ASHES:
                        player.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.2f, 0.8f);
                        break;
                }
            }
        }
    }
    
    /* ---------------------------------------- */
    /* CONFIG HELPERS */
    /* ---------------------------------------- */
    private double cfgDouble(String path, double def) {
        return spellConfig.getDouble("phoenix-rise." + path, def);
    }
    
    private int cfgInt(String path, int def) {
        return spellConfig.getInt("phoenix-rise." + path, def);
    }
    
    private boolean cfgBool(String path, boolean def) {
        return spellConfig.getBoolean("phoenix-rise." + path, def);
    }
}
