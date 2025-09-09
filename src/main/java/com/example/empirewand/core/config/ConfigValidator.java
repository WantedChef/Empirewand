package com.example.empirewand.core.config;

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
            if (config.contains("messages")) {
                ConfigurationSection messages = config.getConfigurationSection("messages");
                if (messages != null) {
                    validateMessageSection(messages, "messages", errors);
                }
            }

            // Validate features section
            if (config.contains("features")) {
                ConfigurationSection features = config.getConfigurationSection("features");
                if (features != null) {
                    for (String key : features.getKeys(false)) {
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

            // Validate cooldowns section
            if (config.contains("cooldowns")) {
                ConfigurationSection cooldowns = config.getConfigurationSection("cooldowns");
                if (cooldowns != null) {
                    for (String key : cooldowns.getKeys(false)) {
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
        } catch (Exception e) {
            errors.add("Error validating main config: " + e.getMessage());
        }

        return errors;
    }

    /**
     * Recursively validates a message section.
     *
     * @param section the section to validate
     * @param path    the current path for error messages
     * @param errors  the list to add errors to
     */
    private void validateMessageSection(ConfigurationSection section, String path, List<String> errors) {
        for (String key : section.getKeys(false)) {
            if (key == null || key.trim().isEmpty()) {
                errors.add(path + " section contains invalid key");
                continue;
            }
            Object value = section.get(key);
            if (value instanceof ConfigurationSection nestedSection) {
                // Recursively validate nested sections
                validateMessageSection(nestedSection, path + "." + key, errors);
            } else if (!(value instanceof String)) {
                errors.add(path + "." + key + " must be a string");
            }
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
                errors.add("Missing required field: config-version");
            } else {
                Object version = spellsConfig.get("config-version");
                if (!(version instanceof String || version instanceof Double || version instanceof Integer)) {
                    errors.add("config-version must be a string or number");
                }
            }

            // Validate spell keys are kebab-case
            Set<String> spellKeys = spellsConfig.getKeys(false);
            spellKeys.remove("config-version"); // Remove config-version from spell keys

            for (String key : spellKeys) {
                if (key == null || key.trim().isEmpty()) {
                    errors.add("Invalid spell key found");
                    continue;
                }

                if (!isKebabCase(key)) {
                    errors.add("Spell key '" + key + "' must be in kebab-case (lowercase with hyphens)");
                }

                // Validate spell structure
                ConfigurationSection spellSection = spellsConfig.getConfigurationSection(key);
                if (spellSection != null) {
                    errors.addAll(validateSpellSection(key, spellSection));
                } else {
                    errors.add("Spell '" + key + "' must be a configuration section");
                }
            }
        } catch (Exception e) {
            errors.add("Error validating spells config: " + e.getMessage());
        }

        return errors;
    }

    /**
     * Validates a single spell configuration section.
     *
     * @param spellKey     the spell key
     * @param spellSection the spell configuration section
     * @return list of validation errors for this spell
     */
    private List<String> validateSpellSection(String spellKey, ConfigurationSection spellSection) {
        List<String> errors = new ArrayList<>();

        if (spellKey == null || spellKey.trim().isEmpty()) {
            errors.add("Spell key cannot be null or empty");
            return errors;
        }

        if (spellSection == null) {
            errors.add("Spell section cannot be null for key: " + spellKey);
            return errors;
        }

        try {
            // Check required fields
            if (!spellSection.contains("display-name")) {
                errors.add("Spell '" + spellKey + "' missing required field: display-name");
            } else if (!(spellSection.get("display-name") instanceof String)) {
                errors.add("Spell '" + spellKey + "'.display-name must be a string");
            }

            if (!spellSection.contains("cooldown")) {
                errors.add("Spell '" + spellKey + "' missing required field: cooldown");
            } else {
                Object cooldown = spellSection.get("cooldown");
                if (!(cooldown instanceof Number)) {
                    errors.add("Spell '" + spellKey + "'.cooldown must be a number");
                } else {
                    long cooldownValue = ((Number) cooldown).longValue();
                    if (cooldownValue < 0) {
                        errors.add("Spell '" + spellKey + "'.cooldown must be non-negative");
                    }
                }
            }

            // Validate values section if present
            if (spellSection.contains("values")) {
                ConfigurationSection values = spellSection.getConfigurationSection("values");
                if (values != null) {
                    for (String valueKey : values.getKeys(false)) {
                        if (valueKey == null || valueKey.trim().isEmpty()) {
                            errors.add("Spell '" + spellKey + "'.values contains invalid key");
                            continue;
                        }
                        Object value = values.get(valueKey);
                        if (!(value instanceof Number || value instanceof String || value instanceof Boolean)) {
                            errors.add("Spell '" + spellKey + "'.values." + valueKey
                                    + " must be a number, string, or boolean");
                        }
                        // Additional range validation could be added here for specific values
                    }
                }
            }

            // Validate flags section if present
            if (spellSection.contains("flags")) {
                ConfigurationSection flags = spellSection.getConfigurationSection("flags");
                if (flags != null) {
                    for (String flagKey : flags.getKeys(false)) {
                        if (flagKey == null || flagKey.trim().isEmpty()) {
                            errors.add("Spell '" + spellKey + "'.flags contains invalid key");
                            continue;
                        }
                        Object flag = flags.get(flagKey);
                        if (!(flag instanceof Boolean)) {
                            errors.add("Spell '" + spellKey + "'.flags." + flagKey + " must be a boolean");
                        }
                    }
                }
            }

            // Check optional standard fields
            if (spellSection.contains("range")) {
                Object range = spellSection.get("range");
                if (!(range instanceof Number)) {
                    errors.add("Spell '" + spellKey + "'.range must be a number");
                } else {
                    double rangeValue = ((Number) range).doubleValue();
                    if (rangeValue < 0) {
                        errors.add("Spell '" + spellKey + "'.range must be non-negative");
                    }
                }
            }

            // Cost field removed from project; ignore if present.

            // Validate fx section if present
            if (spellSection.contains("fx")) {
                ConfigurationSection fx = spellSection.getConfigurationSection("fx");
                if (fx != null) {
                    if (fx.contains("particles")) {
                        Object particles = fx.get("particles");
                        if (!(particles instanceof String)) {
                            errors.add("Spell '" + spellKey + "'.fx.particles must be a string");
                        }
                    }
                    if (fx.contains("sound")) {
                        Object sound = fx.get("sound");
                        if (!(sound instanceof String)) {
                            errors.add("Spell '" + spellKey + "'.fx.sound must be a string");
                        }
                    }
                } else {
                    errors.add("Spell '" + spellKey + "'.fx must be a configuration section");
                }
            }
        } catch (Exception e) {
            errors.add("Error validating spell section '" + spellKey + "': " + e.getMessage());
        }

        return errors;
    }

    /**
     * Checks if a string is in kebab-case.
     *
     * @param str the string to check
     * @return true if kebab-case, false otherwise
     */
    private boolean isKebabCase(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            return KEBAB_CASE_PATTERN.matcher(str).matches();
        } catch (Exception e) {
            return false;
        }
    }
}
