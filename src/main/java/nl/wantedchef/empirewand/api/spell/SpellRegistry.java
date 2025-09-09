package nl.wantedchef.empirewand.api.spell;

import nl.wantedchef.empirewand.api.EmpireWandService;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.spell.Spell;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SpellRegistry extends EmpireWandService {

    @NotNull
    Optional<Spell<?>> getSpell(@NotNull String key);

    @NotNull
    Map<String, Spell<?>> getAllSpells();

    @NotNull
    Set<String> getSpellKeys();

    boolean isSpellRegistered(@NotNull String key);

    @NotNull
    String getSpellDisplayName(@NotNull String key);

    @NotNull
    SpellBuilder createSpell(@NotNull String key);

    boolean registerSpell(@NotNull Spell<?> spell);

    boolean unregisterSpell(@NotNull String key);

    @NotNull
    Optional<SpellMetadata> getSpellMetadata(@NotNull String key);

    boolean updateSpellMetadata(@NotNull String key, @NotNull SpellMetadata metadata);

    @NotNull
    Set<String> getSpellCategories();

    @NotNull
    Set<String> getSpellsByCategory(@NotNull String category);

    @NotNull
    Set<String> getSpellsByTag(@NotNull String tag);

    @NotNull
    Set<String> getSpellTags();

    @NotNull
    List<Spell<?>> findSpells(@NotNull SpellQuery query);

    @NotNull
    SpellQuery.Builder createQuery();

    int getSpellCount();

    int getSpellCountByCategory(@NotNull String category);

    int getEnabledSpellCount();

    // Toggleable spell methods

    /**
     * Gets a toggleable spell by its key.
     * 
     * @param key the spell key
     * @return an Optional containing the ToggleableSpell if found and is
     *         toggleable, empty otherwise
     */
    @NotNull
    Optional<ToggleableSpell> getToggleableSpell(@NotNull String key);

    /**
     * Gets all toggleable spells registered in this registry.
     * 
     * @return a map of spell keys to ToggleableSpell instances
     */
    @NotNull
    Map<String, ToggleableSpell> getAllToggleableSpells();

    /**
     * Gets the keys of all toggleable spells.
     * 
     * @return a set of toggleable spell keys
     */
    @NotNull
    Set<String> getToggleableSpellKeys();

    /**
     * Checks if a spell is toggleable.
     * 
     * @param key the spell key
     * @return true if the spell exists and is toggleable, false otherwise
     */
    boolean isToggleableSpell(@NotNull String key);

    /**
     * Gets the count of toggleable spells.
     * 
     * @return the number of toggleable spells
     */
    int getToggleableSpellCount();

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
        Spell<?> build();
    }

    @FunctionalInterface
    interface SpellExecutor {
        void execute(@NotNull SpellContext context);
    }

    @FunctionalInterface
    interface SpellValidator {
        boolean validate(@NotNull SpellContext context);
    }

    interface SpellContext {
        // Placeholder
    }

    interface SpellQuery {
        @Nullable
        String getCategory();

        @Nullable
        String getTag();

        @Nullable
        String getNameContains();

        long getMaxCooldown();

        double getMinRange();

        double getMaxRange();

        int getMaxLevelRequirement();

        @Nullable
        Boolean isEnabled();

        @Nullable
        SortField getSortField();

        @Nullable
        SortOrder getSortOrder();

        int getLimit();

        List<Spell<?>> execute();

        enum SortField {
            NAME, COOLDOWN, RANGE, LEVEL_REQUIREMENT, CATEGORY
        }

        enum SortOrder {
            ASCENDING, DESCENDING
        }

        interface Builder {
            @NotNull
            Builder category(@NotNull String category);

            @NotNull
            Builder tag(@NotNull String tag);

            @NotNull
            Builder nameContains(@NotNull String text);

            @NotNull
            Builder cooldown(long maxTicks);

            @NotNull
            Builder range(double min, double max);

            @NotNull
            Builder levelRequirement(int maxLevel);

            @NotNull
            Builder enabled(boolean enabled);

            @NotNull
            Builder sortBy(@NotNull SortField field);

            @NotNull
            Builder sortOrder(@NotNull SortOrder order);

            @NotNull
            Builder limit(int limit);

            @NotNull
            SpellQuery build();
        }
    }
}
