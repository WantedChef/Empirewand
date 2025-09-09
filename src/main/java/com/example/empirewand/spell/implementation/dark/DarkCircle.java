package com.example.empirewand.spell.implementation.dark;

import com.example.empirewand.api.EmpireWandAPI;

import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class DarkCircle extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Dark Circle";
            this.description = "Creates a pulling void circle that launches enemies into the air.";
            this.cooldown = Duration.ofSeconds(10);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new DarkCircle(this);
        }
    }

    private DarkCircle(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "dark-circle";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        Location center = player.getLocation();
        var world = center.getWorld();
        if (world == null) {
            return null;
        }

        var config = spellConfig;
        var radius = config.getDouble("values.radius", 10.0);
        var pullStrength = config.getDouble("values.pull-strength", 0.4);
        var launchPower = config.getDouble("values.launch-power", 2.2);
        var pullDuration = config.getInt("values.pull-duration-ticks", 30);
        var launchDelay = config.getInt("values.launch-delay-ticks", 10);
        var detonationDamage = config.getDouble("values.detonation-damage", 4.0);
        var radialKnockback = config.getDouble("values.radial-knockback", 1.0);
        var witherDuration = config.getInt("values.wither-duration-ticks", 60);
        var friendlyFire = config.getBoolean("values.friendly-fire", false);

        var entities = world.getNearbyEntities(center, radius, radius, radius);
        for (var entity : entities) {
            if (entity instanceof LivingEntity living) {
                if (living.equals(player) && !friendlyFire) {
                    continue;
                }
                if (living.isDead() || !living.isValid()) {
                    continue;
                }

                startPullEffect(living, center, pullStrength, pullDuration, launchPower, launchDelay,
                        detonationDamage, radialKnockback, witherDuration, player, context);
            }
        }

        createCircleEffect(center, radius, context);
        context.fx().playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler
    }

    private void startPullEffect(LivingEntity target, Location center, double pullStrength, int pullDuration,
            double launchPower, int launchDelay, double detonationDamage, double radialKnockback,
            int witherDuration, Player caster, SpellContext context) {
        new BukkitRunnable() {
            private int ticks = 0;
            private final int launchTick = pullDuration + Math.max(0, launchDelay);

            @Override
            public void run() {
                if (ticks >= launchTick + 2 || target.isDead() || !target.isValid()) {
                    this.cancel();
                    return;
                }

                if (ticks < pullDuration) {
                    var direction = center.toVector().subtract(target.getLocation().toVector()).normalize();
                    target.setVelocity(direction.multiply(pullStrength));
                    context.fx().spawnParticles(target.getLocation(), Particle.SMOKE, 3, 0.1, 0.1, 0.1, 0.05);
                    context.fx().spawnParticles(target.getLocation(), Particle.SOUL_FIRE_FLAME, 2, 0.2, 0.2, 0.2,
                            0.02);
                } else if (ticks == pullDuration - 5) {
                    context.fx().playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.3f);
                    context.fx().spawnParticles(target.getLocation(), Particle.FLASH, 5, 0.5, 0.5, 0.5, 0.1);
                } else if (ticks == launchTick) {
                    target.setVelocity(new Vector(0, launchPower, 0));
                    context.fx().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
                    context.fx().spawnParticles(target.getLocation(), Particle.EXPLOSION, 15, 0.5, 0.5, 0.5, 0.1);

                    if (detonationDamage > 0) {
                        target.damage(detonationDamage, caster);
                    }
                    if (radialKnockback > 0) {
                        var away = target.getLocation().toVector().subtract(center.toVector()).normalize()
                                .multiply(radialKnockback).setY(0.15);
                        target.setVelocity(target.getVelocity().add(away));
                    }
                    if (witherDuration > 0) {
                        target.addPotionEffect(
                                new PotionEffect(PotionEffectType.WITHER, witherDuration, 0, false, true));
                    }
                }
                ticks++;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    private void createCircleEffect(Location center, double radius, SpellContext context) {
        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 40) {
                    this.cancel();
                    return;
                }
                for (int i = 0; i < 360; i += 6) {
                    double angle = Math.toRadians(i);
                    double x = center.getX() + radius * Math.cos(angle);
                    double z = center.getZ() + radius * Math.sin(angle);
                    Location particleLoc = new Location(center.getWorld(), x, center.getY(), z);
                    context.fx().spawnParticles(particleLoc, Particle.SOUL_FIRE_FLAME, 1, 0, 0, 0, 0);
                    if (ticks % 4 == 0) {
                        context.fx().spawnParticles(particleLoc, Particle.SMOKE, 1, 0, 0, 0, 0.02);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
    }
}