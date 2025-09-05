package com.example.empirewand.spell;

import com.example.empirewand.EmpireWandPlugin;
import com.example.empirewand.core.ConfigService;
import com.example.empirewand.core.FxService;
import org.bukkit.entity.Player;

public record SpellContext(EmpireWandPlugin plugin, Player caster, ConfigService config, FxService fx) {
}
