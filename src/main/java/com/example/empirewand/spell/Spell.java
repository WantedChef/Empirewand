package com.example.empirewand.spell;

import net.kyori.adventure.text.Component;
import java.time.Duration;

public interface Spell {
    // Legacy method for backward compatibility
    void execute(SpellContext context);

    // New harmonized spell framework methods
    default boolean canCast(SpellContext context) {
        return prereq().canCast();
    }

    default void applyCost(SpellContext context) {
        // Default: no cost
    }

    default Duration getCooldown(SpellContext context) {
        return Duration.ofMillis(context.config().getSpellsConfig().getLong(key() + ".cooldown", 1000L));
    }

    default CastResult cast(SpellContext context) {
        execute(context);
        return CastResult.SUCCESS;
    }

    // Existing methods
    String getName();

    String key();

    Component displayName();

    Prereq prereq();

    /**
     * Optional categorization of a spell. Defaults to automatic resolution from
     * key.
     */
    default SpellType type() {
        return SpellTypes.resolveTypeFromKey(key());
    }
}
