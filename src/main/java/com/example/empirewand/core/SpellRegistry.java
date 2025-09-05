package com.example.empirewand.core;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.implementation.Comet;
import com.example.empirewand.spell.implementation.Explosive;
import com.example.empirewand.spell.implementation.Leap;
import com.example.empirewand.spell.implementation.MagicMissile;
import com.example.empirewand.spell.implementation.Heal;
import com.example.empirewand.spell.implementation.GlacialSpike;
import com.example.empirewand.spell.implementation.GraspingVines;
import com.example.empirewand.spell.implementation.LifeSteal;
import com.example.empirewand.spell.implementation.Polymorph;
import com.example.empirewand.spell.implementation.EtherealForm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SpellRegistry {
    private final Map<String, Spell> spells = new HashMap<>();

    public SpellRegistry() {
        registerSpells();
    }

    private void registerSpells() {
        register(new Leap());
        register(new Comet());
        register(new Explosive());
        register(new MagicMissile());
        register(new Heal());
        register(new GlacialSpike());
        register(new GraspingVines());
        register(new LifeSteal());
        register(new Polymorph());
        register(new EtherealForm());
    }

    public void register(Spell spell) {
        spells.put(spell.getName(), spell);
    }

    public Spell getSpell(String key) {
        return spells.get(key);
    }

    public Map<String, Spell> getAllSpells() {
        return Collections.unmodifiableMap(spells);
    }
}
