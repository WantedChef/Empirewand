package com.example.empirewand.spell.implementation;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

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
                ctx.fx().trail(start, end, Particle.CRIT, blocked ? 1 : 3);
            }
        }.runTaskTimer(context.plugin(), 0L, Math.max(1L, delay));
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
