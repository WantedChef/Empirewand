package nl.wantedchef.empirewand.api.impl;

import nl.wantedchef.empirewand.api.service.CooldownService;
import nl.wantedchef.empirewand.api.ServiceHealth;
import nl.wantedchef.empirewand.api.Version;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Adapter for CooldownService that wraps core CooldownService implementation.
 * Delegates all cooldown operations to core while providing API contract.
 * Implements EmpireWandService base methods with defaults.
 *
 * <h2>Usage Example:</h2>
 * 
 * <pre>{@code
 * // Get the cooldown service from the API
 * CooldownService cooldowns = EmpireWandAPI.getProvider().getCooldownService();
 *
 * // Check if a spell is on cooldown
 * boolean onCooldown = cooldowns.isOnCooldown(playerId, "fireball", currentTicks);
 * boolean onCooldownWithWand = cooldowns.isOnCooldown(playerId, "fireball", currentTicks, wand);
 *
 * // Get remaining cooldown time
 * long remainingTicks = cooldowns.remaining(playerId, "fireball", currentTicks);
 *
 * // Set a cooldown
 * long cooldownEndTicks = currentTicks + 100; // 5 seconds at 20 TPS
 * cooldowns.set(playerId, "fireball", cooldownEndTicks);
 *
 * // Manage cooldown disabling per wand
 * cooldowns.setCooldownDisabled(playerId, wand, true); // Disable cooldowns for this wand
 * boolean disabled = cooldowns.isCooldownDisabled(playerId, wand);
 *
 * // Clear all cooldowns for a player
 * cooldowns.clearAll(playerId);
 * }</pre>
 *
 * @since 2.0.0
 */
public class CooldownServiceAdapter implements CooldownService {

    private final nl.wantedchef.empirewand.framework.service.CooldownService core;

    /**
     * Constructor.
     *
     * @param core the core CooldownService to wrap
     */
    public CooldownServiceAdapter(nl.wantedchef.empirewand.framework.service.CooldownService core) {
        if (core == null) {
            throw new IllegalArgumentException("core CooldownService cannot be null");
        }
        this.core = core;
    }

    // EmpireWandService implementations

    @Override
    public @NotNull String getServiceName() {
        return "CooldownService";
    }

    @Override
    public @NotNull Version getServiceVersion() {
        return Version.of(2, 0, 0);
    }

    @Override
    public boolean isEnabled() {
        return true; // Assume enabled if core is injected
    }

    @Override
    public @NotNull ServiceHealth getHealth() {
        try {
            // Check if core service is available
            if (core == null) {
                return ServiceHealth.UNHEALTHY;
            }

            return ServiceHealth.HEALTHY;
        } catch (Exception e) {
            return ServiceHealth.UNHEALTHY;
        }
    }

    @Override
    public void reload() {
        try {
            // Clear all cooldowns on reload for a fresh start
            if (core != null) {
                // Note: This is a design decision - reload could either clear all cooldowns
                // or preserve them. Here we choose to preserve them but could add a parameter
                // to control this behavior in the future.
            }
        } catch (Exception e) {
            // Log error but don't propagate - graceful degradation
            System.err.println("Failed to reload cooldown service: " + e.getMessage());
        }
    }

    // CooldownService implementations

    @Override
    public boolean isOnCooldown(@NotNull UUID playerId, @NotNull String key, long nowTicks) {
        return core.isOnCooldown(playerId, key, nowTicks);
    }

    @Override
    public boolean isOnCooldown(@NotNull UUID playerId, @NotNull String key, long nowTicks, @NotNull ItemStack wand) {
        return core.isOnCooldown(playerId, key, nowTicks, wand);
    }

    @Override
    public long remaining(@NotNull UUID playerId, @NotNull String key, long nowTicks) {
        return core.remaining(playerId, key, nowTicks);
    }

    @Override
    public long remaining(@NotNull UUID playerId, @NotNull String key, long nowTicks, @NotNull ItemStack wand) {
        return core.remaining(playerId, key, nowTicks, wand);
    }

    @Override
    public void set(@NotNull UUID playerId, @NotNull String key, long untilTicks) {
        core.set(playerId, key, untilTicks);
    }

    @Override
    public void clearAll(@NotNull UUID playerId) {
        core.clearAll(playerId);
    }

    @Override
    public void setCooldownDisabled(@NotNull UUID playerId, @NotNull ItemStack wand, boolean disabled) {
        core.setCooldownDisabled(playerId, wand, disabled);
    }

    @Override
    public boolean isCooldownDisabled(@NotNull UUID playerId, @NotNull ItemStack wand) {
        return core.isCooldownDisabled(playerId, wand);
    }

    // Additional API-specific methods can be added here if needed beyond core

}
