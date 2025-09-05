package com.example.empirewand.spell.implementation;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import com.example.empirewand.core.Keys;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class LittleSpark implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double damage = spells.getDouble("little-spark.values.damage", 2.0);
        double knockbackStrength = spells.getDouble("little-spark.values.knockback-strength", 0.3);
        double speed = spells.getDouble("little-spark.values.speed", 0.8);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        // Launch snowball projectile
        Location spawnLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(0.5));
        Snowball proj = player.getWorld().spawn(spawnLoc, Snowball.class);
        proj.setVelocity(player.getEyeLocation().getDirection().multiply(speed));
        proj.setShooter(player);

        // Tag projectile
        proj.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL,
            Keys.STRING_TYPE.getType(), "little-spark");

        // Register listener for hit detection
        context.plugin().getServer().getPluginManager().registerEvents(
            new SparkListener(context, damage, knockbackStrength, friendlyFire), context.plugin());

        // Cast sound
        context.fx().playSound(player, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.5f);
    }

    private static class SparkListener implements Listener {
        private final SpellContext context;
        private final double damage;
        private final double knockbackStrength;
        private final boolean friendlyFire;

        public SparkListener(SpellContext context, double damage, double knockbackStrength, boolean friendlyFire) {
            this.context = context;
            this.damage = damage;
            this.knockbackStrength = knockbackStrength;
            this.friendlyFire = friendlyFire;
        }

        @EventHandler
        public void onProjectileHit(ProjectileHitEvent event) {
            Projectile projectile = event.getEntity();
            if (!(projectile instanceof Snowball)) return;

            var pdc = projectile.getPersistentDataContainer();
            String spellType = pdc.get(Keys.PROJECTILE_SPELL, Keys.STRING_TYPE.getType());
            if (!"little-spark".equals(spellType)) return;

            // Check if hit entity
            if (event.getHitEntity() instanceof LivingEntity living) {
                if (living.equals(projectile.getShooter()) && !friendlyFire) return;
                if (living.isDead() || !living.isValid()) return;

                // Apply damage
                living.damage(damage, context.caster());

                // Apply knockback
                Vector direction = living.getLocation().toVector()
                    .subtract(projectile.getLocation().toVector()).normalize();
                living.setVelocity(direction.multiply(knockbackStrength));

                // Effects
                context.fx().spawnParticles(living.getLocation(), Particle.ELECTRIC_SPARK, 10, 0.2, 0.2, 0.2, 0.1);
                context.fx().playSound(living.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.5f, 1.0f);
            }

            // Remove projectile
            projectile.remove();
        }
    }

    @Override
    public String getName() {
        return "little-spark";
    }

    @Override
    public String key() {
        return "little-spark";
    }

    @Override
    public Component displayName() {
        return Component.text("Little Spark");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}