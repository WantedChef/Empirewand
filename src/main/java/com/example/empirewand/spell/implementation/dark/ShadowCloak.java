package com.example.empirewand.spell.implementation.dark;

import com.example.empirewand.api.EmpireWandAPI;

import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class ShadowCloak extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Shadow Cloak";
            this.description = "Become invisible for a short time.";
            this.manaCost = 6;
            this.cooldown = java.time.Duration.ofSeconds(30);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new ShadowCloak(this);
        }
    }

    private ShadowCloak(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "shadow-cloak";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        int duration = spellConfig.getInt("values.duration-ticks", 120);
        int speedAmp = spellConfig.getInt("values.speed-amplifier", 1);

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, speedAmp, false, true));

        context.fx().spawnParticles(player.getLocation(), Particle.WITCH, 40, 0.6, 1.0, 0.6, 0.01);
        context.fx().playSound(player, Sound.ENTITY_ENDERMAN_AMBIENT, 0.6f, 0.5f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect, no handling needed.
    }
}