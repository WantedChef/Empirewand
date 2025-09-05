package com.example.empirewand.spell;

import org.bukkit.entity.Player;

public interface Spell {
    void execute(SpellContext context);
    String getName();
    // TODO: add other methods from technical.md (key, displayName, prereq)
}
