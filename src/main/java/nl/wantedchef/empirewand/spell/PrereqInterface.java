package nl.wantedchef.empirewand.spell;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

/**
 * Represents a prerequisite system for spell casting.
 * <p>
 * This interface defines a strategy pattern for implementing various types of
 * prerequisites that must be satisfied before a spell can be cast. Prerequisites
 * can include player level requirements, item requirements, permission checks,
 * cooldown verification, and custom conditions.
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * // Create a level prerequisite
 * PrereqInterface levelPrereq = new PrereqInterface.LevelPrereq(10);
 * 
 * // Create a composite prerequisite
 * PrereqInterface composite = new PrereqInterface.CompositePrereq(
 *     List.of(levelPrereq, new PrereqInterface.ItemPrereq(Material.DIAMOND, 1))
 * );
 * 
 * // Check prerequisites
 * SpellContext context = new SpellContext(...);
 * PrereqInterface.CheckResult result = composite.check(context);
 * if (!result.canCast()) {
 *     player.sendMessage(result.reason());
 * }
 * }</pre>
 *
 * <p>
 * <strong>Thread Safety:</strong> Implementations should be immutable and thread-safe.
 *
 * @since 1.0.0
 */
public interface PrereqInterface {

    /**
     * Checks if the prerequisite is met for the given context.
     * <p>
     * This method performs the actual prerequisite check and returns a result
     * indicating whether the spell can be cast and any failure reason.
     * <p>
     * <strong>Note:</strong> Implementations should be efficient as this method
     * may be called frequently during spell casting attempts.
     *
     * @param context the spell context containing player and environment information
     * @return the check result indicating success or failure
     * @throws NullPointerException if context is null
     */
    @NotNull
    CheckResult check(@NotNull SpellContext context);

    /**
     * Gets the reason why the prerequisite failed, if any.
     * <p>
     * Default implementation returns null. Override to provide specific failure reasons.
     *
     * @return the failure reason, or null if the prerequisite was met
     */
    @Nullable
    default Component getFailureReason() {
        return null;
    }

    /**
     * Result of a prerequisite check.
     * <p>
     * This immutable record provides information about whether a prerequisite
     * was satisfied and an optional reason message for failures.
     *
     * @param canCast true if the prerequisite is satisfied, false otherwise
     * @param reason the failure reason, null if successful
     */
    record CheckResult(boolean canCast, @Nullable Component reason) {
        /** Predefined success result without a message */
        public static final CheckResult SUCCESS = new CheckResult(true, null);

        /**
         * Creates a failure result with the specified reason.
         * <p>
         * The reason will be displayed to the player as an error message.
         *
         * @param reason the failure reason, must not be null
         * @return a failure CheckResult with the provided reason
         * @throws NullPointerException if reason is null
         */
        @NotNull
        public static CheckResult failure(@NotNull Component reason) {
            Objects.requireNonNull(reason, "Reason cannot be null");
            return new CheckResult(false, reason.color(NamedTextColor.RED));
        }

        /**
         * Creates a success result with a custom message.
         * <p>
         * Use this when you want to provide additional feedback on successful checks.
         *
         * @param message the success message
         * @return a success CheckResult with the provided message
         * @throws NullPointerException if message is null
         */
        @NotNull
        public static CheckResult success(@NotNull Component message) {
            Objects.requireNonNull(message, "Message cannot be null");
            return new CheckResult(true, message.color(NamedTextColor.GREEN));
        }

        /**
         * Checks if the prerequisite was satisfied.
         *
         * @return true if the prerequisite was met, false otherwise
         */
        public boolean isSuccess() {
            return canCast;
        }

        /**
         * Checks if the prerequisite failed.
         *
         * @return true if the prerequisite was not met, false otherwise
         */
        public boolean isFailure() {
            return !canCast;
        }
    }

    /**
     * Prerequisite that always allows casting.
     * <p>
     * This is a no-op prerequisite that always returns success. Use this as a
     * default when no specific prerequisites are required.
     */
    class NonePrereq implements PrereqInterface {
        @Override
        @NotNull
        public CheckResult check(@NotNull SpellContext context) {
            Objects.requireNonNull(context, "Context cannot be null");
            return CheckResult.SUCCESS;
        }
    }

