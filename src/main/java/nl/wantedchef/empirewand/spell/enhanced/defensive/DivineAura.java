package nl.wantedchef.empirewand.spell.enhanced.defensive;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import java.util.Objects;
import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * A divine healing spell that creates a powerful healing aura,
 * restoring health and granting beneficial effects to allies.
 * <p>
 * This spell creates a healing aura around the caster that periodically restores
 * health to nearby allies and grants beneficial potion effects. The spell includes
 * visual effects with expanding rings of particles and a central beam, and provides
 * audio feedback when activated and deactivated.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Area of effect healing for allies</li>
 *   <li>Regeneration and resistance potion effects</li>
 *   <li>Animated particle visual effects</li>
 *   <li>Audio feedback for activation and deactivation</li>
 *   <li>Configurable radius, heal amount, and duration</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell divineAura = new DivineAura.Builder(api)
 *     .name("Divine Aura")
 *     .description("Creates a powerful healing aura that restores health and grants beneficial effects to allies.")
 *     .cooldown(Duration.ofSeconds(45))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class DivineAura extends Spell<Void> {

    /**
     * Builder for creating DivineAura spell instances.
     * <p>
     * Provides a fluent API for configuring the divine aura spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new DivineAura spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Divine Aura";
            this.description = "Creates a powerful healing aura that restores health and grants beneficial effects to allies.";
            this.cooldown = Duration.ofSeconds(45);
            this.spellType = SpellType.HEAL;
        }

        /**
         * Builds and returns a new DivineAura spell instance.
         *
         * @return the constructed DivineAura spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new DivineAura(this);
        }
    }

    /**
     * Constructs a new DivineAura spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private DivineAura(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "divine-aura"
     */
    @Override
    @NotNull
    public String key() {
        return "divine-aura";
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
     * Executes the divine aura spell logic.
     * <p>
     * This method creates a healing aura around the caster that periodically
     * restores health to nearby allies and grants beneficial potion effects.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();
        var world = player.getWorld();

        // Configuration
        double radius = spellConfig.getDouble("values.radius", 15.0);
        double healAmount = spellConfig.getDouble("values.heal-amount", 2.0);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 200);
        boolean affectsPlayers = spellConfig.getBoolean("flags.affects-players", true);
        boolean grantsRegeneration = spellConfig.getBoolean("flags.grants-regeneration", true);
        boolean grantsResistance = spellConfig.getBoolean("flags.grants-resistance", true);

        // Play initial sound
        if (world != null) {
            world.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.5f);
        }

        // Start aura effect
        context.plugin().getTaskManager().runTaskTimer(
            new DivineAuraTask(context, player.getLocation(), radius, healAmount, durationTicks, affectsPlayers, 
                              grantsRegeneration, grantsResistance),
            0L, 5L
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
     * A runnable that handles the divine aura's effects over time.
     * <p>
     * This task manages the aura's healing effects and visual particle effects.
     */
    private static class DivineAuraTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final double radius;
        private final double healAmount;
        private final int durationTicks;
        private final boolean affectsPlayers;
        private final boolean grantsRegeneration;
        private final boolean grantsResistance;
        private final World world;
        private int ticks = 0;
        private final int maxTicks;

        /**
         * Creates a new DivineAuraTask instance.
         *
         * @param context the spell context
         * @param center the center location of the aura
         * @param radius the radius of the aura effect
         * @param healAmount the amount of health to restore per tick
         * @param durationTicks the duration of the aura in ticks
         * @param affectsPlayers whether the aura affects players
         * @param grantsRegeneration whether the aura grants regeneration
         * @param grantsResistance whether the aura grants resistance
         */
        public DivineAuraTask(@NotNull SpellContext context, @NotNull Location center, double radius, double healAmount,
                             int durationTicks, boolean affectsPlayers, boolean grantsRegeneration, boolean grantsResistance) {
            this.context = Objects.requireNonNull(context, "Context cannot be null");
            this.center = Objects.requireNonNull(center, "Center location cannot be null");
            this.radius = radius;
            this.healAmount = healAmount;
            this.durationTicks = durationTicks;
            this.affectsPlayers = affectsPlayers;
            this.grantsRegeneration = grantsRegeneration;
            this.grantsResistance = grantsResistance;
            this.world = center.getWorld();
            this.maxTicks = durationTicks / 5; // Convert to our tick interval
        }

        /**
         * Runs the divine aura task, applying healing effects and creating visual effects.
         */
        @Override
        public void run() {
            if (world == null) {
                this.cancel();
                return;
            }
            
            if (ticks >= maxTicks) {
                this.cancel();
                // Play end sound
                world.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 2.0f, 1.5f);
                return;
            }

            // Apply healing and effects
            applyAuraEffects();

            // Create visual effects
            createVisualEffects();

            ticks++;
        }

        /**
         * Applies the aura's healing effects to entities within the radius.
         * <p>
         * This method heals nearby allies and grants beneficial potion effects.
         */
        private void applyAuraEffects() {
            if (world == null) {
                return;
            }
            
            Collection<LivingEntity> nearbyEntities = world.getNearbyLivingEntities(center, radius, radius, radius);
            
            for (LivingEntity entity : nearbyEntities) {
                // Skip dead or invalid entities
                if (entity.isDead() || !entity.isValid()) continue;
                
                // Apply to caster always
                if (entity.equals(context.caster())) {
                    healAndApplyEffects(entity);
                    continue;
                }
                
                // Apply to players if enabled
                if (entity instanceof Player && affectsPlayers) {
                    healAndApplyEffects(entity);
                    continue;
                }
                
                // Apply to mobs only if they are tamed or on the same team
                if (!(entity instanceof Player)) {
                    // In a real implementation, you would check if the mob is tamed by the player
                    // For now, we'll skip non-player entities to avoid unintended behavior
                    continue;
                }
            }
        }

        /**
         * Heals an entity and applies beneficial potion effects.
         * <p>
         * This method restores health to the entity and optionally grants regeneration
         * and resistance potion effects.
         *
         * @param entity the entity to heal and apply effects to
         */
        private void healAndApplyEffects(@NotNull LivingEntity entity) {
            Objects.requireNonNull(entity, "Entity cannot be null");
            
            // Heal entity
            var healthAttribute = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (healthAttribute != null) {
                double maxHealth = healthAttribute.getValue();
                entity.setHealth(Math.min(maxHealth, entity.getHealth() + healAmount));
            }
            
            // Grant regeneration if enabled
            if (grantsRegeneration) {
                entity.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 1, false, false));
            }
            
            // Grant resistance if enabled
            if (grantsResistance) {
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 80, 0, false, false));
            }
            
            // Visual effect for healed entity
            var entityLocation = entity.getLocation();
            if (world != null && entityLocation != null) {
                world.spawnParticle(Particle.HEART, entityLocation.add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0.01);
            }
        }

        /**
         * Creates the aura's visual particle effects.
         * <p>
         * This method generates expanding rings of particles and a central beam
         * to visualize the aura's effect.
         */
        private void createVisualEffects() {
            if (world == null) {
                return;
            }
            
            // Create expanding rings of particles
            double currentRadius = radius * (1.0 - (ticks / (double) maxTicks));
            
            // Main ring
            for (int i = 0; i < 36; i++) {
                double angle = 2 * Math.PI * i / 36;
                double x = currentRadius * Math.cos(angle);
                double z = currentRadius * Math.sin(angle);
                Location particleLoc = center.clone().add(x, 0.1, z);
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
            }
            
            // Inner ring
            double innerRadius = currentRadius * 0.7;
            for (int i = 0; i < 24; i++) {
                double angle = 2 * Math.PI * i / 24;
                double x = innerRadius * Math.cos(angle);
                double z = innerRadius * Math.sin(angle);
                Location particleLoc = center.clone().add(x, 0.1, z);
                world.spawnParticle(Particle.ENCHANT, particleLoc, 1, 0, 0, 0, 0);
            }
            
            // Central beam
            if (ticks % 4 == 0) {
                for (int i = 0; i < 10; i++) {
                    Location beamLoc = center.clone().add(0, i * 0.5, 0);
                    world.spawnParticle(Particle.WITCH, beamLoc, 2, 0.2, 0.2, 0.2, 0.01);
                }
            }
            
            // Random sparkles
            if (ticks % 2 == 0) {
                for (int i = 0; i < 5; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double distance = Math.random() * radius;
                    double x = center.getX() + Math.cos(angle) * distance;
                    double z = center.getZ() + Math.sin(angle) * distance;
                    Location sparkleLoc = new Location(world, x, center.getY() + 0.1, z);
                    world.spawnParticle(Particle.TOTEM_OF_UNDYING, sparkleLoc, 1, 0, 0, 0, 0);
                }
            }
        }
    }
}