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

public class BloodNova implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double baseDamage = spells.getDouble("blood-nova.values.base-damage", 4.0);
        double damagePerCharge = spells.getDouble("blood-nova.values.damage-per-charge", 2.0);
        double radius = spells.getDouble("blood-nova.values.radius", 4.0);
        double knockbackStrength = spells.getDouble("blood-nova.values.knockback-strength", 1.0);
        boolean hitPlayers = spells.getBoolean("blood-nova.flags.hit-players", true);
        boolean hitMobs = spells.getBoolean("blood-nova.flags.hit-mobs", true);

        // Get current charges
        int charges = BloodTap.getCurrentBloodCharges(player);
        if (charges == 0) {
            context.fx().fizzle(player);
            return;
        }

        // Calculate total damage
        double totalDamage = baseDamage + (charges * damagePerCharge);

        // Find targets
        for (LivingEntity entity : player.getWorld().getLivingEntities()) {
            if (entity.getLocation().distance(player.getLocation()) <= radius && !entity.equals(player)) {
                if (entity instanceof Player && !hitPlayers) continue;
                if (!(entity instanceof Player) && !hitMobs) continue;

                // Apply damage
                entity.damage(totalDamage, player);

                // Apply knockback
                Vector knockback = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                knockback.multiply(knockbackStrength);
                knockback.setY(0.3); // Small upward component
                entity.setVelocity(entity.getVelocity().add(knockback));
            }
        }

        // Consume all charges
        BloodTap.consumeBloodCharges(player, charges, context);

        // Visuals and SFX
        context.fx().playSound(player, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
        spawnNovaParticles(player, radius);
    }

    private void spawnNovaParticles(Player player, double radius) {
        for (int i = 0; i < 100; i++) {
            double angle = 2 * Math.PI * i / 100;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(x, 1, z), 1,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 0, 0), 1.0f));
            player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(x, 1, z), 1, 0, 0, 0, 0);
        }

        // Center explosion particles
        for (int i = 0; i < 50; i++) {
            double x = (Math.random() - 0.5) * radius * 2;
            double z = (Math.random() - 0.5) * radius * 2;
            player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation().add(x, 1, z), 1, 0, 0, 0, 0);
        }
    }

    @Override
    public String getName() {
        return "blood-nova";
    }

    @Override
    public String key() {
        return "blood-nova";
    }

    @Override
    public Component displayName() {
        return Component.text("Blood Nova");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}