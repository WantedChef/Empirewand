package com.example.empirewand.spell.implementation.heal;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Particle;
import org.bukkit.Location;

import com.example.empirewand.spell.Prereq;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;

import net.kyori.adventure.text.Component;

public class GodCloud implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player caster = context.caster();
        if (caster == null || !caster.isValid()) {
            return;
        }

        // Config values
        var spells = context.config().getSpellsConfig();
        int durationTicks = spells.getInt("god-cloud.values.duration-ticks", 600); // 30 seconds
        int particleInterval = spells.getInt("god-cloud.values.particle-interval", 2);
        int particleCount = spells.getInt("god-cloud.values.particle-count", 6);
        double haloRadius = spells.getDouble("god-cloud.halo.radius", 2.8);
        int haloPoints = spells.getInt("god-cloud.halo.points", 24);
        int wispCount = spells.getInt("god-cloud.wisp.count", 4);

        // Cancel existing GodCloud if active
        cancelExistingGodCloud(caster);

        // Start cloud effect task
        startCloudEffect(caster, durationTicks, particleInterval, particleCount, haloRadius, haloPoints, wispCount,
                context);
    }

    private void startCloudEffect(Player player, int duration, int interval, int count, double haloRadius,
            int haloPoints, int wispCount, SpellContext context) {
        // Set metadata to indicate GodCloud is active
        if (!player.isValid()) {
            return;
        }
        player.setMetadata("god_cloud_active", new org.bukkit.metadata.FixedMetadataValue(context.plugin(), true));

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration || !player.isValid() || player.isDead()) {
                    // Remove metadata when done
                    if (player.isValid()) {
                        player.removeMetadata("god_cloud_active", context.plugin());
                    }
                    this.cancel();
                    return;
                }

                // Only spawn particles if player is flying or gliding
                if (player.isFlying() || player.isGliding()) {
                    Location base = player.getLocation();
                    // Base cloud cushion
                    context.fx().spawnParticles(
                            base.clone().add(0, -0.9, 0),
                            Particle.CLOUD,
                            count,
                            0.3, 0.05, 0.3,
                            0);

                    // Halo ring (slow rotating) every few ticks
                    double angleOffset = (ticks / 8.0);
                    for (int i = 0; i < haloPoints; i++) {
                        double a = (2 * Math.PI * i / haloPoints) + angleOffset;
                        double x = Math.cos(a) * haloRadius;
                        double z = Math.sin(a) * haloRadius;
                        base.getWorld().spawnParticle(Particle.CRIT, base.getX() + x, base.getY() + 0.2,
                                base.getZ() + z, 1, 0, 0, 0, 0);
                    }

                    // Drifting wisps ascending
                    for (int i = 0; i < wispCount; i++) {
                        double rx = (Math.random() - 0.5) * haloRadius * 1.2;
                        double rz = (Math.random() - 0.5) * haloRadius * 1.2;
                        base.getWorld().spawnParticle(Particle.CLOUD, base.getX() + rx, base.getY() + 0.4,
                                base.getZ() + rz, 1, 0.05, 0.2, 0.05, 0.0005);
                    }

                    if (ticks % 40 == 0) { // Every 2 seconds
                        var plLoc = player.getLocation();
                        if (plLoc != null) {
                            context.fx().playSound(plLoc, org.bukkit.Sound.AMBIENT_CAVE, 0.3f, 1.0f);
                        }
                    }
                }

                ticks += interval;
            }
        }.runTaskTimer(context.plugin(), 0L, interval);
    }

    private void cancelExistingGodCloud(Player player) {
        if (player == null) {
            return;
        }
        if (player.hasMetadata("god_cloud_active")) {
            var plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("EmpireWand");
            if (plugin != null) {
                player.removeMetadata("god_cloud_active", plugin);
            }
        }
    }

    @Override
    public String getName() {
        return "god-cloud";
    }

    @Override
    public String key() {
        return "god-cloud";
    }

    @Override
    public Component displayName() {
        return Component.text("God Cloud");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
