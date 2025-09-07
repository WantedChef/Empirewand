package com.example.empirewand.spell.implementation.projectile;

import java.util.Objects;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.example.empirewand.spell.Prereq;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;

import net.kyori.adventure.text.Component;

/**
 * MagicMissile – Fires a sequence of homing-like beams at the looked-at target.
 * Enhanced lightly with extra ENCHANTMENT style particles for a more "magisch"
 * gevoel
 * while keeping implementation simple & performant (no temp blocks).
 */
public class MagicMissile implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        var spells = context.config().getSpellsConfig();
        double damage = spells.getDouble("magic-missile.values.damage-per-missile", 3.0);
        int missiles = spells.getInt("magic-missile.values.missile-count", 3);
        int delay = spells.getInt("magic-missile.values.delay-ticks", 7);
        boolean requiresLos = spells.getBoolean("magic-missile.flags.requires-los", true);
        int extraParticles = spells.getInt("magic-missile.values.extra_particle_count", 4);

        Entity lookedAt = player.getTargetEntity(20);
        if (!(lookedAt instanceof LivingEntity target) || target.isDead() || !target.isValid()) {
            context.fx().fizzle(player);
            return;
        }

        // Cast sound
        context.fx().playSound(player, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.2f);

        // Prefer using the server scheduler (tests mock server.getScheduler())
        var server = context.plugin().getServer();
        final int period = Math.max(1, delay);
        server.getScheduler().runTaskTimer(context.plugin(), new Runnable() {
            int count = 0;

            @Override
            public void run() {
                if (count >= missiles || target.isDead() || !target.isValid() || !player.isValid()
                        || player.getWorld() != target.getWorld()) {
                    return;
                }
                if (requiresLos && !player.hasLineOfSight(target)) {
                    drawBeam(player, target, context, 1, true);
                    count++;
                    return;
                }
                drawBeam(player, target, context, extraParticles, false);
                boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);
                if (!(target instanceof Player p && !friendlyFire
                        && Objects.equals(p.getUniqueId(), player.getUniqueId()))) {
                    target.damage(damage, player);
                }
                count++;
            }
        }, 0L, period);
    }

    private void drawBeam(Player from, LivingEntity to, SpellContext ctx, int particleFactor, boolean blocked) {
        var start = from.getEyeLocation();
        var end = to.getEyeLocation();
        ctx.fx().trail(start, end, Particle.CRIT, blocked ? 1 : Math.max(1, particleFactor));

        // Extra ambient sparkle along path for epic look
        if (!blocked && particleFactor > 0) {
            var vec = end.toVector().subtract(start.toVector());
            double length = vec.length();
            if (length > 0.0001) {
                var dir = vec.normalize();
                int steps = (int) Math.max(2, length * 4); // 0.25 block steps
                for (int i = 0; i < steps; i += 2) { // skip every other for perf
                    var point = start.clone().add(dir.clone().multiply(i * 0.25));
                    start.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0.02, 0.02, 0.02, 0.0);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "magic-missile";
    }

    @Override
    public String key() {
        return "magic-missile";
    }

    @Override
    public Component displayName() {
        return Component.text("Magic Missile");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
