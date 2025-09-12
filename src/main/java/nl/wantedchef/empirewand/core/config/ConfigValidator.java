package nl.wantedchef.empirewand.core.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;


import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Validates configuration files for structure, types, value ranges, and
 * kebab-case spell keys.
 */
public class ConfigValidator {

    private static final Pattern KEBAB_CASE_PATTERN = Pattern.compile("^[a-z]+(-[a-z]+)*$");

    /**
     * Validates the main config.yml file.
     *
     * @param config the configuration to validate
     * @return list of validation errors, empty if valid
     */
    public List<String> validateMainConfig(FileConfiguration config) {
        List<String> errors = new ArrayList<>();

        if (config == null) {
            errors.add("Configuration cannot be null");
            return errors;
        }

        try {
            // Check config-version
            if (!config.contains("config-version")) {
                errors.add("Missing required field: config-version");
            } else {
                Object version = config.get("config-version");
                if (!(version instanceof String || version instanceof Double || version instanceof Integer)) {
                    errors.add("config-version must be a string or number");
                }
            }

            // Validate messages section
            if (config.isConfigurationSection("messages")) {
                ConfigurationSection messages = config.getConfigurationSection("messages");
                if (messages != null) {
                    validateMessageSection(messages, "messages", errors);
                }
            } else if (config.contains("messages")) {
                errors.add("'messages' must be a configuration section.");
            }

            // Validate features section
            if (config.isConfigurationSection("features")) {
                ConfigurationSection features = config.getConfigurationSection("features");
                if (features != null) {
                    Set<String> keys = features.getKeys(false);
                    if (keys != null) {
                        for (String key : keys) {
                            if (key == null || key.trim().isEmpty()) {
                                errors.add("features section contains invalid key");
                                continue;
                            }
                            Object value = features.get(key);
                            if (!(value instanceof Boolean)) {
                                errors.add("features." + key + " must be a boolean");
                            }
                        }
                    }
                }
            } else if (config.contains("features")) {
                errors.add("'features' must be a configuration section.");
            }

            // Validate cooldowns section
            if (config.isConfigurationSection("cooldowns")) {
                ConfigurationSection cooldowns = config.getConfigurationSection("cooldowns");
                if (cooldowns != null) {
                    Set<String> keys = cooldowns.getKeys(false);
                    if (keys != null) {
                        for (String key : keys) {
                            if (key == null || key.trim().isEmpty()) {
                                errors.add("cooldowns section contains invalid key");
                                continue;
                            }
                            Object value = cooldowns.get(key);
                            if (!(value instanceof Number)) {
                                errors.add("cooldowns." + key + " must be a number");
                            } else {
                                long cooldownValue = ((Number) value).longValue();
                                if (cooldownValue < 0) {
                                    errors.add("cooldowns." + key + " must be non-negative");
                                }
                            }
                        }
                    }
                }
            } else if (config.contains("cooldowns")) {
                errors.add("'cooldowns' must be a configuration section.");
            }

            // Validate categories section
            if (config.isConfigurationSection("categories")) {
                ConfigurationSection categories = config.getConfigurationSection("categories");
                if (categories != null) {
                    Set<String> categoryKeys = categories.getKeys(false);
                    if (categoryKeys != null) {
                        for (String categoryName : categoryKeys) {
                            if (categoryName == null || categoryName.trim().isEmpty()) {
                                errors.add("categories section contains invalid category name");
                                continue;
                            }
                            ConfigurationSection category = categories.getConfigurationSection(categoryName);
                            if (category != null) {
                                Object spellsObj = category.get("spells");
                                if (spellsObj instanceof List) {
                                    List<?> spellsList = (List<?>) spellsObj;
                                    for (int i = 0; i < spellsList.size(); i++) {
                                        Object spell = spellsList.get(i);
                                        if (!(spell instanceof String)) {
                                            errors.add("categories." + categoryName + ".spells[" + i + "] must be a string");
                                        } else if (!isValidKebabCase((String) spell)) {
                                            errors.add("categories." + categoryName + ".spells[" + i + "] must be kebab-case");
                                        }
                                    }
                                } else if (spellsObj != null) {
                                    errors.add("categories." + categoryName + ".spells must be a list");
                                }
                            }
                        }
                    }
                }
            } else if (config.contains("categories")) {
                errors.add("'categories' must be a configuration section.");
            }
        } catch (Exception e) {
            errors.add("Error validating main config: " + e.getMessage());
        }

        return errors;
    }

