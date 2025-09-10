package nl.wantedchef.empirewand.spell.enhanced.summoning;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A powerful necromancy spell that summons a swarm of minions to fight for you.
 */
public class SummonSwarm extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Summon Swarm";
            this.description = "Summons a swarm of minions to fight for you.";
            this.cooldown = java.time.Duration.ofSeconds(70);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new SummonSwarm(this);
        }
    }

    private SummonSwarm(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "summon-swarm";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(SpellContext context) {
        Player player = context.caster();

        // Configuration
        int minionCount = spellConfig.getInt("values.minion-count", 8);
        double radius = spellConfig.getDouble("values.radius", 8.0);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 300);
        double minionHealth = spellConfig.getDouble("values.minion-health", 10.0);
        double minionDamage = spellConfig.getDouble("values.minion-damage", 4.0);

        // Play summoning sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 2.0f, 0.8f);

        // Start summoning effect
        new SummonSwarmTask(context, player.getLocation(), minionCount, radius, durationTicks, 
                           minionHealth, minionDamage)
                .runTaskTimer(context.plugin(), 0L, 5L);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled in scheduler
    }

    private static class SummonSwarmTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final int minionCount;
        private final double radius;
        private final int durationTicks;
        private final double minionHealth;
        private final double minionDamage;
        private final World world;
        private int ticks = 0;
        private final int maxTicks;
        private final List<Vex> summonedMinions = new ArrayList<>();
        private final Random random = new Random();

        public SummonSwarmTask(SpellContext context, Location center, int minionCount, double radius, 
                              int durationTicks, double minionHealth, double minionDamage) {
            this.context = context;
            this.center = center;
            this.minionCount = minionCount;
            this.radius = radius;
            this.durationTicks = durationTicks;
            this.minionHealth = minionHealth;
            this.minionDamage = minionDamage;
            this.world = center.getWorld();
            this.maxTicks = durationTicks / 5; // Convert to our tick interval
        }

        @Override
        public void run() {
            if (ticks >= maxTicks) {
                this.cancel();
                dismissMinions();
                return;
            }

            // Summon minions at the beginning
            if (ticks == 0) {
                summonMinions();
            }

            // Update minion behavior
            updateMinionBehavior();

            // Create visual effects
            createVisualEffects();

            ticks++;
        }

        private void summonMinions() {
            Player player = context.caster();
            
            for (int i = 0; i < minionCount; i++) {
                // Calculate position in a circle around the player
                double angle = 2 * Math.PI * i / minionCount;
                double x = center.getX() + Math.cos(angle) * radius;
                double z = center.getZ() + Math.sin(angle) * radius;
                Location spawnLoc = new Location(world, x, world.getHighestBlockYAt((int)x, (int)z) + 1, z);
                
                // Spawn vex minion
                Vex minion = world.spawn(spawnLoc, Vex.class, vex -> {
                    vex.setTarget(player); // Make it follow the player
                    vex.setHealth(minionHealth);
                });
                
                summonedMinions.add(minion);
                
                // Visual effect for summoning
                world.spawnParticle(Particle.SMOKE, spawnLoc, 20, 0.5, 0.5, 0.5, 0.1);
                world.spawnParticle(Particle.SQUID_INK, spawnLoc, 10, 0.3, 0.3, 0.3, 0.01);
            }
            
            // Sound effect
            world.playSound(center, Sound.ENTITY_VEX_CHARGE, 1.5f, 1.2f);
        }

        private void updateMinionBehavior() {
            Player player = context.caster();
            Location playerLoc = player.getLocation();
            
            for (Vex minion : summonedMinions) {
                if (minion.isValid() && !minion.isDead()) {
                    // Make minions follow the player
                    minion.setTarget(player);
                    
                    // Find nearby enemies to attack
                    LivingEntity target = findNearestEnemy(minion);
                    if (target != null && target.isValid() && !target.isDead()) {
                        minion.setTarget(target);
                    }
                    
                    org.bukkit.util.Vector direction = playerLoc.toVector().subtract(minion.getLocation().toVector()).normalize();
                        minion.setVelocity(direction.multiply(0.5));
                    
                    // Visual effect periodically
                    if (ticks % 20 == 0) {
                        world.spawnParticle(Particle.SMOKE, minion.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.01);
                    }
                }
            }
        }

        private LivingEntity findNearestEnemy(Vex minion) {
            LivingEntity nearest = null;
            double nearestDistance = 16.0; // Max search distance
            
            for (Entity entity : minion.getNearbyEntities(16, 16, 16)) {
                if (entity instanceof LivingEntity && !(entity instanceof Vex) && 
                    !entity.equals(context.caster()) && entity.isValid() && !entity.isDead()) {
                    
                    double distance = entity.getLocation().distance(minion.getLocation());
                    if (distance < nearestDistance) {
                        nearest = (LivingEntity) entity;
                        nearestDistance = distance;
                    }
                }
            }
            
            return nearest;
        }

        private void createVisualEffects() {
            // Create magical circle around the center
            double currentRadius = radius + Math.sin(ticks * 0.2) * 1.5;
            
            for (int i = 0; i < 24; i++) {
                double angle = (2 * Math.PI * i / 24) + (ticks * 0.1);
                double x = currentRadius * Math.cos(angle);
                double z = currentRadius * Math.sin(angle);
                Location particleLoc = center.clone().add(x, 0.1, z);
                world.spawnParticle(Particle.ENCHANT, particleLoc, 1, 0, 0, 0, 0);
            }
            
            // Create particles around minions
            for (Vex minion : summonedMinions) {
                if (minion.isValid() && !minion.isDead()) {
                    if (ticks % 10 == 0) {
                        world.spawnParticle(Particle.SMOKE, minion.getLocation().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.01);
                    }
                }
            }
            
            // Create central vortex
            if (ticks % 8 == 0) {
                for (int i = 0; i < 10; i++) {
                    double height = i * 0.3;
                    Location vortexLoc = center.clone().add(0, height, 0);
                    world.spawnParticle(Particle.WITCH, vortexLoc, 2, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }

        private void dismissMinions() {
            // Dismiss all minions with visual effects
            for (Vex minion : summonedMinions) {
                if (minion.isValid() && !minion.isDead()) {
                    // Visual effect for dismissal
                    world.spawnParticle(Particle.SMOKE, minion.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                    world.spawnParticle(Particle.EXPLOSION, minion.getLocation(), 1, 0, 0, 0, 0);
                    minion.remove();
                }
            }
            
            // Sound effect
            world.playSound(center, Sound.ENTITY_EVOKER_CAST_SPELL, 1.5f, 1.2f);
            
            summonedMinions.clear();
        }
    }
}