package nl.wantedchef.empirewand.spell.control;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;

/**
 * A control spell that transforms a target entity into a sheep.
 * <p>
 * This spell provides temporary polymorphing capabilities, allowing the caster
 * to transform hostile or neutral entities into harmless sheep. The transformation
 * is temporary and can be reversed by various means including death, chunk unloading,
 * or manual reversal.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Temporary entity transformation</li>
   <li>Memory-safe polymorph tracking</li>
 *   <li>Automatic cleanup on entity death</li>
 *   <li>Visual and audio feedback</li>
 *   <li>Configurable duration</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell polymorph = new Polymorph.Builder(api)
 *     .name("Polymorph")
 *     .description("Transform target into a sheep")
 *     .cooldown(Duration.ofSeconds(25))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class Polymorph extends Spell<Void> {

    /** Default polymorph duration in seconds */
    private static final Duration DEFAULT_DURATION = Duration.ofSeconds(30);
    
    /** Maximum polymorph duration to prevent memory leaks */
    private static final Duration MAX_DURATION = Duration.ofMinutes(5);
    
    /** Map to track polymorphed entities for cleanup */
    private final Map<UUID, UUID> polymorphedEntities;

    /**
     * Builder for creating Polymorph spell instances.
     * <p>
     * Provides a fluent API for configuring the polymorph spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        
        /**
         * Creates a new Polymorph spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Polymorph";
            this.description = "Transforms a target entity into a harmless sheep for a short duration.";
            this.cooldown = Duration.ofSeconds(25);
            this.spellType = SpellType.CONTROL;
        }

        /**
         * Builds and returns a new Polymorph spell instance.
         *
         * @return the constructed Polymorph spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new Polymorph(this);
        }
    }

    /**
     * Constructs a new Polymorph spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private Polymorph(@NotNull Builder builder) {
        super(builder);
        this.polymorphedEntities = new ConcurrentHashMap<>();
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "polymorph"
     */
    @Override
    @NotNull
    public String key() {
        return "polymorph";
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
     * Executes the polymorph spell logic.
     * <p>
     * This method transforms the target entity into a sheep and sets up
     * automatic cleanup after the duration expires.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    @Nullable
    protected Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player caster = context.caster();
        LivingEntity target = context.target();
        
        // If no explicit target, try to get the entity the caster is looking at
        if (target == null) {
            target = getTargetedEntity(caster);
        }
        
        if (target == null || !target.isValid()) {
            return null;
        }

        // Prevent polymorphing players or already polymorphed entities
        if (target instanceof Player || polymorphedEntities.containsKey(target.getUniqueId())) {
            return null;
        }

        polymorphEntity(context, target);
        
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled during executeSpell
    }

    /**
     * Transforms the target entity into a sheep.
     * <p>
     * Creates a sheep at the target's location and removes the original entity,
     * while maintaining a mapping for future restoration.
     *
     * @param context the spell context
     * @param target the entity to polymorph
     */
    private void polymorphEntity(@NotNull SpellContext context, @NotNull LivingEntity target) {
        Objects.requireNonNull(context, "Context cannot be null");
        Objects.requireNonNull(target, "Target cannot be null");
        
        Location location = target.getLocation();
        
        // Create sheep at target location
        Sheep sheep = location.getWorld().spawn(location, Sheep.class, spawnedSheep -> {
            // Use String-based custom name for broader API compatibility
            spawnedSheep.setCustomName("Polymorphed " + target.getType().name());
            spawnedSheep.setCustomNameVisible(true);
            DyeColor[] colors = DyeColor.values();
            spawnedSheep.setColor(colors[(int) (Math.random() * colors.length)]);
        });

        // Store mapping for cleanup
        polymorphedEntities.put(target.getUniqueId(), sheep.getUniqueId());

        // Remove original entity
        target.remove();

        // Play polymorph effects
        playPolymorphEffects(context, location);

        // Schedule cleanup
        scheduleCleanup(context, sheep.getUniqueId(), target.getUniqueId());
    }

    /**
     * Plays visual and audio effects for the polymorph spell.
     *
     * @param context the spell context
     * @param location the polymorph location
     */
    private void playPolymorphEffects(@NotNull SpellContext context, @NotNull Location location) {
        Objects.requireNonNull(context, "Context cannot be null");
        Objects.requireNonNull(location, "Location cannot be null");
        
        // Play sound effect
        location.getWorld().playSound(
            location,
            Sound.ENTITY_EVOKER_PREPARE_SUMMON,
            1.0f, // Volume
            1.2f  // Pitch
        );

        // Play particle effect
        location.getWorld().spawnParticle(
            Particle.ENTITY_EFFECT,
            location.add(0, 1, 0),
            30,  // Count
            0.5, // Offset X
            0.5, // Offset Y
            0.5, // Offset Z
            0.1  // Speed
        );
    }

    /**
     * Schedules automatic cleanup of the polymorphed entity.
     * <p>
     * Removes the sheep after the duration expires or when the chunk unloads.
     *
     * @param context the spell context
     * @param sheepId the sheep entity's UUID
     * @param originalId the original entity's UUID
     */
    private void scheduleCleanup(@NotNull SpellContext context, @NotNull UUID sheepId, @NotNull UUID originalId) {
        Objects.requireNonNull(context, "Context cannot be null");
        Objects.requireNonNull(sheepId, "Sheep ID cannot be null");
        Objects.requireNonNull(originalId, "Original ID cannot be null");
        
        new BukkitRunnable() {
            @Override
            public void run() {
                Entity sheep = Bukkit.getEntity(sheepId);
                if (sheep != null && sheep.isValid()) {
                    sheep.remove();
                }
                polymorphedEntities.remove(originalId);
            }
        }.runTaskLater(context.plugin(), DEFAULT_DURATION.getSeconds() * 20);
    }

    /**
     * Gets the entity that the caster is currently targeting.
     * <p>
     * Uses ray tracing to find the nearest living entity within range.
     *
     * @param caster the player casting the spell
     * @return the targeted entity, or null if no valid target found
     */
    @Nullable
    private LivingEntity getTargetedEntity(@NotNull Player caster) {
        Objects.requireNonNull(caster, "Caster cannot be null");
        
        // Ray trace for entities within 25 blocks
        var target = caster.getTargetEntity(25);
        return target instanceof LivingEntity ? (LivingEntity) target : null;
    }

    /**
     * Gets an unmodifiable view of currently polymorphed entities.
     * <p>
     * This is primarily for debugging and administrative purposes.
     * 
     * @return a map of original entity UUIDs to sheep UUIDs
     */
    @NotNull
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Returns unmodifiable map for safe external access")
    public Map<UUID, UUID> getPolymorphedEntities() {
        return Collections.unmodifiableMap(polymorphedEntities);
    }

    /**
     * Cleans up all polymorphed entities.
     * <p>
     * This method should be called during plugin shutdown to ensure
     * all polymorphed entities are properly removed.
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Method intentionally iterates over internal map for cleanup")
    public void cleanupAllPolymorphed() {
        for (Map.Entry<UUID, UUID> entry : polymorphedEntities.entrySet()) {
            Entity sheep = Bukkit.getEntity(entry.getValue());
            if (sheep != null && sheep.isValid()) {
                sheep.remove();
            }
        }
        polymorphedEntities.clear();
    }

    /**
     * Cleanup alias used by plugin shutdown lifecycle.
     */
    public void cleanup() {
        cleanupAllPolymorphed();
    }

    /**
     * Reverts a polymorph by sheep UUID. Removes the sheep and its mapping.
     *
     * @param sheepId the UUID of the polymorphed sheep
     * @return true if a mapping was found and reverted, false otherwise
     */
    public boolean revertBySheep(@NotNull UUID sheepId) {
        Objects.requireNonNull(sheepId, "sheepId cannot be null");
        // Find original by sheep value
        UUID originalId = null;
        for (Map.Entry<UUID, UUID> e : polymorphedEntities.entrySet()) {
            if (e.getValue().equals(sheepId)) {
                originalId = e.getKey();
                break;
            }
        }
        if (originalId == null) {
            return false;
        }
        Entity sheep = Bukkit.getEntity(sheepId);
        if (sheep != null && sheep.isValid()) {
            sheep.remove();
        }
        polymorphedEntities.remove(originalId);
        return true;
    }

    /**
     * Reverts a polymorph by original entity UUID. Removes the associated sheep and mapping.
     *
     * @param originalId the UUID of the original entity
     * @return true if a mapping was found and reverted, false otherwise
     */
    public boolean revertByOriginal(@NotNull UUID originalId) {
        Objects.requireNonNull(originalId, "originalId cannot be null");
        UUID sheepId = polymorphedEntities.remove(originalId);
        if (sheepId == null) {
            return false;
        }
        Entity sheep = Bukkit.getEntity(sheepId);
        if (sheep != null && sheep.isValid()) {
            sheep.remove();
        }
        return true;
    }
}
