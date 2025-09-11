package nl.wantedchef.empirewand.spell;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.event.SpellCastEvent;
import nl.wantedchef.empirewand.api.service.CooldownService;
import java.time.Duration;
import java.util.logging.Level;
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
 * structure for all spell implementations.
 *
 * <p>
 * This class defines the core properties and behaviors of a spell, such as its
 * name,
 * cooldown, and casting logic. It employs a generic type {@code <T>} to
 * represent the result or
 * "effect" produced by the spell's execution, allowing for flexible and
 * type-safe communication
 * between asynchronous execution and synchronous effect handling.
 *
 * <p>
 * Spells are constructed using a {@link Builder}, promoting immutability and a
 * clear, fluent API for
 * defining spell properties.
 *
 * @param <T> The type of the effect object produced by
 *            {@link #executeSpell(SpellContext)}. Use
 *            {@link Void} if no effect object is needed.
 */
public abstract class Spell<T> {

    protected final String name;
    protected final String description;
    protected final Duration cooldown;
    protected final SpellType spellType;
    protected final EmpireWandAPI api;

    protected ReadableConfig spellConfig;

    protected Spell(Builder<T> builder) {
        // API may be null during migration of builders; prefer service lookups from
        // context or static API access where needed.
        if (builder.name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        if (builder.description == null) {
            throw new IllegalArgumentException("Description cannot be null");
        }
        if (builder.cooldown == null) {
            throw new IllegalArgumentException("Cooldown cannot be null");
        }
        if (builder.spellType == null) {
            throw new IllegalArgumentException("Spell type cannot be null");
        }
        this.name = builder.name;
        this.description = builder.description;
        this.cooldown = builder.cooldown;
        this.spellType = builder.spellType;
        this.api = builder.api;
    }

    /**
     * Returns the unique, machine-readable key for this spell (e.g., "fireball").
     *
     * @return The spell's key.
     */
    @NotNull
    public abstract String key();

    /**
     * Returns the human-readable display name for this spell.
     *
     * @return The spell's display name component.
     */
    @NotNull
    public Component displayName() {
        return Component.text(name);
    }

    /**
     * Gets the prerequisites required to cast this spell.
     *
     * @return The prerequisite checker for this spell.
     */
    @NotNull
    public abstract PrereqInterface prereq();

    /**
     * Gets the high-level classification of this spell.
     *
     * @return The {@link SpellType}.
     */
    @NotNull
    public SpellType type() {
        return spellType;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    @NotNull
    public Duration getCooldown() {
        return cooldown;
    }

    /**
     * Loads the spell's specific configuration from the provided section. This is
     * typically called once
     * during spell registration.
     *
     * @param config The {@link ReadableConfig} for this spell.
     */
    public void loadConfig(@NotNull ReadableConfig config) {
        this.spellConfig = config;
    }

    /**
     * Checks if the spell can be cast in the given context.
     *
     * @param context The context of the spell cast.
     * @return {@code true} if the spell can be cast, otherwise {@code false}.
     */
    public boolean canCast(@NotNull SpellContext context) {
        return prereq().check(context).canCast();
    }

    // Note: No resource cost system is used in this project.

    /**
     * The core logic of the spell's execution. This method may be run
     * asynchronously.
     *
     * @param context The context of the spell cast.
     * @return The effect produced by the spell, or {@code null} if no effect object
     *         is needed.
     */
    @Nullable
    protected abstract T executeSpell(@NotNull SpellContext context);

    /**
     * Handles the effect produced by {@link #executeSpell(SpellContext)}. This
     * method is always run on the
     * main server thread.
     *
     * @param context The context of the spell cast.
     * @param effect  The effect object to handle.
     */
    protected abstract void handleEffect(@NotNull SpellContext context, @NotNull T effect);

    /**
     * Handles the impact of a projectile associated with this spell. This is called
     * by a global listener.
     *
     * @param event  The {@link ProjectileHitEvent}.
     * @param caster The player who cast the spell.
     */
    public void onProjectileHit(@NotNull ProjectileHitEvent event, @NotNull Player caster) {
        // To be implemented by projectile-based spells.
    }

    /**
     * Initiates the spell cast, checking prerequisites, applying costs, and
     * delegating to the
     * appropriate synchronous or asynchronous execution handler.
     *
     * @param context The context of the spell cast.
     * @return The result of the cast attempt.
     */
    @NotNull
    public final CastResult cast(@NotNull SpellContext context) {
        PrereqInterface.CheckResult prereqResult = prereq().check(context);
        if (!prereqResult.success()) {
            return CastResult.fail(prereqResult.reason());
        }

        // Check and apply cooldown
        if (context.caster() instanceof Player player) {
            CooldownCheckResult cooldownResult = checkCooldown(player);
            if (!cooldownResult.isSuccess()) {
                return CastResult.fail(cooldownResult.reason());
            }
            applyCooldown(player);
        }

        if (requiresAsyncExecution()) {
            return castAsync(context);
        } else {
            return castSync(context);
        }
    }

    /**
     * Determines if this spell's execution logic should be run off the main server
     * thread.
     *
     * @return {@code true} if async execution is required, otherwise {@code false}.
     */
    protected boolean requiresAsyncExecution() {
        return false; // Default to synchronous execution.
    }

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
            context.plugin().getLogger().log(Level.WARNING, "Error casting spell " + key(), e);
            CastResult result = CastResult.fail(Component.text("Spell casting failed."));
            fireSpellCastEvent(context, result);
            return result;
        }
    }

    @NotNull
    private CastResult castAsync(@NotNull SpellContext context) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    T effect = executeSpell(context);
                    if (effect != null) {
                        // Handle effect on main thread
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                handleEffect(context, effect);
                                fireSpellCastEvent(context, CastResult.SUCCESS);
                            }
                        }.runTask(context.plugin());
                    } else {
                        // If there's no effect to handle, fire the event immediately.
                        fireSpellCastEvent(context, CastResult.SUCCESS);
                    }
                } catch (Exception e) {
                    context.plugin().getLogger().log(Level.WARNING, "Error casting spell " + key(), e);
                    // Fire failure event on main thread
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            fireSpellCastEvent(context, CastResult.fail(Component.text("Spell casting failed.")));
                        }
                    }.runTask(context.plugin());
                }
            }
        }.runTaskAsynchronously(context.plugin());

        // Return immediately; the event will carry the final result.
        return CastResult.SUCCESS;
    }

    private void fireSpellCastEvent(@NotNull SpellContext context, @NotNull CastResult result) {
        Runnable task = () -> {
            // The API's SpellCastEvent constructor expects (Player, Spell<?>, String).
            // Convert the CastResult to a String for the event payload.
            SpellCastEvent event = new SpellCastEvent(context.caster(), this, result.toString());
            Bukkit.getPluginManager().callEvent(event);
        };
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(context.plugin(), task);
        }
    }

    /**
     * Checks if the spell is on cooldown for the given player.
     *
     * @param player the player to check
     * @return the cooldown result
     */
    protected CooldownCheckResult checkCooldown(@NotNull Player player) {
        CooldownService cooldownService = EmpireWandAPI.getService(CooldownService.class);
        if (cooldownService == null) {
            return CooldownCheckResult.success();
        }
        
        return cooldownService.checkCooldown(player, key(), cooldown);
    }
    
    /**
     * Applies the cooldown for this spell to the given player.
     *
     * @param player the player to apply cooldown to
     */
    protected void applyCooldown(@NotNull Player player) {
        CooldownService cooldownService = EmpireWandAPI.getService(CooldownService.class);
        if (cooldownService != null) {
            cooldownService.setCooldown(player, key(), cooldown);
        }
    }
    
    /**
     * Result of a cooldown check.
     */
    protected static class CooldownCheckResult {
        private final boolean success;
        private final String reason;
        
        private CooldownCheckResult(boolean success, String reason) {
            this.success = success;
            this.reason = reason;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String reason() {
            return reason;
        }
        
        public static CooldownCheckResult success() {
            return new CooldownCheckResult(true, null);
        }
        
        public static CooldownCheckResult fail(String reason) {
            return new CooldownCheckResult(false, reason);
        }
    }

    /**
     * A fluent builder for constructing {@link Spell} instances.
     *
     * @param <T> The effect type of the spell being built.
     */
    public abstract static class Builder<T> {
        protected final EmpireWandAPI api;
        protected String name;
        protected String description;
        protected Duration cooldown = Duration.ofSeconds(1);
        protected SpellType spellType = SpellType.MISC;

        protected Builder(@Nullable EmpireWandAPI api) {
            this.api = api;
        }

        @NotNull
        public Builder<T> name(@NotNull String name) {
            this.name = name;
            return this;
        }

        @NotNull
        public Builder<T> description(@NotNull String description) {
            this.description = description;
            return this;
        }

        // Resource cost system removed entirely.

        @NotNull
        public Builder<T> cooldown(@NotNull Duration cooldown) {
            this.cooldown = cooldown;
            return this;
        }

        @NotNull
        public Builder<T> type(@NotNull SpellType spellType) {
            this.spellType = spellType;
            return this;
        }

        @NotNull
        public abstract Spell<T> build();
    }
}
