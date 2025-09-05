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

public class ArcaneOrb implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        var spells = context.config().getSpellsConfig();
        double speed = spells.getDouble("arcane-orb.values.speed", 0.6);
        double radius = spells.getDouble("arcane-orb.values.radius", 3.5);
        double damage = spells.getDouble("arcane-orb.values.damage", 8.0);
        double knockback = spells.getDouble("arcane-orb.values.knockback", 0.6);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        Location spawnLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(0.5));
        Snowball orb = player.getWorld().spawn(spawnLoc, Snowball.class);
        orb.setVelocity(player.getEyeLocation().getDirection().multiply(speed));
        orb.setShooter(player);

        // Tag projectile
        orb.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL,
            org.bukkit.persistence.PersistentDataType.STRING, "arcane-orb");

        // Register hit listener for this cast
        context.plugin().getServer().getPluginManager().registerEvents(
            new HitListener(context, radius, damage, knockback, friendlyFire), context.plugin());

        // Launch FX
        context.fx().playSound(player, Sound.ENTITY_EVOKER_CAST_SPELL, 0.8f, 1.2f);
        context.fx().spawnParticles(player.getEyeLocation(), Particle.ENCHANT, 20, 0.3, 0.3, 0.3, 0.0);
    }

    private static class HitListener implements Listener {
        private final SpellContext context;
        private final double radius;
        private final double damage;
        private final double knockback;
        private final boolean friendlyFire;

        HitListener(SpellContext context, double radius, double damage, double knockback, boolean friendlyFire) {
            this.context = context;
            this.radius = radius;
            this.damage = damage;
            this.knockback = knockback;
            this.friendlyFire = friendlyFire;
        }

        @EventHandler
        public void onProjectileHit(ProjectileHitEvent event) {
            Projectile projectile = event.getEntity();
            if (!(projectile instanceof Snowball)) return;
            String key = projectile.getPersistentDataContainer().get(Keys.PROJECTILE_SPELL, org.bukkit.persistence.PersistentDataType.STRING);
            if (!"arcane-orb".equals(key)) return;

            Location hit = projectile.getLocation();

            // Impact FX
            context.fx().impact(hit, Particle.EXPLOSION, 30, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.0f);

            // AoE damage/knockback
            for (var e : hit.getWorld().getNearbyEntities(hit, radius, radius, radius)) {
                if (!(e instanceof LivingEntity living)) continue;
                if (projectile.getShooter() != null && projectile.getShooter().equals(living) && !friendlyFire) continue;
                if (living.isDead() || !living.isValid()) continue;

                living.damage(damage, context.caster());
                Vector push = living.getLocation().toVector().subtract(hit.toVector()).normalize().multiply(knockback).setY(0.2);
                living.setVelocity(living.getVelocity().add(push));
            }

            projectile.remove();
        }
    }

    @Override
    public String getName() { return "arcane-orb"; }
    @Override
    public String key() { return "arcane-orb"; }
    @Override
    public Component displayName() { return Component.text("Arcane Orb"); }
    @Override
    public Prereq prereq() { return new Prereq(true, Component.text("")); }
}

