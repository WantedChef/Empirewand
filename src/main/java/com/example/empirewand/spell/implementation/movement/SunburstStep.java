package com.example.empirewand.spell.implementation.movement;

import com.example.empirewand.api.EmpireWandAPI;

import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import java.util.Collection;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class SunburstStep extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Sunburst Step";
            this.description = "Dash forward in a burst of light, healing allies and damaging enemies.";
            this.manaCost = 15; // Example
            this.cooldown = java.time.Duration.ofSeconds(18);
            this.spellType = SpellType.MOVEMENT;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new SunburstStep(this);
        }
    }

    private SunburstStep(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "sunburst-step";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double maxDistance = spellConfig.getDouble("values.max-distance", 12.0);
        Location startLocation = player.getLocation();
        Vector direction = startLocation.getDirection().normalize();
        Location destination = findSafeDestination(startLocation, direction, maxDistance);

        if (destination == null) {
            context.fx().fizzle(player);
            return null;
        }

        player.teleport(destination);
        applyPulseEffects(player, destination);
        createVisualEffects(context, startLocation, destination);

        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    private Location findSafeDestination(Location start, Vector direction, double maxDistance) {
        for (double d = maxDistance; d >= 1.0; d -= 0.5) {
            Location testLoc = start.clone().add(direction.clone().multiply(d));
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
        return feet.getType().isAir() && head.getType().isAir() && ground.getType().isSolid();
    }

    private void applyPulseEffects(Player player, Location center) {
        double pulseRadius = spellConfig.getDouble("values.pulse-radius", 4.0);
        double allyHeal = spellConfig.getDouble("values.ally-heal", 2.0);
        double enemyDamage = spellConfig.getDouble("values.enemy-damage", 3.0);
        boolean hitPlayers = spellConfig.getBoolean("flags.hit-players", true);
        boolean hitMobs = spellConfig.getBoolean("flags.hit-mobs", true);

        Collection<LivingEntity> affectedEntities = player.getWorld().getNearbyLivingEntities(center, pulseRadius);
        for (LivingEntity entity : affectedEntities) {
            if (entity.equals(player))
                continue;

            if (entity instanceof Player) { // Simple ally check
                if (!hitPlayers)
                    continue;
                var maxAttr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                double max = maxAttr != null ? maxAttr.getValue() : entity.getHealth();
                entity.setHealth(Math.min(max, entity.getHealth() + allyHeal));
            } else if (hitMobs) {
                entity.damage(enemyDamage, player);
            }
        }
    }

    private void createVisualEffects(SpellContext context, Location start, Location end) {
        // Sounds
        context.fx().playSound(start, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        context.fx().playSound(end, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);

        // Trail
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();
        if (distance > 0) {
            direction.normalize();
            for (double d = 0; d < distance; d += 0.3) {
                Location particleLoc = start.clone().add(direction.clone().multiply(d));
                context.fx().spawnParticles(particleLoc, Particle.DUST, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.0f));
            }
        }

        // Impact Burst
        new BukkitRunnable() {
            private int t = 0;

            @Override
            public void run() {
                if (t++ > 10) {
                    this.cancel();
                    return;
                }
                double radius = t * 0.4;
                for (int i = 0; i < 20; i++) {
                    double angle = (2 * Math.PI * i) / 20;
                    Location particleLoc = end.clone().add(Math.cos(angle) * radius, 0.5, Math.sin(angle) * radius);
                    context.fx().spawnParticles(particleLoc, Particle.DUST, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.ORANGE, 1.0f));
                }
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }
}