package com.example.empirewand.spell.implementation;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.example.empirewand.core.Keys;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class LightningArrow implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double damage = spells.getDouble("lightning-arrow.values.damage", 8.0); // 4 hearts
        boolean blockDamage = spells.getBoolean("lightning-arrow.flags.block-damage", false);
        boolean glowing = spells.getBoolean("lightning-arrow.flags.glowing", true);
        int glowingDuration = spells.getInt("lightning-arrow.values.glowing-duration-ticks", 60);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        // Launch arrow
        Arrow arrow = player.getWorld().spawn(player.getEyeLocation(), Arrow.class);
        arrow.setVelocity(player.getEyeLocation().getDirection().multiply(2.0));
        arrow.setShooter(player);

        // Tag projectile
        arrow.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL,
                Keys.STRING_TYPE.getType(), "lightning-arrow");

        // Register listener for hit detection
        context.plugin().getServer().getPluginManager().registerEvents(
                new ArrowListener(context, damage, blockDamage, glowing, glowingDuration, friendlyFire),
                context.plugin());

        // Cast sound
        context.fx().playSound(player, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
    }

    private static class ArrowListener implements Listener {
        private final SpellContext context;
        private final double damage;
        private final boolean blockDamage;
        private final boolean glowing;
        private final int glowingDuration;
        private final boolean friendlyFire;

        public ArrowListener(SpellContext context, double damage, boolean blockDamage,
                boolean glowing, int glowingDuration, boolean friendlyFire) {
            this.context = context;
            this.damage = damage;
            this.blockDamage = blockDamage;
            this.glowing = glowing;
            this.glowingDuration = glowingDuration;
            this.friendlyFire = friendlyFire;
        }

        @EventHandler
        public void onProjectileHit(ProjectileHitEvent event) {
            Projectile projectile = event.getEntity();
            if (!(projectile instanceof Arrow))
                return;

            var pdc = projectile.getPersistentDataContainer();
            String spellType = pdc.get(Keys.PROJECTILE_SPELL, Keys.STRING_TYPE.getType());
            if (!"lightning-arrow".equals(spellType))
                return;

            Location hitLoc = projectile.getLocation();

            // Strike lightning
            if (blockDamage) {
                projectile.getWorld().strikeLightning(hitLoc);
            } else {
                projectile.getWorld().strikeLightningEffect(hitLoc);
            }

            // Damage nearby entities manually if no block damage
            if (!blockDamage) {
                for (var entity : hitLoc.getWorld().getNearbyEntities(hitLoc, 3.0, 3.0, 3.0)) {
                    if (!(entity instanceof LivingEntity living))
                        continue;
                    if (living.equals(projectile.getShooter()) && !friendlyFire)
                        continue;
                    if (living.isDead() || !living.isValid())
                        continue;

                    living.damage(damage, context.caster());

                    // Apply glowing effect
                    if (glowing) {
                        living.addPotionEffect(
                                new PotionEffect(PotionEffectType.GLOWING, glowingDuration, 0, false, true));
                    }
                }
            }

            // Effects
            context.fx().spawnParticles(hitLoc, Particle.ELECTRIC_SPARK, 20, 0.5, 0.5, 0.5, 0.1);

            // Remove projectile
            projectile.remove();
        }
    }

    @Override
    public String getName() {
        return "lightning-arrow";
    }

    @Override
    public String key() {
        return "lightning-arrow";
    }

    @Override
    public Component displayName() {
        return Component.text("Lightning Arrow");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}