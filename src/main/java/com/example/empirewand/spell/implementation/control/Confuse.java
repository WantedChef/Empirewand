package com.example.empirewand.spell.implementation.control;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.EffectService;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class Confuse extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Confuse";
            this.description = "Confuses and slows a target entity.";
            this.manaCost = 8;
            this.cooldown = java.time.Duration.ofSeconds(15);
            this.spellType = SpellType.CONTROL;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new Confuse(this);
        }
    }

    private Confuse(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "confuse";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        double range = spellConfig.getDouble("values.range", 10.0);
        int duration = spellConfig.getInt("values.duration-ticks", 100);
        int slownessAmplifier = spellConfig.getInt("values.slowness-amplifier", 1);

        LivingEntity target = null;
        Entity rawTarget = player.getTargetEntity((int) range);
        if (rawTarget instanceof LivingEntity le) {
            target = le;
        }
        if (target == null || target.isDead() || !target.isValid()) {
            context.fx().showError(player, "no-target");
            return null;
        }

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, slownessAmplifier, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, duration, 0, false, true));

        context.fx().playSound(player, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.0f);
        context.fx().spawnParticles(target.getLocation(), Particle.ENCHANT, 20, 0.5, 0.5, 0.5, 0.0);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effect is handled in executeSpell
    }
}