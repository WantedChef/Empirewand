package nl.wantedchef.empirewand.spell.enhanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.ProjectileSpell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Color;
import org.bukkit.entity.DragonFireball;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * A devastating spell that fires a cone of dragon's breath,
 * dealing massive damage and applying wither effect.
 */
public class DragonsBreath extends ProjectileSpell<DragonFireball> {

    public static class Builder extends ProjectileSpell.Builder<DragonFireball> {
        public Builder(EmpireWandAPI api) {
            super(api, DragonFireball.class);
            this.name = "Dragon's Breath";
            this.description = "Fires a cone of dragon's breath that deals massive damage and applies wither effect.";
            this.cooldown = java.time.Duration.ofSeconds(30);
            this.spellType = SpellType.FIRE;
            this.trailParticle = Particle.DRAGON_BREATH;
            this.hitSound = Sound.ENTITY_ENDER_DRAGON_HURT;
        }

        @Override
        @NotNull
        public ProjectileSpell<DragonFireball> build() {
            return new DragonsBreath(this);
        }
    }

    private DragonsBreath(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "dragons-breath";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected void launchProjectile(@NotNull SpellContext context) {
        Player player = context.caster();

        // Configuration
        double speed = spellConfig.getDouble("values.speed", 1.2);
        int coneAngle = spellConfig.getInt("values.cone-angle", 45);
        int projectileCount = spellConfig.getInt("values.projectile-count", 1);
        double damage = spellConfig.getDouble("values.damage", 7.0);
        int witherDuration = spellConfig.getInt("values.wither-duration-ticks", 100);
        int witherAmplifier = spellConfig.getInt("values.wither-amplifier", 1);

        // Play launch sound
        context.fx().playSound(player, Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.5f, 0.8f);

        // Launch multiple dragon fireballs in a cone
        Vector direction = player.getEyeLocation().getDirection().normalize();
        
        for (int i = 0; i < projectileCount; i++) {
            // Calculate spread for cone effect
            Vector spreadDirection = direction.clone();
            if (projectileCount > 1) {
                double angleOffset = (i - (projectileCount - 1) / 2.0) * Math.toRadians(coneAngle) / (projectileCount - 1);
                // Apply horizontal spread
                spreadDirection.rotateAroundY(angleOffset);
            }
            
            DragonFireball fireball = player.launchProjectile(DragonFireball.class, spreadDirection.multiply(speed));
            
            // Store damage information in the fireball
            fireball.setCustomName("dragons_breath_" + damage + "_" + witherDuration + "_" + witherAmplifier);
            
            // Visual trail
            context.fx().followParticles(context.plugin(), fireball, Particle.DRAGON_BREATH, 8, 0.1, 0.1, 0.1, 0.02, null, 1L);
        }
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile, @NotNull ProjectileHitEvent event) {
        Location hitLocation = projectile.getLocation();
        World world = hitLocation.getWorld();
        if (world == null) return;

        // Parse damage information from custom name
        double damage = 7.0;
        int witherDuration = 100;
        int witherAmplifier = 1;
        
        if (projectile.getCustomName() != null && projectile.getCustomName().startsWith("dragons_breath_")) {
            String[] parts = projectile.getCustomName().split("_");
            if (parts.length >= 4) {
                try {
                    damage = Double.parseDouble(parts[2]);
                    witherDuration = Integer.parseInt(parts[3]);
                    witherAmplifier = Integer.parseInt(parts[4]);
                } catch (NumberFormatException ignored) {}
            }
        }

        // Create area effect cloud for lingering dragon breath
        AreaEffectCloud cloud = world.spawn(hitLocation, AreaEffectCloud.class);
        cloud.setRadius(3.0f);
        cloud.setRadiusPerTick(-0.02f);
        cloud.setDuration(120);
        cloud.setParticle(Particle.DRAGON_BREATH);
        cloud.setColor(Color.fromRGB(100, 0, 200));
        cloud.setSource(context.caster());

        // Damage and apply effects to entities in the area
        for (Entity entity : world.getNearbyEntities(hitLocation, 4, 4, 4)) {
            if (entity.equals(context.caster())) continue;
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;
                living.damage(damage, context.caster());
                living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, witherDuration, witherAmplifier, false, true));
                
                // Visual effect
                world.spawnParticle(Particle.DAMAGE_INDICATOR, living.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.01);
            }
        }

        // Additional explosion effects
        world.spawnParticle(Particle.EXPLOSION, hitLocation, 15, 1, 1, 1, 0.1);
        world.spawnParticle(Particle.DRAGON_BREATH, hitLocation, 50, 2, 2, 2, 0.1);
        
        // Sound effect
        world.playSound(hitLocation, Sound.ENTITY_ENDER_DRAGON_HURT, 1.5f, 0.8f);
    }
}