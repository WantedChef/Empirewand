package com.example.empirewand.spell.implementation.lightning;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class LightningBolt implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double range = spells.getDouble("lightning-bolt.values.range", 20.0);
        double damage = spells.getDouble("lightning-bolt.values.damage", 24.0); // 12 hearts
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        // Get target
        var targetEntity = player.getTargetEntity((int) range);
        if (!(targetEntity instanceof LivingEntity target) || target.isDead() || !target.isValid()) {
            context.fx().fizzle(player);
            return;
        }

        // Check if target is exposed to sky
        Location targetLoc = target.getLocation();
        if (!isExposedToSky(targetLoc)) {
            context.fx().fizzle(player);
            return;
        }

        // Strike lightning
        target.getWorld().strikeLightning(targetLoc);

        // Apply custom damage at the strike location immediately
        damageAtStrike(context, targetLoc, damage, friendlyFire);

        // Cast sound
        context.fx().playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
    }

    private boolean isExposedToSky(Location location) {
        for (int y = location.getBlockY() + 1; y <= location.getWorld().getMaxHeight(); y++) {
            Location checkLoc = new Location(location.getWorld(), location.getX(), y, location.getZ());
            if (checkLoc.getBlock().getType().isSolid()) {
                return false;
            }
        }
        return true;
    }

    private static void damageAtStrike(SpellContext context, Location strikeLoc, double damage, boolean friendlyFire) {
        for (var entity : strikeLoc.getWorld().getNearbyEntities(strikeLoc, 2.0, 2.0, 2.0)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living.equals(context.caster()) && !friendlyFire) continue;
            if (living.isDead() || !living.isValid()) continue;

            living.damage(damage, context.caster());
            context.fx().spawnParticles(living.getLocation(), Particle.ELECTRIC_SPARK, 15, 0.3, 0.3, 0.3, 0.1);
            context.fx().playSound(living.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f);
        }
    }

    @Override
    public String getName() {
        return "lightning-bolt";
    }

    @Override
    public String key() {
        return "lightning-bolt";
    }

    @Override
    public Component displayName() {
        return Component.text("Lightning Bolt");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
