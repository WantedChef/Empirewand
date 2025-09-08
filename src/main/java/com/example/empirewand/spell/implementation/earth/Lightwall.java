package com.example.empirewand.spell.implementation.earth;

import com.example.empirewand.api.EmpireWandAPI;

import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class Lightwall extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Lightwall";
            this.description = "Creates a temporary wall of light that knocks back entities.";
            this.manaCost = 8;
            this.cooldown = java.time.Duration.ofSeconds(18);
            this.spellType = SpellType.EARTH;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new Lightwall(this);
        }
    }

    private final double width;
    private final double height;
    private final int duration;
    private final double knockbackStrength;
    private final int blindnessDuration;
    private final boolean hitPlayers;
    private final boolean hitMobs;

    private Lightwall(Builder builder) {
        super(builder);
        this.width = spellConfig.getDouble("values.width", 6.0);
        this.height = spellConfig.getDouble("values.height", 3.0);
        this.duration = spellConfig.getInt("values.duration-ticks", 100);
        this.knockbackStrength = spellConfig.getDouble("values.knockback-strength", 0.5);
        this.blindnessDuration = spellConfig.getInt("values.blindness-duration-ticks", 30);
        this.hitPlayers = spellConfig.getBoolean("flags.hit-players", true);
        this.hitMobs = spellConfig.getBoolean("flags.hit-mobs", true);
    }

    @Override
    public String key() {
        return "lightwall";
    }

    @Override
    public Component displayName() {
        return Component.text("Lichtmuur");
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        Location center = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(3));
        Vector right = player.getEyeLocation().getDirection().crossProduct(new Vector(0, 1, 0)).normalize();

        List<ArmorStand> wallStands = createWallStands(center, right);
        new WallTask(context, wallStands, knockbackStrength, blindnessDuration, hitPlayers, hitMobs).runTaskTimer(context.plugin(), 0L, 1L);

        new BukkitRunnable() {
            @Override
            public void run() {
                wallStands.forEach(stand -> {
                    if (stand.isValid()) stand.remove();
                });
            }
        }.runTaskLater(context.plugin(), duration);

        spawnWallParticles(context, center, width, height, right);
        context.fx().playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.2f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    private List<ArmorStand> createWallStands(Location center, Vector right) {
        List<ArmorStand> wallStands = new ArrayList<>();
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                Vector offset = right.clone().multiply(w - width / 2).add(new Vector(0, h, 0));
                Location standLocation = center.clone().add(offset);

                ArmorStand stand = center.getWorld().spawn(standLocation, ArmorStand.class, s -> {
                    s.setInvisible(true);
                    s.setMarker(true);
                    s.setGravity(false);
                    s.setInvulnerable(true);
                });
                wallStands.add(stand);
            }
        }
        return wallStands;
    }

    private class WallTask extends BukkitRunnable {
        private final SpellContext context;
        private final List<ArmorStand> wallStands;
        private final double knockbackStrength;
        private final int blindnessDuration;
        private final boolean hitPlayers;
        private final boolean hitMobs;

        public WallTask(SpellContext context, List<ArmorStand> wallStands, double knockbackStrength, int blindnessDuration, boolean hitPlayers, boolean hitMobs) {
            this.context = context;
            this.wallStands = wallStands;
            this.knockbackStrength = knockbackStrength;
            this.blindnessDuration = blindnessDuration;
            this.hitPlayers = hitPlayers;
            this.hitMobs = hitMobs;
        }

        @Override
        public void run() {
            if (wallStands.isEmpty() || !wallStands.get(0).isValid()) {
                this.cancel();
                return;
            }

            wallStands.forEach(stand -> {
                if (!stand.isValid()) return;

                stand.getWorld().getNearbyEntities(stand.getLocation(), 1.5, 1.5, 1.5).forEach(entity -> {
                    if (entity instanceof LivingEntity living) {
                        if ((entity instanceof Player && !hitPlayers) || (!(entity instanceof Player) && !hitMobs)) return;

                        Vector knockback = living.getLocation().toVector().subtract(stand.getLocation().toVector()).normalize();
                        living.setVelocity(entity.getVelocity().add(knockback.multiply(knockbackStrength).setY(0.2)));
                        living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessDuration, 0));
                    }
                });
            });

            if (System.currentTimeMillis() % 1000 < 50) { // Roughly every second
                ArmorStand firstStand = wallStands.get(0);
                Location center = firstStand.getLocation(); // Approximate center
                double width = 6; // Re-approximate for visuals
                double height = 3;
                Vector right = firstStand.getLocation().getDirection().crossProduct(new Vector(0, 1, 0)).normalize();
                spawnWallParticles(context, center, width, height, right);
            }
        }
    }

    private void spawnWallParticles(SpellContext context, Location center, double width, double height, Vector right) {
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                Vector offset = right.clone().multiply(w - width / 2).add(new Vector(0, h, 0));
                Location particleLoc = center.clone().add(offset);
                context.fx().spawnParticles(particleLoc, Particle.WHITE_ASH, 2, 0.1, 0.1, 0.1, 0);
                context.fx().spawnParticles(particleLoc, Particle.GLOW, 1, 0, 0, 0, 0);
            }
        }
    }
}