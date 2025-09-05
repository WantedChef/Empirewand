package com.example.empirewand.spell.implementation;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.Location;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

import java.util.HashSet;
import java.util.Set;

public class SolarLance implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double range = spells.getDouble("solar-lance.values.range", 20.0);
        double damage = spells.getDouble("solar-lance.values.damage", 6.0);
        int glowingDuration = spells.getInt("solar-lance.values.glowing-duration-ticks", 60);
        int maxPierce = spells.getInt("solar-lance.values.max-pierce", 3);
        double sampleStep = spells.getDouble("solar-lance.values.sample-step", 0.5);
        boolean hitPlayers = spells.getBoolean("solar-lance.flags.hit-players", true);
        boolean hitMobs = spells.getBoolean("solar-lance.flags.hit-mobs", true);

        Vector direction = player.getLocation().getDirection().normalize();
        Location start = player.getEyeLocation();
        Location end = start.clone().add(direction.clone().multiply(range));

        // Trace along the line
        Set<LivingEntity> hitEntities = new HashSet<>();
        Location current = start.clone();

        for (double dist = 0; dist <= range && hitEntities.size() < maxPierce; dist += sampleStep) {
            // Check for entities at current location
            for (LivingEntity entity : player.getWorld().getLivingEntities()) {
                if (entity.equals(player) || entity.isDead() || !entity.isValid()) continue;
                if (entity.getLocation().distance(current) <= 1.0) {
                    if (entity instanceof Player && !hitPlayers) continue;
                    if (!(entity instanceof Player) && !hitMobs) continue;
                    hitEntities.add(entity);
                }
            }

            current.add(direction.clone().multiply(sampleStep));
        }

        // Apply effects to hit entities
        for (LivingEntity target : hitEntities) {
            target.damage(damage, player);
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowingDuration, 0));
        }

        // Visuals and SFX
        context.fx().playSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.5f);
        spawnLanceTrail(start, end, context);
    }

    private void spawnLanceTrail(Location start, Location end, SpellContext context) {
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);

        for (double d = 0; d <= distance; d += 0.5) {
            Location particleLoc = start.clone().add(direction.clone().multiply(d));

            // Crit magic particles
            particleLoc.getWorld().spawnParticle(Particle.CRIT, particleLoc, 3, 0, 0, 0, 0);

            // Golden redstone dust
            particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 2,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 215, 0), 1.0f));
        }
    }

    @Override
    public String getName() {
        return "solar-lance";
    }

    @Override
    public String key() {
        return "solar-lance";
    }

    @Override
    public Component displayName() {
        return Component.text("Zonschicht");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}