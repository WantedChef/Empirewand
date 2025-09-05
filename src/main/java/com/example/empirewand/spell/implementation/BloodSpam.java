package com.example.empirewand.spell.implementation;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.example.empirewand.spell.ProjectileSpell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class BloodSpam implements ProjectileSpell {
    @Override
    public void execute(SpellContext context) {
        Player caster = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        int projectileCount = spells.getInt("blood-spam.values.projectile-count", 8);
        double damage = spells.getDouble("blood-spam.values.damage", 1.0);
        int delayTicks = spells.getInt("blood-spam.values.delay-ticks", 2);

        // Launch projectiles in burst
        new BukkitRunnable() {
            private int launched = 0;

            @Override
            public void run() {
                if (launched >= projectileCount) {
                    this.cancel();
                    return;
                }

                launchProjectile(caster, context, damage);
                launched++;
            }
        }.runTaskTimer(context.plugin(), 0L, delayTicks);
    }

    private void launchProjectile(Player caster, SpellContext context, double damage) {
        // Launch snowball as projectile
        Snowball projectile = caster.launchProjectile(Snowball.class);

        // Register projectile with spell system
        projectile.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey("empirewand", "projectile.spell"),
            org.bukkit.persistence.PersistentDataType.STRING,
            getName()
        );
        projectile.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey("empirewand", "projectile.owner"),
            org.bukkit.persistence.PersistentDataType.STRING,
            caster.getUniqueId().toString()
        );

        // Add slight random spread
        Vector direction = caster.getEyeLocation().getDirection();
        direction = direction.add(new Vector(
            (Math.random() - 0.5) * 0.3,
            (Math.random() - 0.5) * 0.3,
            (Math.random() - 0.5) * 0.3
        )).normalize();

        projectile.setVelocity(direction.multiply(1.3));

        // Store damage in projectile metadata
        projectile.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey("empirewand", "blood-spam-damage"),
            org.bukkit.persistence.PersistentDataType.DOUBLE,
            damage
        );

        // Visual effects
        context.fx().spawnParticles(
            caster.getEyeLocation(),
            Particle.DUST,
            5,
            0.1, 0.1, 0.1,
            0,
            new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.0f)
        );
    }

    @Override
    public void onProjectileHit(SpellContext context, Projectile projectile, ProjectileHitEvent event) {
        // Get damage from projectile metadata
        Double damage = projectile.getPersistentDataContainer().get(
            new org.bukkit.NamespacedKey("empirewand", "blood-spam-damage"),
            org.bukkit.persistence.PersistentDataType.DOUBLE
        );

        if (damage == null || event.getHitEntity() == null) {
            return;
        }

        if (event.getHitEntity() instanceof LivingEntity living) {
            living.damage(damage);

            // Visual effects on hit
            context.fx().spawnParticles(
                living.getLocation(),
                Particle.DUST,
                8,
                0.2, 0.2, 0.2,
                0,
                new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.0f)
            );
        }
    }

    @Override
    public String getName() {
        return "blood-spam";
    }

    @Override
    public String key() {
        return "blood-spam";
    }

    @Override
    public Component displayName() {
        return Component.text("Blood Spam");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}