package nl.wantedchef.empirewand.spell.lightning;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

/**
 * Unleashes a bolt of lightning that jumps between targets with enhanced effects.
 */
public class ChainLightningEnhanced extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Enhanced Chain Lightning";
            this.description =
                    "Unleashes a powerful bolt of lightning that jumps between targets with enhanced visual effects.";
            this.cooldown = java.time.Duration.ofSeconds(12);
            this.spellType = SpellType.LIGHTNING;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new ChainLightningEnhanced(this);
        }
    }

    private ChainLightningEnhanced(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "chain-lightning-enhanced";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double range = spellConfig.getDouble("values.range", 25.0);
        double jumpRadius = spellConfig.getDouble("values.jump-radius", 10.0);
        int jumps = spellConfig.getInt("values.jumps", 6);
        double damage = spellConfig.getDouble("values.damage", 10.0);
        boolean friendlyFire = spellConfig.getBoolean("values.friendly_fire", false);
        int arcParticleCount = spellConfig.getInt("values.arc_particle_count", 12);
        int arcSteps = spellConfig.getInt("values.arc_steps", 16);
        double maxArcLength = spellConfig.getDouble("values.max_arc_length", 20.0);

        LivingEntity firstTarget = null;
        
        // Try to find a target entity first
        var targetEntity = player.getTargetEntity((int) range);
        if (targetEntity instanceof LivingEntity living && living.isValid() && !living.isDead()) {
            firstTarget = living;
        }
        
        // If no entity target, try to find entities near the target block
        if (firstTarget == null) {
            var targetBlock = player.getTargetBlock(null, (int) range);
            if (targetBlock != null) {
                Location blockLoc = targetBlock.getLocation();
                // Find nearest living entity to the block (within 3 blocks)
                firstTarget = blockLoc.getWorld().getNearbyLivingEntities(blockLoc, 3.0).stream()
                    .filter(entity -> entity.isValid() && !entity.isDead())
                    .filter(entity -> !entity.equals(player) || friendlyFire)
                    .min(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(blockLoc)))
                    .orElse(null);
                
                // If still no target, create a dummy strike at the block location
                if (firstTarget == null) {
                    // Strike the block location and find nearby entities to chain to
                    blockLoc.getWorld().strikeLightning(blockLoc);
                    firstTarget = blockLoc.getWorld().getNearbyLivingEntities(blockLoc, jumpRadius).stream()
                        .filter(entity -> entity.isValid() && !entity.isDead())
                        .filter(entity -> !entity.equals(player) || friendlyFire)
                        .min(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(blockLoc)))
                        .orElse(null);
                }
            }
        }

        if (firstTarget == null) {
            context.fx().fizzle(player);
            return null;
        }

        // Start enhanced chain lightning effect
        new EnhancedChainEffect(context, player, firstTarget, jumps, jumpRadius, damage, friendlyFire,
                arcParticleCount, arcSteps, maxArcLength).runTaskTimer(context.plugin(), 0L, 2L);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled in scheduler
    }

    private static class EnhancedChainEffect extends BukkitRunnable {
        private final SpellContext context;
        private final Player caster;
        private final LivingEntity firstTarget;
        private final int maxJumps;
        private final double jumpRadius;
        private final double damage;
        private final boolean friendlyFire;
        private final int arcParticleCount;
        private final int arcSteps;
        private final double maxArcLength;
        private final Set<LivingEntity> hitEntities = new HashSet<>();
        private final Queue<ChainSegment> chainSegments = new ArrayDeque<>();
        private int ticks = 0;
        private LivingEntity currentTarget;
        private LivingEntity nextTarget;
        private boolean isFinished = false;

        public EnhancedChainEffect(SpellContext context, Player caster, LivingEntity firstTarget,
                int maxJumps, double jumpRadius, double damage, boolean friendlyFire,
                int arcParticleCount, int arcSteps, double maxArcLength) {
            this.context = context;
            this.caster = caster;
            this.firstTarget = firstTarget;
            this.maxJumps = maxJumps;
            this.jumpRadius = jumpRadius;
            this.damage = damage;
            this.friendlyFire = friendlyFire;
            this.arcParticleCount = arcParticleCount;
            this.arcSteps = arcSteps;
            this.maxArcLength = maxArcLength;
            this.currentTarget = firstTarget;
            this.hitEntities.add(firstTarget);
        }

        @Override
        public void run() {
            if (ticks > 100 || isFinished) { // Safety timeout
                this.cancel();
                return;
            }

            // Process chain segments
            processChainSegments();

            // Advance chain if ready
            if (chainSegments.isEmpty() && ticks % 8 == 0) {
                advanceChain();
            }

            // Check if we're done
            if (isFinished && chainSegments.isEmpty()) {
                this.cancel();
                return;
            }

            ticks++;
        }

        private void processChainSegments() {
            Iterator<ChainSegment> iterator = chainSegments.iterator();
            while (iterator.hasNext()) {
                ChainSegment segment = iterator.next();
                segment.update();
                if (segment.isComplete()) {
                    iterator.remove();
                }
            }
        }

        private void advanceChain() {
            if (currentTarget == null || !isValidTarget(currentTarget)) {
                isFinished = true;
                return;
            }

            // Damage current target
            currentTarget.damage(damage, caster);

            // Apply status effects
            applyStatusEffects(currentTarget);

            // Visual effects for current target
            context.fx().spawnParticles(currentTarget.getLocation().add(0, 1, 0),
                    Particle.ELECTRIC_SPARK, 30, 0.5, 0.8, 0.5, 0.2);
            context.fx().playSound(currentTarget.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT,
                    0.8f, 1.3f);

            // Find next target
            nextTarget = findNextTarget(currentTarget);

            if (nextTarget != null) {
                // Create chain segment
                ChainSegment segment = new ChainSegment(currentTarget.getEyeLocation(),
                        nextTarget.getEyeLocation());
                chainSegments.add(segment);

                // Add to hit entities
                hitEntities.add(nextTarget);

                // Set next as current for next iteration
                currentTarget = nextTarget;
            } else {
                isFinished = true;
            }
        }

        private boolean isValidTarget(LivingEntity target) {
            return target.isValid() && !target.isDead() && (!target.equals(caster) || friendlyFire);
        }

        private LivingEntity findNextTarget(LivingEntity current) {
            Location currentLoc = current.getLocation();

            return current.getWorld().getNearbyLivingEntities(currentLoc, jumpRadius).stream()
                    .filter(entity -> !hitEntities.contains(entity))
                    .filter(entity -> !entity.equals(caster) || friendlyFire)
                    .filter(entity -> entity.isValid() && !entity.isDead())
                    .min(Comparator.comparingDouble(
                            entity -> entity.getLocation().distanceSquared(currentLoc)))
                    .orElse(null);
        }

        private void applyStatusEffects(LivingEntity target) {
            // Apply weakness
            target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.WEAKNESS, 100, 1, false, false));

            // Apply slow
            target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS, 80, 2, false, false));
        }

        private class ChainSegment {
            private final Location start;
            private final Location end;
            private final Vector direction;
            private final double length;
            private double progress = 0.0;
            private static final double SPEED = 0.3;
            private final List<Location> boltPoints = new ArrayList<>();

            public ChainSegment(Location start, Location end) {
                this.start = start.clone();
                this.end = end.clone();
                this.direction = end.toVector().subtract(start.toVector()).normalize();
                this.length = start.distance(end);

                // Generate lightning bolt points
                generateBoltPoints();
            }

            private void generateBoltPoints() {
                boltPoints.add(start.clone());

                Vector full = end.toVector().subtract(start.toVector());
                int steps = Math.max(5, (int) (length / 2));

                for (int i = 1; i < steps; i++) {
                    double t = (double) i / steps;
                    Location point = start.clone().add(full.clone().multiply(t));

                    // Add some randomness to create a zigzag effect
                    if (i > 0 && i < steps - 1) {
                        double offset = (Math.random() - 0.5) * 2;
                        point.add(new Vector((Math.random() - 0.5) * offset,
                                (Math.random() - 0.5) * offset * 0.5,
                                (Math.random() - 0.5) * offset));
                    }

                    boltPoints.add(point);
                }

                boltPoints.add(end.clone());
            }

            public void update() {
                progress = Math.min(1.0, progress + SPEED);

                // Render the chain segment
                renderChain();
            }

            private void renderChain() {
                // Render main bolt
                for (int i = 0; i < boltPoints.size() - 1; i++) {
                    Location from = boltPoints.get(i);
                    Location to = boltPoints.get(i + 1);

                    // Main lightning particles
                    from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, from, 3, 0.1, 0.1, 0.1,
                            0.05);

                    // Connection particles
                    Vector connection = to.toVector().subtract(from.toVector());
                    int particleCount = (int) (from.distance(to) * 3);
                    for (int j = 0; j < particleCount; j++) {
                        double t = (double) j / particleCount;
                        Location particleLoc = from.clone().add(connection.clone().multiply(t));
                        from.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                    }
                }

                // Occasionally create branching lightning
                if (Math.random() < 0.3) {
                    Location branchPoint =
                            boltPoints.get((int) (Math.random() * boltPoints.size()));
                    createBranch(branchPoint);
                }
            }

            private void createBranch(Location from) {
                // Create a small branch of lightning
                Vector branchDirection =
                        new Vector(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5)
                                .normalize().multiply(2);

                Location to = from.clone().add(branchDirection);

                // Draw branch
                Vector connection = to.toVector().subtract(from.toVector());
                int particleCount = (int) (from.distance(to) * 4);
                for (int j = 0; j < particleCount; j++) {
                    double t = (double) j / particleCount;
                    Location particleLoc = from.clone().add(connection.clone().multiply(t));
                    from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0.05,
                            0.05, 0.05, 0.02);
                }
            }

            public boolean isComplete() {
                return progress >= 1.0;
            }
        }
    }
}
