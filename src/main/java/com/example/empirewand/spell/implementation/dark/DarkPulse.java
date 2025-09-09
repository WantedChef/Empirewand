package com.example.empirewand.spell.implementation.dark;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.ConfigService;

import com.example.empirewand.core.storage.Keys;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class DarkPulse extends Spell<Void> {

    public record Config(
            double range,
            int witherDuration,
            int witherAmplifier,
            int blindDuration,
            double speed,
            double radius,
            double damage,
            double knockback,
            boolean friendlyFire,
            int ringParticleCount,
            double ringRadiusStep,
            int ringEveryTicks) {
    }

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Dark Pulse";
            this.description = "Launches a pulsing void skull.";
            this.cooldown = java.time.Duration.ofSeconds(8);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new DarkPulse(this);
        }
    }

    private Config config;

    private DarkPulse(Builder builder) {
        super(builder);
        // Config will be initialized lazily when first accessed
    }

    private Config getConfig() {
        if (config == null) {
            // This will be called after loadConfig has been called
            config = new Config(
                    spellConfig.getDouble("values.range", 24.0),
                    spellConfig.getInt("values.wither-duration-ticks", 120),
                    spellConfig.getInt("values.wither-amplifier", 1),
                    spellConfig.getInt("values.blind-duration-ticks", 60),
                    spellConfig.getDouble("values.speed", 1.8),
                    spellConfig.getDouble("values.explosion-radius", 4.0),
                    spellConfig.getDouble("values.damage", 6.0),
                    spellConfig.getDouble("values.knockback", 0.6),
                    EmpireWandAPI.getService(ConfigService.class).getMainConfig().getBoolean("features.friendly-fire",
                            false),
                    spellConfig.getInt("values.ring-particle-count", 14),
                    spellConfig.getDouble("values.ring-radius-step", 0.25),
                    spellConfig.getInt("values.ring-interval-ticks", 2));
        }
        return config;
    }

    @Override
    public String key() {
        return "dark-pulse";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        var targetEntity = player.getTargetEntity((int) getConfig().range);
        if (!(targetEntity instanceof LivingEntity target) || target.isDead() || !target.isValid()) {
            context.fx().fizzle(player);
            return null;
        }

        Vector direction = target.getLocation().toVector().subtract(player.getEyeLocation().toVector()).normalize();
        Location spawnLoc = player.getEyeLocation().add(direction.clone().multiply(0.5));

        WitherSkull skull = player.getWorld().spawn(spawnLoc, WitherSkull.class);
        skull.setCharged(true);
        skull.setVelocity(direction.multiply(getConfig().speed));
        skull.setShooter(player);
        skull.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, Keys.STRING_TYPE.getType(), key());

        new RingPulse(context, skull, getConfig().ringParticleCount, getConfig().ringRadiusStep,
                getConfig().ringEveryTicks)
                .runTaskTimer(context.plugin(), 0L, 1L);
        context.fx().playSound(player, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.0f);

        context.plugin().getServer().getPluginManager().registerEvents(
                new PulseListener(context, getConfig().witherDuration, getConfig().witherAmplifier,
                        getConfig().blindDuration,
                        getConfig().radius, getConfig().damage, getConfig().knockback, getConfig().friendlyFire),
                context.plugin());
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled by the listener.
    }

    private static class RingPulse extends BukkitRunnable {
        private final SpellContext context;
        private final WitherSkull projectile;
        private final int ringParticleCount;
        private final double radiusStep;
        private final int interval;
        private int ticks = 0;
        private double accumRadius = 0;

        RingPulse(SpellContext context, WitherSkull projectile, int ringParticleCount, double radiusStep,
                int interval) {
            this.context = context;
            this.projectile = projectile;
            this.ringParticleCount = Math.max(4, ringParticleCount);
            this.radiusStep = Math.max(0.05, radiusStep);
            this.interval = Math.max(1, interval);
        }

        @Override
        public void run() {
            if (!projectile.isValid() || projectile.isDead()) {
                cancel();
                return;
            }
            if (ticks++ > 20 * 6) {
                cancel();
                return;
            }

            if (ticks % interval != 0)
                return;

            accumRadius += radiusStep;
            Location center = projectile.getLocation();

            for (int i = 0; i < ringParticleCount; i++) {
                double angle = (Math.PI * 2 * i) / ringParticleCount;
                double x = center.getX() + accumRadius * Math.cos(angle);
                double z = center.getZ() + accumRadius * Math.sin(angle);
                Location p = new Location(center.getWorld(), x, center.getY() + 0.15, z);
                context.fx().spawnParticles(p, Particle.SMOKE, 1, 0, 0, 0, 0);
                if (i % 3 == 0) {
                    context.fx().spawnParticles(p, Particle.SOUL_FIRE_FLAME, 1, 0, 0, 0, 0.0);
                }
            }
            context.fx().spawnParticles(center, Particle.REVERSE_PORTAL, 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    private static class PulseListener implements Listener {
        private final SpellContext context;
        private final int witherDuration;
        private final int witherAmplifier;
        private final int blindDuration;
        private final double radius;
        private final double damage;
        private final double knockback;
        private final boolean friendlyFire;

        public PulseListener(SpellContext context, int witherDuration, int witherAmplifier, int blindDuration,
                double radius, double damage, double knockback, boolean friendlyFire) {
            this.context = context;
            this.witherDuration = witherDuration;
            this.witherAmplifier = witherAmplifier;
            this.blindDuration = blindDuration;
            this.radius = radius;
            this.damage = damage;
            this.knockback = knockback;
            this.friendlyFire = friendlyFire;
        }

        @EventHandler
        public void onProjectileHit(ProjectileHitEvent event) {
            Projectile projectile = event.getEntity();
            if (!(projectile instanceof WitherSkull))
                return;

            String spellType = projectile.getPersistentDataContainer().get(Keys.PROJECTILE_SPELL,
                    Keys.STRING_TYPE.getType());
            if (!"dark-pulse".equals(spellType))
                return;

            Location hitLoc = projectile.getLocation();
            context.fx().impact(hitLoc, Particle.SOUL_FIRE_FLAME, 50, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.7f);

            for (var entity : hitLoc.getWorld().getNearbyEntities(hitLoc, radius, radius, radius)) {
                if (entity instanceof LivingEntity living) {
                    if (living.equals(projectile.getShooter()) && !friendlyFire)
                        continue;
                    if (living.isDead() || !living.isValid())
                        continue;

                    if (damage > 0)
                        living.damage(damage, (projectile.getShooter() instanceof Player p) ? p : null);
                    living.addPotionEffect(
                            new PotionEffect(PotionEffectType.WITHER, witherDuration, witherAmplifier, false, true));
                    if (blindDuration > 0)
                        living.addPotionEffect(
                                new PotionEffect(PotionEffectType.BLINDNESS, blindDuration, 0, false, true));
                    if (knockback > 0) {
                        Vector kb = living.getLocation().toVector().subtract(hitLoc.toVector()).normalize()
                                .multiply(knockback).setY(0.2);
                        living.setVelocity(kb);
                    }
                }
            }

            projectile.remove();
            HandlerList.unregisterAll(this);
        }
    }
}