package com.example.empirewand.api;

import com.example.empirewand.spell.Spell;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Query interface for searching and filtering spells.
 * Provides a fluent API for building complex spell queries.
 *
 * @since 2.0.0
 */
public interface SpellQuery {

    /**
     * Executes the query and returns matching spells.
     *
     * @return a list of matching spells
     */
    @NotNull
    List<Spell> execute();

    /**
     * Builder for creating SpellQuery instances.
     *
     * @since 2.0.0
     */
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

    /**
     * Sort fields for spell queries.
     *
     * @since 2.0.0
     */
    enum SortField {
        NAME,
        COOLDOWN,
        RANGE,
        LEVEL_REQUIREMENT,
        CATEGORY
    }

    /**
     * Sort order for spell queries.
     *
     * @since 2.0.0
     */
    enum SortOrder {
        ASCENDING,
        DESCENDING
    }
}