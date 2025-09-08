package com.example.empirewand.spell.implementation.heal;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.EffectService;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Heal extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Heal";
            this.description = "Heals the caster.";
            this.manaCost = 5; // Example
            this.cooldown = java.time.Duration.ofSeconds(5);
            this.spellType = SpellType.HEAL;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new Heal(this);
        }
    }

    private Heal(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "heal";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        double healAmount = spellConfig.getDouble("values.heal-amount", 8.0);
        var maxAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = maxAttr != null ? maxAttr.getValue() : 20.0;

        player.setHealth(Math.min(maxHealth, player.getHealth() + healAmount));

        context.fx().spawnParticles(player.getLocation().add(0, 1.0, 0), Particle.HEART, 8, 0.4, 0.4, 0.4, 0.01);
        context.fx().playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }
}