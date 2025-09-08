package com.example.empirewand.spell.implementation.aura;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.ConfigService;

import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class Aura extends Spell<Void> {

    public record Config(double radius, double damage, int duration, int tickInterval, int ringParticleCount, double ringMaxRadius, double ringExpandStep) {}

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Aura";
            this.description = "Creates a damaging aura around the caster.";
            this.manaCost = 10;
            this.cooldown = java.time.Duration.ofSeconds(30);
            this.spellType = SpellType.AURA;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new Aura(this);
        }
    }

    private Aura(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "aura";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        Config config = loadConfig();
        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig().getBoolean("features.friendly-fire", false);

        startAuraScheduler(player, config, friendlyFire, context);

        context.fx().playSound(player, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.5f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effect is handled in the scheduler
    }

    private Config loadConfig() {
        double radius = spellConfig.getDouble("values.radius", 5.0);
        double damage = spellConfig.getDouble("values.damage-per-tick", 2.0);
        int duration = spellConfig.getInt("values.duration-ticks", 200);
        int tickInterval = spellConfig.getInt("values.tick-interval", 20);
        int ringParticleCount = spellConfig.getInt("values.ring-particle-count", 40);
        double ringMaxRadius = spellConfig.getDouble("values.ring-max-radius", radius);
        double ringExpandStep = spellConfig.getDouble("values.ring-expand-step", 0.6);

        return new Config(radius, damage, duration, tickInterval, ringParticleCount, ringMaxRadius, ringExpandStep);
    }

    private void startAuraScheduler(Player caster, Config config, boolean friendlyFire, SpellContext context) {
        new BukkitRunnable() {
            private int ticks = 0;
            private double currentRingRadius = 0;

            @Override
            public void run() {
                if (ticks >= config.duration() || !caster.isValid() || caster.isDead()) {
                    this.cancel();
                    return;
                }

                Location center = caster.getLocation();
                var world = center.getWorld();
                if (world == null)
                    return;

                for (var entity : world.getNearbyEntities(center, config.radius(), config.radius(), config.radius())) {
                    if (entity instanceof LivingEntity living && !living.equals(caster) && !living.isDead()
                            && living.isValid()) {
                        living.damage(config.damage(), caster);
                        context.fx().spawnParticles(living.getLocation(), Particle.PORTAL, 5, 0.2, 0.2, 0.2, 0.1);
                    }
                }

                currentRingRadius += config.ringExpandStep();
                if (currentRingRadius > config.ringMaxRadius())
                    currentRingRadius = config.ringExpandStep();
                createRingParticles(context, center, currentRingRadius, config.ringParticleCount());

                ticks += config.tickInterval();
            }
        }.runTaskTimer(context.plugin(), 0L, config.tickInterval());
    }

    private void createRingParticles(SpellContext context, Location center, double radius, int count) {
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
}