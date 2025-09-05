package com.example.empirewand.spell.implementation;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class EmpireAura implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double radius = spells.getDouble("empire-aura.values.radius", 6.0);
        int duration = spells.getInt("empire-aura.values.duration-ticks", 400); // 20 seconds
        int tickInterval = spells.getInt("empire-aura.values.tick-interval", 10); // 0.5 seconds

        // Start aura task
        startAuraTask(player, radius, duration, tickInterval, context);
    }

    private void startAuraTask(Player caster, double radius, int duration, int tickInterval, SpellContext context) {
        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration || !caster.isValid() || caster.isDead()) {
                    this.cancel();
                    return;
                }

                Location center = caster.getLocation();

                // Apply effects to nearby entities
                for (var entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                    if (!(entity instanceof LivingEntity living)) continue;
                    if (living.isDead() || !living.isValid()) continue;

                    if (living instanceof Player targetPlayer) {
                        // Buff allies (same world check for simplicity)
                        if (targetPlayer.getWorld().equals(caster.getWorld())) {
                            targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, false, true));
                            targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, true));

                            // Ally particles
                            context.fx().spawnParticles(targetPlayer.getLocation(), Particle.SMOKE, 3, 0.2, 0.2, 0.2, 0.1);
                        }
                    } else {
                        // Debuff enemies
                        living.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, true));
                        living.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, true));

                        // Enemy particles
                        context.fx().spawnParticles(living.getLocation(), Particle.SMOKE, 3, 0.2, 0, 0.2, 0.1);
                    }
                }

                // Aura ring particles
                createAuraRing(center, radius, context);

                ticks += tickInterval;
            }
        }.runTaskTimer(context.plugin(), 0L, tickInterval);
    }

    private void createAuraRing(Location center, double radius, SpellContext context) {
        for (int i = 0; i < 360; i += 20) {
            double angle = Math.toRadians(i);
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location particleLoc = new Location(center.getWorld(), x, center.getY(), z);

            context.fx().spawnParticles(particleLoc, Particle.SMOKE, 1, 0, 0, 0, 0);
        }
    }

    @Override
    public String getName() {
        return "empire-aura";
    }

    @Override
    public String key() {
        return "empire-aura";
    }

    @Override
    public Component displayName() {
        return Component.text("Empire Aura");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}