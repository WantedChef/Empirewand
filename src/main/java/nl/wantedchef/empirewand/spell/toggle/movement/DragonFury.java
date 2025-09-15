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
 * DragonFury — Enhanced draconic flight with spectacular wings.
 *
 * Design goals:
 * - Keep only essential effects: SLOW_FALLING and RESISTANCE (short duration, re‑applied).
 * - Render spectacular high-resolution wings with 10x visual enhancement.
 * - Player-controlled movement without automatic flight assistance.
 * - Subtle activate/deactivate sounds; no chat, no boss bars, no heavy helpers.
 */
public final class DragonFury extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /* DATA */
    /* ---------------------------------------- */
    private final Map<UUID, DragonRiderData> dragonRiders = new WeakHashMap<>();
    // Removed unused config toggles; keep the spell minimal and predictable

    /* ---------------------------------------- */
    /* BUILDER */
    /* ---------------------------------------- */
    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Dragon Fury";
            description = "Enhanced draconic flight with spectacular wings and protection.";
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
        private final BukkitTask ticker;
        private int tickCounter = 0;
        // Smoothed yaw (radians) to stabilize wing rendering while turning
        private double smoothedYawRad = Double.NaN;
        // Mana drain configuration
        private static final double MANA_DRAIN_PER_TICK = 0.5;

        DragonRiderData(Player player, SpellContext context) {
            this.player = player;
            
            // Subtle activation sound (kept lightweight)
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 0.9f);
            
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
            // Minimal: subtle deactivation cue
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 1.0f);
            
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

            // Increment tick counter for timing
            tickCounter++;

            // Minimal fixed effects
            int duration = 60;
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 0, false, false));
            
            // Removed automatic flight controls - player controls movement manually

            // Enhanced dragon wings with 10x visual upgrade
            Location base = player.getLocation();
            World world = base.getWorld();
            if (world != null) {
                // Update wing positioning every tick for maximum smoothness
                // Smooth yaw to avoid jitter while turning (ignore pitch)
                double yawRad = Math.toRadians(base.getYaw());
                if (Double.isNaN(smoothedYawRad)) smoothedYawRad = yawRad;
                double delta = normalizeAngle(yawRad - smoothedYawRad);
                smoothedYawRad = smoothedYawRad + delta * 0.15; // smoother low-pass filter

                Vector forwardDir = new Vector(-Math.sin(smoothedYawRad), 0, Math.cos(smoothedYawRad)).normalize();
                Vector right = new Vector(forwardDir.getZ(), 0, -forwardDir.getX()).normalize();
                Vector up = new Vector(0, 1, 0);
                Vector back = forwardDir.clone().multiply(-1);

                // Enhanced wing parameters for 10x visual upgrade
                int points = 25; // 2.5x more resolution for smoother curves
                double span = 2.5; // 40% larger wingspan
                double backOffset = 0.6; // Closer to player for better positioning
                double wingHeight = 0.3; // Wings positioned at shoulder height

                // Enhanced color scheme with multiple layers
                Color primaryOutline = Color.fromRGB(20, 5, 5); // Dark red outline
                Color secondaryOutline = Color.fromRGB(60, 15, 15); // Medium red
                Color membrane = Color.fromRGB(120, 30, 30); // Bright red membrane
                Color glow = Color.fromRGB(200, 50, 50); // Glowing effect
                Color accent = Color.fromRGB(255, 100, 100); // Bright accent

                for (int side = -1; side <= 1; side += 2) {
                    Vector sideVec = right.clone().multiply(side);

                    Location prevPoint = null;
                    for (int i = 0; i < points; i++) {
                        double t = i / (double) (points - 1);
                        
                        // Enhanced wing curve with more detail
                        double outward = span * t;
                        double arch = wingHeight + Math.sin(t * Math.PI) * 1.2; // More pronounced arch
                        double backZ = backOffset + t * 0.3; // Better positioning
                        
                        // Add wing tip detail
                        double tipFlare = t > 0.8 ? (t - 0.8) * 5.0 : 0.0;
                        outward += tipFlare * 0.3;

                        Location p = base.clone()
                                .add(sideVec.clone().multiply(outward))
                                .add(up.clone().multiply(arch))
                                .add(back.clone().multiply(backZ));

                        // Multi-layered particle effects for 10x visual impact
                        
                        // Primary outline spine (thicker)
                        world.spawnParticle(Particle.DUST, p, 3, 0.05, 0.05, 0.05, 0,
                                new Particle.DustOptions(primaryOutline, 1.2f));
                        
                        // Secondary outline for depth
                        world.spawnParticle(Particle.DUST, p, 2, 0.1, 0.1, 0.1, 0,
                                new Particle.DustOptions(secondaryOutline, 0.8f));
                        
                        // Glowing membrane effect
                        world.spawnParticle(Particle.DUST, p, 4, 0.15, 0.15, 0.15, 0,
                                new Particle.DustOptions(membrane, 0.6f));
                        
                        // Accent particles for sparkle effect
                        if (tickCounter % 3 == 0) {
                            world.spawnParticle(Particle.DUST, p, 1, 0.2, 0.2, 0.2, 0,
                                    new Particle.DustOptions(accent, 0.4f));
                        }
                        
                        // Wing membrane lines with enhanced detail
                        if (prevPoint != null) {
                            drawEnhancedLine(world, prevPoint, p, membrane, 8); // More particles per line
                            drawEnhancedLine(world, prevPoint, p, glow, 4); // Glow layer
                        }
                        prevPoint = p;
                    }
                    
                    // Add wing membrane cross-sections for more realistic look
                    for (int section = 1; section < 4; section++) {
                        double sectionT = section / 4.0;
                        double outward = span * sectionT;
                        double arch = wingHeight + Math.sin(sectionT * Math.PI) * 1.2;
                        double backZ = backOffset + sectionT * 0.3;
                        
                        Location startPoint = base.clone()
                                .add(sideVec.clone().multiply(outward))
                                .add(up.clone().multiply(arch))
                                .add(back.clone().multiply(backZ));
                        
                        Location endPoint = base.clone()
                                .add(sideVec.clone().multiply(outward * 0.3)) // Inner membrane
                                .add(up.clone().multiply(arch * 0.7))
                                .add(back.clone().multiply(backZ));
                        
                        drawEnhancedLine(world, startPoint, endPoint, membrane, 6);
                    }
                }
                
                // Add wing flapping animation effect
                double flapPhase = (tickCounter * 0.1) % (2 * Math.PI);
                double flapIntensity = Math.sin(flapPhase) * 0.1;
                
                // Add trailing particles behind wings
                for (int i = 0; i < 3; i++) {
                    Location trailPoint = base.clone()
                            .add(right.clone().multiply((i - 1) * 0.8))
                            .add(up.clone().multiply(0.2 + flapIntensity))
                            .add(back.clone().multiply(1.2));
                    
                    world.spawnParticle(Particle.DUST, trailPoint, 2, 0.1, 0.1, 0.1, 0,
                            new Particle.DustOptions(glow, 0.3f));
                }
            }
            handleSpecialAbilities();
        }

        private double normalizeAngle(double a) {
            while (a > Math.PI) a -= 2 * Math.PI;
            while (a < -Math.PI) a += 2 * Math.PI;
            return a;
        }

        
        private void drawEnhancedLine(World world, Location from, Location to, Color color, int steps) {
            double dx = (to.getX() - from.getX()) / steps;
            double dy = (to.getY() - from.getY()) / steps;
            double dz = (to.getZ() - from.getZ()) / steps;
            for (int i = 1; i < steps; i++) {
                Location p = from.clone().add(dx * i, dy * i, dz * i);
                // Enhanced line with multiple particle layers
                world.spawnParticle(Particle.DUST, p, 2, 0.05, 0.05, 0.05, 0, new Particle.DustOptions(color, 0.7f));
                world.spawnParticle(Particle.DUST, p, 1, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(color, 0.4f));
            }
        }
        
        private void removeDragonEffects() {
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
            player.removePotionEffect(PotionEffectType.RESISTANCE);
            // No other effects are applied by this minimal version
        }

        
        // No additional state-specific sounds in minimal version

        private void handleSpecialAbilities() {
            // Mana drain
            if (!drainMana(MANA_DRAIN_PER_TICK)) {
                stop();
                return;
            }
            
            // Removed fire breath and aerial dash abilities
        }

        private boolean drainMana(double amount) {
            // Integration with mana system
            // Return true if successful
            return true; // Placeholder
        }

    }
    
    /* ---------------------------------------- */
    /* CONFIG HELPERS */
    /* ---------------------------------------- */
    private int cfgInt(String path, int def) {
        return spellConfig.getInt("dragon-fury." + path, def);
    }

}
