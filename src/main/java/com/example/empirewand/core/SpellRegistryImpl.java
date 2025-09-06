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
import com.example.empirewand.spell.implementation.Aura;
import com.example.empirewand.spell.implementation.BlazeLaunch;
import com.example.empirewand.spell.implementation.BloodBarrier;
import com.example.empirewand.spell.implementation.BloodBlock;
import com.example.empirewand.spell.implementation.BloodNova;
import com.example.empirewand.spell.implementation.BloodSpam;
import com.example.empirewand.spell.implementation.BloodTap;
import com.example.empirewand.spell.implementation.CometShower;
import com.example.empirewand.spell.implementation.Confuse;
import com.example.empirewand.spell.implementation.CrimsonChains;
import com.example.empirewand.spell.implementation.DarkCircle;
import com.example.empirewand.spell.implementation.DarkPulse;
import com.example.empirewand.spell.implementation.EarthQuake;
import com.example.empirewand.spell.implementation.EmpireAura;
import com.example.empirewand.spell.implementation.EmpireComet;
import com.example.empirewand.spell.implementation.EmpireEscape;
import com.example.empirewand.spell.implementation.EmpireLaunch;
import com.example.empirewand.spell.implementation.EmpireLevitate;
import com.example.empirewand.spell.implementation.ExplosionTrail;
import com.example.empirewand.spell.implementation.ExplosionWave;
import com.example.empirewand.spell.implementation.Fireball;
import com.example.empirewand.spell.implementation.FlameWave;
import com.example.empirewand.spell.implementation.GodCloud;
import com.example.empirewand.spell.implementation.Hemorrhage;
import com.example.empirewand.spell.implementation.LifeReap;
import com.example.empirewand.spell.implementation.LightningArrow;
import com.example.empirewand.spell.implementation.LightningBolt;
import com.example.empirewand.spell.implementation.LightningStorm;
import com.example.empirewand.spell.implementation.Lightwall;
import com.example.empirewand.spell.implementation.LittleSpark;
import com.example.empirewand.spell.implementation.MephidicReap;
import com.example.empirewand.spell.implementation.PoisonWave;
import com.example.empirewand.spell.implementation.RadiantBeacon;
import com.example.empirewand.spell.implementation.RitualOfUnmaking;
import com.example.empirewand.spell.implementation.SolarLance;
import com.example.empirewand.spell.implementation.SoulSever;
import com.example.empirewand.spell.implementation.Spark;
import com.example.empirewand.spell.implementation.SunburstStep;
import com.example.empirewand.spell.implementation.ShadowStep;
import com.example.empirewand.spell.implementation.Teleport;
import com.example.empirewand.spell.implementation.ThunderBlast;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SpellRegistryImpl implements com.example.empirewand.api.SpellRegistry {
    private final Map<String, Spell> spells = new HashMap<>();

    public SpellRegistryImpl() {
        registerSpells();
    }

    private void registerSpells() {
        // Core spells
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

        // Advanced spells
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

        // Additional spells from spells.yml
        register(new Aura());
        register(new BlazeLaunch());
        register(new BloodBarrier());
        register(new BloodBlock());
        register(new BloodNova());
        register(new BloodSpam());
        register(new BloodTap());
        register(new CometShower());
        register(new Confuse());
        register(new CrimsonChains());
        register(new DarkCircle());
        register(new DarkPulse());
        register(new EarthQuake());
        register(new EmpireAura());
        register(new EmpireComet());
        register(new EmpireEscape());
        register(new EmpireLaunch());
        register(new EmpireLevitate());
        register(new ExplosionTrail());
        register(new ExplosionWave());
        register(new Fireball());
        register(new FlameWave());
        register(new GodCloud());
        register(new Hemorrhage());
        register(new LifeReap());
        register(new LightningArrow());
        register(new LightningBolt());
        register(new LightningStorm());
        register(new Lightwall());
        register(new LittleSpark());
        register(new MephidicReap());
        register(new PoisonWave());
        register(new RadiantBeacon());
        register(new RitualOfUnmaking());
        register(new SolarLance());
        register(new SoulSever());
        register(new Spark());
        register(new SunburstStep());
        register(new ShadowStep());
        register(new Teleport());
        register(new ThunderBlast());
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
        Spell spell = getSpell(key);
        if (spell != null) {
            return spell.displayName().toString();
        }
        return key;
    }
}