    /**
     * Recursively validates a message section.
     *
     * @param section the section to validate
     * @param path    the path to this section
     * @param errors  the list to collect errors in
     */
    private void validateMessageSection(ConfigurationSection section, String path, List<String> errors) {
        if (section == null || path == null || errors == null) {
            return;
        }

        try {
            Set<String> keys = section.getKeys(false);
            if (keys == null) {
                return;
            }

            for (String key : keys) {
                if (key == null || key.trim().isEmpty()) {
                    errors.add(path + " contains invalid key");
                    continue;
                }
                Object value = section.get(key);
                if (value instanceof ConfigurationSection) {
                    validateMessageSection((ConfigurationSection) value, path + "." + key, errors);
                } else if (!(value instanceof String)) {
                    errors.add(path + "." + key + " must be a string");
                }
            }
        } catch (Exception e) {
            errors.add("Error validating message section " + path + ": " + e.getMessage());
        }
    }

    /**
     * Validates the spells.yml file.
     *
     * @param spellsConfig the spells configuration to validate
     * @return list of validation errors, empty if valid
     */
    public List<String> validateSpellsConfig(FileConfiguration spellsConfig) {
        List<String> errors = new ArrayList<>();

        if (spellsConfig == null) {
            errors.add("Spells configuration cannot be null");
            return errors;
        }

        try {
            // Check config-version
            if (!spellsConfig.contains("config-version")) {
                errors.add("Missing required field: config-version in spells.yml");
            } else {
                Object version = spellsConfig.get("config-version");
                if (!(version instanceof String || version instanceof Double || version instanceof Integer)) {
                    errors.add("config-version in spells.yml must be a string or number");
                }
            }

            // Validate spells section
            if (!spellsConfig.contains("spells")) {
                errors.add("Missing required section: spells");
            } else {
                ConfigurationSection spells = spellsConfig.getConfigurationSection("spells");
                if (spells == null) {
                    errors.add("spells section must be a configuration section");
                } else {
                    Set<String> spellKeys = spells.getKeys(false);
                    if (spellKeys == null || spellKeys.isEmpty()) {
                        errors.add("spells section is empty");
                    } else {
                        for (String spellKey : spellKeys) {
                            if (spellKey == null || spellKey.trim().isEmpty()) {
                                errors.add("spells section contains invalid spell key");
                                continue;
                            }
                            if (!isValidKebabCase(spellKey)) {
                                errors.add("Spell key '" + spellKey + "' must be kebab-case");
                            }
                            ConfigurationSection spell = spells.getConfigurationSection(spellKey);
                            if (spell == null) {
                                errors.add("Spell '" + spellKey + "' must be a configuration section");
                            } else {
                                validateSpellSection(spell, spellKey, errors);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            errors.add("Error validating spells config: " + e.getMessage());
        }

        return errors;
    }

    /**
     * Validates a spell configuration section.
     *
     * @param spell     the spell configuration section
     * @param spellKey  the spell key
     * @param errors    the list to collect errors in
     */
    private void validateSpellSection(ConfigurationSection spell, String spellKey, List<String> errors) {
        if (spell == null || spellKey == null || errors == null) {
            return;
        }

        try {
            // Validate display-name
            if (!spell.contains("display-name")) {
                errors.add("Spell '" + spellKey + "' missing required field: display-name");
            } else {
                Object displayName = spell.get("display-name");
                if (!(displayName instanceof String)) {
                    errors.add("Spell '" + spellKey + "' display-name must be a string");
                } else if (((String) displayName).trim().isEmpty()) {
                    errors.add("Spell '" + spellKey + "' display-name cannot be empty");
                }
            }

            // Validate description
            if (!spell.contains("description")) {
                errors.add("Spell '" + spellKey + "' missing required field: description");
            } else {
                Object description = spell.get("description");
                if (!(description instanceof String)) {
                    errors.add("Spell '" + spellKey + "' description must be a string");
                }
            }

            // Validate cooldown
            if (spell.contains("cooldown")) {
                Object cooldown = spell.get("cooldown");
                if (!(cooldown instanceof Number)) {
                    errors.add("Spell '" + spellKey + "' cooldown must be a number");
                } else {
                    long cooldownValue = ((Number) cooldown).longValue();
                    if (cooldownValue < 0) {
                        errors.add("Spell '" + spellKey + "' cooldown must be non-negative");
                    }
                }
            }

            // Validate mana-cost
            if (spell.contains("mana-cost")) {
                Object manaCost = spell.get("mana-cost");
                if (!(manaCost instanceof Number)) {
                    errors.add("Spell '" + spellKey + "' mana-cost must be a number");
                } else {
                    int manaCostValue = ((Number) manaCost).intValue();
                    if (manaCostValue < 0) {
                        errors.add("Spell '" + spellKey + "' mana-cost must be non-negative");
                    }
                }
            }

            // Validate type
            if (!spell.contains("type")) {
                errors.add("Spell '" + spellKey + "' missing required field: type");
            } else {
                Object type = spell.get("type");
                if (!(type instanceof String)) {
                    errors.add("Spell '" + spellKey + "' type must be a string");
                } else {
                    String typeStr = (String) type;
                    if (typeStr.trim().isEmpty()) {
                        errors.add("Spell '" + spellKey + "' type cannot be empty");
                    }
                }
            }

            // Validate category
            if (spell.contains("category")) {
                Object category = spell.get("category");
                if (!(category instanceof String)) {
                    errors.add("Spell '" + spellKey + "' category must be a string");
                } else {
                    String categoryStr = (String) category;
                    if (categoryStr.trim().isEmpty()) {
                        errors.add("Spell '" + spellKey + "' category cannot be empty");
                    }
                }
            }

            // Validate permission
            if (spell.contains("permission")) {
                Object permission = spell.get("permission");
                if (!(permission instanceof String)) {
                    errors.add("Spell '" + spellKey + "' permission must be a string");
                }
            }

            // Validate enabled
            if (spell.contains("enabled")) {
                Object enabled = spell.get("enabled");
                if (!(enabled instanceof Boolean)) {
                    errors.add("Spell '" + spellKey + "' enabled must be a boolean");
                }
            }

            // Validate particle-count
            if (spell.contains("particle-count")) {
                Object particleCount = spell.get("particle-count");
                if (!(particleCount instanceof Number)) {
                    errors.add("Spell '" + spellKey + "' particle-count must be a number");
                } else {
                    int count = ((Number) particleCount).intValue();
                    if (count < 0) {
                        errors.add("Spell '" + spellKey + "' particle-count must be non-negative");
                    }
                }
            }

            // Validate radius
            if (spell.contains("radius")) {
                Object radius = spell.get("radius");
                if (!(radius instanceof Number)) {
                    errors.add("Spell '" + spellKey + "' radius must be a number");
                } else {
                    double radiusValue = ((Number) radius).doubleValue();
                    if (radiusValue < 0) {
                        errors.add("Spell '" + spellKey + "' radius must be non-negative");
                    }
                }
            }

            // Validate damage
            if (spell.contains("damage")) {
                Object damage = spell.get("damage");
                if (!(damage instanceof Number)) {
                    errors.add("Spell '" + spellKey + "' damage must be a number");
                } else {
                    double damageValue = ((Number) damage).doubleValue();
                    if (damageValue < 0) {
                        errors.add("Spell '" + spellKey + "' damage must be non-negative");
                    }
                }
            }

            // Validate duration
            if (spell.contains("duration")) {
                Object duration = spell.get("duration");
                if (!(duration instanceof Number)) {
                    errors.add("Spell '" + spellKey + "' duration must be a number");
                } else {
                    long durationValue = ((Number) duration).longValue();
                    if (durationValue < 0) {
                        errors.add("Spell '" + spellKey + "' duration must be non-negative");
                    }
                }
            }

            // Validate sound
            if (spell.contains("sound")) {
                Object sound = spell.get("sound");
                if (!(sound instanceof String)) {
                    errors.add("Spell '" + spellKey + "' sound must be a string");
                }
            }

            // Validate effects
            if (spell.contains("effects")) {
                Object effects = spell.get("effects");
                if (!(effects instanceof ConfigurationSection)) {
                    errors.add("Spell '" + spellKey + "' effects must be a configuration section");
                }
            }

            // Validate range
            if (spell.contains("range")) {
                Object range = spell.get("range");
                if (!(range instanceof Number)) {
                    errors.add("Spell '" + spellKey + "' range must be a number");
                } else {
                    double rangeValue = ((Number) range).doubleValue();
                    if (rangeValue < 0) {
                        errors.add("Spell '" + spellKey + "' range must be non-negative");
                    }
                }
            }

            // Validate values section
            if (spell.contains("values")) {
                Object values = spell.get("values");
                if (!(values instanceof ConfigurationSection)) {
                    errors.add("Spell '" + spellKey + "' values must be a configuration section");
                } else {
                    validateValuesSection((ConfigurationSection) values, spellKey, errors);
                }
            }

            // Validate flags section
            if (spell.contains("flags")) {
                Object flags = spell.get("flags");
                if (!(flags instanceof ConfigurationSection)) {
                    errors.add("Spell '" + spellKey + "' flags must be a configuration section");
                } else {
                    validateFlagsSection((ConfigurationSection) flags, spellKey, errors);
                }
            }

            // Validate fx section
            if (spell.contains("fx")) {
                Object fx = spell.get("fx");
                if (!(fx instanceof ConfigurationSection)) {
                    errors.add("Spell '" + spellKey + "' fx must be a configuration section");
                } else {
                    validateFxSection((ConfigurationSection) fx, spellKey, errors);
                }
            }

        } catch (Exception e) {
            errors.add("Error validating spell '" + spellKey + "': " + e.getMessage());
        }
    }

    /**
     * Validates a spell's values section.
     *
     * @param values   the values configuration section
     * @param spellKey the spell key
     * @param errors   the list to collect errors in
     */
    private void validateValuesSection(ConfigurationSection values, String spellKey, List<String> errors) {
        if (values == null || spellKey == null || errors == null) {
            return;
        }

        try {
            Set<String> valueKeys = values.getKeys(false);
            if (valueKeys != null) {
                for (String valueKey : valueKeys) {
                    if (valueKey == null || valueKey.trim().isEmpty()) {
                        errors.add("Spell '" + spellKey + "' values section contains invalid key");
                        continue;
                    }
                    
                    Object value = values.get(valueKey);
                    String path = "Spell '" + spellKey + "' values." + valueKey;
                    
                    // Validate specific value types based on common spell parameters
                    if (isNumericValueKey(valueKey)) {
                        if (!(value instanceof Number)) {
                            errors.add(path + " must be a number");
                        } else {
                            double numValue = ((Number) value).doubleValue();
                            if (isMustBePositiveKey(valueKey) && numValue <= 0) {
                                errors.add(path + " must be positive");
                            } else if (isMustBeNonNegativeKey(valueKey) && numValue < 0) {
                                errors.add(path + " must be non-negative");
                            }
                        }
                    } else if (isIntegerValueKey(valueKey)) {
                        if (!(value instanceof Number)) {
                            errors.add(path + " must be a number");
                        } else {
                            int intValue = ((Number) value).intValue();
                            if (isMustBePositiveKey(valueKey) && intValue <= 0) {
                                errors.add(path + " must be positive");
                            } else if (isMustBeNonNegativeKey(valueKey) && intValue < 0) {
                                errors.add(path + " must be non-negative");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            errors.add("Error validating values section for spell '" + spellKey + "': " + e.getMessage());
        }
    }

    /**
     * Validates a spell's flags section.
     *
     * @param flags    the flags configuration section
     * @param spellKey the spell key
     * @param errors   the list to collect errors in
     */
    private void validateFlagsSection(ConfigurationSection flags, String spellKey, List<String> errors) {
        if (flags == null || spellKey == null || errors == null) {
            return;
        }

        try {
            Set<String> flagKeys = flags.getKeys(false);
            if (flagKeys != null) {
                for (String flagKey : flagKeys) {
                    if (flagKey == null || flagKey.trim().isEmpty()) {
                        errors.add("Spell '" + spellKey + "' flags section contains invalid key");
                        continue;
                    }
                    
                    Object flag = flags.get(flagKey);
                    String path = "Spell '" + spellKey + "' flags." + flagKey;
                    
                    // Most flags should be booleans
                    if (!(flag instanceof Boolean)) {
                        errors.add(path + " must be a boolean");
                    }
                }
            }
        } catch (Exception e) {
            errors.add("Error validating flags section for spell '" + spellKey + "': " + e.getMessage());
        }
    }

    /**
     * Validates a spell's fx (effects) section.
     *
     * @param fx       the fx configuration section
     * @param spellKey the spell key
     * @param errors   the list to collect errors in
     */
    private void validateFxSection(ConfigurationSection fx, String spellKey, List<String> errors) {
        if (fx == null || spellKey == null || errors == null) {
            return;
        }

        try {
            // Validate particles
            if (fx.contains("particles")) {
                Object particles = fx.get("particles");
                if (!(particles instanceof String)) {
                    errors.add("Spell '" + spellKey + "' fx.particles must be a string");
                }
            }

            // Validate sound
            if (fx.contains("sound")) {
                Object sound = fx.get("sound");
                if (!(sound instanceof String)) {
                    errors.add("Spell '" + spellKey + "' fx.sound must be a string");
                }
            }

        } catch (Exception e) {
            errors.add("Error validating fx section for spell '" + spellKey + "': " + e.getMessage());
        }
    }

    /**
     * Checks if a value key should be numeric.
     */
    private boolean isNumericValueKey(String key) {
        if (key == null) return false;
        // Exclude boolean-like keys that incidentally contain numeric-related words
        if ("cancel-on-damage".equals(key)) return false;

        return key.contains("damage") ||
               key.contains("radius") ||
               key.contains("range") ||
               key.contains("yield") ||
               key.contains("speed") ||
               key.contains("distance") ||
               key.contains("power") ||
               key.contains("strength") ||
               key.contains("multiplier") ||
               key.contains("modifier");
    }

    /**
     * Checks if a value key should be an integer.
     */
    private boolean isIntegerValueKey(String key) {
        return key != null && (
            key.contains("count") || 
            key.contains("amount") || 
            key.contains("ticks") ||
            key.contains("duration-ticks") ||
            key.contains("delay-ticks") ||
            key.contains("level")
        );
    }

    /**
     * Checks if a value key must be positive (> 0).
     */
    private boolean isMustBePositiveKey(String key) {
        return key != null && (
            key.contains("damage") && !key.contains("reduction") ||
            key.equals("range") ||
            key.equals("radius") ||
            key.contains("count") && !key.contains("extra") ||
            key.contains("speed") && !key.contains("reduction")
        );
    }

    /**
     * Checks if a value key must be non-negative (>= 0).
     */
    private boolean isMustBeNonNegativeKey(String key) {
        return key != null && (
            key.contains("ticks") ||
            key.contains("delay") ||
            key.contains("extra") ||
            key.contains("modifier") ||
            key.contains("multiplier")
        );
    }

    /**
     * Checks if a string is valid kebab-case.
     *
     * @param str the string to check
     * @return true if valid kebab-case, false otherwise
     */
    private boolean isValidKebabCase(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        return KEBAB_CASE_PATTERN.matcher(str).matches();
    }
}
