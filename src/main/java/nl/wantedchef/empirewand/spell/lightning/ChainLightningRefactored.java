package nl.wantedchef.empirewand.spell.lightning;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Unleashes a bolt of lightning that jumps between targets. Refactored for better performance and
 * code quality.
 */
public class ChainLightningRefactored extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Chain Lightning";
            this.description = "Unleashes a bolt of lightning that jumps between targets.";
            this.cooldown = java.time.Duration.ofSeconds(12);
            this.spellType = SpellType.LIGHTNING;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new ChainLightningRefactored(this);
        }
    }

    private ChainLightningRefactored(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "chain-lightning";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(SpellContext context) {
        Player player = context.caster();

        // Configuration
        double range = spellConfig.getDouble("values.range", 20.0);
        double jumpRadius = spellConfig.getDouble("values.jump-radius", 8.0);
        int jumps = spellConfig.getInt("values.jumps", 4);
        double damage = spellConfig.getDouble("values.damage", 8.0);
        boolean friendlyFire = false; // TODO: Get from config service
        int arcParticleCount = spellConfig.getInt("values.arc_particle_count", 8);
        int arcSteps = spellConfig.getInt("values.arc_steps", 12);
        double maxArcLength = spellConfig.getDouble("values.max_arc_length", 15.0);

        // Get first target
        LivingEntity firstTarget = getValidTarget(player, range);
        if (firstTarget == null) {
            context.fx().fizzle(player);
            return null;
        }

        // Chain lightning through targets
        chainLightning(context, firstTarget, jumps, jumpRadius, damage, friendlyFire,
                arcParticleCount, arcSteps, maxArcLength);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    /**
     * Gets a valid target for the lightning chain.
     *
     * @param player The player casting the spell.
     * @param range The range to search for targets.
     * @return A valid living entity target, or null if none found.
     */
    private LivingEntity getValidTarget(Player player, double range) {
        var targetEntity = player.getTargetEntity((int) range);
        if (!(targetEntity instanceof LivingEntity)) {
            return null;
        }

        LivingEntity target = (LivingEntity) targetEntity;
        return target.isDead() || !target.isValid() ? null : target;
    }

    /**
     * Chains lightning through multiple targets.
     */
    private void chainLightning(SpellContext context, LivingEntity firstTarget, int maxJumps,
            double jumpRadius, double damage, boolean friendlyFire, int arcParticleCount,
            int arcSteps, double maxArcLength) {
        Set<LivingEntity> hitEntities = new HashSet<>();
        LivingEntity currentTarget = firstTarget;

        // Add first target to hit set
        hitEntities.add(currentTarget);

        for (int i = 0; i < maxJumps && currentTarget != null; i++) {
            // Validate current target
            if (!isValidTarget(context.caster(), currentTarget, friendlyFire)) {
                break;
            }

            // Damage current target
            currentTarget.damage(damage, context.caster());
            context.fx().spawnParticles(currentTarget.getLocation(), Particle.ELECTRIC_SPARK, 20,
                    0.3, 0.6, 0.3, 0.1);
            context.fx().playSound(currentTarget.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                    0.6f, 1.2f);

            // Find next target
            LivingEntity nextTarget =
                    findNextTarget(context, currentTarget, jumpRadius, hitEntities, friendlyFire);

            // Render arc to next target
            if (nextTarget != null) {
                renderArc(currentTarget.getEyeLocation(), nextTarget.getEyeLocation(),
                        arcParticleCount, arcSteps, maxArcLength);
                hitEntities.add(nextTarget);
            }

            currentTarget = nextTarget;
        }
    }

    /**
     * Validates if a target is valid for lightning damage.
     */
    private boolean isValidTarget(Player caster, LivingEntity target, boolean friendlyFire) {
        return target.isValid() && !target.isDead() && (!target.equals(caster) || friendlyFire);
    }

    /**
     * Finds the next valid target for the lightning chain.
     */
    private LivingEntity findNextTarget(SpellContext context, LivingEntity currentTarget,
            double jumpRadius, Set<LivingEntity> hitEntities, boolean friendlyFire) {
        Location currentLoc = currentTarget.getLocation();

        return currentTarget.getWorld().getNearbyLivingEntities(currentLoc, jumpRadius).stream()
                .filter(entity -> !hitEntities.contains(entity))
                .filter(entity -> !entity.equals(context.caster()) || friendlyFire)
                .filter(entity -> entity.isValid() && !entity.isDead())
                .min(Comparator.comparingDouble(
                        entity -> entity.getLocation().distanceSquared(currentLoc)))
                .orElse(null);
    }

    /**
     * Renders an electric arc between two locations.
     */
    private void renderArc(Location from, Location to, int particleCount, int steps,
            double maxLen) {
        Vector full = to.toVector().subtract(from.toVector());
        double length = full.length();
        if (length < 0.01 || length > maxLen)
            return;

        Vector step = full.clone().multiply(1.0 / steps);
        double jitterScale = Math.min(0.6, Math.max(0.15, length * 0.08));
        Location cursor = from.clone();

        for (int i = 0; i <= steps; i++) {
            Location point = (i == 0 || i == steps) ? cursor.clone()
                    : cursor.clone().add((Math.random() - 0.5) * jitterScale,
                            (Math.random() - 0.5) * jitterScale,
                            (Math.random() - 0.5) * jitterScale);

            from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, particleCount, 0.05, 0.05,
                    0.05, 0.02);

            if (i % Math.max(2, steps / 6) == 0) {
                from.getWorld().spawnParticle(Particle.CRIT, point, 1, 0, 0, 0, 0);
            }
            cursor.add(step);
        }
    }
}