    /**
     * Prerequisite based on player level.
     * <p>
     * Checks if the casting player has reached a minimum required level.
     */
    class LevelPrereq implements PrereqInterface {
        private final int requiredLevel;

        /**
         * Creates a new level prerequisite.
         *
         * @param requiredLevel the minimum level required to cast the spell
         * @throws IllegalArgumentException if requiredLevel is negative
         */
        public LevelPrereq(int requiredLevel) {
            if (requiredLevel < 0) {
                throw new IllegalArgumentException("Required level cannot be negative");
            }
            this.requiredLevel = requiredLevel;
        }

        @Override
        @NotNull
        public CheckResult check(@NotNull SpellContext context) {
            Objects.requireNonNull(context, "Context cannot be null");
            
            if (context.caster().getLevel() >= requiredLevel) {
                return CheckResult.SUCCESS;
            } else {
                return CheckResult.failure(
                    Component.text("You need level " + requiredLevel + " to cast this spell")
                );
            }
        }

        /**
         * Creates a LevelPrereq from configuration.
         * <p>
         * Reads the "level" value from the configuration section.
         *
         * @param config the configuration section
         * @return a new LevelPrereq instance
         * @throws NullPointerException if config is null
         */
        @NotNull
        public static LevelPrereq fromConfig(@NotNull ConfigurationSection config) {
            Objects.requireNonNull(config, "Config cannot be null");
            int level = config.getInt("level", 0);
            return new LevelPrereq(level);
        }

        /**
         * Gets the required level for this prerequisite.
         *
         * @return the required level
         */
        public int getRequiredLevel() {
            return requiredLevel;
        }
    }

    /**
     * Prerequisite based on having a specific item.
     * <p>
     * Checks if the casting player has the required item(s) in their inventory.
     */
    class ItemPrereq implements PrereqInterface {
        private final Material material;
        private final int amount;

        /**
         * Creates a new item prerequisite.
         *
         * @param material the required material type
         * @param amount the required amount
         * @throws NullPointerException if material is null
         * @throws IllegalArgumentException if amount is negative
         */
        public ItemPrereq(@NotNull Material material, int amount) {
            this.material = Objects.requireNonNull(material, "Material cannot be null");
            if (amount < 0) {
                throw new IllegalArgumentException("Amount cannot be negative");
            }
            this.amount = amount;
        }

        @Override
        @NotNull
        public CheckResult check(@NotNull SpellContext context) {
            Objects.requireNonNull(context, "Context cannot be null");
            
            if (context.caster().getInventory().contains(material, amount)) {
                return CheckResult.SUCCESS;
            } else {
                String itemName = material.name().toLowerCase().replace('_', ' ');
                return CheckResult.failure(
                    Component.text("You need " + amount + " " + itemName + " to cast this spell")
                );
            }
        }

        /**
         * Creates an ItemPrereq from configuration.
         * <p>
         * Reads "material" and "amount" values from the configuration section.
         * Invalid materials will default to AIR.
         *
         * @param config the configuration section
         * @return a new ItemPrereq instance
         * @throws NullPointerException if config is null
         */
        @NotNull
        public static ItemPrereq fromConfig(@NotNull ConfigurationSection config) {
            Objects.requireNonNull(config, "Config cannot be null");
            
            String materialName = config.getString("material", "AIR");
            Material material;
            try {
                material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Log warning for invalid material
                java.util.logging.Logger.getLogger(PrereqInterface.class.getName())
                    .log(Level.WARNING, "Invalid material in config: " + materialName);
                material = Material.AIR;
            }
            
            int amount = config.getInt("amount", 1);
            return new ItemPrereq(material, amount);
        }

        /**
         * Gets the required material for this prerequisite.
         *
         * @return the required material
         */
        @NotNull
        public Material getMaterial() {
            return material;
        }

        /**
         * Gets the required amount for this prerequisite.
         *
         * @return the required amount
         */
        public int getAmount() {
            return amount;
        }
    }

