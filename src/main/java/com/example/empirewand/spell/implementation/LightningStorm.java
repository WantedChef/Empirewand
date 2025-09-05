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

public class LightningStorm implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        Location center = player.getLocation();

        // Config values
        var spells = context.config().getSpellsConfig();
        int strikes = spells.getInt("lightning-storm.values.strikes", 8);
        double radius = spells.getDouble("lightning-storm.values.radius", 10.0);
        double damage = spells.getDouble("lightning-storm.values.damage", 16.0); // 8 hearts
        int delayBetweenStrikes = spells.getInt("lightning-storm.values.delay-ticks", 10); // 0.5 seconds
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        // Schedule lightning strikes
        new BukkitRunnable() {
            private int strikeCount = 0;

            @Override
            public void run() {
                if (strikeCount >= strikes) {
                    this.cancel();
                    return;
                }

                // Random location within radius
                double angle = Math.random() * 2 * Math.PI;
                double distance = Math.random() * radius;
                double x = center.getX() + distance * Math.cos(angle);
                double z = center.getZ() + distance * Math.sin(angle);
                Location strikeLoc = new Location(center.getWorld(), x, center.getY(), z);

                // Strike lightning
                center.getWorld().strikeLightning(strikeLoc);

                // Apply custom damage for this strike only
                damageAtStrike(context, strikeLoc, damage, friendlyFire);

                strikeCount++;
            }
        }.runTaskTimer(context.plugin(), 0L, delayBetweenStrikes);

        // Cast sound
        context.fx().playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);
    }

    private static void damageAtStrike(SpellContext context, Location strikeLoc, double damage, boolean friendlyFire) {
        for (var entity : strikeLoc.getWorld().getNearbyEntities(strikeLoc, 3.0, 3.0, 3.0)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living.equals(context.caster()) && !friendlyFire) continue;
            if (living.isDead() || !living.isValid()) continue;
            living.damage(damage, context.caster());
            context.fx().spawnParticles(living.getLocation(), Particle.ELECTRIC_SPARK, 10, 0.2, 0.2, 0.2, 0.1);
        }
    }

    @Override
    public String getName() {
        return "lightning-storm";
    }

    @Override
    public String key() {
        return "lightning-storm";
    }

    @Override
    public Component displayName() {
        return Component.text("Lightning Storm");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
