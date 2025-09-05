package com.example.empirewand.core;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class Keys {

    // Plugin instance for factory methods
    private static Plugin plugin;

    // NamespacedKeys - using hardcoded values for static initialization
    public static final NamespacedKey WAND_KEY = new NamespacedKey("empirewand", "wand.key");
    public static final NamespacedKey WAND_SPELLS = new NamespacedKey("empirewand", "wand.spells");
    public static final NamespacedKey WAND_ACTIVE_INDEX = new NamespacedKey("empirewand", "wand.active_index");
    public static final NamespacedKey PROJECTILE_SPELL = new NamespacedKey("empirewand", "projectile.spell");
    public static final NamespacedKey PROJECTILE_OWNER = new NamespacedKey("empirewand", "projectile.owner");
    public static final NamespacedKey ETHEREAL_ACTIVE = new NamespacedKey("empirewand", "ethereal.active");
    public static final NamespacedKey ETHEREAL_EXPIRES_TICK = new NamespacedKey("empirewand", "ethereal.expires_tick");

    // Reusable PersistentDataType wrappers
    public static final PersistentDataTypeWrapper<String> STRING_TYPE = new PersistentDataTypeWrapper<>(PersistentDataType.STRING);
    public static final PersistentDataTypeWrapper<Integer> INTEGER_TYPE = new PersistentDataTypeWrapper<>(PersistentDataType.INTEGER);
    public static final PersistentDataTypeWrapper<Long> LONG_TYPE = new PersistentDataTypeWrapper<>(PersistentDataType.LONG);
    public static final PersistentDataTypeWrapper<Double> DOUBLE_TYPE = new PersistentDataTypeWrapper<>(PersistentDataType.DOUBLE);
    public static final PersistentDataTypeWrapper<Byte> BYTE_TYPE = new PersistentDataTypeWrapper<>(PersistentDataType.BYTE);
    public static final PersistentDataTypeWrapper<Byte> BOOLEAN_TYPE = new PersistentDataTypeWrapper<>(PersistentDataType.BYTE);

    private Keys() { }

    /**
     * Initialize the Keys class with the plugin instance.
     * This must be called before using factory methods.
     *
     * @param pluginInstance the plugin instance
     */
    public static void initialize(Plugin pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * Creates a NamespacedKey with the plugin's namespace.
     *
     * @param key the key string
     * @return the NamespacedKey
     */
    public static NamespacedKey createKey(String key) {
        if (plugin == null) {
            throw new IllegalStateException("Keys must be initialized with a plugin instance before use");
        }
        return new NamespacedKey(plugin, key);
    }

    /**
     * Creates a custom NamespacedKey with the plugin's namespace.
     *
     * @param namespace the namespace
     * @param key the key string
     * @return the NamespacedKey
     */
    public static NamespacedKey createKey(String namespace, String key) {
        if (plugin == null) {
            throw new IllegalStateException("Keys must be initialized with a plugin instance before use");
        }
        return new NamespacedKey(namespace, key);
    }

    /**
     * Wrapper class for PersistentDataType to provide type safety and reusability.
     *
     * @param <T> the data type
     */
    public static class PersistentDataTypeWrapper<T> {
        private final PersistentDataType<T, T> type;

        private PersistentDataTypeWrapper(PersistentDataType<T, T> type) {
            this.type = type;
        }

        public PersistentDataType<T, T> getType() {
            return type;
        }
    }
}