package com.example.empirewand.spell.implementation.earth;

import com.example.empirewand.api.EmpireWandAPI;

import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class EarthQuake extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Earth Quake";
            this.description = "Shakes the ground and knocks back nearby entities.";
            this.manaCost = 10;
            this.cooldown = java.time.Duration.ofSeconds(20);
            this.spellType = SpellType.EARTH;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new EarthQuake(this);
        }
    }

    private EarthQuake(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "earth-quake";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player caster = context.caster();

        double radius = spellConfig.getDouble("values.radius", 7.0);
        double knockbackStrength = spellConfig.getDouble("values.knockback-strength", 1.1);
        double verticalBoost = spellConfig.getDouble("values.vertical-boost", 0.35);

        for (var entity : caster.getWorld().getNearbyEntities(caster.getLocation(), radius, radius, radius)) {
            if (entity instanceof LivingEntity living && !living.equals(caster) && !living.isDead() && living.isValid()) {
                Vector casterToTarget = living.getLocation().toVector().subtract(caster.getLocation().toVector());
                Vector knockback = casterToTarget.normalize().multiply(knockbackStrength).setY(verticalBoost);
                living.setVelocity(knockback);
                context.fx().spawnParticles(living.getLocation(), Particle.CRIT, 10, 0.3, 0.3, 0.3, 0.1);
            }
        }

        context.fx().spawnParticles(caster.getLocation(), Particle.EXPLOSION, 50, radius, 0.5, radius, 0.1);
        context.fx().playSound(caster.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
        context.fx().playSound(caster.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.8f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }
}