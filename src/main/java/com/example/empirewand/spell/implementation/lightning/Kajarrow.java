package com.example.empirewand.spell.implementation.lightning;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.ConfigService;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.ProjectileSpell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

/**
 * Kajarrow - Lightning arrow that creates thunder around the impact area.
 * The arrow deals normal arrow damage to the target, while thunder strikes
 * randomly
 * around the landing area and can hit other entities.
 */
public class Kajarrow extends ProjectileSpell<Arrow> {

    private static final Random RANDOM = new Random();

    public static class Builder extends ProjectileSpell.Builder<Arrow> {
        public Builder(EmpireWandAPI api) {
            super(api, Arrow.class);
            this.name = "Kajarrow";
            this.description = "Fires a lightning arrow that creates thunder around the impact area.";
            this.cooldown = java.time.Duration.ofSeconds(10);
            this.spellType = SpellType.LIGHTNING;
            this.trailParticle = Particle.ELECTRIC_SPARK;
            this.hitSound = Sound.ENTITY_LIGHTNING_BOLT_THUNDER;
        }

        @Override
        @NotNull
        public ProjectileSpell<Arrow> build() {
            return new Kajarrow(this);
        }
    }

    private Kajarrow(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "kajarrow";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected void launchProjectile(@NotNull SpellContext context) {
        Player caster = context.caster();

        // Get configuration values
        double speed = spellConfig.getDouble("values.speed", 2.0);
        double baseDamage = spellConfig.getDouble("values.base_damage", 6.0);
        boolean critical = spellConfig.getBoolean("flags.critical", true);

        caster.launchProjectile(Arrow.class, caster.getEyeLocation().getDirection().multiply(speed), arrow -> {
            arrow.setShooter(caster);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            arrow.setCritical(critical);
            arrow.setDamage(baseDamage);

            // Set persistent data for identification
            arrow.getPersistentDataContainer().set(
                    com.example.empirewand.core.storage.Keys.PROJECTILE_SPELL,
                    PersistentDataType.STRING,
                    key());
            arrow.getPersistentDataContainer().set(
                    com.example.empirewand.core.storage.Keys.PROJECTILE_OWNER,
                    PersistentDataType.STRING,
                    caster.getUniqueId().toString());
        });

        context.fx().playSound(caster, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event) {
        Location hitLoc = projectile.getLocation();
        if (hitLoc == null || hitLoc.getWorld() == null) {
            return;
        }
        Player caster = context.caster();

        // Configuration values
        int thunderCount = spellConfig.getInt("values.thunder_count", 3);
        double thunderRadius = spellConfig.getDouble("values.thunder_radius", 5.0);
        double thunderDamage = spellConfig.getDouble("values.thunder_damage", 4.0);
        boolean blockDamage = spellConfig.getBoolean("flags.block_damage", false);
        boolean applyGlowing = spellConfig.getBoolean("flags.glowing", true);
        int glowingDuration = spellConfig.getInt("values.glowing_duration_ticks", 100);
        double poisonChance = spellConfig.getDouble("values.poison_chance", 0.3);
        int poisonDuration = spellConfig.getInt("values.poison_duration_ticks", 60);
        int poisonAmplifier = spellConfig.getInt("values.poison_amplifier", 0);

        // Create thunder strikes around the impact area
        for (int i = 0; i < thunderCount; i++) {
            // Random location within the thunder radius
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double distance = RANDOM.nextDouble() * thunderRadius;
            double x = hitLoc.getX() + Math.cos(angle) * distance;
            double z = hitLoc.getZ() + Math.sin(angle) * distance;
            Location thunderLoc = new Location(hitLoc.getWorld(), x, hitLoc.getY(), z);

            // Create lightning effect
            if (blockDamage) {
                thunderLoc.getWorld().strikeLightning(thunderLoc);
            } else {
                thunderLoc.getWorld().strikeLightningEffect(thunderLoc);
            }

            // Damage nearby entities (excluding the main target if it was an entity)
            List<Entity> nearbyEntities = thunderLoc.getWorld().getNearbyEntities(thunderLoc, 2.0, 2.0, 2.0)
                    .stream()
                    .filter(e -> e instanceof LivingEntity)
                    .filter(e -> !e.equals(caster) || EmpireWandAPI.getService(ConfigService.class).getMainConfig()
                            .getBoolean("features.friendly-fire", false))
                    .toList();

            for (Entity entity : nearbyEntities) {
                LivingEntity living = (LivingEntity) entity;

                // Don't damage the main target again if it was hit by the arrow
                if (event.getHitEntity() != null && event.getHitEntity().equals(living)) {
                    continue;
                }

                living.damage(thunderDamage, caster);

                // Apply glowing effect
                if (applyGlowing) {
                    living.addPotionEffect(new PotionEffect(
                            PotionEffectType.GLOWING,
                            glowingDuration,
                            0,
                            false,
                            true));
                }

                // Apply poison with chance
                if (RANDOM.nextDouble() < poisonChance) {
                    living.addPotionEffect(new PotionEffect(
                            PotionEffectType.POISON,
                            poisonDuration,
                            poisonAmplifier,
                            false,
                            true));
                }
            }

            // Play thunder sound
            if (context.fx() != null) {
                context.fx().playSound(thunderLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);

                // Add electric spark particles
                context.fx().spawnParticles(thunderLoc, Particle.ELECTRIC_SPARK, 15, 0.5, 0.5, 0.5, 0.1);
            }
        }

        // Add impact particles at the main hit location
        if (context.fx() != null) {
            context.fx().spawnParticles(hitLoc, Particle.EXPLOSION, 20, 0.3, 0.3, 0.3, 0.1);
            context.fx().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
        }
    }
}