package com.example.empirewand.spell.implementation;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChainLightning implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        var spells = context.config().getSpellsConfig();
        double range = spells.getDouble("chain-lightning.values.range", 20.0);
        double jumpRadius = spells.getDouble("chain-lightning.values.jump-radius", 8.0);
        int jumps = spells.getInt("chain-lightning.values.jumps", 4);
        double damage = spells.getDouble("chain-lightning.values.damage", 8.0);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        // Visual-only enhancement parameters (purely cosmetic)
        int arcParticleCount = spells.getInt("chain-lightning.values.arc_particle_count", 8); // per segment burst
        int arcSteps = spells.getInt("chain-lightning.values.arc_steps", 12); // interpolation steps along arc
        double maxArcLength = spells.getDouble("chain-lightning.values.max_arc_length", 15.0); // clamp for safety

        var first = player.getTargetEntity((int) range);
        if (!(first instanceof LivingEntity current) || current.isDead() || !current.isValid()) {
            context.fx().fizzle(player);
            return;
        }

        Set<LivingEntity> hit = new HashSet<>();
        List<LivingEntity> chain = new ArrayList<>();

        for (int i = 0; i < Math.max(1, jumps); i++) {
            if (current == null || !current.isValid() || current.isDead())
                break;
            if (current.equals(player) && !friendlyFire)
                break;
            hit.add(current);
            chain.add(current);

            // Damage and FX on current
            current.damage(damage, player);
            context.fx().spawnParticles(current.getLocation(), Particle.ELECTRIC_SPARK, 20, 0.3, 0.6, 0.3, 0.1);
            context.fx().playSound(current.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.2f);

            // Find next candidate within jumpRadius
            final Location currentLoc = current.getLocation();
            LivingEntity next = current.getWorld().getNearbyEntities(currentLoc, jumpRadius, jumpRadius, jumpRadius)
                    .stream()
                    .filter(e -> e instanceof LivingEntity)
                    .map(e -> (LivingEntity) e)
                    .filter(l -> !hit.contains(l))
                    .filter(l -> !(l.equals(player) && !friendlyFire))
                    .filter(l -> l.isValid() && !l.isDead())
                    .min(Comparator.comparingDouble(l -> l.getLocation().distanceSquared(currentLoc)))
                    .orElse(null);

            // Enhanced arc visuals between strikes
            if (next != null) {
                renderArc(context, current.getEyeLocation(), next.getEyeLocation(), arcParticleCount, arcSteps,
                        maxArcLength);
            }

            current = next;
        }

        // Small push-back on the last struck entity for feel
        if (!chain.isEmpty()) {
            LivingEntity last = chain.get(chain.size() - 1);
            Vector away = last.getLocation().toVector().subtract(player.getLocation().toVector()).normalize()
                    .multiply(0.2).setY(0.1);
            last.setVelocity(last.getVelocity().add(away));
        }
    }

    /**
     * Renders an electric arc between two points using linear interpolation with
     * slight random jitter
     * to create a jagged lightning feel. Purely visual; does not affect gameplay.
     */
    private void renderArc(SpellContext context, Location from, Location to, int particleCount, int steps,
            double maxLen) {
        if (from == null || to == null || from.getWorld() == null || to.getWorld() == null)
            return;
        if (!from.getWorld().equals(to.getWorld()))
            return;
        // Normalize input points with slight vertical offset
        Location start = from.clone().add(0, 0.25, 0);
        Vector full = to.clone().add(0, 0.25, 0).toVector().subtract(start.toVector());
        double length = full.length();
        if (length < 0.001)
            return;
        if (length > maxLen) {
            full.normalize().multiply(maxLen);
            length = maxLen;
        }

        Vector step = full.clone().multiply(1.0 / Math.max(1, steps));
        double jitterScale = Math.min(0.6, Math.max(0.15, length * 0.08));
        Location cursor = start.clone();
        for (int i = 0; i <= steps; i++) {
            Location point = cursor.clone();
            if (i != 0 && i != steps) {
                point.add((Math.random() - 0.5) * jitterScale,
                        (Math.random() - 0.5) * jitterScale,
                        (Math.random() - 0.5) * jitterScale);
            }
            point.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, particleCount, 0.05, 0.05, 0.05, 0.02);
            if (i % Math.max(2, steps / 6) == 0) {
                point.getWorld().spawnParticle(Particle.CRIT, point, 1, 0, 0, 0, 0);
            }
            cursor.add(step);
        }
    }

    @Override
    public String getName() {
        return "chain-lightning";
    }

    @Override
    public String key() {
        return "chain-lightning";
    }

    @Override
    public Component displayName() {
        return Component.text("Chain Lightning");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
