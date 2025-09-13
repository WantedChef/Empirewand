package nl.wantedchef.empirewand.spell;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Enumeration of high-level spell types used for categorization and organization.
 * <p>
 * This enum defines the primary categories for spells in the Empire Wand plugin.
 * Spell types are used for:
 * <ul>
 *   <li>Spell organization in menus and GUIs</li>
 *   <li>Type-based spell binding and selection</li>
 *   <li>Resistance and vulnerability calculations</li>
 *   <li>Command filtering (e.g., /ew bindtype)</li>
 *   <li>Grouping spells for configuration purposes</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Examples:</strong>
 * <pre>{@code
 * // Set spell type in builder
 * Spell spell = new Fireball.Builder(api)
 *     .type(SpellType.FIRE)
 *     .build();
 * 
 * // Filter spells by type
 * List<Spell> fireSpells = spellRegistry.getSpellsByType(SpellType.FIRE);
 * 
 * // Check spell type for resistance calculations
 * if (spell.type() == SpellType.LIGHTNING) {
 *     damage *= target.getLightningResistance();
 * }
 * }</pre>
 *
 * <p>
 * <strong>Thread Safety:</strong> Enum values are immutable and thread-safe.
 *
 * @since 1.0.0
 */
public enum SpellType {
    /** Spells that harness electrical energy and lightning effects */
    LIGHTNING,
    
    /** Spells that deal poison damage or apply poison effects */
    POISON,
    
    /** Spells that manipulate life force, blood magic, or vitality */
    LIFE,
    
    /** Spells that restore health or provide healing effects */
    HEAL,
    
    /** Spells that use dark magic, shadow, or necromantic energies */
    DARK,
    
    /** Spells that create fire, heat, or explosive effects */
    FIRE,
    
    /** Spells that create ice, frost, or freezing effects */
    ICE,
    
    /** Spells that manipulate earth, stone, or terrain */
    EARTH,
    
    /** Spells that control weather, wind, or atmospheric conditions */
    WEATHER,
    
    /** Spells that provide teleportation, movement, or mobility effects */
    MOVEMENT,
    
    /** Spells that launch physical projectiles or magical orbs */
    PROJECTILE,
    
    /** Spells that create persistent area effects or protective barriers */
    AURA,
    
    /** Spells that control or manipulate other entities or objects */
    CONTROL,
    
    /** Enhanced versions of existing spells with improved effects */
    ENHANCED,
    
    /** Spells that don't fit into other categories or have unique effects */
    MISC;

    /**
     * Gets the display name for this spell type.
     * <p>
     * Returns a human-readable version of the enum name with proper capitalization.
     *
     * @return the display name for this spell type
     */
    public String getDisplayName() {
        String name = name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }

    /**
     * Gets the configuration key for this spell type.
     * <p>
     * Returns the lowercase enum name suitable for use in configuration files.
     *
     * @return the configuration key
     */
    public String getConfigKey() {
        return name().toLowerCase();
    }

    /**
     * Checks if this spell type matches the given string (case-insensitive).
     * <p>
     * Useful for command parsing and configuration loading.
     *
     * @param type the type string to check
     * @return true if the string matches this spell type, false otherwise
     */
    public boolean matches(String type) {
        return type != null && name().equalsIgnoreCase(type.trim());
    }

    /**
     * Gets the spell type from a string representation.
     * <p>
     * Case-insensitive lookup that returns MISC for invalid types.
     *
     * @param type the string representation
     * @return the corresponding SpellType, or MISC if not found
     */
    @NotNull
    public static SpellType fromString(@Nullable String type) {
        if (type == null) {
            return MISC;
        }
        
        try {
            return valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MISC;
        }
    }
}
