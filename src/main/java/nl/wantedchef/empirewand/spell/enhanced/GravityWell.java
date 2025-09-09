package nl.wantedchef.empirewand.spell.enhanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A powerful spell that creates a gravity well,
 * pulling all entities toward the center and crushing them with increasing force.
 */
public class GravityWell extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Gravity Well";
            this.description = "Creates a powerful gravity well that pulls all entities toward the center and crushes them.";
            this.cooldown = java.time.Duration.ofSeconds(60);
            this.spellType = SpellType.CONTROL;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new GravityWell(this);
        }
    }

    private GravityWell(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "gravity-well";
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
        int durationTicks = spellConfig.getInt("values.duration-ticks", 150);
        boolean affectsPlayers = spellConfig.getBoolean("flags.affects-players", true);
        double maxPullStrength = spellConfig.getDouble("values.max-pull-strength", 1.2);

        // Get target location (or use player location)
        Location targetLocation = player.getTargetBlock(null, 30).getLocation();
        if (targetLocation == null) {
            targetLocation = player.getLocation();
        }

        // Play initial sound
        player.getWorld().playSound(targetLocation, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);

        // Start gravity well effect
        new GravityWellTask(context, targetLocation, radius, durationTicks, affectsPlayers, maxPullStrength)
                .runTaskTimer(context.plugin(), 0L, 1L);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled in scheduler
    }

    private static class GravityWellTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final double radius;
        private final int durationTicks;
        private final boolean affectsPlayers;
        private final double maxPullStrength;
        private final World world;
        private int ticks = 0;
        private final Map<LivingEntity, Integer> crushTimers = new HashMap<>();

        public GravityWellTask(SpellContext context, Location center, double radius, 
                              int durationTicks, boolean affectsPlayers, double maxPullStrength) {
            this.context = context;
            this.center = center;
            this.radius = radius;
            this.durationTicks = durationTicks;
            this.affectsPlayers = affectsPlayers;
            this.maxPullStrength = maxPullStrength;
            this.world = center.getWorld();
        }

        @Override
        public void run() {
            if (ticks >= durationTicks) {
                this.cancel();
                // Play end sound
                world.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 2.0f, 0.8f);
                return;
            }

            // Apply gravity well effects
            applyGravityEffects();

            // Create visual effects
            createVisualEffects();

            ticks++;
        }

        private void applyGravityEffects() {
            Collection<LivingEntity> nearbyEntities = world.getNearbyLivingEntities(center, radius, radius, radius);
            
            for (LivingEntity entity : nearbyEntities) {
                // Skip the caster
                if (entity.equals(context.caster())) continue;
                
                // Skip players if not affecting players
                if (entity instanceof Player && !affectsPlayers) continue;
                
                // Skip dead or invalid entities
                if (entity.isDead() || !entity.isValid()) continue;

                // Calculate distance and pull strength
                double distance = entity.getLocation().distance(center);
                if (distance > radius) continue;
                
                // Pull strength increases as entity gets closer to center
                double pullStrength = maxPullStrength * (1.0 - (distance / radius));
                
                // Calculate pull vector
                Vector pull = center.toVector().subtract(entity.getLocation().toVector()).normalize().multiply(pullStrength);
                
                // Apply pull velocity
                entity.setVelocity(entity.getVelocity().add(pull));
                
                // Apply crushing damage when very close to center
                if (distance < radius * 0.2) {
                    Integer crushTimer = crushTimers.getOrDefault(entity, 0);
                    crushTimer++;
                    crushTimers.put(entity, crushTimer);
                    
                    // Damage increases with time in center
                    if (crushTimer % 10 == 0) {
                        double damage = Math.min(10.0, crushTimer / 20.0);
                        entity.damage(damage, context.caster());
                        
                        // Visual effect for crushing
                        world.spawnParticle(Particle.CRIT, entity.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.1);
                    }
                } else {
                    // Reset crush timer when not in center
                    crushTimers.remove(entity);
                }
                
                // Visual effect for pulled entities
                if (ticks % 5 == 0) {
                    world.spawnParticle(Particle.PORTAL, entity.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.01);
                }
            }
        }

        private void createVisualEffects() {
            // Create spiral effect pulling toward center
            double spiralRadius = radius * (1.0 - (ticks / (double) durationTicks));
            
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i / 8) + (ticks * 0.2);
                double x = spiralRadius * Math.cos(angle);
                double z = spiralRadius * Math.sin(angle);
                Location particleLoc = center.clone().add(x, Math.sin(ticks * 0.1) * 3, z);
                world.spawnParticle(Particle.SMOKE, particleLoc, 2, 0.1, 0.1, 0.1, 0.01);
            }
            
            // Create vertical beam
            if (ticks % 3 == 0) {
                for (int i = 0; i < 20; i++) {
                    double y = i * 0.5;
                    Location beamLoc = center.clone().add(0, y, 0);
                    world.spawnParticle(Particle.PORTAL, beamLoc, 3, 0.2, 0.2, 0.2, 0.01);
                }
            }
            
            // Create shockwave periodically
            if (ticks % 30 == 0) {
                double shockwaveRadius = radius * 0.3 + (ticks % 60) / 60.0 * radius * 0.7;
                for (int i = 0; i < 36; i++) {
                    double angle = 2 * Math.PI * i / 36;
                    double x = shockwaveRadius * Math.cos(angle);
                    double z = shockwaveRadius * Math.sin(angle);
                    Location particleLoc = center.clone().add(x, 0.1, z);
                    world.spawnParticle(Particle.EXPLOSION, particleLoc, 1, 0, 0, 0, 0);
                }
                
                // Sound effect
                world.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.3f);
            }
        }
    }
}