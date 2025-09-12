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
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * A devastating spell that fires a cone of dragon's breath,
 * dealing massive damage and applying wither effect.
 * <p>
 * This spell launches dragon fireballs in a cone pattern that explode on impact,
 * dealing damage to entities in the area and applying wither effects. The spell
 * creates a lingering area effect cloud of dragon breath and includes visual and
 * audio feedback for both launch and impact.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Cone-pattern dragon fireball projectiles</li>
 *   <li>Area of effect damage on impact</li>
 *   <li>Wither potion effects on affected entities</li>
 *   <li>Lingering dragon breath area effect cloud</li>
 *   <li>Dragon breath particle trails</li>
 *   <li>Audio feedback for launch and impact</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell dragonsBreath = new DragonsBreath.Builder(api)
 *     .name("Dragon's Breath")
 *     .description("Fires a cone of dragon's breath that deals massive damage and applies wither effect.")
 *     .cooldown(Duration.ofSeconds(30))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class DragonsBreath extends ProjectileSpell<DragonFireball> {

    /**
     * Builder for creating DragonsBreath spell instances.
     * <p>
     * Provides a fluent API for configuring the dragon's breath spell with sensible defaults.
     */
    public static class Builder extends ProjectileSpell.Builder<DragonFireball> {
        /**
         * Creates a new DragonsBreath spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@Nullable EmpireWandAPI api) {
            super(api, DragonFireball.class);
            this.name = "Dragon's Breath";
            this.description = "Fires a cone of dragon's breath that deals massive damage and applies wither effect.";
            this.cooldown = Duration.ofSeconds(30);
            this.spellType = SpellType.FIRE;
            this.trailParticle = Particle.DRAGON_BREATH;
            this.hitSound = Sound.ENTITY_ENDER_DRAGON_HURT;
        }

        /**
         * Builds and returns a new DragonsBreath spell instance.
         *
         * @return the constructed DragonsBreath spell
         */
        @Override
        @NotNull
        public ProjectileSpell<DragonFireball> build() {
            return new DragonsBreath(this);
        }
    }

    /**
     * Constructs a new DragonsBreath spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private DragonsBreath(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "dragons-breath"
     */
    @Override
    @NotNull
    public String key() {
        return "dragons-breath";
    }

    /**
     * Returns the prerequisites for casting this spell.
     * <p>
     * Currently, this spell has no prerequisites beyond standard casting requirements.
     *
     * @return a no-op prerequisite
     */
    @Override
    @NotNull
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    /**
     * Launches the dragon's breath projectiles.
     * <p>
     * This method launches multiple dragon fireballs in a cone pattern from the
     * caster's position and creates particle trails for visual feedback.
     *
     * @param context the spell context containing caster and target information
     */
    @Override
    protected void launchProjectile(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
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

    /**
     * Handles the projectile hit event.
     * <p>
     * This method creates an area effect cloud of dragon breath, applies damage
     * and wither effects to entities in the area, and generates visual and audio
     * feedback for the impact.
     *
     * @param context the spell context
     * @param projectile the projectile that hit
     * @param event the projectile hit event
     */
    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile, @NotNull ProjectileHitEvent event) {
        Objects.requireNonNull(context, "Context cannot be null");
        Objects.requireNonNull(projectile, "Projectile cannot be null");
        Objects.requireNonNull(event, "Event cannot be null");
        
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
            if (entity instanceof LivingEntity living) {
                living.damage(damage, context.caster());
                living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, witherDuration, witherAmplifier, false, true));
                
                // Visual effect
                var entityLocation = entity.getLocation();
                if (entityLocation != null) {
                    world.spawnParticle(Particle.DAMAGE_INDICATOR, entityLocation.add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.01);
                }
            }
        }

        // Additional explosion effects
        world.spawnParticle(Particle.EXPLOSION, hitLocation, 15, 1, 1, 1, 0.1);
        world.spawnParticle(Particle.DRAGON_BREATH, hitLocation, 50, 2, 2, 2, 0.1);
        
        // Sound effect
        world.playSound(hitLocation, Sound.ENTITY_ENDER_DRAGON_HURT, 1.5f, 0.8f);
    }
}