package nl.wantedchef.empirewand.core.config.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Data model representing the configurable settings for a wand.
 * This class is immutable and uses the builder pattern for construction.
 */
public class WandSettings {

    private final String wandKey;
    private final boolean cooldownBlock;
    private final boolean griefBlockDamage;
    private final boolean playerDamage;
    private final WandDifficulty difficulty;

    private WandSettings(@NotNull Builder builder) {
        this.wandKey = Objects.requireNonNull(builder.wandKey, "Wand key cannot be null");
        this.cooldownBlock = builder.cooldownBlock;
        this.griefBlockDamage = builder.griefBlockDamage;
        this.playerDamage = builder.playerDamage;
        this.difficulty = Objects.requireNonNull(builder.difficulty, "Difficulty cannot be null");
    }

    /**
     * Gets the unique key identifying this wand.
     *
     * @return The wand key
     */
    @NotNull
    public String getWandKey() {
        return wandKey;
    }

    /**
     * Gets whether cooldown blocking is enabled.
     * When true, players cannot cast spells while any spell is on cooldown.
     *
     * @return True if cooldown blocking is enabled
     */
    public boolean isCooldownBlock() {
        return cooldownBlock;
    }

    /**
     * Gets whether grief block damage is enabled.
     * When true, spells can damage/modify world blocks.
     *
     * @return True if grief block damage is enabled
     */
    public boolean isGriefBlockDamage() {
        return griefBlockDamage;
    }

    /**
     * Gets whether player damage is enabled.
     * When true, spells can damage other players.
     *
     * @return True if player damage is enabled
     */
    public boolean isPlayerDamage() {
        return playerDamage;
    }

    /**
     * Gets the difficulty level of this wand.
     *
     * @return The wand difficulty
     */
    @NotNull
    public WandDifficulty getDifficulty() {
        return difficulty;
    }

    /**
     * Creates a new builder with the current settings as defaults.
     *
     * @return A new builder
     */
    @NotNull
    public Builder toBuilder() {
        return new Builder(wandKey)
                .cooldownBlock(cooldownBlock)
                .griefBlockDamage(griefBlockDamage)
                .playerDamage(playerDamage)
                .difficulty(difficulty);
    }

    /**
     * Creates a new builder with default settings for the given wand key.
     *
     * @param wandKey The wand key
     * @return A new builder with defaults
     */
    @NotNull
    public static Builder builder(@NotNull String wandKey) {
        return new Builder(wandKey);
    }

    /**
     * Creates default settings for a wand.
     *
     * @param wandKey The wand key
     * @return Default wand settings
     */
    @NotNull
    public static WandSettings defaultSettings(@NotNull String wandKey) {
        return new Builder(wandKey).build();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        WandSettings that = (WandSettings) obj;
        return cooldownBlock == that.cooldownBlock &&
               griefBlockDamage == that.griefBlockDamage &&
               playerDamage == that.playerDamage &&
               Objects.equals(wandKey, that.wandKey) &&
               difficulty == that.difficulty;
    }

    @Override
    public int hashCode() {
        return Objects.hash(wandKey, cooldownBlock, griefBlockDamage, playerDamage, difficulty);
    }

    @Override
    public String toString() {
        return "WandSettings{" +
                "wandKey='" + wandKey + '\'' +
                ", cooldownBlock=" + cooldownBlock +
                ", griefBlockDamage=" + griefBlockDamage +
                ", playerDamage=" + playerDamage +
                ", difficulty=" + difficulty +
                '}';
    }

    /**
     * Builder class for constructing WandSettings instances.
     */
    public static class Builder {
        private final String wandKey;
        private boolean cooldownBlock = false;
        private boolean griefBlockDamage = true;
        private boolean playerDamage = true;
        private WandDifficulty difficulty = WandDifficulty.MEDIUM;

        private Builder(@NotNull String wandKey) {
            this.wandKey = Objects.requireNonNull(wandKey, "Wand key cannot be null");
        }

        /**
         * Sets whether cooldown blocking is enabled.
         *
         * @param cooldownBlock True to enable cooldown blocking
         * @return This builder
         */
        @NotNull
        public Builder cooldownBlock(boolean cooldownBlock) {
            this.cooldownBlock = cooldownBlock;
            return this;
        }

        /**
         * Sets whether grief block damage is enabled.
         *
         * @param griefBlockDamage True to enable grief block damage
         * @return This builder
         */
        @NotNull
        public Builder griefBlockDamage(boolean griefBlockDamage) {
            this.griefBlockDamage = griefBlockDamage;
            return this;
        }

        /**
         * Sets whether player damage is enabled.
         *
         * @param playerDamage True to enable player damage
         * @return This builder
         */
        @NotNull
        public Builder playerDamage(boolean playerDamage) {
            this.playerDamage = playerDamage;
            return this;
        }

        /**
         * Sets the difficulty level.
         *
         * @param difficulty The difficulty level
         * @return This builder
         */
        @NotNull
        public Builder difficulty(@NotNull WandDifficulty difficulty) {
            this.difficulty = Objects.requireNonNull(difficulty, "Difficulty cannot be null");
            return this;
        }

        /**
         * Builds the WandSettings instance.
         *
         * @return The constructed WandSettings
         */
        @NotNull
        public WandSettings build() {
            return new WandSettings(this);
        }
    }
}