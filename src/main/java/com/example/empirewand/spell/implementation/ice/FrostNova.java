package com.example.empirewand.spell.implementation.ice;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Location;
import com.example.empirewand.visual.RingRenderer;
import com.example.empirewand.visual.SpiralEmitter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class FrostNova implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        var spells = context.config().getSpellsConfig();
        double radius = spells.getDouble("frost-nova.values.radius", 5.0);
        double damage = spells.getDouble("frost-nova.values.damage", 6.0);
        int slowDuration = spells.getInt("frost-nova.values.slow-duration-ticks", 100);
        int slowAmplifier = spells.getInt("frost-nova.values.slow-amplifier", 2);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        // Damage & slow application
        for (var entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living))
                continue;
            if (living.equals(player) && !friendlyFire)
                continue;
            if (living.isDead() || !living.isValid())
                continue;

            living.damage(damage, player);
            living.addPotionEffect(
                    new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, slowAmplifier, false, true));
            context.fx().spawnParticles(living.getLocation(), Particle.SNOWFLAKE, 10, 0.3, 0.3, 0.3, 0.05);
        }

        Location center = player.getLocation();
        context.fx().impact(center, Particle.CLOUD, 30, Sound.BLOCK_SNOW_BREAK, 0.8f, 1.2f);

        // Visual ring & expanding swirl (purely cosmetic)
        int ringParticles = spells.getInt("frost-nova.ring-particle-count", 32);
        double ringStep = spells.getDouble("frost-nova.ring-expand-step", 0.4);
        int swirlDensity = spells.getInt("frost-nova.snow-swirl-density", 18);
        int burstCount = spells.getInt("frost-nova.ice-burst-count", 6);

        new BukkitRunnable() {
            double currentRadius = 0.4;
            int ticks = 0;
            final int maxTicks = (int) Math.ceil(radius / ringStep) + 5;

            @Override
            public void run() {
                if (ticks++ > maxTicks || center.getWorld() == null) {
                    cancel();
                    return;
                }
                // Ring
                RingRenderer.renderRing(center, currentRadius, ringParticles, (loc, vec) -> {
                    loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 1, 0, 0, 0, 0);
                });
                // Swirl (simple upward spiral shell around current ring)
                SpiralEmitter.emit(center.clone().add(0, 0.05, 0), 0.6, 1, swirlDensity, currentRadius * 0.25,
                        Particle.SNOWFLAKE);
                if (ticks % 4 == 0) {
                    for (int i = 0; i < burstCount; i++) {
                        double angle = (2 * Math.PI * i) / burstCount;
                        double x = Math.cos(angle) * currentRadius * 0.6;
                        double z = Math.sin(angle) * currentRadius * 0.6;
                        center.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, center.getX() + x, center.getY() + 0.2,
                                center.getZ() + z, 2, 0.05, 0.05, 0.05, 0.01);
                    }
                }
                currentRadius += ringStep;
                if (currentRadius >= radius) {
                    cancel();
                }
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
    }

    @Override
    public String getName() {
        return "frost-nova";
    }

    @Override
    public String key() {
        return "frost-nova";
    }

    @Override
    public Component displayName() {
        return Component.text("Frost Nova");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
