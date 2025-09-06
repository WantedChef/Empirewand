package com.example.empirewand.spell;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Centralized helper for resolving a SpellType from a spell key.
 * Keeps us from having to touch every spell implementation.
 */
public final class SpellTypes {
    private static final List<String> LIGHTNING_PREFIXES = List.of("lightning-", "thunder-", "spark", "little-spark", "solar-lance");
    private static final List<String> POISON_PREFIXES = List.of("poison-", "mephidic-", "crimson-chains", "soul-sever");
    private static final List<String> LIFE_PREFIXES = List.of("blood-", "life-", "hemorrhage");
    private static final List<String> HEAL_PREFIXES = List.of("heal", "radiant-beacon", "god-cloud", "blood-barrier");
    private static final List<String> DARK_PREFIXES = List.of("dark-", "shadow-");

    private SpellTypes() {}

    public static SpellType resolveTypeFromKey(String key) {
        if (key == null) return SpellType.MISC;
        String k = key.toLowerCase(Locale.ROOT);
        if (startsWithAny(k, LIGHTNING_PREFIXES)) return SpellType.LIGHTNING;
        if (startsWithAny(k, POISON_PREFIXES)) return SpellType.POISON;
        if (startsWithAny(k, LIFE_PREFIXES)) return SpellType.LIFE;
        if (startsWithAny(k, HEAL_PREFIXES)) return SpellType.HEAL;
        if (startsWithAny(k, DARK_PREFIXES)) return SpellType.DARK;
        return SpellType.MISC;
    }

    public static List<String> validTypeNames() {
        return Collections.unmodifiableList(Arrays.asList(
            "lightning", "poison", "life", "heal", "dark"
        ));
    }

    private static boolean startsWithAny(String key, List<String> prefixes) {
        for (String p : prefixes) {
            if (key.startsWith(p)) return true;
        }
        return false;
    }
}

