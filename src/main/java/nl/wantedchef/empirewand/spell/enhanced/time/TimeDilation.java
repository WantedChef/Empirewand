package nl.wantedchef.empirewand.spell.enhanced.time;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A powerful spell that dilates time in an area,
 * drastically slowing enemies while allowing the caster to move at normal speed.
 */
public class TimeDilation extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Time Dilation";
            this.description = "Dilates time in an area, drastically slowing enemies while you move at normal speed.";
            this.cooldown = java.time.Duration.ofSeconds(65);
            this.spellType = SpellType.CONTROL;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new TimeDilation(this);
        }
    }

    private TimeDilation(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "time-dilation";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(SpellContext context) {
        Player player = context.caster();

        // Configuration
        double radius = spellConfig.getDouble("values.radius", 20.0);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 180);
        int slowAmplifier = spellConfig.getInt("values.slow-amplifier", 5);
        boolean affectsPlayers = spellConfig.getBoolean("flags.affects-players", true);

        // Play initial sound
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.3f);

        // Start time dilation effect
        new TimeDilationTask(context, player.getLocation(), radius, durationTicks, slowAmplifier, affectsPlayers)
                .runTaskTimer(context.plugin(), 0L, 3L);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled in scheduler
    }

    private static class TimeDilationTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final double radius;
        private final int durationTicks;
        private final int slowAmplifier;
        private final boolean affectsPlayers;
        private final World world;
        private int ticks = 0;
        private final int maxTicks;

        public TimeDilationTask(SpellContext context, Location center, double radius, 
                               int durationTicks, int slowAmplifier, boolean affectsPlayers) {
            this.context = context;
            this.center = center;
            this.radius = radius;
            this.durationTicks = durationTicks;
            this.slowAmplifier = slowAmplifier;
            this.affectsPlayers = affectsPlayers;
            this.world = center.getWorld();
            this.maxTicks = durationTicks / 3; // Convert to our tick interval
        }

        @Override
        public void run() {
            if (ticks >= maxTicks) {
                this.cancel();
                // Play end sound
                world.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 2.0f, 0.3f);
                return;
            }

            // Apply time dilation effects
            applyTimeDilationEffects();

            // Create visual effects
            createVisualEffects();

            ticks++;
        }

        private void applyTimeDilationEffects() {
            Collection<LivingEntity> nearbyEntities = world.getNearbyLivingEntities(center, radius, radius, radius);
            
            for (LivingEntity entity : nearbyEntities) {
                // Skip the caster (they are unaffected)
                if (entity.equals(context.caster())) continue;
                
                // Skip players if not affecting players
                if (entity instanceof Player && !affectsPlayers) continue;
                
                // Skip dead or invalid entities
                if (entity.isDead() || !entity.isValid()) continue;

                // Apply extreme slowness
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, slowAmplifier, false, false));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 10, 200, false, false));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 10, 5, false, false));
                
                // Visual effect for time-slowed entities
                if (ticks % 5 == 0) {
                    world.spawnParticle(Particle.CLOUD, entity.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.01);
                    world.spawnParticle(Particle.PORTAL, entity.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0.01);
                }
            }
        }

        private void createVisualEffects() {
            // Create time distortion field
            double currentRadius = radius * (1.0 - (ticks / (double) maxTicks) * 0.5);
            
            // Outer ring
            for (int i = 0; i < 36; i++) {
                double angle = 2 * Math.PI * i / 36;
                double x = currentRadius * Math.cos(angle);
                double z = currentRadius * Math.sin(angle);
                Location particleLoc = center.clone().add(x, 0.1, z);
                world.spawnParticle(Particle.PORTAL, particleLoc, 1, 0, 0, 0, 0);
            }
            
            // Inner ring with faster rotation
            double innerRadius = currentRadius * 0.7;
            for (int i = 0; i < 24; i++) {
                double angle = (2 * Math.PI * i / 24) + (ticks * 0.3);
                double x = innerRadius * Math.cos(angle);
                double z = innerRadius * Math.sin(angle);
                Location particleLoc = center.clone().add(x, 0.1, z);
                world.spawnParticle(Particle.CLOUD, particleLoc, 1, 0, 0, 0, 0);
            }
            
            // Central time vortex
            if (ticks % 2 == 0) {
                for (int i = 0; i < 10; i++) {
                    double angle = (2 * Math.PI * i / 10) + (ticks * 0.5);
                    double distance = Math.sin(ticks * 0.1) * 2;
                    double x = distance * Math.cos(angle);
                    double z = distance * Math.sin(angle);
                    Location particleLoc = center.clone().add(x, 0.1, z);
                    world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                }
            }
            
            // Time particles floating upward
            if (ticks % 4 == 0) {
                for (int i = 0; i < 5; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double distance = Math.random() * radius;
                    double x = center.getX() + Math.cos(angle) * distance;
                    double z = center.getZ() + Math.sin(angle) * distance;
                    Location particleLoc = new Location(world, x, center.getY() + Math.random() * 5, z);
                    world.spawnParticle(Particle.DRAGON_BREATH, particleLoc, 1, 0, 0, 0, 0);
                }
            }
        }
    }
}