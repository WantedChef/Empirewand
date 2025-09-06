package com.example.empirewand.spell.implementation;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class SunburstStep implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        if (player == null || !player.isValid()) {
            return;
        }

        // Config values
        var spells = context.config().getSpellsConfig();
        double maxDistance = spells.getDouble("sunburst-step.values.max-distance", 10.0);
        double pulseRadius = spells.getDouble("sunburst-step.values.pulse-radius", 3.5);
        double allyHeal = spells.getDouble("sunburst-step.values.ally-heal", 1.0);
        double enemyDamage = spells.getDouble("sunburst-step.values.enemy-damage", 1.0);
        boolean hitPlayers = spells.getBoolean("sunburst-step.flags.hit-players", true);
        boolean hitMobs = spells.getBoolean("sunburst-step.flags.hit-mobs", true);

        var loc = player.getLocation();
        Vector direction = loc.getDirection().normalize();
        Location start = loc.clone();
        Location destination = findSafeDestination(start, direction, maxDistance);

        if (destination == null) {
            context.fx().fizzle(player);
            return;
        }

        // Perform teleport
        if (!player.teleport(destination)) {
            context.fx().fizzle(player);
            return;
        }

        // Apply pulse effects
        applyPulseEffects(player, destination, pulseRadius, allyHeal, enemyDamage, hitPlayers, hitMobs);

        // Visuals and SFX
        context.fx().playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        spawnDashTrail(start, destination);
    }

    private Location findSafeDestination(Location start, Vector direction, double maxDistance) {
        int steps = (int) Math.ceil((maxDistance - 1.0) / 0.5);
        for (int i = 0; i <= steps; i++) {
            double dist = maxDistance - (i * 0.5);
            if (dist < 1.0)
                break;
            Location testLoc = start.clone().add(direction.clone().multiply(dist));
            if (isSafeLocation(testLoc)) {
                return testLoc;
            }
        }
        return null;
    }

    private boolean isSafeLocation(Location location) {
        Block feet = location.getBlock();
        Block head = location.clone().add(0, 1, 0).getBlock();
        Block ground = location.clone().add(0, -1, 0).getBlock();

        return feet.getType() == Material.AIR && head.getType() == Material.AIR && ground.getType().isSolid();
    }

    private void applyPulseEffects(Player player, Location center, double radius, double allyHeal, double enemyDamage,
            boolean hitPlayers, boolean hitMobs) {
        for (LivingEntity entity : player.getWorld().getLivingEntities()) {
            if (entity.getLocation().distance(center) <= radius && !entity.equals(player)) {
                boolean isAlly = entity instanceof Player
                        && ((Player) entity).getUniqueId().equals(player.getUniqueId());

                if (isAlly) {
                    // Heal ally
                    var attr = entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                    double maxHealth = attr != null ? attr.getValue() : entity.getHealth();
                    double newHealth = Math.min(maxHealth, entity.getHealth() + allyHeal);
                    entity.setHealth(newHealth);
                } else {
                    // Damage enemy
                    if ((entity instanceof Player && hitPlayers) || (!(entity instanceof Player) && hitMobs)) {
                        entity.damage(enemyDamage, player);
                    }
                }
            }
        }
    }

    private void spawnDashTrail(Location start, Location end) {
        // start/end are guaranteed by callers
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();
        if (distance == 0) {
            return;
        }
        direction.normalize();
        int steps = (int) Math.ceil(distance / 0.5);
        double stepSize = distance / steps;

        for (int i = 0; i <= steps; i++) {
            double d = i * stepSize;
            Location particleLoc = start.clone().add(direction.clone().multiply(d));

            // End rod particles
            particleLoc.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);

            // Golden redstone dust
            particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 2,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 215, 0), 1.0f));
        }
    }

    @Override
    public String getName() {
        return "sunburst-step";
    }

    @Override
    public String key() {
        return "sunburst-step";
    }

    @Override
    public Component displayName() {
        return Component.text("Sunburst Step");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}