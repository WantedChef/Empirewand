package com.example.empirewand.spell.implementation;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;

public class MagicMissile implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values with sensible defaults
        var spells = context.config().getSpellsConfig();
        double damage = spells.getDouble("magic-missile.values.damage-per-missile", 3.0);
        int missiles = spells.getInt("magic-missile.values.missile-count", 3);
        int delay = spells.getInt("magic-missile.values.delay-ticks", 7);
        boolean requiresLos = spells.getBoolean("magic-missile.flags.requires-los", true);

        Entity lookedAt = player.getTargetEntity(20);
        if (!(lookedAt instanceof LivingEntity target) || target.isDead() || !target.isValid()) {
            context.fx().fizzle(player);
            return;
        }

        // Cast SFX
        context.fx().playSound(player, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.2f);

        new BukkitRunnable() {
            private int count = 0;

            @Override
            public void run() {
                if (count >= missiles || target.isDead() || !target.isValid() || !player.isValid() || player.getWorld() != target.getWorld()) {
                    this.cancel();
                    return;
                }

                // Optional line-of-sight gate
                if (requiresLos && !player.hasLineOfSight(target)) {
                    // Still draw a faint beam to indicate block
                    drawBeam(player, target, context, true);
                    count++;
                    return;
                }

                // Visual beam then apply damage
                drawBeam(player, target, context, false);

                // Self-protection when friendly-fire disabled (self-only check)
                boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);
                if (!(target instanceof Player p && !friendlyFire && p.getUniqueId().equals(player.getUniqueId()))) {
                    target.damage(damage, player);
                }

                count++;
            }

            private void drawBeam(Player from, LivingEntity to, SpellContext ctx, boolean blocked) {
                var start = from.getEyeLocation();
                var end = to.getEyeLocation();
                Vector dir = end.toVector().subtract(start.toVector());
                double length = dir.length();
                if (length <= 0.001) return;
                dir.normalize();
                int steps = Math.max(8, (int) Math.min(24, length * 4));
                Vector step = dir.clone().multiply(length / steps);
                var point = start.clone();
                for (int i = 0; i < steps; i++) {
                    // Use widely-available particle to avoid API differences
                    ctx.fx().spawnParticles(point, Particle.CRIT, blocked ? 1 : 3, 0.0, 0.0, 0.0, 0);
                    point.add(step);
                }
            }
        }.runTaskTimer(context.plugin(), 0L, Math.max(1L, delay));
    }

    @Override
    public String getName() {
        return "magic-missile";
    }
}
