package nl.wantedchef.empirewand.spell.enhanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * A defensive spell that creates a powerful energy shield around allies, absorbing damage and
 * reflecting projectiles.
 * <p>
 * This spell creates a protective energy shield around nearby allies that grants absorption
 * hearts and resistance effects. The spell can also reflect projectiles back to their shooters
 * when they hit shielded entities. Visual effects include rotating shield rings and energy beams
 * connecting shielded allies.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Area of effect absorption hearts for allies</li>
 *   <li>Resistance potion effects</li>
 *   <li>Projectile reflection capability</li>
 *   <li>Animated shield visual effects</li>
 *   <li>Energy beams connecting shielded entities</li>
 *   <li>Audio feedback for activation and deactivation</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell energyShield = new EnergyShield.Builder(api)
 *     .name("Energy Shield")
 *     .description("Creates a powerful energy shield around allies that absorbs damage and reflects projectiles.")
 *     .cooldown(Duration.ofSeconds(40))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class EnergyShield extends Spell<Void> {

    /**
     * Builder for creating EnergyShield spell instances.
     * <p>
     * Provides a fluent API for configuring the energy shield spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new EnergyShield spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Energy Shield";
            this.description =
                    "Creates a powerful energy shield around allies that absorbs damage and reflects projectiles.";
            this.cooldown = Duration.ofSeconds(40);
            this.spellType = SpellType.AURA;
        }

        /**
         * Builds and returns a new EnergyShield spell instance.
         *
         * @return the constructed EnergyShield spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new EnergyShield(this);
        }
    }

    /**
     * Constructs a new EnergyShield spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private EnergyShield(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "energy-shield"
     */
    @Override
    @NotNull
    public String key() {
        return "energy-shield";
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
     * Executes the energy shield spell logic.
     * <p>
     * This method creates an energy shield effect around the caster that grants
     * absorption hearts and resistance effects to nearby allies.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();

        // Configuration
        double radius = spellConfig.getDouble("values.radius", 12.0);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 150);
        double absorptionHearts = spellConfig.getDouble("values.absorption-hearts", 10.0);
        boolean affectsPlayers = spellConfig.getBoolean("flags.affects-players", true);
        boolean reflectsProjectiles = spellConfig.getBoolean("flags.reflects-projectiles", true);

        // Play initial sound
        World world = player.getWorld();
        if (world == null) {
            return null; // Can't cast a spell in a null world
        }
        world.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.8f);

        // Start energy shield effect
        context.plugin().getTaskManager().runTaskTimer(
            new EnergyShieldTask(context, player.getLocation(), radius, durationTicks, absorptionHearts,
                    affectsPlayers, reflectsProjectiles),
            0L, 4L
        );
        return null;
    }

    /**
     * Handles the spell effect after execution.
     * <p>
     * This spell's effects are handled asynchronously through BukkitRunnables.
     *
     * @param context the spell context
     * @param result the result of the spell execution (always null for this spell)
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled in scheduler
    }

    /**
     * A runnable that handles the energy shield's effects over time.
     * <p>
     * This task manages the shield's application to entities, visual effects, and cleanup.
     */
    private static class EnergyShieldTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final double radius;
        private final int durationTicks;
        private final double absorptionHearts;
        private final boolean affectsPlayers;
        private final boolean reflectsProjectiles;
        private final World world;
        private int ticks = 0;
        private final int maxTicks;
        private final Map<UUID, ShieldData> shieldedEntities = new HashMap<>();
        private final Set<UUID> reflectedProjectiles = new HashSet<>();

        /**
         * Creates a new EnergyShieldTask instance.
         *
         * @param context the spell context
         * @param center the center location of the shield effect
         * @param radius the radius of the shield effect
         * @param durationTicks the duration of the shield in ticks
         * @param absorptionHearts the number of absorption hearts to grant
         * @param affectsPlayers whether the shield affects players
         * @param reflectsProjectiles whether the shield reflects projectiles
         */
        public EnergyShieldTask(@NotNull SpellContext context, @NotNull Location center, double radius,
                int durationTicks, double absorptionHearts, boolean affectsPlayers,
                boolean reflectsProjectiles) {
            this.context = Objects.requireNonNull(context, "Context cannot be null");
            this.center = Objects.requireNonNull(center, "Center location cannot be null");
            this.radius = radius;
            this.durationTicks = durationTicks;
            this.absorptionHearts = absorptionHearts;
            this.affectsPlayers = affectsPlayers;
            this.reflectsProjectiles = reflectsProjectiles;
            this.world = center.getWorld();
            this.maxTicks = durationTicks / 4; // Convert to our tick interval
        }

        /**
         * Runs the energy shield task, applying shields to entities and creating visual effects.
         */
        @Override
        public void run() {
            if (world == null) {
                this.cancel();
                return;
            }
            
            if (ticks >= maxTicks) {
                this.cancel();
                removeShields();
                // Play end sound
                world.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 2.0f, 1.8f);
                return;
            }

            // Apply shields to entities
            applyShields();

            // Create visual effects
            createVisualEffects();

            ticks++;
        }

        /**
         * Applies shields to entities within the radius.
         * <p>
         * This method grants absorption hearts and resistance effects to nearby allies.
         */
        private void applyShields() {
            if (world == null) {
                return;
            }
            
            Collection<LivingEntity> nearbyEntities =
                    world.getNearbyLivingEntities(center, radius, radius, radius);

            for (LivingEntity entity : nearbyEntities) {
                // Skip dead or invalid entities
                if (entity.isDead() || !entity.isValid())
                    continue;

                // Apply to caster always
                if (entity.equals(context.caster())) {
                    applyShieldToEntity(entity);
                    continue;
                }

                // Apply to players if enabled
                if (entity instanceof Player && affectsPlayers) {
                    applyShieldToEntity(entity);
                    continue;
                }

                // Apply to mobs only if they are tamed or on the same team
                if (!(entity instanceof Player)) {
                    // In a real implementation, you would check if the mob is tamed by the player
                    // For now, we'll skip non-player entities to avoid unintended behavior
                }
            }
        }

        /**
         * Applies the energy shield to a specific entity.
         * <p>
         * This method grants absorption hearts and resistance effects to the entity
         * and creates visual feedback for the shield application.
         *
         * @param entity the entity to apply the shield to
         */
        private void applyShieldToEntity(@NotNull LivingEntity entity) {
            Objects.requireNonNull(entity, "Entity cannot be null");
            
            UUID entityId = entity.getUniqueId();

            // Check if entity already has a shield
            ShieldData shieldData = shieldedEntities.get(entityId);
            if (shieldData == null) {
                // Create new shield
                shieldData = new ShieldData(entity, absorptionHearts);
                shieldedEntities.put(entityId, shieldData);

                // Apply absorption effect
                // Calculate the amplifier. Each level adds 2 hearts (4 half-hearts).
                // Amplifier 0 = 2 hearts, 1 = 4 hearts, etc.
                int amplifier = (int) Math.ceil(absorptionHearts / 2.0) - 1;
                if (amplifier < 0) {
                    amplifier = 0; // Ensure amplifier is not negative
                }
                entity.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, durationTicks,
                        amplifier, false, false));
                entity.addPotionEffect(
                        new PotionEffect(PotionEffectType.RESISTANCE, 20, 2, false, false));

                // Visual effect for shield application
                var entityLocation = entity.getLocation();
                if (world != null && entityLocation != null) {
                    world.spawnParticle(Particle.END_ROD, entityLocation.add(0, 1, 0), 20, 0.5,
                            0.5, 0.5, 0.1);
                }
            }

            // Update shield visual
            if (ticks % 10 == 0) {
                createShieldVisual(entity);
            }
        }

        /**
         * Creates the shield visual effect around an entity.
         * <p>
         * This method generates a rotating ring of particles around the entity to
         * visualize the shield effect.
         *
         * @param entity the entity to create the shield visual for
         */
        private void createShieldVisual(@NotNull LivingEntity entity) {
            Objects.requireNonNull(entity, "Entity cannot be null");
            
            var loc = entity.getLocation();
            if (world == null || loc == null) {
                return;
            }
            
            loc = loc.add(0, 1, 0);
            double shieldRadius = 1.0 + (Math.sin(ticks * 0.5) * 0.2);

            for (int i = 0; i < 12; i++) {
                double angle = 2 * Math.PI * i / 12;
                double x = shieldRadius * Math.cos(angle);
                double z = shieldRadius * Math.sin(angle);
                Location particleLoc = loc.clone().add(x, 0, z);
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
            }

            // Sound effect occasionally
            if (ticks % 20 == 0) {
                world.playSound(loc, Sound.BLOCK_GLASS_STEP, 0.3f, 1.5f);
            }
        }

        /**
         * Creates the energy shield's visual effects.
         * <p>
         * This method generates rotating shield rings around the center and energy
         * beams connecting shielded entities.
         */
        private void createVisualEffects() {
            if (world == null) {
                return;
            }
            
            // Create rotating shield ring around center
            double currentRadius = radius + Math.sin(ticks * 0.3) * 2;

            for (int i = 0; i < 36; i++) {
                double angle = (2 * Math.PI * i / 36) + (ticks * 0.2);
                double x = currentRadius * Math.cos(angle);
                double z = currentRadius * Math.sin(angle);
                Location particleLoc = center.clone().add(x, 1, z);
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
            }

            // Create energy beams between shielded entities
            if (ticks % 8 == 0) {
                List<LivingEntity> entities = new ArrayList<>();
                for (ShieldData data : shieldedEntities.values()) {
                    if (data.entity.isValid() && !data.entity.isDead()) {
                        entities.add(data.entity);
                    }
                }

                for (int i = 0; i < entities.size(); i++) {
                    LivingEntity from = entities.get(i);
                    LivingEntity to = entities.get((i + 1) % entities.size());
                    
                    var fromLocation = from.getLocation();
                    var toLocation = to.getLocation();
                    
                    if (fromLocation != null && toLocation != null) {
                        // Create beam between entities
                        createEnergyBeam(fromLocation.add(0, 1, 0),
                                toLocation.add(0, 1, 0));
                    }
                }
            }
        }

        /**
         * Creates an energy beam between two locations.
         * <p>
         * This method generates a line of electric spark particles between the from
         * and to locations to visualize the energy beam.
         *
         * @param from the starting location of the beam
         * @param to the ending location of the beam
         */
        private void createEnergyBeam(@NotNull Location from, @NotNull Location to) {
            Objects.requireNonNull(from, "From location cannot be null");
            Objects.requireNonNull(to, "To location cannot be null");
            
            if (world == null) {
                return;
            }
            
            Vector direction = to.toVector().subtract(from.toVector());
            double distance = direction.length();
            if (distance < 0.01)
                return;

            Vector step = direction.normalize().multiply(0.5);
            for (double d = 0; d < distance; d += 0.5) {
                Location particleLoc = from.clone().add(step.clone().multiply(d));
                world.spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0, 0, 0, 0);
            }
        }

        /**
         * Removes the energy shields from all entities.
         * <p>
         * This method removes the absorption potion effects from all shielded entities.
         */
        private void removeShields() {
            // Remove absorption effects
            for (ShieldData data : shieldedEntities.values()) {
                if (data.entity.isValid() && !data.entity.isDead()) {
                    data.entity.removePotionEffect(PotionEffectType.ABSORPTION);
                }
            }

            shieldedEntities.clear();
        }

        /**
         * Handles projectile reflection when a shielded entity is hit.
         * <p>
         * This method reflects projectiles back to their shooters when they hit
         * shielded entities and creates visual feedback for the reflection.
         *
         * @param projectile the projectile that hit a shielded entity
         * @param hitEntity the entity that was hit by the projectile
         */
        // Handle damage reflection - this would typically be called from an event listener
        public void handleProjectileHit(@NotNull Projectile projectile, @NotNull LivingEntity hitEntity) {
            Objects.requireNonNull(projectile, "Projectile cannot be null");
            Objects.requireNonNull(hitEntity, "Hit entity cannot be null");
            
            if (world == null) {
                return;
            }
            
            if (!reflectsProjectiles) {
                return;
            }
            if (!reflectedProjectiles.contains(projectile.getUniqueId())
                    && shieldedEntities.containsKey(hitEntity.getUniqueId())) {

                // Reflect projectile back to shooter
                if (projectile.getShooter() instanceof LivingEntity shooter) {
                    Vector reflectDirection = shooter.getLocation().toVector()
                            .subtract(hitEntity.getLocation().toVector()).normalize();
                    projectile.setVelocity(
                            reflectDirection.multiply(projectile.getVelocity().length() * 1.2));

                    // Mark as reflected to prevent infinite bouncing
                    reflectedProjectiles.add(projectile.getUniqueId());

                    // Visual effect
                    var projectileLocation = projectile.getLocation();
                    if (projectileLocation != null) {
                        world.spawnParticle(Particle.ELECTRIC_SPARK, projectileLocation, 10, 0.2,
                                0.2, 0.2, 0.1);
                        world.playSound(projectileLocation, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.8f);
                    }
                }
            }
        }

        /**
         * Data class for tracking shielded entities.
         * <p>
         * This class holds information about entities that have been shielded by the spell.
         */
        private static class ShieldData {
            final LivingEntity entity;
            // You can add strength fields here later if needed

            /**
             * Creates a new ShieldData instance.
             *
             * @param entity the shielded entity
             * @param absorptionHearts the number of absorption hearts granted
             */
            ShieldData(@NotNull LivingEntity entity, double absorptionHearts) {
                this.entity = Objects.requireNonNull(entity, "Entity cannot be null");
            }
        }
    }
}
