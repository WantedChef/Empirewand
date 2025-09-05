package com.example.empirewand.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Public API interface for managing EmpireWand wands.
 *
 * <p>This interface provides methods to create, identify, and manipulate wands programmatically.
 * External plugins can use this to integrate wand functionality.</p>
 *
 * <p><b>API Stability:</b> Experimental - Subject to change in future versions</p>
 *
 * @since 1.1.0
 */
public interface WandService {

    /**
     * Creates a new EmpireWand.
     *
     * @return a new wand ItemStack
     */
    @NotNull
    ItemStack createWand();

    /**
     * Checks if an ItemStack is an EmpireWand.
     *
     * @param item the item to check
     * @return true if the item is a wand, false otherwise
     */
    boolean isWand(@Nullable ItemStack item);

    /**
     * Gets the spells bound to a wand.
     *
     * @param wand the wand ItemStack
     * @return a list of spell keys bound to the wand
     */
    @NotNull
    List<String> getBoundSpells(@NotNull ItemStack wand);

    /**
     * Binds a spell to a wand.
     *
     * @param wand the wand ItemStack
     * @param spellKey the spell key to bind
     * @return true if the spell was bound successfully, false otherwise
     */
    boolean bindSpell(@NotNull ItemStack wand, @NotNull String spellKey);

    /**
     * Unbinds a spell from a wand.
     *
     * @param wand the wand ItemStack
     * @param spellKey the spell key to unbind
     * @return true if the spell was unbound successfully, false otherwise
     */
    boolean unbindSpell(@NotNull ItemStack wand, @NotNull String spellKey);

    /**
     * Sets the active spell index for a wand.
     *
     * @param wand the wand ItemStack
     * @param index the spell index (0-based)
     * @return true if the index was set successfully, false otherwise
     */
    boolean setActiveSpell(@NotNull ItemStack wand, int index);

    /**
     * Gets the active spell index for a wand.
     *
     * @param wand the wand ItemStack
     * @return the active spell index, or -1 if no active spell
     */
    int getActiveSpellIndex(@NotNull ItemStack wand);

    /**
     * Gets the active spell key for a wand.
     *
     * @param wand the wand ItemStack
     * @return the active spell key, or null if no active spell
     */
    @Nullable
    String getActiveSpellKey(@NotNull ItemStack wand);

    /**
     * Gets the wand a player is currently holding.
     *
     * @param player the player to check
     * @return the wand ItemStack, or null if player is not holding a wand
     */
    @Nullable
    ItemStack getHeldWand(@NotNull Player player);

    /**
     * Gives a wand to a player.
     *
     * @param player the player to give the wand to
     * @return true if the wand was given successfully, false otherwise
     */
    boolean giveWand(@NotNull Player player);
}