package com.example.empirewand.spell.implementation;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.ProjectileHitEvent;

import com.example.empirewand.core.Keys;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class Fireball implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double explosionYield = spells.getDouble("fireball.values.yield", 3.0);
        double projectileSpeed = spells.getDouble("fireball.values.speed", 1.0);
        boolean incendiary = spells.getBoolean("fireball.flags.incendiary", true);
        boolean blockDamage = spells.getBoolean("fireball.flags.block-damage", true);

        // Launch fireball
        Location spawnLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(0.5));
        org.bukkit.entity.Fireball fireball = player.getWorld().spawn(spawnLoc, org.bukkit.entity.Fireball.class);
        fireball.setVelocity(player.getEyeLocation().getDirection().multiply(projectileSpeed));
        fireball.setYield((float) explosionYield);
        fireball.setIsIncendiary(incendiary);
        fireball.setShooter(player);

        // Tag projectile
        fireball.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL,
                Keys.STRING_TYPE.getType(), "fireball");

        // Register listener for custom explosion handling
        context.plugin().getServer().getPluginManager().registerEvents(
                new FireballListener(context, blockDamage), context.plugin());

        // Cast sound
        context.fx().playSound(player, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
    }

    private static class FireballListener implements Listener {
        private final SpellContext context;
        private final boolean blockDamage;

        public FireballListener(SpellContext context, boolean blockDamage) {
            this.context = context;
            this.blockDamage = blockDamage;
        }

        @EventHandler
        public void onProjectileHit(ProjectileHitEvent event) {
            Projectile projectile = event.getEntity();
            if (!(projectile instanceof org.bukkit.entity.Fireball))
                return;

            var pdc = projectile.getPersistentDataContainer();
            String spellType = pdc.get(Keys.PROJECTILE_SPELL, Keys.STRING_TYPE.getType());
            if (!"fireball".equals(spellType))
                return;

            Location hitLoc = projectile.getLocation();

            // Create custom explosion if block damage is disabled
            if (!blockDamage) {
                // Damage nearby entities manually
                for (var entity : hitLoc.getWorld().getNearbyEntities(hitLoc, 4.0, 4.0, 4.0)) {
                    if (!(entity instanceof LivingEntity living))
                        continue;
                    if (living.equals(projectile.getShooter()))
                        continue; // Self-immunity
                    if (living.isDead() || !living.isValid())
                        continue;

                    // Calculate damage based on distance
                    double distance = living.getLocation().distance(hitLoc);
                    double damage = 20.0 * (1.0 - distance / 4.0); // Max 10 hearts at center
                    if (damage > 0) {
                        living.damage(Math.max(damage, 1.0), context.caster());
                    }
                }

                // Explosion particles without block damage
                context.fx().spawnParticles(hitLoc, Particle.EXPLOSION, 30, 0.5, 0.5, 0.5, 0.1);
                context.fx().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            }

            // Remove projectile
            projectile.remove();

            // Unregister listener after handling to avoid leaks
            HandlerList.unregisterAll(this);
        }
    }

    @Override
    public String getName() {
        return "fireball";
    }

    @Override
    public String key() {
        return "fireball";
    }

    @Override
    public Component displayName() {
        return Component.text("Fireball");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
