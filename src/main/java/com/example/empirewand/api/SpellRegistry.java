package com.example.empirewand.api;

import com.example.empirewand.spell.Spell;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Enhanced API interface for accessing EmpireWand's spell registry.
 *
 * <p>
 * This interface provides comprehensive spell management capabilities including
 * spell creation, modification, metadata management, categorization, and
 * advanced querying.
 * </p>
 *
 * <p>
 * <b>API Stability:</b> Stable - Follows semantic versioning
 * </p>
 *
 * <p>
 * <b>Usage Examples:</b>
 * 
 * <pre>{@code
 * // Get services
 * SpellRegistry registry = EmpireWandAPI.getService(SpellRegistry.class);
 *
 * // Basic spell lookup
 * Optional<Spell> spell = registry.getSpell("magic-missile");
 *
 * // Advanced querying
 * List<Spell> fireSpells = registry.createQuery()
 *         .category("fire")
 *         .manaCost(0, 50)
 *         .enabled(true)
 *         .build()
 *         .execute();
 *
 * // Spell creation
 * Spell newSpell = registry.createSpell("my-spell")
 *         .name("My Custom Spell")
 *         .description("A custom spell")
 *         .category("custom")
 *         .manaCost(25)
 *         .cooldown(100)
 *         .build();
 *
 * registry.registerSpell(newSpell);
 * }</pre>
 *
 * @since 2.0.0
 */
public interface SpellRegistry extends EmpireWandService {

    // ===== EXISTING METHODS (ENHANCED) =====

    /**
     * Gets a spell by its registry key.
     *
     * @param key the spell key (kebab-case)
     * @return the spell instance, or null if not found
     * @deprecated Use {@link #getSpell(String)} returning Optional instead
     */
    @Deprecated(forRemoval = true)
    @Nullable
    Spell getSpellNullable(@NotNull String key);

    /**
     * Gets a spell by its registry key.
     *
     * @param key the spell key (kebab-case)
     * @return an Optional containing the spell instance, or empty if not found
     */
    @NotNull
    Optional<Spell> getSpell(@NotNull String key);

    /**
     * Gets all registered spells.
     *
     * @return an unmodifiable map of spell keys to spell instances
     */
    @NotNull
    Map<String, Spell> getAllSpells();

    /**
     * Gets all registered spell keys.
     *
     * @return an unmodifiable set of spell keys
     */
    @NotNull
    Set<String> getSpellKeys();

    /**
     * Checks if a spell is registered.
     *
     * @param key the spell key to check
     * @return true if the spell is registered, false otherwise
     */
    boolean isSpellRegistered(@NotNull String key);

    /**
     * Gets the display name for a spell.
     *
     * @param key the spell key
     * @return the display name, or the key if not configured
     */
    @NotNull
    String getSpellDisplayName(@NotNull String key);

    // ===== NEW SPELL MANAGEMENT METHODS =====

    /**
     * Creates a new spell builder for constructing custom spells.
     *
     * @param key the unique spell key (kebab-case)
     * @return a new SpellBuilder instance
     * @throws IllegalArgumentException if the key is already registered
     */
    @NotNull
    SpellBuilder createSpell(@NotNull String key);

    /**
     * Registers a spell in the registry.
     *
     * @param spell the spell to register
     * @return true if the spell was registered successfully, false otherwise
     * @throws IllegalArgumentException if a spell with the same key already exists
     */
    boolean registerSpell(@NotNull Spell spell);

    /**
     * Unregisters a spell from the registry.
     *
     * @param key the spell key to unregister
     * @return true if the spell was unregistered successfully, false otherwise
     */
    boolean unregisterSpell(@NotNull String key);

    // ===== SPELL METADATA METHODS =====

    /**
     * Gets the metadata for a spell.
     *
     * @param key the spell key
     * @return an Optional containing the spell metadata, or empty if not found
     */
    @NotNull
    Optional<SpellMetadata> getSpellMetadata(@NotNull String key);

