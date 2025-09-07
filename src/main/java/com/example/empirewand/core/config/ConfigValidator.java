package com.example.empirewand.core.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates configuration files for structure, types, value ranges, and kebab-case spell keys.
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
                for (String key : messages.getKeys(false)) {
                    Object value = messages.get(key);
                    if (!(value instanceof String)) {
                        errors.add("messages." + key + " must be a string");
                    }
                }
            }
        }

        // Validate features section
        if (config.contains("features")) {
            ConfigurationSection features = config.getConfigurationSection("features");
            if (features != null) {
                for (String key : features.getKeys(false)) {
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

        return errors;
    }

    /**
     * Validates the spells.yml file.
     *
     * @param spellsConfig the spells configuration to validate
     * @return list of validation errors, empty if valid
     */
    public List<String> validateSpellsConfig(FileConfiguration spellsConfig) {
        List<String> errors = new ArrayList<>();

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

        return errors;
    }

    /**
     * Validates a single spell configuration section.
     *
     * @param spellKey the spell key
     * @param spellSection the spell configuration section
     * @return list of validation errors for this spell
     */
    private List<String> validateSpellSection(String spellKey, ConfigurationSection spellSection) {
        List<String> errors = new ArrayList<>();

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
                    Object value = values.get(valueKey);
                    if (!(value instanceof Number || value instanceof String || value instanceof Boolean)) {
                        errors.add("Spell '" + spellKey + "'.values." + valueKey + " must be a number, string, or boolean");
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

        if (spellSection.contains("mana-cost")) {
            Object manaCost = spellSection.get("mana-cost");
            if (!(manaCost instanceof Number)) {
                errors.add("Spell '" + spellKey + "'.mana-cost must be a number");
            } else {
                double manaCostValue = ((Number) manaCost).doubleValue();
                if (manaCostValue < 0) {
                    errors.add("Spell '" + spellKey + "'.mana-cost must be non-negative");
                }
            }
        }

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

        return errors;
    }

    /**
     * Checks if a string is in kebab-case.
     *
     * @param str the string to check
     * @return true if kebab-case, false otherwise
     */
    private boolean isKebabCase(String str) {
        return KEBAB_CASE_PATTERN.matcher(str).matches();
    }
}
