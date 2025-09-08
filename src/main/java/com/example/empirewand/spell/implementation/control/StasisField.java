package com.example.empirewand.spell.implementation.control;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.ConfigService;
import com.example.empirewand.api.EffectService;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class StasisField extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Stasis Field";
            this.description = "Creates a field that slows and weakens nearby entities.";
            this.manaCost = 18;
            this.cooldown = java.time.Duration.ofSeconds(45);
            this.spellType = SpellType.CONTROL;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new StasisField(this);
        }
    }

    private StasisField(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "stasis-field";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    private record Config(double radius, int duration, boolean friendlyFire, int bubbleCount, double bubbleRadius, int sweepInterval) {}

    private Config loadConfig(SpellContext context) {
        return new Config(
                spellConfig.getDouble("values.radius", 6.0),
                spellConfig.getInt("values.duration-ticks", 80),
                EmpireWandAPI.getService(ConfigService.class).getMainConfig().getBoolean("features.friendly-fire", false),
                spellConfig.getInt("bubble-count", 5),
                spellConfig.getDouble("bubble-radius", 1.2),
                spellConfig.getInt("sweep-interval-ticks", 20)
        );
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        Config config = loadConfig(context);

        applyEffects(context, player, config);
        playSound(context, player);
        spawnParticles(context, player, config);

        return null;
    }

    private void applyEffects(SpellContext context, Player player, Config config) {
        for (var entity : player.getWorld().getNearbyEntities(player.getLocation(), config.radius, config.radius, config.radius)) {
            if (entity instanceof LivingEntity living) {
                if (living.equals(player) && !config.friendlyFire)
                    continue;
                if (living.isDead() || !living.isValid())
                    continue;

                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, config.duration, 250, false, true));
                living.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, config.duration, 5, false, true));
                context.fx().spawnParticles(living.getLocation(), Particle.ENCHANT, 20, 0.4, 0.7, 0.4, 0.0);
            }
        }
    }

    private void playSound(SpellContext context, Player player) {
        context.fx().playSound(player, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 0.8f);
    }

    private void spawnParticles(SpellContext context, Player player, Config config) {
        Location center = player.getLocation();

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t > config.duration || center.getWorld() == null) {
                    cancel();
                    return;
                }
                for (int i = 0; i < config.bubbleCount; i++) {
                    double ang = (2 * Math.PI * i) / config.bubbleCount + (t * 0.05);
                    double yOff = 0.5 + Math.sin(t * 0.1 + i) * 0.4;
                    double x = Math.cos(ang) * config.bubbleRadius;
                    double z = Math.sin(ang) * config.bubbleRadius;
                    var point = center.clone().add(x, yOff, z);
                    context.fx().spawnParticles(point, Particle.ENCHANT, 1, 0.0, 0.0, 0.0, 0.0);
                }
                if (t % config.sweepInterval == 0) {
                    for (int i = 0; i < 24; i++) {
                        double a = (2 * Math.PI * i) / 24;
                        double x = Math.cos(a) * (config.bubbleRadius + 0.4);
                        double z = Math.sin(a) * (config.bubbleRadius + 0.4);
                        var ringPoint = center.clone().add(x, 0.05, z);
                        context.fx().spawnParticles(ringPoint, Particle.PORTAL, 2, 0.02, 0.02, 0.02, 0.01);
                    }
                }
                t += 2;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }
}