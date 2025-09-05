package com.example.empirewand.spell;

import com.example.empirewand.EmpireWandPlugin;
import com.example.empirewand.core.ConfigService;
import com.example.empirewand.core.FxService;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public record SpellContext(
    EmpireWandPlugin plugin,
    Player caster,
    ConfigService config,
    FxService fx,
    LivingEntity target,
    Location targetLocation,
    String spellKey
) {
    // Constructor for backward compatibility
    public SpellContext(EmpireWandPlugin plugin, Player caster, ConfigService config, FxService fx) {
        this(plugin, caster, config, fx, null, null, null);
    }

    // Factory method for targeted spells
    public static SpellContext targeted(EmpireWandPlugin plugin, Player caster, ConfigService config, FxService fx, LivingEntity target, String spellKey) {
        return new SpellContext(plugin, caster, config, fx, target, target != null ? target.getLocation() : null, spellKey);
    }

    // Factory method for location-targeted spells
    public static SpellContext location(EmpireWandPlugin plugin, Player caster, ConfigService config, FxService fx, Location location, String spellKey) {
        return new SpellContext(plugin, caster, config, fx, null, location, spellKey);
    }
}
