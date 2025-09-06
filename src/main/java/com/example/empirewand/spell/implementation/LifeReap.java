package com.example.empirewand.spell.implementation;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.ArrayList;

public class LifeReap implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        if (player == null)
            return;

        // Config values
        var spells = context.config().getSpellsConfig();
        double damage = spells.getDouble("life-reap.values.damage", 4.0);
        double healPerTarget = spells.getDouble("life-reap.values.heal-per-target", 0.8);
        double range = spells.getDouble("life-reap.values.range", 5.0);
        double angleDegrees = spells.getDouble("life-reap.values.angle-degrees", 120.0);
        boolean hitPlayers = spells.getBoolean("life-reap.flags.hit-players", true);
        boolean hitMobs = spells.getBoolean("life-reap.flags.hit-mobs", true);

        // Find targets in cone
        List<LivingEntity> targets = getEntitiesInCone(player, range, angleDegrees);
        targets.removeIf(
                entity -> (entity instanceof Player && !hitPlayers) || (!(entity instanceof Player) && !hitMobs));

        if (targets.isEmpty()) {
            context.fx().fizzle(player);
            return;
        }

        // Apply damage and calculate heal
        double totalHeal = 0.0;
        for (LivingEntity target : targets) {
            target.damage(damage, player);
            totalHeal += healPerTarget;
        }

        // Heal player
        var maxAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = maxAttr != null ? maxAttr.getValue() : 20.0;
        double newHealth = Math.min(maxHealth, player.getHealth() + totalHeal);
        player.setHealth(newHealth);

        // Visuals and SFX
        context.fx().playSound(player, Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.8f);
        spawnSweepParticles(player);
    }

    private List<LivingEntity> getEntitiesInCone(Player player, double range, double angleDegrees) {
        List<LivingEntity> targets = new ArrayList<>();
        org.bukkit.World world = player.getWorld();
        var originLoc = player.getLocation();
        Vector playerDir = originLoc.getDirection();
        if (playerDir == null) {
            return targets; // No direction available
        }
        Vector playerDirection = playerDir.normalize();
        double angleRadians = Math.toRadians(angleDegrees / 2.0);

        for (LivingEntity entity : world.getLivingEntities()) {
            if (entity.equals(player) || entity.isDead() || !entity.isValid())
                continue;

            var entLoc = entity.getLocation();
            var playerLoc = player.getLocation();
            Vector toEntity = entLoc.toVector().subtract(playerLoc.toVector());
            double distance = toEntity.length();
            if (distance > range)
                continue;

            toEntity = toEntity.normalize();
            double dotProduct = playerDirection.dot(toEntity);
            double entityAngle = Math.acos(dotProduct);

            if (entityAngle <= angleRadians) {
                targets.add(entity);
            }
        }

        return targets;
    }

    private void spawnSweepParticles(Player player) {
        var loc = player.getLocation();
        org.bukkit.World world = player.getWorld();
        if (world == null) {
            return; // Invalid world
        }

        // Sweep attack particles
        world.spawnParticle(org.bukkit.Particle.SWEEP_ATTACK, loc.add(0, 1, 0), 1, 0, 0, 0, 0);

        // Dark redstone dust trail
        Vector direction = loc.getDirection().multiply(0.5);
        for (int i = 0; i < 10; i++) {
            world.spawnParticle(Particle.DUST, loc.add(direction.clone().multiply(i)), 5,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(64, 0, 0), 1.0f));
        }
    }

    @Override
    public String getName() {
        return "life-reap";
    }

    @Override
    public String key() {
        return "life-reap";
    }

    @Override
    public Component displayName() {
        return Component.text("Levenszuiger");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}