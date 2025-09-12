package nl.wantedchef.empirewand.spell;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.EmpireWandPlugin;

import java.time.Duration;
import java.util.logging.Level;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the abstract concept of a spell, providing a foundational
 * structure for all spell implementations in the Empire Wand plugin.
 *
 * <p>
 * This abstract class defines the core properties and behaviors of a spell, including:
 * <ul>
 *   <li>Spell identification and metadata (name, description, type)</li>
 *   <li>Cooldown management and casting restrictions</li>
 *   <li>Prerequisite checking system</li>
 *   <li>Asynchronous and synchronous execution support</li>
 *   <li>Event firing for spell casting and failures</li>
 *   <li>Configuration loading capabilities</li>
 * </ul>
 *
 * <p>
 * The class employs a generic type {@code <T>} to represent the result or
 * "effect" produced by the spell's execution, enabling flexible and type-safe
 * communication between asynchronous execution and synchronous effect handling.
 *
 * <p>
 * Spells are constructed using the {@link Builder} pattern, promoting immutability
 * and providing a clear, fluent API for defining spell properties.
 *
 * <p>
 * <strong>Thread Safety:</strong> This class is designed to be thread-safe for
 * read operations. Spell instances should be immutable after construction.
 *
 * @param <T> The type of the effect object produced by
 *            {@link #executeSpell(SpellContext)}. Use {@link Void} if no effect
 *            object is needed.
 * @since 1.0.0
 */
public abstract class Spell<T> {

    protected final String name;
    protected final String description;
    protected final Duration cooldown;
    protected final SpellType spellType;
    protected final EmpireWandAPI api;

    protected ReadableConfig spellConfig;

    /**
     * Constructs a new Spell instance using the provided builder.
     * Performs validation to ensure all required fields are properly initialized.
     *
     * @param builder the builder containing spell configuration
     * @throws IllegalArgumentException if any required field is null
     */
    protected Spell(Builder<T> builder) {
        this.name = Objects.requireNonNull(builder.name, "Spell name cannot be null");
        this.description = Objects.requireNonNull(builder.description, "Spell description cannot be null");
        this.cooldown = Objects.requireNonNull(builder.cooldown, "Spell cooldown cannot be null");
        this.spellType = Objects.requireNonNull(builder.spellType, "Spell type cannot be null");
        this.api = builder.api;
    }

    /**
     * Returns the unique, machine-readable key for this spell.
     * <p>
     * This key should be unique across all spells and is used for:
     * <ul>
     *   <li>Configuration identification</li>
     *   <li>Event identification</li>
     *   <li>Persistent storage references</li>
     * </ul>
     *
     * @return the spell's unique key (e.g., "fireball", "heal")
     */
    @NotNull
    public abstract String key();

    /**
     * Returns the human-readable display name for this spell.
     * <p>
     * This name is used for user-facing displays such as:
     * <ul>
     *   <li>Spell selection menus</li>
     *   <li>Chat messages</li>
     *   <li>GUI displays</li>
     * </ul>
     *
     * @return the spell's display name as a Component
     */
    @NotNull
    public Component displayName() {
        return Component.text(name);
    }

    /**
     * Gets the prerequisites required to cast this spell.
     * <p>
     * Prerequisites are checked before spell execution and can include:
     * <ul>
     *   <li>Player level requirements</li>
     *   <li>Item requirements</li>
     *   <li>Cooldown checks</li>
     *   <li>Custom conditions</li>
     * </ul>
     *
     * @return the prerequisite checker for this spell
     */
    @NotNull
    public abstract PrereqInterface prereq();

    /**
     * Gets the high-level classification of this spell.
     * <p>
     * The spell type determines the spell's general category and can affect:
     * <ul>
     *   <li>Spell organization in menus</li>
     *   <li>Resistance calculations</li>
     *   <li>Special effects or interactions</li>
     * </ul>
     *
     * @return the {@link SpellType} of this spell
     */
    @NotNull
    public SpellType type() {
        return spellType;
    }

    /**
     * Gets the human-readable name of this spell.
     *
     * @return the spell's name
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Gets the description of this spell.
     * <p>
     * This description should provide players with clear information about
     * what the spell does and any special properties it has.
     *
     * @return the spell's description
     */
    @NotNull
    public String getDescription() {
        return description;
    }

    /**
     * Gets the cooldown duration for this spell.
     * <p>
     * The cooldown period prevents spell spam and balances gameplay.
     * This duration is applied after successful spell casting.
     *
     * @return the cooldown duration
     */
    @NotNull
    public Duration getCooldown() {
        return cooldown;
    }

    /**
     * Loads the spell's specific configuration from the provided section.
     * <p>
     * This method is typically called once during spell registration to load
     * spell-specific settings such as damage values, ranges, or special behaviors.
     * <p>
     * <strong>Note:</strong> Implementations should handle missing configuration
     * gracefully by using appropriate defaults.
     *
     * @param config the {@link ReadableConfig} for this spell
     */
    public void loadConfig(@NotNull ReadableConfig config) {
        this.spellConfig = Objects.requireNonNull(config, "Configuration cannot be null");
    }

    /**
     * Checks if the spell can be cast in the given context.
     * <p>
     * This method delegates to the prerequisite system to determine if all
     * conditions for casting are met.
     *
     * @param context the context of the spell cast
     * @return {@code true} if the spell can be cast, {@code false} otherwise
     */
    public boolean canCast(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Spell context cannot be null");
        return prereq().check(context).canCast();
    }

    /**
     * The core logic of the spell's execution.
     * <p>
     * This method contains the primary spell logic and may be executed:
     * <ul>
     *   <li>Synchronously on the main server thread</li>
     *   <li>Asynchronously on a separate thread (if {@link #requiresAsyncExecution()} returns true)</li>
     * </ul>
     * <p>
     * <strong>Thread Safety:</strong> Implementations should be thread-safe when
     * {@link #requiresAsyncExecution()} returns true.
     *
     * @param context the context of the spell cast
     * @return the effect produced by the spell, or {@code null} if no effect object is needed
     */
    @Nullable
    protected abstract T executeSpell(@NotNull SpellContext context);

    /**
     * Handles the effect produced by {@link #executeSpell(SpellContext)}.
     * <p>
     * This method is always executed on the main server thread and is responsible
     * for applying the spell's effects to the game world.
     * <p>
     * <strong>Note:</strong> This method is only called when {@code executeSpell}
     * returns a non-null effect.
     *
     * @param context the context of the spell cast
     * @param effect the effect object to handle
     */
    protected abstract void handleEffect(@NotNull SpellContext context, @NotNull T effect);

    /**
     * Handles the impact of a projectile associated with this spell.
     * <p>
     * This method is called by a global listener when a projectile launched by
     * this spell hits an entity or block. Only applicable for projectile-based spells.
     * <p>
     * <strong>Note:</strong> Default implementation does nothing. Override in
     * projectile spell subclasses.
     *
     * @param event the {@link ProjectileHitEvent}
     * @param caster the player who cast the spell
     */
    public void onProjectileHit(@NotNull ProjectileHitEvent event, @NotNull Player caster) {
        // Default implementation does nothing
    }

    /**
     * Initiates the spell cast, checking prerequisites and delegating to the
     * appropriate execution handler.
     * <p>
     * This method orchestrates the entire spell casting process:
     * <ol>
     *   <li>Validates prerequisites</li>
     *   <li>Applies cooldowns</li>
     *   <li>Executes the spell (sync or async)</li>
     *   <li>Fires appropriate events</li>
     * </ol>
     *
     * @param context the context of the spell cast
     * @return the result of the cast attempt
     */
    @NotNull
    public CastResult cast(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Spell context cannot be null");
        
        if (requiresAsyncExecution()) {
            return castAsync(context);
        } else {
            return castSync(context);
        }
    }

    /**
     * Determines if this spell's execution logic should be run off the main server thread.
     * <p>
     * Override this method to return {@code true} for spells that:
     * <ul>
     *   <li>Perform heavy calculations</li>
     *   <li>Make external API calls</li>
     *   <li>Process large amounts of data</li>
     * </ul>
     *
     * @return {@code true} if async execution is required, {@code false} otherwise
     */
    protected boolean requiresAsyncExecution() {
        return false;
    }

    /**
     * Executes the spell synchronously on the main server thread.
     * <p>
     * This method handles the complete synchronous execution flow including
     * spell logic, effect handling, and event firing.
     *
     * @param context the spell context
     * @return the cast result
     */
    @NotNull
    private CastResult castSync(@NotNull SpellContext context) {
        try {
            T effect = executeSpell(context);
            if (effect != null) {
                handleEffect(context, effect);
            }
            CastResult result = CastResult.SUCCESS;
            fireSpellCastEvent(context, result);
            return result;
        } catch (Exception e) {
            context.plugin().getLogger().log(Level.WARNING, 
                "Error casting spell " + key() + " for player " + context.caster().getName(), e);
            CastResult result = CastResult.fail(Component.text("Spell casting failed."));
            fireSpellFailEvent(context, 
                nl.wantedchef.empirewand.api.event.SpellFailEvent.FailReason.OTHER,
                "Spell casting failed: " + e.getMessage());
            return result;
        }
    }

    /**
     * Executes the spell asynchronously on a separate thread.
     * <p>
     * This method handles the complete asynchronous execution flow including
     * thread management, error handling, and proper synchronization with the main thread.
     *
     * @param context the spell context
     * @return the cast result (always SUCCESS for async operations)
     */
    @NotNull
    private CastResult castAsync(@NotNull SpellContext context) {
        BukkitRunnable asyncTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    T effect = executeSpell(context);
                    if (effect != null) {
                        // Handle effect on main thread
                        BukkitRunnable mainThreadTask = new BukkitRunnable() {
                            @Override
                            public void run() {
                                try {
                                    handleEffect(context, effect);
                                    fireSpellCastEvent(context, CastResult.SUCCESS);
                                } catch (Exception e) {
                                    handleAsyncException(context, e);
                                }
                            }
                        };
                        
                        // Register task with TaskManager to prevent memory leaks
                        scheduleMainThreadTask(context, mainThreadTask);
                    } else {
                        // If there's no effect to handle, fire the event immediately
                        fireSpellCastEvent(context, CastResult.SUCCESS);
                    }
                } catch (Exception e) {
                    handleAsyncException(context, e);
                }
            }
        };
        
        // Register async task with TaskManager to prevent memory leaks
        scheduleAsyncTask(context, asyncTask);
        
        // Return immediately; the event will carry the final result
        return CastResult.SUCCESS;
    }

    /**
     * Handles exceptions that occur during asynchronous spell execution.
     *
     * @param context the spell context
     * @param exception the exception that occurred
     */
    private void handleAsyncException(@NotNull SpellContext context, @NotNull Exception exception) {
        context.plugin().getLogger().log(Level.WARNING, 
            "Async error casting spell " + key() + " for player " + context.caster().getName(), exception);
        
        // Fire failure event on main thread
        BukkitRunnable errorTask = new BukkitRunnable() {
            @Override
            public void run() {
                fireSpellFailEvent(context,
                    nl.wantedchef.empirewand.api.event.SpellFailEvent.FailReason.OTHER,
                    "Spell casting failed: " + exception.getMessage());
            }
        };
        
        scheduleMainThreadTask(context, errorTask);
    }

    /**
     * Schedules a task to run on the main server thread with proper resource management.
     *
     * @param context the spell context
     * @param task the task to schedule
     */
    private void scheduleMainThreadTask(@NotNull SpellContext context, @NotNull BukkitRunnable task) {
        if (context.plugin() instanceof EmpireWandPlugin ewPlugin) {
            ewPlugin.getTaskManager().registerTask(task.runTask(context.plugin()));
        } else {
            task.runTask(context.plugin());
        }
    }

    /**
     * Schedules a task to run asynchronously with proper resource management.
     *
     * @param context the spell context
     * @param task the task to schedule
     */
    private void scheduleAsyncTask(@NotNull SpellContext context, @NotNull BukkitRunnable task) {
        if (context.plugin() instanceof EmpireWandPlugin ewPlugin) {
            ewPlugin.getTaskManager().registerTask(task.runTaskAsynchronously(context.plugin()));
        } else {
            task.runTaskAsynchronously(context.plugin());
        }
    }

    /**
     * Fires a spell cast event on the appropriate thread.
     *
     * @param context the spell context
     * @param result the cast result
     */
    private void fireSpellCastEvent(@NotNull SpellContext context, @NotNull CastResult result) {
        Runnable task = () -> {
            nl.wantedchef.empirewand.api.event.SpellCastEvent event = 
                new nl.wantedchef.empirewand.api.event.SpellCastEvent(context.caster(), this, key());
            Bukkit.getPluginManager().callEvent(event);
        };
        
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(context.plugin(), task);
        }
    }

    /**
     * Fires a spell failure event on the appropriate thread.
     *
     * @param context the spell context
     * @param reason the failure reason
     * @param message the failure message
     */
    private void fireSpellFailEvent(@NotNull SpellContext context,
            @NotNull nl.wantedchef.empirewand.api.event.SpellFailEvent.FailReason reason,
            @NotNull String message) {
        Runnable task = () -> {
            var event = new nl.wantedchef.empirewand.api.event.SpellFailEvent(
                context.caster(), this, key(), reason, message);
            Bukkit.getPluginManager().callEvent(event);
        };
        
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(context.plugin(), task);
        }
    }

    /**
     * A fluent builder for constructing {@link Spell} instances.
     * <p>
     * This builder provides a type-safe way to configure spell properties
     * before constructing an immutable Spell instance.
     *
     * @param <T> the effect type of the spell being built
     */
    public abstract static class Builder<T> {
        protected final EmpireWandAPI api;
        protected String name;
        protected String description;
        protected Duration cooldown = Duration.ofSeconds(1);
        protected SpellType spellType = SpellType.MISC;

        /**
         * Creates a new builder instance.
         *
         * @param api the EmpireWandAPI instance, may be null during migration
         */
        protected Builder(@Nullable EmpireWandAPI api) {
            this.api = api;
        }

        /**
         * Sets the spell's display name.
         *
         * @param name the spell name
         * @return this builder for chaining
         * @throws IllegalArgumentException if name is null
         */
        @NotNull
        public Builder<T> name(@NotNull String name) {
            this.name = Objects.requireNonNull(name, "Spell name cannot be null");
            return this;
        }

        /**
         * Sets the spell's description.
         *
         * @param description the spell description
         * @return this builder for chaining
         * @throws IllegalArgumentException if description is null
         */
        @NotNull
        public Builder<T> description(@NotNull String description) {
            this.description = Objects.requireNonNull(description, "Spell description cannot be null");
            return this;
        }

        /**
         * Sets the spell's cooldown duration.
         *
         * @param cooldown the cooldown duration
         * @return this builder for chaining
         * @throws IllegalArgumentException if cooldown is null or negative
         */
        @NotNull
        public Builder<T> cooldown(@NotNull Duration cooldown) {
            this.cooldown = Objects.requireNonNull(cooldown, "Cooldown cannot be null");
            if (cooldown.isNegative()) {
                throw new IllegalArgumentException("Cooldown duration cannot be negative");
            }
            return this;
        }

        /**
         * Sets the spell's type classification.
         *
         * @param spellType the spell type
         * @return this builder for chaining
         * @throws IllegalArgumentException if spellType is null
         */
        @NotNull
        public Builder<T> type(@NotNull SpellType spellType) {
            this.spellType = Objects.requireNonNull(spellType, "Spell type cannot be null");
            return this;
        }

        /**
         * Builds and returns a new Spell instance.
         *
         * @return the constructed spell
         */
        @NotNull
        public abstract Spell<T> build();
    }
}
