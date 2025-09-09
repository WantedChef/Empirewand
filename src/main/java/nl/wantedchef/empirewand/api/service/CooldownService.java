package nl.wantedchef.empirewand.api.service;

import nl.wantedchef.empirewand.api.EmpireWandService;
import nl.wantedchef.empirewand.api.common.AnyThread;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Service for managing spell and wand cooldowns.
 * Provides advanced cooldown tracking and management capabilities.
 * 
 * @since 2.0.0
 */
public interface CooldownService extends EmpireWandService {

    /**
     * Checks if a player-spell combination is on cooldown.
     * 
     * @param playerId the player UUID
     * @param key      the spell key
     * @param nowTicks current ticks
     * @return true if on cooldown
     */
    @AnyThread
    boolean isOnCooldown(@NotNull UUID playerId, @NotNull String key, long nowTicks);

    /**
     * Checks if a player-wand-spell combination is on cooldown, considering
     * disables.
     * 
     * @param playerId the player UUID
     * @param key      the spell key
     * @param nowTicks current ticks
     * @param wand     the wand ItemStack
     * @return true if on cooldown
     */
    @AnyThread
    boolean isOnCooldown(@NotNull UUID playerId, @NotNull String key, long nowTicks, @NotNull ItemStack wand);

    /**
     * Gets remaining cooldown ticks for a player-spell.
     * 
     * @param playerId the player UUID
     * @param key      the spell key
     * @param nowTicks current ticks
     * @return remaining ticks, 0 if not on cooldown
     */
    @AnyThread
    long remaining(@NotNull UUID playerId, @NotNull String key, long nowTicks);

    /**
     * Gets remaining cooldown ticks for a player-wand-spell, considering disables.
     * 
     * @param playerId the player UUID
     * @param key      the spell key
     * @param nowTicks current ticks
     * @param wand     the wand ItemStack
     * @return remaining ticks, 0 if not on cooldown or disabled
     */
    @AnyThread
    long remaining(@NotNull UUID playerId, @NotNull String key, long nowTicks, @NotNull ItemStack wand);

    /**
     * Sets a cooldown for a player-spell.
     * 
     * @param playerId   the player UUID
     * @param key        the spell key
     * @param untilTicks ticks until cooldown ends
     */
    @AnyThread
    void set(@NotNull UUID playerId, @NotNull String key, long untilTicks);

    /**
     * Clears all cooldowns for a player.
     * 
     * @param playerId the player UUID
     */
    @AnyThread
    void clearAll(@NotNull UUID playerId);

    /**
     * Sets cooldown disabled state for a player-wand.
     * 
     * @param playerId the player UUID
     * @param wand     the wand ItemStack
     * @param disabled true to disable cooldowns
     */
    @AnyThread
    void setCooldownDisabled(@NotNull UUID playerId, @NotNull ItemStack wand, boolean disabled);

    /**
     * Checks if cooldowns are disabled for a player-wand.
     * 
     * @param playerId the player UUID
     * @param wand     the wand ItemStack
     * @return true if disabled
     */
    @AnyThread
    boolean isCooldownDisabled(@NotNull UUID playerId, @NotNull ItemStack wand);
}
