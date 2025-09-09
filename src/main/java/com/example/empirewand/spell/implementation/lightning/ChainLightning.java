package com.example.empirewand.spell.implementation.lightning;

import com.example.empirewand.api.ConfigService;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class ChainLightning extends Spell<Void> {

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
            return new ChainLightning(this);
        }
    }

    private ChainLightning(Builder builder) {
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

        double range = spellConfig.getDouble("values.range", 20.0);
        double jumpRadius = spellConfig.getDouble("values.jump-radius", 8.0);
        int jumps = spellConfig.getInt("values.jumps", 4);
        double damage = spellConfig.getDouble("values.damage", 8.0);
        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig()
                .getBoolean("features.friendly-fire", false);
        int arcParticleCount = spellConfig.getInt("values.arc_particle_count", 8);
        int arcSteps = spellConfig.getInt("values.arc_steps", 12);
        double maxArcLength = spellConfig.getDouble("values.max_arc_length", 15.0);

        var first = player.getTargetEntity((int) range);
        if (!(first instanceof LivingEntity current) || current.isDead() || !current.isValid()) {
            context.fx().fizzle(player);
            return null; // Fixed: Added missing return statement
        }

        Set<LivingEntity> hit = new HashSet<>();
        hit.add(current); // Fixed: Add first target to hit set immediately

        for (int i = 0; i < jumps && current != null; i++) { // Fixed: Added null check in loop condition
            if (!current.isValid() || current.isDead() || (current.equals(player) && !friendlyFire)) {
                break;
            }

            current.damage(damage, player);
            context.fx().spawnParticles(current.getLocation(), Particle.ELECTRIC_SPARK, 20, 0.3, 0.6, 0.3, 0.1);
            context.fx().playSound(current.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.2f);

            final Location currentLoc = current.getLocation();
            LivingEntity next = current.getWorld().getNearbyLivingEntities(currentLoc, jumpRadius).stream()
                    .filter(e -> !hit.contains(e) && !(e.equals(player) && !friendlyFire) && e.isValid() && !e.isDead())
                    .min(Comparator.comparingDouble(l -> l.getLocation().distanceSquared(currentLoc)))
                    .orElse(null);

            if (next != null) {
                renderArc(current.getEyeLocation(), next.getEyeLocation(), arcParticleCount, arcSteps, maxArcLength);
                hit.add(next); // Add to hit set before next iteration
            }
            current = next;
        }
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    private void renderArc(Location from, Location to, int particleCount, int steps, double maxLen) {
        Vector full = to.toVector().subtract(from.toVector());
        double length = full.length();
        if (length < 0.01 || length > maxLen)
            return;

        Vector step = full.clone().multiply(1.0 / steps);
        double jitterScale = Math.min(0.6, Math.max(0.15, length * 0.08));
        Location cursor = from.clone();

        for (int i = 0; i <= steps; i++) {
            Location point = (i == 0 || i == steps) ? cursor.clone()
                    : cursor.clone().add((Math.random() - 0.5) * jitterScale, (Math.random() - 0.5) * jitterScale,
                            (Math.random() - 0.5) * jitterScale);
            from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, particleCount, 0.05, 0.05, 0.05, 0.02);
            if (i % Math.max(2, steps / 6) == 0) {
                from.getWorld().spawnParticle(Particle.CRIT, point, 1, 0, 0, 0, 0);
            }
            cursor.add(step);
        }
    }
}