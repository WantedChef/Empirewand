package com.example.empirewand.core.services.toggle;

import com.example.empirewand.api.ServiceHealth;
import com.example.empirewand.api.Version;
import com.example.empirewand.api.spell.toggle.SpellManager;
import com.example.empirewand.api.spell.toggle.ToggleableSpell;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Implementation of the SpellManager service.
 * Manages the state and lifecycle of toggleable spells.
 */
public class SpellManagerImpl implements SpellManager {

    private final Plugin plugin;
    private final Map<UUID, Set<ToggleableSpell>> activeSpells = new ConcurrentHashMap<>();

    public SpellManagerImpl(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean toggleSpell(@NotNull Player player, @NotNull Spell<?> spell, @NotNull SpellContext context) {
        if (!isToggleableSpell(spell)) {
            return false;
        }

        ToggleableSpell toggleableSpell = (ToggleableSpell) spell;
        try {
            toggleableSpell.toggle(player, context);

            if (toggleableSpell.isActive(player)) {
                addActiveSpell(player, toggleableSpell);
            } else {
                removeActiveSpell(player, toggleableSpell);
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error toggling spell " + spell.key() + " for player " + player.getName(), e);
            return false;
        }
    }

    @Override
    public boolean activateSpell(@NotNull Player player, @NotNull Spell<?> spell, @NotNull SpellContext context) {
        if (!isToggleableSpell(spell)) {
            return false;
        }

        ToggleableSpell toggleableSpell = (ToggleableSpell) spell;
        if (toggleableSpell.isActive(player)) {
            return false; // Already active
        }

        try {
            toggleableSpell.activate(player, context);
            addActiveSpell(player, toggleableSpell);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error activating spell " + spell.key() + " for player " + player.getName(), e);
            return false;
        }
    }

    @Override
    public boolean deactivateSpell(@NotNull Player player, @NotNull Spell<?> spell, @NotNull SpellContext context) {
        if (!isToggleableSpell(spell)) {
            return false;
        }

        ToggleableSpell toggleableSpell = (ToggleableSpell) spell;
        if (!toggleableSpell.isActive(player)) {
            return false; // Not active
        }

        try {
            toggleableSpell.deactivate(player, context);
            removeActiveSpell(player, toggleableSpell);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error deactivating spell " + spell.key() + " for player " + player.getName(), e);
            return false;
        }
    }

    @Override
    public boolean isSpellActive(@NotNull Player player, @NotNull Spell<?> spell) {
        if (!isToggleableSpell(spell)) {
            return false;
        }

        ToggleableSpell toggleableSpell = (ToggleableSpell) spell;
        return toggleableSpell.isActive(player);
    }

    @Override
    @NotNull
    public Set<ToggleableSpell> getActiveSpells(@NotNull Player player) {
        return new HashSet<>(activeSpells.getOrDefault(player.getUniqueId(), Collections.emptySet()));
    }

    @Override
    public void deactivateAllSpells(@NotNull Player player) {
        Set<ToggleableSpell> playerSpells = activeSpells.remove(player.getUniqueId());
        if (playerSpells != null && !playerSpells.isEmpty()) {
            for (ToggleableSpell spell : playerSpells) {
                try {
                    spell.forceDeactivate(player);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING,
                            "Error force-deactivating spell for player " + player.getName(), e);
                }
            }
        }
    }

    @Override
    public int getActiveSpellCount(@NotNull Player player) {
        return activeSpells.getOrDefault(player.getUniqueId(), Collections.emptySet()).size();
    }

    @Override
    public boolean isToggleableSpell(@NotNull Spell<?> spell) {
        return spell instanceof ToggleableSpell;
    }

    // Helper methods

    private void addActiveSpell(@NotNull Player player, @NotNull ToggleableSpell spell) {
        activeSpells.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(spell);
    }

    private void removeActiveSpell(@NotNull Player player, @NotNull ToggleableSpell spell) {
        Set<ToggleableSpell> playerSpells = activeSpells.get(player.getUniqueId());
        if (playerSpells != null) {
            playerSpells.remove(spell);
            if (playerSpells.isEmpty()) {
                activeSpells.remove(player.getUniqueId());
            }
        }
    }

    // EmpireWandService implementation

    @Override
    @NotNull
    public String getServiceName() {
        return "SpellManager";
    }

    @Override
    @NotNull
    public Version getServiceVersion() {
        return Version.of(2, 1, 0);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    @NotNull
    public ServiceHealth getHealth() {
        return ServiceHealth.HEALTHY;
    }

    @Override
    public void reload() {
        // Clear all active spells on reload
        for (Map.Entry<UUID, Set<ToggleableSpell>> entry : activeSpells.entrySet()) {
            UUID playerId = entry.getKey();
            Set<ToggleableSpell> playerSpells = entry.getValue();
            if (playerSpells != null) {
                for (ToggleableSpell spell : playerSpells) {
                    try {
                        // Try to get player from UUID and force deactivate
                        Player player = plugin.getServer().getPlayer(playerId);
                        if (player != null && player.isOnline()) {
                            spell.forceDeactivate(player);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING,
                                "Error deactivating spell during reload", e);
                    }
                }
            }
        }
        activeSpells.clear();
    }
}