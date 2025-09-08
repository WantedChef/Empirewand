package com.example.empirewand.spell.implementation.fire;

import com.example.empirewand.api.EmpireWandAPI;

import com.example.empirewand.core.storage.Keys;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.ProjectileSpell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class Explosive extends ProjectileSpell<WitherSkull> {

    public static class Builder extends ProjectileSpell.Builder<WitherSkull> {
        public Builder(EmpireWandAPI api) {
            super(api, WitherSkull.class);
            this.name = "Explosive";
            this.description = "Launches an explosive skull.";
            this.manaCost = 10; // Example value
            this.cooldown = java.time.Duration.ofSeconds(8);
            this.spellType = SpellType.FIRE;
            this.trailParticle = Particle.SMOKE;
        }

        @Override
        @NotNull
        public ProjectileSpell<WitherSkull> build() {
            return new Explosive(this);
        }
    }

    private Explosive(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "explosive";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected void launchProjectile(@NotNull SpellContext context) {
        Player caster = context.caster();
        float yield = (float) spellConfig.getDouble("values.radius", 4.0);
        boolean setsFire = spellConfig.getBoolean("flags.sets-fire", false);

        context.fx().playSound(caster, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.0f);
        caster.launchProjectile(WitherSkull.class, caster.getEyeLocation().getDirection(), skull -> {
            skull.setYield(yield);
            skull.setIsIncendiary(setsFire);
            skull.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, key());
            skull.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING, caster.getUniqueId().toString());
        });
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile, @NotNull ProjectileHitEvent event) {
        context.fx().impact(projectile.getLocation());
    }
}