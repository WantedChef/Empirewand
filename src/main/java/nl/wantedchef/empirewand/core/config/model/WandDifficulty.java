package nl.wantedchef.empirewand.core.config.model;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the difficulty levels for wands.
 * Each difficulty has associated properties like display materials and colors.
 */
public enum WandDifficulty {
    EASY(Material.LIME_DYE, "Easy", "§a"),
    MEDIUM(Material.YELLOW_DYE, "Medium", "§e"),
    HARD(Material.RED_DYE, "Hard", "§c");

    private final Material displayMaterial;
    private final String displayName;
    private final String colorCode;

    WandDifficulty(@NotNull Material displayMaterial, @NotNull String displayName, @NotNull String colorCode) {
        this.displayMaterial = displayMaterial;
        this.displayName = displayName;
        this.colorCode = colorCode;
    }

    /**
     * Gets the material used to represent this difficulty in GUIs.
     *
     * @return The display material
     */
    @NotNull
    public Material getDisplayMaterial() {
        return displayMaterial;
    }

    /**
     * Gets the human-readable display name.
     *
     * @return The display name
     */
    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the color code for this difficulty.
     *
     * @return The color code (e.g., "§a" for green)
     */
    @NotNull
    public String getColorCode() {
        return colorCode;
    }

    /**
     * Gets the formatted display name with color.
     *
     * @return The colored display name
     */
    @NotNull
    public String getColoredDisplayName() {
        return colorCode + displayName;
    }

    /**
     * Parses a difficulty from a string value.
     *
     * @param value The string value to parse
     * @return The difficulty, or MEDIUM as default if not found
     */
    @NotNull
    public static WandDifficulty fromString(@NotNull String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM; // Default fallback
        }
    }
}