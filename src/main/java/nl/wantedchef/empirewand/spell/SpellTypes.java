package nl.wantedchef.empirewand.spell;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for working with {@link SpellType} enums.
 * <p>
 * This class provides comprehensive utility methods for spell type resolution,
 * validation, and conversion. It includes intelligent key-to-type mapping based
 * on common naming patterns and provides cached lookups for performance.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Spell key to type resolution</li>
 *   <li>String to type conversion with fuzzy matching</li>
 *   <li>Validation of spell type names</li>
 *   <li>Cached lookups for performance</li>
 *   <li>Comprehensive pattern matching</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Examples:</strong>
 * <pre>{@code
 * // Resolve spell type from key
 * SpellType type = SpellTypes.resolveTypeFromKey("fireball");
 * 
 * // Validate type name
 * boolean valid = SpellTypes.isValidType("FIRE");
 * 
 * // Get all valid type names
 * Set<String> typeNames = SpellTypes.getValidTypeNames();
 * 
 * // Convert string to type with fallback
 * SpellType type = SpellTypes.fromString("lightning", SpellType.MISC);
 * }</pre>
 *
 * <p>
 * <strong>Thread Safety:</strong> This class is thread-safe and uses caching
 * for improved performance.
 *
 * @since 1.0.0
 */
public final class SpellTypes {

    /** Cache for spell key to type mappings */
    private static final Map<String, SpellType> KEY_CACHE = new ConcurrentHashMap<>();
    
    /** Cache for string to type mappings */
    private static final Map<String, SpellType> STRING_CACHE = new ConcurrentHashMap<>();
    
    /** Predefined keyword mappings for spell type detection */
    private static final Map<SpellType, Set<String>> TYPE_KEYWORDS = Map.ofEntries(
        Map.entry(SpellType.LIGHTNING, Set.of(
            "lightning", "spark", "bolt", "thunder", "chain", "solar", 
            "electric", "shock", "zap", "volt"
        )),
        Map.entry(SpellType.POISON, Set.of(
            "poison", "toxic", "venom", "mephidic", "crimson", "acid", 
            "contagion", "plague", "bane"
        )),
        Map.entry(SpellType.LIFE, Set.of(
            "life", "blood", "vampir", "hemo", "sanguine", "vitality", 
            "essence", "soul", "spirit"
        )),
        Map.entry(SpellType.HEAL, Set.of(
            "heal", "cure", "mend", "restore", "regenerate", "radiant", 
            "beacon", "sanctuary", "bless"
        )),
        Map.entry(SpellType.DARK, Set.of(
            "dark", "shadow", "void", "abyss", "black", "necro", 
            "death", "grim", "sinister"
        )),
        Map.entry(SpellType.FIRE, Set.of(
            "fire", "flame", "heat", "burn", "blaze", "inferno", 
            "magma", "lava", "scorch", "ember"
        )),
        Map.entry(SpellType.ICE, Set.of(
            "ice", "frost", "freeze", "chill", "glacial", "blizzard", 
            "snow", "arctic", "cryo", "rime"
        )),
        Map.entry(SpellType.EARTH, Set.of(
            "earth", "stone", "rock", "geo", "terrain", "seismic", 
            "quake", "land", "ground", "crystal"
        )),
        Map.entry(SpellType.WEATHER, Set.of(
            "weather", "wind", "gust", "storm", "tornado", "hurricane", 
            "rain", "cloud", "sky", "atmospheric"
        )),
        Map.entry(SpellType.MOVEMENT, Set.of(
            "move", "teleport", "blink", "leap", "dash", "step", 
            "warp", "shift", "escape", "translocate"
        )),
        Map.entry(SpellType.PROJECTILE, Set.of(
            "projectile", "orb", "missile", "dart", "bolt", "shot", 
            "launch", "throw", "cast"
        )),
        Map.entry(SpellType.AURA, Set.of(
            "aura", "field", "barrier", "shield", "ward", "veil", 
            "cloak", "shroud", "mantle"
        )),
        Map.entry(SpellType.CONTROL, Set.of(
            "control", "confuse", "polymorph", "dominate", "compel", 
            "charm", "bind", "ensnare", "enthrall"
        ))
    );

    /** Cached unmodifiable set of all spell type names */
    private static final Set<String> VALID_TYPE_NAMES;

    /** Cached unmodifiable list of all spell type names */
    private static final List<String> VALID_TYPE_NAMES_LIST;

    /** Cached unmodifiable list of all spell types */
    private static final List<SpellType> ALL_TYPES;

    static {
        VALID_TYPE_NAMES = Arrays.stream(SpellType.values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());
        
        VALID_TYPE_NAMES_LIST = VALID_TYPE_NAMES.stream().toList();

        ALL_TYPES = List.of(SpellType.values());
    }

