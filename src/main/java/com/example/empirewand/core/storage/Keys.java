package com.example.empirewand.core.storage;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public final class Keys {

    // NamespacedKeys - using hardcoded values for static initialization
    public static final NamespacedKey WAND_KEY = new NamespacedKey("empirewand", "wand.key");
    public static final NamespacedKey MEPHIDANTES_ZEIST_KEY = new NamespacedKey("empirewand", "mephidantes_zeist.key");
    public static final NamespacedKey WAND_SPELLS = new NamespacedKey("empirewand", "wand.spells");
    public static final NamespacedKey WAND_ACTIVE_INDEX = new NamespacedKey("empirewand", "wand.active_index");
    public static final NamespacedKey PROJECTILE_SPELL = new NamespacedKey("empirewand", "projectile.spell");
    public static final NamespacedKey PROJECTILE_OWNER = new NamespacedKey("empirewand", "projectile.owner");
    public static final NamespacedKey ETHEREAL_ACTIVE = new NamespacedKey("empirewand", "ethereal.active");
    public static final NamespacedKey ETHEREAL_EXPIRES_TICK = new NamespacedKey("empirewand", "ethereal.expires_tick");

    // Reusable PersistentDataType wrappers
    public static final PersistentDataTypeWrapper<String> STRING_TYPE = new PersistentDataTypeWrapper<>(
            PersistentDataType.STRING);
    public static final PersistentDataTypeWrapper<Integer> INTEGER_TYPE = new PersistentDataTypeWrapper<>(
            PersistentDataType.INTEGER);
    public static final PersistentDataTypeWrapper<Long> LONG_TYPE = new PersistentDataTypeWrapper<>(
            PersistentDataType.LONG);
    public static final PersistentDataTypeWrapper<Double> DOUBLE_TYPE = new PersistentDataTypeWrapper<>(
            PersistentDataType.DOUBLE);
    public static final PersistentDataTypeWrapper<Byte> BYTE_TYPE = new PersistentDataTypeWrapper<>(
            PersistentDataType.BYTE);
    public static final PersistentDataTypeWrapper<Byte> BOOLEAN_TYPE = new PersistentDataTypeWrapper<>(
            PersistentDataType.BYTE);

    private Keys() {
    }

    // Removed mutable static plugin reference; factory methods now use explicit
    // namespace

    public static NamespacedKey createKey(String key) {
        return new NamespacedKey("empirewand", key);
    }

    public static NamespacedKey createKey(String namespace, String key) {
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
