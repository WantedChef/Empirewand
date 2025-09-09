package com.example.empirewand.spell.implementation.movement;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Leap extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Leap";
            this.description = "Leaps you forward.";
            this.cooldown = java.time.Duration.ofSeconds(2);
            this.spellType = SpellType.MOVEMENT;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new Leap(this);
        }
    }

    private Leap(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "leap";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        double multiplier = spellConfig.getDouble("values.velocity-multiplier", 1.5);
        double verticalBoost = spellConfig.getDouble("values.vertical-boost", 0.0);

        player.setVelocity(player.getLocation().getDirection().normalize().multiply(multiplier).setY(verticalBoost));

        context.fx().spawnParticles(player.getLocation(), Particle.CLOUD, 16, 0.3, 0.1, 0.3, 0.02);
        context.fx().playSound(player, Sound.ENTITY_RABBIT_JUMP, 0.8f, 1.2f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }
}