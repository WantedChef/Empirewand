package com.example.empirewand.spell.implementation;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class ThunderBlast implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        Location center = player.getLocation();

        // Config values
        var spells = context.config().getSpellsConfig();
        double radius = spells.getDouble("thunder-blast.values.radius", 6.0);
        double damage = spells.getDouble("thunder-blast.values.damage", 16.0); // 8 hearts
        int strikes = spells.getInt("thunder-blast.values.strikes", 3);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        // Strike lightning at multiple locations
        for (int i = 0; i < strikes; i++) {
            // Random location within radius
            double angle = Math.random() * 2 * Math.PI;
            double distance = Math.random() * radius;
            double x = center.getX() + distance * Math.cos(angle);
            double z = center.getZ() + distance * Math.sin(angle);
            Location strikeLoc = new Location(center.getWorld(), x, center.getY(), z);

            // Strike lightning
            center.getWorld().strikeLightning(strikeLoc);

            // Apply custom damage at this strike location immediately
            damageAtStrike(context, strikeLoc, damage, friendlyFire);
        }

        // Cast sound
        context.fx().playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
    }


    private static void damageAtStrike(SpellContext context, Location strikeLoc, double damage, boolean friendlyFire) {
        for (var entity : strikeLoc.getWorld().getNearbyEntities(strikeLoc, 3.0, 3.0, 3.0)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living.equals(context.caster()) && !friendlyFire) continue;
            if (living.isDead() || !living.isValid()) continue;

            living.damage(damage, context.caster());
            context.fx().spawnParticles(living.getLocation(), Particle.ELECTRIC_SPARK, 10, 0.2, 0.2, 0.2, 0.1);
        }
    }

    @Override
    public String getName() {
        return "thunder-blast";
    }

    @Override
    public String key() {
        return "thunder-blast";
    }

    @Override
    public Component displayName() {
        return Component.text("Thunder Blast");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
