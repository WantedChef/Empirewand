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
    private static final List<String> LIGHTNING_PREFIXES = List.of("lightning-", "thunder-", "spark", "little-spark",
            "solar-lance", "chain-lightning", "lightning-");
    private static final List<String> POISON_PREFIXES = List.of("poison-", "mephidic-", "crimson-chains", "soul-sever");
    private static final List<String> LIFE_PREFIXES = List.of("blood-", "life-", "hemorrhage");
    private static final List<String> HEAL_PREFIXES = List.of("heal", "radiant-beacon", "god-cloud", "blood-barrier");
    private static final List<String> DARK_PREFIXES = List.of("dark-", "shadow-");
    private static final List<String> FIRE_PREFIXES = List.of("fireball", "flame-", "blaze-", "explosive",
            "explosion-", "comet", "empire-comet", "comet-shower");
    private static final List<String> ICE_PREFIXES = List.of("frost-", "glacial-", "ice-");
    private static final List<String> EARTH_PREFIXES = List.of("sandstorm", "grasping-", "earth-", "lightwall");
    private static final List<String> WEATHER_PREFIXES = List.of("gust", "tornado");
    private static final List<String> MOVEMENT_PREFIXES = List.of("teleport", "blink-", "sunburst-", "leap",
            "empire-escape");
    private static final List<String> PROJECTILE_PREFIXES = List.of("magic-missile", "arcane-orb");
    private static final List<String> AURA_PREFIXES = List.of("empire-aura", "aura");
    private static final List<String> CONTROL_PREFIXES = List.of("polymorph", "confuse", "stasis-");

    private SpellTypes() {
    }

    public static SpellType resolveTypeFromKey(String key) {
        if (key == null)
            return SpellType.MISC;
        String k = key.toLowerCase(Locale.ROOT);
        if (startsWithAny(k, LIGHTNING_PREFIXES))
            return SpellType.LIGHTNING;
        if (startsWithAny(k, POISON_PREFIXES))
            return SpellType.POISON;
        if (startsWithAny(k, LIFE_PREFIXES))
            return SpellType.LIFE;
        if (startsWithAny(k, HEAL_PREFIXES))
            return SpellType.HEAL;
        if (startsWithAny(k, DARK_PREFIXES))
            return SpellType.DARK;
        if (startsWithAny(k, FIRE_PREFIXES))
            return SpellType.FIRE;
        if (startsWithAny(k, PROJECTILE_PREFIXES))
            return SpellType.PROJECTILE;
        if (startsWithAny(k, ICE_PREFIXES))
            return SpellType.ICE;
        if (startsWithAny(k, EARTH_PREFIXES))
            return SpellType.EARTH;
        if (startsWithAny(k, WEATHER_PREFIXES))
            return SpellType.WEATHER;
        if (startsWithAny(k, MOVEMENT_PREFIXES))
            return SpellType.MOVEMENT;
        if (startsWithAny(k, AURA_PREFIXES))
            return SpellType.AURA;
        if (startsWithAny(k, CONTROL_PREFIXES))
            return SpellType.CONTROL;
        return SpellType.MISC;
    }

    public static List<String> validTypeNames() {
        return Collections.unmodifiableList(Arrays.asList(
                "lightning", "poison", "life", "heal", "dark", "fire", "ice", "earth", "weather", "movement",
                "projectile", "aura", "control"));
    }

    private static boolean startsWithAny(String key, List<String> prefixes) {
        for (String p : prefixes) {
            if (key.startsWith(p))
                return true;
        }
        return false;
    }
}
