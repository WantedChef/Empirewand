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

public class Aura implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double radius = spells.getDouble("aura.values.radius", 5.0);
        double damage = spells.getDouble("aura.values.damage-per-tick", 2.0);
        int duration = spells.getInt("aura.values.duration-ticks", 200); // 10 seconds
        int tickInterval = spells.getInt("aura.values.tick-interval", 20); // 1 second
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        // Start aura effect
        startAuraScheduler(player, radius, damage, duration, tickInterval, friendlyFire, context);

        // Cast sound
        context.fx().playSound(player, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.5f);
    }

    private void startAuraScheduler(Player caster, double radius, double damage, int duration,
                                   int tickInterval, boolean friendlyFire, SpellContext context) {
        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration || !caster.isValid() || caster.isDead()) {
                    this.cancel();
                    return;
                }

                // Damage nearby entities
                Location center = caster.getLocation();
                for (var entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                    if (!(entity instanceof LivingEntity living)) continue;
                    if (living.equals(caster) && !friendlyFire) continue;
                    if (living.isDead() || !living.isValid()) continue;

                    // Apply damage
                    living.damage(damage, caster);

                    // Damage particles
                    context.fx().spawnParticles(living.getLocation(), Particle.PORTAL, 5, 0.2, 0.2, 0.2, 0.1);
                }

                // Aura particles around caster
                createAuraParticles(center, radius, context);

                ticks += tickInterval;
            }
        }.runTaskTimer(context.plugin(), 0L, tickInterval);
    }

    private void createAuraParticles(Location center, double radius, SpellContext context) {
        for (int i = 0; i < 360; i += 20) {
            double angle = Math.toRadians(i);
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location particleLoc = new Location(center.getWorld(), x, center.getY(), z);

            context.fx().spawnParticles(particleLoc, Particle.PORTAL, 1, 0, 0, 0, 0);
        }
    }

    @Override
    public String getName() {
        return "aura";
    }

    @Override
    public String key() {
        return "aura";
    }

    @Override
    public Component displayName() {
        return Component.text("Aura");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}