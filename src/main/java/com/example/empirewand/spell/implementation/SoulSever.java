package com.example.empirewand.spell.implementation;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

import java.util.HashSet;
import java.util.Set;

public class SoulSever implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double dashDistance = spells.getDouble("soul-sever.values.dash-distance", 8.0);
        double damage = spells.getDouble("soul-sever.values.damage", 2.0);
        int nauseaDuration = spells.getInt("soul-sever.values.nausea-duration-ticks", 40);
        int nauseaAmplifier = spells.getInt("soul-sever.values.nausea-amplifier", 0);
        double sampleStep = spells.getDouble("soul-sever.values.sample-step", 0.5);
        boolean hitPlayers = spells.getBoolean("soul-sever.flags.hit-players", true);
        boolean hitMobs = spells.getBoolean("soul-sever.flags.hit-mobs", true);

        Vector direction = player.getLocation().getDirection().normalize();
        Location start = player.getLocation().clone();
        Location end = start.clone().add(direction.clone().multiply(dashDistance));

        // Check for safe destination
        if (!isSafeLocation(end)) {
            context.fx().fizzle(player);
            return;
        }

        // Perform dash
        player.teleport(end);

        // Find entities along the path
        Set<LivingEntity> hitEntities = new HashSet<>();
        Vector step = direction.clone().multiply(sampleStep);
        Location current = start.clone();

        for (double dist = 0; dist <= dashDistance; dist += sampleStep) {
            // Check for entities at current location
            for (LivingEntity entity : player.getWorld().getLivingEntities()) {
                if (entity.equals(player) || entity.isDead() || !entity.isValid()) continue;
                if (entity.getLocation().distance(current) <= 1.0) {
                    if (entity instanceof Player && !hitPlayers) continue;
                    if (!(entity instanceof Player) && !hitMobs) continue;
                    hitEntities.add(entity);
                }
            }

            current.add(step);
        }

        // Apply effects to hit entities
        for (LivingEntity target : hitEntities) {
            target.damage(damage, player);
            target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, nauseaDuration, nauseaAmplifier));
        }

        // Visuals and SFX
        context.fx().playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        spawnDashTrail(start, end, context, player);
    }

    private boolean isSafeLocation(Location location) {
        Block feet = location.getBlock();
        Block head = location.clone().add(0, 1, 0).getBlock();
        Block ground = location.clone().add(0, -1, 0).getBlock();

        return feet.getType() == Material.AIR && head.getType() == Material.AIR && ground.getType().isSolid();
    }

    private void spawnDashTrail(Location start, Location end, SpellContext context, Player player) {
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);

        for (double d = 0; d <= distance; d += 0.5) {
            Location particleLoc = start.clone().add(direction.clone().multiply(d));
            player.getWorld().spawnParticle(Particle.SMOKE, particleLoc, 3, 0, 0, 0, 0);
        }
    }

    @Override
    public String getName() {
        return "soul-sever";
    }

    @Override
    public String key() {
        return "soul-sever";
    }

    @Override
    public Component displayName() {
        return Component.text("Zielsplinters");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}