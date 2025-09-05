package com.example.empirewand.spell.implementation;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class ExplosionTrail implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        int duration = spells.getInt("explosion-trail.values.duration-ticks", 100); // 5 seconds
        double damage = spells.getDouble("explosion-trail.values.damage", 8.0); // 4 hearts
        int tickInterval = spells.getInt("explosion-trail.values.tick-interval", 10); // 0.5 seconds
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        // Start trail effect
        startTrailScheduler(player, duration, damage, tickInterval, friendlyFire, context);

        // Cast sound
        context.fx().playSound(player, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
    }

    private void startTrailScheduler(Player caster, int duration, double damage, int tickInterval,
                                   boolean friendlyFire, SpellContext context) {
        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration || !caster.isValid() || caster.isDead()) {
                    this.cancel();
                    return;
                }

                Location playerLoc = caster.getLocation();

                // Create explosion at player location
                for (var entity : playerLoc.getWorld().getNearbyEntities(playerLoc, 3.0, 3.0, 3.0)) {
                    if (!(entity instanceof LivingEntity living)) continue;
                    if (living.equals(caster) && !friendlyFire) continue;
                    if (living.isDead() || !living.isValid()) continue;

                    living.damage(damage, caster);
                }

                // Explosion effects
                context.fx().spawnParticles(playerLoc, Particle.EXPLOSION, 20, 0.3, 0.3, 0.3, 0.1);
                context.fx().playSound(playerLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

                ticks += tickInterval;
            }
        }.runTaskTimer(context.plugin(), 0L, tickInterval);
    }

    @Override
    public String getName() {
        return "explosion-trail";
    }

    @Override
    public String key() {
        return "explosion-trail";
    }

    @Override
    public Component displayName() {
        return Component.text("Explosion Trail");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}