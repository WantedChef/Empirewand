package com.example.empirewand.spell.implementation.life;

import com.example.empirewand.api.ConfigService;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.ProjectileSpell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class LifeSteal extends ProjectileSpell<Snowball> {

    public static class Builder extends ProjectileSpell.Builder<Snowball> {
        public Builder(EmpireWandAPI api) {
            super(api, Snowball.class);
            this.name = "Life Steal";
            this.description = "Steals health from the target.";
            this.manaCost = 7; // Example
            this.cooldown = java.time.Duration.ofSeconds(3);
            this.spellType = SpellType.LIFE;
            this.trailParticle = null; // Custom trail
            this.hitSound = Sound.ENTITY_GENERIC_DRINK;
        }

        @Override
        @NotNull
        public ProjectileSpell<Snowball> build() {
            return new LifeSteal(this);
        }
    }

    private LifeSteal(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "lifesteal";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected void launchProjectile(@NotNull SpellContext context) {
        super.launchProjectile(context);
        context.fx().playSound(context.caster(), Sound.ENTITY_WITCH_THROW, 0.8f, 1.2f);
        context.fx().followParticles(context.plugin(), context.caster().launchProjectile(Snowball.class),
                Particle.DUST, 8, 0.08, 0.08, 0.08, 0, new Particle.DustOptions(Color.fromRGB(170, 0, 0), 1.0f), 1L);
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event) {
        if (!(event.getHitEntity() instanceof LivingEntity target))
            return;

        double damage = spellConfig.getDouble("values.damage", 4.0);
        double stealModifier = spellConfig.getDouble("values.steal-modifier", 0.5);
        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig().getBoolean("features.friendly-fire", false);

        Player caster = context.caster();
        if (target.equals(caster) && !friendlyFire)
            return;

        target.damage(damage, caster);
        double stolenHealth = damage * stealModifier;
        double maxHealth = Objects.requireNonNull(caster.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getValue();
        caster.setHealth(Math.min(maxHealth, caster.getHealth() + stolenHealth));

        context.fx().spawnParticles(caster.getLocation(), Particle.HEART, 5, 0.5, 1, 0.5, 0);
    }
}