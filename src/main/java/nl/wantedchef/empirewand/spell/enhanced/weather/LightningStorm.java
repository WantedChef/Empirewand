package nl.wantedchef.empirewand.spell.enhanced.weather;

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
 * A devastating spell that summons a lightning storm over a large area,
 * with chain lightning that jumps between enemies.
 */
public class LightningStorm extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Lightning Storm";
            this.description = "Summons a devastating lightning storm that strikes multiple enemies with chain lightning.";
            this.cooldown = java.time.Duration.ofSeconds(50);
            this.spellType = SpellType.LIGHTNING;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new LightningStorm(this);
        }
    }

    private LightningStorm(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "lightning-storm";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(SpellContext context) {
        Player player = context.caster();

        // Configuration
        double radius = spellConfig.getDouble("values.radius", 25.0);
        int lightningCount = spellConfig.getInt("values.lightning-count", 15);
        double damage = spellConfig.getDouble("values.damage", 8.0);
        int chainJumps = spellConfig.getInt("values.chain-jumps", 3);
        double chainRadius = spellConfig.getDouble("values.chain-radius", 6.0);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 120);

        // Find all entities in radius
        List<LivingEntity> potentialTargets = new ArrayList<>();
        for (LivingEntity entity : player.getWorld().getNearbyLivingEntities(player.getLocation(), radius, radius, radius)) {
            if (entity.equals(player)) continue;
            if (entity.isDead() || !entity.isValid()) continue;
            potentialTargets.add(entity);
        }

        // Play initial sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f, 0.5f);

        // Start lightning storm effect
        new LightningStormTask(context, player.getLocation(), potentialTargets, lightningCount, damage, chainJumps, chainRadius, durationTicks)
                .runTaskTimer(context.plugin(), 0L, 3L);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled in scheduler
    }

    private static class LightningStormTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final List<LivingEntity> potentialTargets;
        private final int lightningCount;
        private final double damage;
        private final int chainJumps;
        private final double chainRadius;
        private final int durationTicks;
        private final Random random = new Random();
        private int ticks = 0;
        private int lightningsSpawned = 0;
        private final Set<LivingEntity> struckEntities = new HashSet<>();

        public LightningStormTask(SpellContext context, Location center, List<LivingEntity> potentialTargets, 
                                  int lightningCount, double damage, int chainJumps, double chainRadius, int durationTicks) {
            this.context = context;
            this.center = center;
            this.potentialTargets = new ArrayList<>(potentialTargets);
            this.lightningCount = lightningCount;
            this.damage = damage;
            this.chainJumps = chainJumps;
            this.chainRadius = chainRadius;
            this.durationTicks = durationTicks;
        }

        @Override
        public void run() {
            if (ticks >= durationTicks || lightningsSpawned >= lightningCount) {
                this.cancel();
                return;
            }

            // Spawn lightning at random intervals
            if (ticks % 8 == 0 && lightningsSpawned < lightningCount && !potentialTargets.isEmpty()) {
                // Select a random target or random location
                LivingEntity target = null;
                Location lightningLocation;
                
                if (!potentialTargets.isEmpty() && random.nextBoolean()) {
                    target = potentialTargets.get(random.nextInt(potentialTargets.size()));
                    lightningLocation = target.getLocation();
                } else {
                    // Random location within radius
                    double angle = random.nextDouble() * 2 * Math.PI;
                    double distance = random.nextDouble() * 20;
                    double x = center.getX() + Math.cos(angle) * distance;
                    double z = center.getZ() + Math.sin(angle) * distance;
                    lightningLocation = new Location(center.getWorld(), x, center.getWorld().getHighestBlockYAt((int)x, (int)z) + 1, z);
                }

                strikeLightning(lightningLocation, target);
                lightningsSpawned++;
            }

            ticks++;
        }

        private void strikeLightning(Location location, LivingEntity initialTarget) {
            World world = location.getWorld();
            if (world == null) return;

            // Strike initial lightning
            world.strikeLightning(location);
            
            // Damage initial target if present
            if (initialTarget != null && !struckEntities.contains(initialTarget)) {
                initialTarget.damage(damage, context.caster());
                context.fx().spawnParticles(initialTarget.getLocation(), Particle.ELECTRIC_SPARK, 20, 0.3, 0.6, 0.3, 0.1);
                struckEntities.add(initialTarget);
                
                // Chain lightning to nearby entities
                chainLightning(initialTarget);
            } else {
                // Visual effect for ground strike
                context.fx().spawnParticles(location, Particle.ELECTRIC_SPARK, 30, 1, 1, 1, 0.1);
            }

            // Visual effects
            context.fx().playSound(location, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f);
        }

        private void chainLightning(LivingEntity current) {
            Set<LivingEntity> hit = new HashSet<>();
            hit.add(current);
            
            LivingEntity chainCurrent = current;
            
            for (int i = 0; i < chainJumps && chainCurrent != null; i++) {
                final LivingEntity finalChainCurrent = chainCurrent;
                LivingEntity next = current.getWorld().getNearbyLivingEntities(chainCurrent.getLocation(), chainRadius).stream()
                        .filter(e -> !hit.contains(e) && !e.equals(context.caster()) && e.isValid() && !e.isDead())
                        .min(Comparator.comparingDouble(l -> l.getLocation().distanceSquared(finalChainCurrent.getLocation())))
                        .orElse(null);

                if (next != null) {
                    // Render arc between entities
                    renderArc(chainCurrent.getEyeLocation(), next.getEyeLocation());
                    
                    // Damage the next entity
                    next.damage(damage * (0.7 * (i + 1)), context.caster());
                    context.fx().spawnParticles(next.getLocation(), Particle.ELECTRIC_SPARK, 15, 0.3, 0.6, 0.3, 0.1);
                    
                    hit.add(next);
                    chainCurrent = next;
                } else {
                    chainCurrent = null;
                }
            }
        }

        private void renderArc(Location from, Location to) {
            Vector full = to.toVector().subtract(from.toVector());
            double length = full.length();
            if (length < 0.01) return;

            Vector step = full.clone().multiply(1.0 / 12);
            Location cursor = from.clone();

            for (int i = 0; i <= 12; i++) {
                Location point = cursor.clone();
                if (i != 0 && i != 12) {
                    point.add((Math.random() - 0.5) * 0.3, (Math.random() - 0.5) * 0.3, (Math.random() - 0.5) * 0.3);
                }
                from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 3, 0.05, 0.05, 0.05, 0.02);
                cursor.add(step);
            }
        }
    }
}