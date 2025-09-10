package nl.wantedchef.empirewand.spell.base;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Utility class for working with SpellType enums.
 * Provides methods to resolve spell types from keys and get valid type names.
 */
public final class SpellTypes {

    private SpellTypes() {
        // Utility class
    }

    /**
     * Resolves a SpellType from a spell key.
     * Uses common naming patterns to determine the type.
     *
     * @param spellKey the spell key to resolve
     * @return the resolved SpellType, or null if not found
     */
    public static SpellType resolveTypeFromKey(String spellKey) {
        if (spellKey == null || spellKey.trim().isEmpty()) {
            return null;
        }

        String key = spellKey.toLowerCase(Locale.ENGLISH);

        // Check for exact matches first
        for (SpellType type : SpellType.values()) {
            if (key.contains(type.name().toLowerCase(Locale.ENGLISH))) {
                return type;
            }
        }

        // Check for common patterns
        if (key.contains("lightning") || key.contains("spark") || key.contains("bolt") ||
                key.contains("thunder") || key.contains("chain") || key.contains("solar")) {
            return SpellType.LIGHTNING;
        }

        if (key.contains("poison") || key.contains("toxic") || key.contains("venom") ||
                key.contains("mephidic") || key.contains("crimson")) {
            return SpellType.POISON;
        }

        if (key.contains("life") || key.contains("blood") || key.contains("vampir") ||
                key.contains("drain") || key.contains("steal") || key.contains("reap")) {
            return SpellType.LIFE;
        }

        if (key.contains("heal") || key.contains("god") || key.contains("radiant") ||
                key.contains("beacon")) {
            return SpellType.HEAL;
        }

        if (key.contains("dark") || key.contains("shadow") || key.contains("void") ||
                key.contains("ritual") || key.contains("unmaking")) {
            return SpellType.DARK;
        }

        if (key.contains("fire") || key.contains("flame") || key.contains("blaze") ||
                key.contains("explosive") || key.contains("comet") || key.contains("fireball")) {
            return SpellType.FIRE;
        }

        if (key.contains("ice") || key.contains("frost") || key.contains("glacial") ||
                key.contains("nova")) {
            return SpellType.ICE;
        }

        if (key.contains("earth") || key.contains("quake") || key.contains("grasp") ||
                key.contains("lightwall")) {
            return SpellType.EARTH;
        }

        if (key.contains("weather") || key.contains("gust") || key.contains("tornado")) {
            return SpellType.WEATHER;
        }

        if (key.contains("movement") || key.contains("teleport") || key.contains("blink") ||
                key.contains("escape") || key.contains("leap") || key.contains("sunburst")) {
            return SpellType.MOVEMENT;
        }

        if (key.contains("projectile") || key.contains("orb") || key.contains("missile")) {
            return SpellType.PROJECTILE;
        }

        if (key.contains("aura") || key.contains("empire")) {
            return SpellType.AURA;
        }

        if (key.contains("control") || key.contains("polymorph") || key.contains("confuse") ||
                key.contains("stasis")) {
            return SpellType.CONTROL;
        }

        // Default to MISC for unrecognized patterns
        return SpellType.MISC;
    }

    /**
     * Returns a list of all valid spell type names.
     *
     * @return list of valid type names in lowercase
     */
    public static List<String> validTypeNames() {
        return Arrays.stream(SpellType.values())
                .map(type -> type.name().toLowerCase(Locale.ENGLISH))
                .collect(Collectors.toList());
    }
}





