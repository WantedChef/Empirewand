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
import com.example.empirewand.spell.implementation.FrostNova;
import com.example.empirewand.spell.implementation.ChainLightning;
import com.example.empirewand.spell.implementation.BlinkStrike;
import com.example.empirewand.spell.implementation.ShadowCloak;
import com.example.empirewand.spell.implementation.StasisField;
import com.example.empirewand.spell.implementation.Gust;
import com.example.empirewand.spell.implementation.ArcaneOrb;
import com.example.empirewand.spell.implementation.VoidSwap;
import com.example.empirewand.spell.implementation.Sandstorm;
import com.example.empirewand.spell.implementation.Tornado;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SpellRegistry implements com.example.empirewand.api.SpellRegistry {
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

        // Newly added spells
        register(new FrostNova());
        register(new ChainLightning());
        register(new BlinkStrike());
        register(new ShadowCloak());
        register(new StasisField());
        register(new Gust());
        register(new ArcaneOrb());
        register(new VoidSwap());
        register(new Sandstorm());
        register(new Tornado());
    }

    public void register(Spell spell) {
        spells.put(spell.getName(), spell);
    }

    @Override
    public Spell getSpell(String key) {
        return spells.get(key);
    }

    @Override
    public Map<String, Spell> getAllSpells() {
        return Collections.unmodifiableMap(spells);
    }

    @Override
    public Set<String> getSpellKeys() {
        return Collections.unmodifiableSet(spells.keySet());
    }

    @Override
    public boolean isSpellRegistered(String key) {
        return spells.containsKey(key);
    }

    @Override
    public String getSpellDisplayName(String key) {
        Spell spell = spells.get(key);
        return spell != null ? spell.displayName().toString() : key;
    }
}
