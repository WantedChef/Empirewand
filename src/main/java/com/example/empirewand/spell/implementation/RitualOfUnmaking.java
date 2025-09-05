package com.example.empirewand.spell.implementation;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

import java.util.List;

public class RitualOfUnmaking implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        int channelTicks = spells.getInt("ritual-of-unmaking.values.channel-ticks", 40);
        double radius = spells.getDouble("ritual-of-unmaking.values.radius", 6.0);
        double damage = spells.getDouble("ritual-of-unmaking.values.damage", 8.0);
        int weaknessDuration = spells.getInt("ritual-of-unmaking.values.weakness-duration-ticks", 120);
        int weaknessAmplifier = spells.getInt("ritual-of-unmaking.values.weakness-amplifier", 0);
        boolean hitPlayers = spells.getBoolean("ritual-of-unmaking.flags.hit-players", true);
        boolean hitMobs = spells.getBoolean("ritual-of-unmaking.flags.hit-mobs", true);

        // Start channeling
        new ChannelTask(context, player, channelTicks, radius, damage, weaknessDuration, weaknessAmplifier, hitPlayers, hitMobs).runTaskTimer(context.plugin(), 0L, 1L);
    }

    private static class ChannelTask extends BukkitRunnable {
        private final SpellContext context;
        private final Player player;
        private final int channelTicks;
        private final double radius;
        private final double damage;
        private final int weaknessDuration;
        private final int weaknessAmplifier;
        private final boolean hitPlayers;
        private final boolean hitMobs;
        private int ticksPassed = 0;

        public ChannelTask(SpellContext context, Player player, int channelTicks, double radius, double damage,
                          int weaknessDuration, int weaknessAmplifier, boolean hitPlayers, boolean hitMobs) {
            this.context = context;
            this.player = player;
            this.channelTicks = channelTicks;
            this.radius = radius;
            this.damage = damage;
            this.weaknessDuration = weaknessDuration;
            this.weaknessAmplifier = weaknessAmplifier;
            this.hitPlayers = hitPlayers;
            this.hitMobs = hitMobs;
        }

        @Override
        public void run() {
            if (!player.isValid() || player.isDead()) {
                this.cancel();
                return;
            }

            // Check for interruption (damage taken)
            if (player.getLastDamage() > 0 && ticksPassed > 0) {
                this.cancel();
                onInterrupt();
                return;
            }

            // Spawn circle particles
            spawnCircleParticles(player, radius);

            ticksPassed++;

            if (ticksPassed >= channelTicks) {
                this.cancel();
                onFinish();
            }
        }

        private void onInterrupt() {
            // 50% cooldown reduction - handled by cooldown service
            context.fx().fizzle(player);
        }

        private void onFinish() {
            // Apply burst
            List<LivingEntity> targets = player.getWorld().getLivingEntities().stream()
                .filter(entity -> entity.getLocation().distance(player.getLocation()) <= radius)
                .filter(entity -> {
                    if (entity instanceof Player && !hitPlayers) return false;
                    if (!(entity instanceof Player) && !hitMobs) return false;
                    return !entity.equals(player);
                })
                .toList();

            for (LivingEntity target : targets) {
                target.damage(damage, player);
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, weaknessDuration, weaknessAmplifier));
            }

            // SFX
            context.fx().playSound(player, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);
            spawnBurstParticles(player, radius);
        }

        private void spawnCircleParticles(Player player, double radius) {
            for (int i = 0; i < 36; i++) {
                double angle = 2 * Math.PI * i / 36;
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(x, 0.1, z), 1, 0, 0, 0, 0);
            }
        }

        private void spawnBurstParticles(Player player, double radius) {
            for (int i = 0; i < 50; i++) {
                double x = (Math.random() - 0.5) * radius * 2;
                double z = (Math.random() - 0.5) * radius * 2;
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(x, 1, z), 1, 0, 0, 0, 0.1);
            }
        }
    }

    @Override
    public String getName() {
        return "ritual-of-unmaking";
    }

    @Override
    public String key() {
        return "ritual-of-unmaking";
    }

    @Override
    public Component displayName() {
        return Component.text("Ritueel van Ontering");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}