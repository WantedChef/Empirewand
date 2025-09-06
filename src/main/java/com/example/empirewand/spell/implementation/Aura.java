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
        // Visual ring config
        int ringParticleCount = spells.getInt("aura.values.ring-particle-count", 40);
        double ringMaxRadius = spells.getDouble("aura.values.ring-max-radius", radius);
        double ringExpandStep = spells.getDouble("aura.values.ring-expand-step", 0.6);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        // Start aura effect
        startAuraScheduler(player, radius, damage, duration, tickInterval, friendlyFire, context,
                ringParticleCount, ringMaxRadius, ringExpandStep);

        // Cast sound
        context.fx().playSound(player, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.5f);
    }

    private void startAuraScheduler(Player caster, double radius, double damage, int duration,
            int tickInterval, boolean friendlyFire, SpellContext context,
            int ringParticleCount, double ringMaxRadius, double ringExpandStep) {
        new BukkitRunnable() {
            private int ticks = 0;
            private double currentRingRadius = 0;

            @Override
            public void run() {
                if (ticks >= duration || !caster.isValid() || caster.isDead()) {
                    this.cancel();
                    return;
                }

                // Damage nearby entities
                Location center = caster.getLocation();
                var world = center.getWorld();
                if (world == null) {
                    return; // Defensive - player world should exist
                }
                for (var entity : world.getNearbyEntities(center, radius, radius, radius)) {
                    if (!(entity instanceof LivingEntity living))
                        continue;
                    if (living.equals(caster) && !friendlyFire)
                        continue;
                    if (living.isDead() || !living.isValid())
                        continue;

                    // Apply damage
                    living.damage(damage, caster);

                    // Damage particles
                    context.fx().spawnParticles(living.getLocation(), Particle.PORTAL, 5, 0.2, 0.2, 0.2, 0.1);
                }

                // Expanding ring visual (purely cosmetic)
                currentRingRadius += ringExpandStep;
                if (currentRingRadius > ringMaxRadius)
                    currentRingRadius = ringExpandStep; // reset cycle
                createRingParticles(center, currentRingRadius, ringParticleCount, context);

                ticks += tickInterval;
            }
        }.runTaskTimer(context.plugin(), 0L, tickInterval);
    }

    private void createRingParticles(Location center, double radius, int count, SpellContext context) {
        if (count <= 0)
            return;
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 * i) / count;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location particleLoc = new Location(center.getWorld(), x, center.getY() + 0.05, z);
            context.fx().spawnParticles(particleLoc, Particle.PORTAL, 1, 0, 0, 0, 0);
            if (i % 8 == 0) {
                context.fx().spawnParticles(particleLoc, Particle.ENCHANT, 1, 0, 0, 0, 0);
            }
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