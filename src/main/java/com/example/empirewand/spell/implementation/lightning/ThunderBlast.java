package com.example.empirewand.spell.implementation.lightning;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import com.example.empirewand.visual.RingRenderer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class ThunderBlast implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        Location center = player.getLocation();
        if (center == null) {
            return; // Invalid location
        }

        // Config values
        var spells = context.config().getSpellsConfig();
        double radius = spells.getDouble("thunder-blast.values.radius", 6.0);
        double damage = spells.getDouble("thunder-blast.values.damage", 16.0); // 8 hearts
        int strikes = spells.getInt("thunder-blast.values.strikes", 3);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        // Strike lightning at multiple locations
        for (int i = 0; i < strikes; i++) {
            // Random location within radius
            double angle = Math.random() * 2 * Math.PI;
            double distance = Math.random() * radius;
            double x = center.getX() + distance * Math.cos(angle);
            double z = center.getZ() + distance * Math.sin(angle);
            Location strikeLoc = new Location(center.getWorld(), x, center.getY(), z);

            // Strike lightning
            center.getWorld().strikeLightning(strikeLoc);

            // Apply custom damage at this strike location immediately
            damageAtStrike(context, strikeLoc, damage, friendlyFire);
        }

        // Cast sound
        context.fx().playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);

        // Visual arcs + shock ring (purely cosmetic)
        int arcCount = spells.getInt("thunder-blast.arc-count", 6);
        double shockRadius = spells.getDouble("thunder-blast.shock-ring-radius", radius);
        int groundDensity = spells.getInt("thunder-blast.ground-spark-density", 10);
        spawnShockVisuals(context, center, arcCount, shockRadius, groundDensity);
    }

    private static void damageAtStrike(SpellContext context, Location strikeLoc, double damage, boolean friendlyFire) {
        for (var entity : strikeLoc.getWorld().getNearbyEntities(strikeLoc, 3.0, 3.0, 3.0)) {
            if (!(entity instanceof LivingEntity living))
                continue;
            if (living.equals(context.caster()) && !friendlyFire)
                continue;
            if (living.isDead() || !living.isValid())
                continue;

            living.damage(damage, context.caster());
            context.fx().spawnParticles(living.getLocation(), Particle.ELECTRIC_SPARK, 10, 0.2, 0.2, 0.2, 0.1);
        }
    }

    @Override
    public String getName() {
        return "thunder-blast";
    }

    @Override
    public String key() {
        return "thunder-blast";
    }

    @Override
    public Component displayName() {
        return Component.text("Thunder Blast");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }

    private void spawnShockVisuals(SpellContext context, Location center, int arcCount, double shockRadius,
            int groundDensity) {
        if (center.getWorld() == null)
            return;
        // Shock ring rendered over a few ticks expanding slightly
        new BukkitRunnable() {
            double r = Math.max(2.0, shockRadius * 0.5);
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > 6) {
                    cancel();
                    return;
                }
                RingRenderer.renderRing(center, r, 30, (loc, vec) -> {
                    loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 1, 0, 0, 0, 0);
                });
                r += (shockRadius - r) * 0.35; // ease outward
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);

        // Ground sparks inside area
        for (int i = 0; i < groundDensity; i++) {
            double ang = Math.random() * 2 * Math.PI;
            double d = Math.random() * shockRadius;
            double x = center.getX() + Math.cos(ang) * d;
            double z = center.getZ() + Math.sin(ang) * d;
            center.getWorld().spawnParticle(Particle.CRIT, x, center.getY() + 0.1, z, 2, 0.05, 0.05, 0.05, 0.01);
        }

        // Lightning shard vertical arcs
        for (int i = 0; i < arcCount; i++) {
            double ang = (2 * Math.PI * i) / arcCount;
            double x = Math.cos(ang) * (shockRadius * 0.6);
            double z = Math.sin(ang) * (shockRadius * 0.6);
            for (int y = 0; y < 6; y++) {
                double jitterX = x + (Math.random() - 0.5) * 0.4;
                double jitterZ = z + (Math.random() - 0.5) * 0.4;
                center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center.getX() + jitterX,
                        center.getY() + y * 0.4, center.getZ() + jitterZ, 1, 0, 0, 0, 0);
            }
        }
    }
}
