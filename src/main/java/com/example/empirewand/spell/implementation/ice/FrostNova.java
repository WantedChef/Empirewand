package com.example.empirewand.spell.implementation.ice;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.ConfigService;

import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import com.example.empirewand.visual.RingRenderer;
import com.example.empirewand.visual.SpiralEmitter;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FrostNova extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Frost Nova";
            this.description = "Creates an expanding nova of frost, damaging and slowing enemies.";
            this.cooldown = java.time.Duration.ofSeconds(15);
            this.spellType = SpellType.ICE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new FrostNova(this);
        }
    }

    private FrostNova(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "frost-nova";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double radius = spellConfig.getDouble("values.radius", 5.0);
        double damage = spellConfig.getDouble("values.damage", 6.0);
        int slowDuration = spellConfig.getInt("values.slow-duration-ticks", 100);
        int slowAmplifier = spellConfig.getInt("values.slow-amplifier", 2);
        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig()
                .getBoolean("features.friendly-fire", false);

        for (var entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (entity instanceof LivingEntity living) {
                if (living.equals(player) && !friendlyFire)
                    continue;
                if (living.isDead() || !living.isValid())
                    continue;

                living.damage(damage, player);
                living.addPotionEffect(
                        new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, slowAmplifier, false, true));
                context.fx().spawnParticles(living.getLocation(), Particle.SNOWFLAKE, 10, 0.3, 0.3, 0.3, 0.05);
            }
        }

        Location center = player.getLocation();
        context.fx().impact(center, Particle.CLOUD, 30, Sound.BLOCK_SNOW_BREAK, 0.8f, 1.2f);

        int ringParticles = spellConfig.getInt("ring-particle-count", 32);
        double ringStep = spellConfig.getDouble("ring-expand-step", 0.4);
        int swirlDensity = spellConfig.getInt("snow-swirl-density", 18);
        int burstCount = spellConfig.getInt("ice-burst-count", 6);

        new NovaVisuals(center, radius, ringStep, ringParticles, swirlDensity, burstCount, context)
                .runTaskTimer(context.plugin(), 0L, 2L);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    private class NovaVisuals extends BukkitRunnable {
        private final Location center;
        private final double maxRadius;
        private final double ringStep;
        private final int ringParticles;
        private final int swirlDensity;
        private final int burstCount;
        private double currentRadius = 0.4;
        private int ticks = 0;

        public NovaVisuals(Location center, double maxRadius, double ringStep, int ringParticles, int swirlDensity,
                int burstCount, SpellContext context) {
            this.center = center;
            this.maxRadius = maxRadius;
            this.ringStep = ringStep;
            this.ringParticles = ringParticles;
            this.swirlDensity = swirlDensity;
            this.burstCount = burstCount;
        }

        @Override
        public void run() {
            if (currentRadius >= maxRadius || center.getWorld() == null) {
                cancel();
                return;
            }

            RingRenderer.renderRing(center, currentRadius, ringParticles,
                    (loc, vec) -> loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 1, 0, 0, 0, 0));
            SpiralEmitter.emit(center.clone().add(0, 0.05, 0), 0.6, 1, swirlDensity, currentRadius * 0.25,
                    Particle.SNOWFLAKE);

            if (ticks % 4 == 0) {
                for (int i = 0; i < burstCount; i++) {
                    double angle = (2 * Math.PI * i) / burstCount;
                    Location burstLoc = center.clone().add(Math.cos(angle) * currentRadius * 0.6, 0.2,
                            Math.sin(angle) * currentRadius * 0.6);
                    center.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, burstLoc, 2, 0.05, 0.05, 0.05, 0.01);
                }
            }
            currentRadius += ringStep;
            ticks++;
        }
    }
}