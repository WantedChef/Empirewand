package com.example.empirewand.spell.implementation.life;

import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Location;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class Hemorrhage implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double baseDamage = spells.getDouble("hemorrhage.values.base-damage", 2.0);
        double movementBonus = spells.getDouble("hemorrhage.values.movement-bonus", 0.5);
        double movementThreshold = spells.getDouble("hemorrhage.values.movement-threshold", 0.8);
        int duration = spells.getInt("hemorrhage.values.duration-ticks", 120);
        int checkInterval = spells.getInt("hemorrhage.values.check-interval-ticks", 10);
        boolean hitPlayers = spells.getBoolean("hemorrhage.flags.hit-players", true);
        boolean hitMobs = spells.getBoolean("hemorrhage.flags.hit-mobs", true);

        Entity targetEntity = player.getTargetEntity(20);
        if (targetEntity == null || !(targetEntity instanceof LivingEntity target) || target.isDead()
                || !target.isValid()) {
            context.fx().fizzle(player);
            return;
        }

        if (target instanceof Player && !hitPlayers) {
            context.fx().fizzle(player);
            return;
        }
        if (!(target instanceof Player) && !hitMobs) {
            context.fx().fizzle(player);
            return;
        }

        // Apply initial damage
        target.damage(baseDamage, player);

        new HemorrhageTask(target, player, movementBonus, movementThreshold, checkInterval, duration)
                .runTaskTimer(context.plugin(), 0L, checkInterval);
    }

    private static class HemorrhageTask extends BukkitRunnable {
        private final LivingEntity target;
        private final Player caster;
        private final double movementBonus;
        private final double movementThreshold;
        private final int checkInterval;
        private final int totalDuration;
        private Location lastLocation;
        private int ticksElapsed = 0;

        public HemorrhageTask(LivingEntity target, Player caster, double movementBonus, double movementThreshold,
                int checkInterval, int totalDuration) {
            this.target = target;
            this.caster = caster;
            this.movementBonus = movementBonus;
            this.movementThreshold = movementThreshold;
            this.checkInterval = checkInterval;
            this.totalDuration = totalDuration;
            this.lastLocation = target.getLocation().clone();
        }

        @Override
        public void run() {
            if (!target.isValid() || target.isDead() || !caster.isValid()) {
                this.cancel();
                return;
            }

            ticksElapsed += checkInterval;

            if (ticksElapsed >= totalDuration) {
                this.cancel();
                return;
            }

            // Check movement
            Location currentLocation = target.getLocation();
            double distance = lastLocation.distance(currentLocation);

            if (distance > movementThreshold) {
                // Apply movement bonus damage
                target.damage(movementBonus, caster);
                spawnBloodParticles(target);
            }

            lastLocation = currentLocation.clone();
        }

        private void spawnBloodParticles(LivingEntity target) {
            for (int i = 0; i < 5; i++) {
                double x = (Math.random() - 0.5) * 1;
                double y = Math.random() * 1;
                double z = (Math.random() - 0.5) * 1;
                target.getWorld().spawnParticle(Particle.FALLING_LAVA, target.getLocation().add(x, y, z), 1, 0, 0, 0,
                        0);
            }
        }
    }

    @Override
    public String getName() {
        return "hemorrhage";
    }

    @Override
    public String key() {
        return "hemorrhage";
    }

    @Override
    public Component displayName() {
        return Component.text("Hemorrhage");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