    /** Private constructor to prevent instantiation */
    private SpellTypes() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Resolves a SpellType from a spell key using intelligent pattern matching.
     * <p>
     * This method uses multiple strategies to determine the spell type:
     * <ol>
     *   <li>Exact keyword matching from predefined sets</li>
     *   <li>Substring matching within the spell key</li>
     *   <li>Caching for improved performance</li>
     * </ol>
     *
     * @param spellKey the spell key to resolve (e.g., "fireball", "lightning_bolt")
     * @return the resolved SpellType, or MISC if no match is found
     */
    @NotNull
    public static SpellType resolveTypeFromKey(@Nullable String spellKey) {
        if (spellKey == null || spellKey.trim().isEmpty()) {
            return SpellType.MISC;
        }

        String key = spellKey.toLowerCase(Locale.ENGLISH).trim();
        
        // Check cache first
        SpellType cached = KEY_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        // Check for exact matches in keywords
        for (Map.Entry<SpellType, Set<String>> entry : TYPE_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (key.contains(keyword)) {
                    KEY_CACHE.put(key, entry.getKey());
                    return entry.getKey();
                }
            }
        }

        // Check for exact enum name matches
        for (SpellType type : SpellType.values()) {
            if (key.contains(type.name().toLowerCase(Locale.ENGLISH))) {
                KEY_CACHE.put(key, type);
                return type;
            }
        }

        // Default to MISC
        KEY_CACHE.put(key, SpellType.MISC);
        return SpellType.MISC;
    }

    /**
     * Converts a string to a SpellType with case-insensitive matching.
     * <p>
     * This method attempts to match the input string against enum names and
     * falls back to {@link #resolveTypeFromKey} for more flexible matching.
     *
     * @param type the string to convert
     * @return the corresponding SpellType, or MISC if no match is found
     */
    @NotNull
    public static SpellType fromString(@Nullable String type) {
        return fromString(type, SpellType.MISC);
    }

    /**
     * Converts a string to a SpellType with case-insensitive matching and fallback.
     * <p>
     * This method attempts to match the input string against enum names and
     * falls back to {@link #resolveTypeFromKey} for more flexible matching.
     *
     * @param type the string to convert
     * @param fallback the fallback type to return if no match is found
     * @return the corresponding SpellType, or the fallback if no match is found
     * @throws NullPointerException if fallback is null
     */
    @NotNull
    public static SpellType fromString(@Nullable String type, @NotNull SpellType fallback) {
        Objects.requireNonNull(fallback, "Fallback cannot be null");
        
        if (type == null || type.trim().isEmpty()) {
            return fallback;
        }

        String normalized = type.trim().toUpperCase(Locale.ENGLISH);
        
        // Check cache first
        SpellType cached = STRING_CACHE.get(normalized);
        if (cached != null) {
            return cached;
        }

        // Try exact enum match
        try {
            SpellType result = SpellType.valueOf(normalized);
            STRING_CACHE.put(normalized, result);
            return result;
        } catch (IllegalArgumentException e) {
            // Fall through to key-based resolution
        }

        // Try key-based resolution
        SpellType result = resolveTypeFromKey(normalized.toLowerCase(Locale.ENGLISH));
        STRING_CACHE.put(normalized, result);
        return result;
    }

    /**
     * Validates if a string is a valid spell type name.
     * <p>
     * Checks against both enum names and common variations.
     *
     * @param type the string to validate
     * @return true if the string represents a valid spell type, false otherwise
     */
    public static boolean isValidType(@Nullable String type) {
        if (type == null || type.trim().isEmpty()) {
            return false;
        }

        String normalized = type.trim().toUpperCase(Locale.ENGLISH);
        
        try {
            SpellType.valueOf(normalized);
            return true;
        } catch (IllegalArgumentException e) {
            return resolveTypeFromKey(normalized.toLowerCase(Locale.ENGLISH)) != SpellType.MISC;
        }
    }

    /**
     * Gets all valid spell type names.
     * <p>
     * Returns a cached, unmodifiable set of all enum names in uppercase.
     *
     * @return an unmodifiable set of valid spell type names
     */
    @NotNull
    public static Set<String> getValidTypeNames() {
        return VALID_TYPE_NAMES;
    }

    /**
     * Backward-compatible alias used by existing commands.
     * <p>
     * Returns a cached, unmodifiable list of all enum names in uppercase.
     *
     * @return a list of valid spell type names
     */
    @NotNull
    public static List<String> validTypeNames() {
        return VALID_TYPE_NAMES_LIST;
    }

    /**
     * Gets all spell types as a list.
     * <p>
     * Returns a cached, unmodifiable list of all enum values in their natural order.
     *
     * @return an unmodifiable list of all spell types
     */
    @NotNull
    public static List<SpellType> getAllTypes() {
        return ALL_TYPES;
    }

    /**
     * Clears all internal caches.
     * <p>
     * This method is primarily intended for testing purposes or when
     * configuration changes require cache invalidation.
     */
    public static void clearCache() {
        KEY_CACHE.clear();
        STRING_CACHE.clear();
    }

    /**
     * Gets the cache statistics for debugging purposes.
     * <p>
     * Returns a string representation of current cache sizes.
     *
     * @return cache statistics string
     */
    @NotNull
    public static String getCacheStats() {
        return String.format("SpellTypes Cache Stats - KeyCache: %d, StringCache: %d", 
            KEY_CACHE.size(), STRING_CACHE.size());
    }
}