    /**
     * Composite prerequisite that combines multiple prerequisites.
     * <p>
     * This prerequisite checks all provided prerequisites in order, with
     * short-circuiting on the first failure. This allows for efficient
     * prerequisite checking.
     */
    class CompositePrereq implements PrereqInterface {
        private final List<PrereqInterface> prerequisites;

        /**
         * Creates a new composite prerequisite.
         *
         * @param prerequisites the list of prerequisites to check
         * @throws NullPointerException if prerequisites is null
         * @throws IllegalArgumentException if prerequisites contains null elements
         */
        public CompositePrereq(@NotNull List<PrereqInterface> prerequisites) {
            Objects.requireNonNull(prerequisites, "Prerequisites list cannot be null");
            if (prerequisites.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("Prerequisites list cannot contain null elements");
            }
            this.prerequisites = List.copyOf(prerequisites);
        }

        @Override
        @NotNull
        public CheckResult check(@NotNull SpellContext context) {
            Objects.requireNonNull(context, "Context cannot be null");
            
            for (PrereqInterface prereq : prerequisites) {
                CheckResult result = prereq.check(context);
                if (!result.canCast()) {
                    return result; // Short-circuit on first failure
                }
            }
            return CheckResult.SUCCESS;
        }

        /**
         * Creates a CompositePrereq from configuration.
         * <p>
         * Reads a list of prerequisite configurations and creates appropriate
         * prerequisite instances based on the "type" field.
         *
         * @param config the configuration section
         * @return a new CompositePrereq instance
         * @throws NullPointerException if config is null
         */
        @NotNull
        public static CompositePrereq fromConfig(@NotNull ConfigurationSection config) {
            Objects.requireNonNull(config, "Config cannot be null");
            
            List<PrereqInterface> prereqs = config.getStringList("prerequisites").stream()
                .map(key -> {
                    ConfigurationSection prereqConfig = config.getConfigurationSection(key);
                    if (prereqConfig == null) {
                        // Log warning for missing config
                        java.util.logging.Logger.getLogger(PrereqInterface.class.getName())
                            .log(Level.WARNING, "Missing prerequisite config: " + key);
                        return new NonePrereq();
                    }
                    
                    String type = prereqConfig.getString("type", "none");
                    return switch (type.toLowerCase()) {
                        case "level" -> LevelPrereq.fromConfig(prereqConfig);
                        case "item" -> ItemPrereq.fromConfig(prereqConfig);
                        case "composite" -> CompositePrereq.fromConfig(prereqConfig);
                        default -> new NonePrereq();
                    };
                })
                .toList();
            
            return new CompositePrereq(prereqs);
        }

        /**
         * Gets the list of prerequisites in this composite.
         *
         * @return an unmodifiable list of prerequisites
         */
        @NotNull
        public List<PrereqInterface> getPrerequisites() {
            return prerequisites;
        }
    }

    /**
     * Loads a PrereqInterface from configuration.
     * <p>
     * This factory method creates the appropriate prerequisite implementation
     * based on the "type" field in the configuration.
     *
     * @param config the configuration section
     * @return the loaded prerequisite
     * @throws NullPointerException if config is null
     */
    @NotNull
    static PrereqInterface loadFromConfig(@NotNull ConfigurationSection config) {
        Objects.requireNonNull(config, "Config cannot be null");
        
        String type = config.getString("type", "none");
        if (type == null) {
            type = "none";
        }
        
        return switch (type.toLowerCase()) {
            case "level" -> LevelPrereq.fromConfig(config);
            case "item" -> ItemPrereq.fromConfig(config);
            case "composite" -> CompositePrereq.fromConfig(config);
            default -> new NonePrereq();
        };
    }

    /**
     * Legacy record for backward compatibility.
     * <p>
     * This record provides backward compatibility with older prerequisite systems.
     *
     * @deprecated Use the new prerequisite system instead
     */
    @SuppressFBWarnings(value = { "EI_EXPOSE_REP" }, 
        justification = "Component is immutable from our usage perspective (Adventure API)")
    @Deprecated
    record Legacy(boolean canCast, Component reason) implements PrereqInterface {
        @Override
        @NotNull
        public CheckResult check(@NotNull SpellContext context) {
            return new CheckResult(canCast, reason);
        }
    }
}
