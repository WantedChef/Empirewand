package com.example.empirewand.spell;

import org.bukkit.Location;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.example.empirewand.EmpireWandPlugin;
import com.example.empirewand.core.ConfigService;
import com.example.empirewand.core.FxService;

/**
 * Immutable context passed to spells. Provides access to core services plus
 * optional targeting information.
 * Defensive cloning is applied for the target location on construction and on
 * accessor to avoid exposing mutable Bukkit objects.
 */
@SuppressFBWarnings(value = { "EI_EXPOSE_REP",
        "EI_EXPOSE_REP2" }, justification = "Location is defensively cloned on construction and accessor; record components kept for concise immutable API")
public record SpellContext(
        EmpireWandPlugin plugin,
        Player caster,
        ConfigService config,
        FxService fx,
        LivingEntity target,
        Location targetLocation,
        String spellKey) {

    // Canonical constructor with defensive clone for targetLocation
    public SpellContext {
        if (targetLocation != null) {
            targetLocation = targetLocation.clone();
        }
    }

    // Convenience constructor (no target / key)
    public SpellContext(EmpireWandPlugin plugin, Player caster, ConfigService config, FxService fx) {
        this(plugin, caster, config, fx, null, null, null);
    }

    // Factory for targeted (entity) spells
    public static SpellContext targeted(EmpireWandPlugin plugin, Player caster, ConfigService config, FxService fx,
            LivingEntity target, String spellKey) {
        Location loc = target != null ? target.getLocation().clone() : null;
        return new SpellContext(plugin, caster, config, fx, target, loc, spellKey);
    }

    // Factory for location based spells
    public static SpellContext location(EmpireWandPlugin plugin, Player caster, ConfigService config, FxService fx,
            Location location, String spellKey) {
        return new SpellContext(plugin, caster, config, fx, null, location != null ? location.clone() : null, spellKey);
    }

    // Defensive clone on accessor
    @Override
    public Location targetLocation() {
        return targetLocation != null ? targetLocation.clone() : null;
    }
}
