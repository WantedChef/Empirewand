package com.example.empirewand.spell.implementation.lightning;

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
                if (entity.equals(player) || entity.isDead() || !entity.isValid())
                    continue;
                if (entity.getLocation().distance(current) <= 1.0) {
                    if (entity instanceof Player && !hitPlayers)
                        continue;
                    if (!(entity instanceof Player) && !hitMobs)
                        continue;
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

        // Visual beam & halo (purely cosmetic)
        int beamLength = spells.getInt("solar-lance.beam-length", (int) range);
        int haloInterval = spells.getInt("solar-lance.halo-interval-ticks", 6);
        boolean reversible = spells.getBoolean("solar-lance.enable-reversible-blocks", true);
        int scorchLen = spells.getInt("solar-lance.scorch-length", 6);
        spawnLanceTrail(start, end, context, beamLength, haloInterval, reversible, scorchLen);
    }

    private void spawnLanceTrail(Location start, Location end, SpellContext context, int beamLength, int haloInterval,
            boolean reversible, int scorchLen) {
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);

        int stepIndex = 0;
        for (double d = 0; d <= distance && d <= beamLength; d += 0.5) {
            Location particleLoc = start.clone().add(direction.clone().multiply(d));

            // Core beam particles
            particleLoc.getWorld().spawnParticle(Particle.CRIT, particleLoc, 2, 0, 0, 0, 0);

            // Warm glow (naive fallback using flame/ ash mix if DUST unsupported)
            try {
                particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 1,
                        new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 215, 0), 1.0f));
            } catch (Throwable t) {
                context.plugin().getLogger()
                        .warning("Error spawning dust particle, falling back to flame: " + t.getMessage());
                particleLoc.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0.02, 0.02, 0.02, 0.001);
            }

            // Halo at interval
            if (haloInterval > 0 && stepIndex % haloInterval == 0) {
                for (int i = 0; i < 10; i++) {
                    double ang = (2 * Math.PI * i) / 10;
                    double r = 0.8;
                    double x = Math.cos(ang) * r;
                    double z = Math.sin(ang) * r;
                    particleLoc.getWorld().spawnParticle(Particle.END_ROD, particleLoc.getX() + x, particleLoc.getY(),
                            particleLoc.getZ() + z, 1, 0, 0, 0, 0);
                }
            }

            // Scorch reversible blocks (very lightweight, one every full block)
            if (reversible && scorchLen > 0 && stepIndex < scorchLen && stepIndex % 2 == 0) {
                var block = particleLoc.getBlock();
                if (block.isEmpty()) {
                    // place light source temporarily (client-side illusion via particles): simulate
                    // via additional particles
                    particleLoc.getWorld().spawnParticle(Particle.SMALL_FLAME, block.getX() + 0.5, block.getY() + 0.1,
                            block.getZ() + 0.5, 4, 0.2, 0.05, 0.2, 0.001);
                }
            }
            stepIndex++;
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