    /**
     * Updates the metadata for a spell.
     *
     * @param key      the spell key
     * @param metadata the new metadata
     * @return true if the metadata was updated successfully, false otherwise
     */
    boolean updateSpellMetadata(@NotNull String key, @NotNull SpellMetadata metadata);

    // ===== CATEGORIZATION AND TAGGING =====

    /**
     * Gets all available spell categories.
     *
     * @return an unmodifiable set of category names
     */
    @NotNull
    Set<String> getSpellCategories();

    /**
     * Gets all spells in a specific category.
     *
     * @param category the category name
     * @return an unmodifiable set of spell keys in the category
     */
    @NotNull
    Set<String> getSpellsByCategory(@NotNull String category);

    /**
     * Gets all spells with a specific tag.
     *
     * @param tag the tag name
     * @return an unmodifiable set of spell keys with the tag
     */
    @NotNull
    Set<String> getSpellsByTag(@NotNull String tag);

    /**
     * Gets all available spell tags.
     *
     * @return an unmodifiable set of tag names
     */
    @NotNull
    Set<String> getSpellTags();

    // ===== ADVANCED QUERYING =====

    /**
     * Finds spells using a query.
     *
     * @param query the spell query
     * @return a list of matching spells
     */
    @NotNull
    List<Spell> findSpells(@NotNull SpellQuery query);

    /**
     * Creates a new spell query builder.
     *
     * @return a new SpellQuery.Builder instance
     */
    @NotNull
    SpellQuery.Builder createQuery();

    // ===== SPELL STATISTICS =====

    /**
     * Gets the total number of registered spells.
     *
     * @return the spell count
     */
    int getSpellCount();

    /**
     * Gets the number of spells in a specific category.
     *
     * @param category the category name
     * @return the spell count in the category
     */
    int getSpellCountByCategory(@NotNull String category);

    /**
     * Gets the number of enabled spells.
     *
     * @return the count of enabled spells
     */
    int getEnabledSpellCount();

    // ===== SPELL BUILDER INTERFACE =====

    /**
     * Builder interface for creating custom spells.
     *
     * @since 2.0.0
     */
    interface SpellBuilder {
        @NotNull
        SpellBuilder name(@NotNull String name);

        @NotNull
        SpellBuilder description(@NotNull String description);

        @NotNull
        SpellBuilder category(@NotNull String category);

        @NotNull
        SpellBuilder tags(@NotNull String... tags);

        @NotNull
        SpellBuilder cooldown(long ticks);

        @NotNull
        SpellBuilder range(double range);

        @NotNull
        SpellBuilder levelRequirement(int level);

        @NotNull
        SpellBuilder enabled(boolean enabled);

        @NotNull
        SpellBuilder iconMaterial(@NotNull String material);

        @NotNull
        SpellBuilder executor(@NotNull SpellExecutor executor);

        @NotNull
        SpellBuilder validator(@NotNull SpellValidator validator);

        @NotNull
        SpellBuilder property(@NotNull String key, @NotNull Object value);

        @NotNull
        Spell build();
    }

    // ===== SPELL EXECUTOR AND VALIDATOR INTERFACES =====

    /**
     * Functional interface for spell execution logic.
     *
     * @since 2.0.0
     */
    @FunctionalInterface
    interface SpellExecutor {
        void execute(@NotNull SpellContext context);
    }

    /**
     * Functional interface for spell validation logic.
     *
     * @since 2.0.0
     */
    @FunctionalInterface
    interface SpellValidator {
        boolean validate(@NotNull SpellContext context);
    }

    // ===== SPELL CONTEXT =====

    /**
     * Context information for spell execution.
     *
     * @since 2.0.0
     */
    interface SpellContext {
        // Context methods would be defined here
        // This is a placeholder for the actual implementation
    }
}