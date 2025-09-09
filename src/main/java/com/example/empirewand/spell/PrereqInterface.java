package com.example.empirewand.spell;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * Represents a prerequisite for casting a spell.
 * Uses strategy pattern with different implementations for various requirement
 * types.
 */
public interface PrereqInterface {

    /**
     * Checks if the prerequisite is met for the given context.
     * 
     * @param context the spell context
     * @return the check result
     */
    @NotNull
    CheckResult check(@NotNull SpellContext context);

    /**
     * Gets the reason why the prerequisite failed, if any.
     * 
     * @return the failure reason, or null if met
     */
    @Nullable
    default Component getFailureReason() {
        return null;
    }

    /**
     * Result of a prerequisite check.
     */
    record CheckResult(boolean canCast, @Nullable Component reason) {
        public static final CheckResult SUCCESS = new CheckResult(true, null);

        public static CheckResult failure(@NotNull Component reason) {
            return new CheckResult(false, reason);
        }
    }

    /**
     * Prerequisite that always allows casting.
     */
    public static class NonePrereq implements PrereqInterface {
        @Override
        @NotNull
        public CheckResult check(@NotNull SpellContext context) {
            return CheckResult.SUCCESS;
        }
    }

    /**
     * Prerequisite based on player level.
     */
    public static class LevelPrereq implements PrereqInterface {
        private final int requiredLevel;

        public LevelPrereq(int requiredLevel) {
            this.requiredLevel = requiredLevel;
        }

        @Override
        @NotNull
        public CheckResult check(@NotNull SpellContext context) {
            if (context.caster().getLevel() >= requiredLevel) {
                return CheckResult.SUCCESS;
            } else {
                return CheckResult.failure(Component.text("You need level " + requiredLevel + " to cast this spell"));
            }
        }

        @NotNull
        public static LevelPrereq fromConfig(@NotNull ConfigurationSection config) {
            int level = config.getInt("level", 0);
            return new LevelPrereq(level);
        }
    }

    /**
     * Prerequisite based on having a specific item.
     */
    public static class ItemPrereq implements PrereqInterface {
        private final org.bukkit.Material material;
        private final int amount;

        public ItemPrereq(@NotNull org.bukkit.Material material, int amount) {
            this.material = material;
            this.amount = amount;
        }

        @Override
        @NotNull
        public CheckResult check(@NotNull SpellContext context) {
            if (context.caster().getInventory().contains(material, amount)) {
                return CheckResult.SUCCESS;
            } else {
                return CheckResult.failure(Component
                        .text("You need " + amount + " " + material.name().toLowerCase() + " to cast this spell"));
            }
        }

        @NotNull
        public static ItemPrereq fromConfig(@NotNull ConfigurationSection config) {
            String materialName = config.getString("material", "AIR");
            if (materialName == null) {
                materialName = "AIR";
            }
            org.bukkit.Material material = org.bukkit.Material.valueOf(materialName.toUpperCase());
            int amount = config.getInt("amount", 1);
            return new ItemPrereq(material, amount);
        }
    }

    /**
     * Composite prerequisite that combines multiple prerequisites with
     * short-circuiting.
     */
    public static class CompositePrereq implements PrereqInterface {
        private final List<PrereqInterface> prerequisites;

        public CompositePrereq(@NotNull List<PrereqInterface> prerequisites) {
            this.prerequisites = List.copyOf(prerequisites);
        }

        @Override
        @NotNull
        public CheckResult check(@NotNull SpellContext context) {
            for (PrereqInterface prereq : prerequisites) {
                CheckResult result = prereq.check(context);
                if (!result.canCast()) {
                    return result; // Short-circuit on first failure
                }
            }
            return CheckResult.SUCCESS;
        }

        @NotNull
        public static CompositePrereq fromConfig(@NotNull ConfigurationSection config) {
            List<PrereqInterface> prereqs = config.getStringList("prerequisites").stream()
                    .map(key -> {
                        ConfigurationSection prereqConfig = config.getConfigurationSection(key);
                        if (prereqConfig == null)
                            return new NonePrereq();
                        String type = prereqConfig.getString("type", "none");
                        return switch (type.toLowerCase()) {
                            case "level" -> LevelPrereq.fromConfig(prereqConfig);
                            case "item" -> ItemPrereq.fromConfig(prereqConfig);
                            default -> new NonePrereq();
                        };
                    })
                    .toList();
            return new CompositePrereq(prereqs);
        }
    }

    /**
     * Loads a PrereqInterface from configuration.
     * 
     * @param config the configuration section
     * @return the loaded prerequisite
     */
    @NotNull
    static PrereqInterface loadFromConfig(@NotNull ConfigurationSection config) {
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

    // Legacy record for backward compatibility
    @SuppressFBWarnings(value = {
            "EI_EXPOSE_REP" }, justification = "Component is immutable from our usage perspective (Adventure API); record provides direct access.")
    record Legacy(boolean canCast, Component reason) implements PrereqInterface {
        @Override
        @NotNull
        public CheckResult check(@NotNull SpellContext context) {
            return new CheckResult(canCast, reason);
        }
    }
}