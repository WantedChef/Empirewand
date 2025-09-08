package com.example.empirewand.spell.implementation.lightning;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.ConfigService;
import com.example.empirewand.api.EffectService;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class LightningStorm extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Lightning Storm";
            this.description = "Calls down a storm of lightning strikes around you.";
            this.manaCost = 25; // Example
            this.cooldown = java.time.Duration.ofSeconds(45);
            this.spellType = SpellType.LIGHTNING;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new LightningStorm(this);
        }
    }

    private LightningStorm(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "lightning-storm";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        Location center = player.getLocation();

        int strikes = spellConfig.getInt("values.strikes", 8);
        double radius = spellConfig.getDouble("values.radius", 10.0);
        double damage = spellConfig.getDouble("values.damage", 16.0);
        int delayBetweenStrikes = spellConfig.getInt("values.delay-ticks", 10);
        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig().getBoolean("features.friendly-fire", false);

        new BukkitRunnable() {
            private int strikeCount = 0;

            @Override
            public void run() {
                if (strikeCount >= strikes) {
                    this.cancel();
                    return;
                }
                double angle = Math.random() * 2 * Math.PI;
                double distance = Math.random() * radius;
                Location strikeLoc = center.clone().add(distance * Math.cos(angle), 0, distance * Math.sin(angle));

                center.getWorld().strikeLightning(strikeLoc);
                damageAtStrike(context, strikeLoc, damage, friendlyFire);
                strikeCount++;
            }
        }.runTaskTimer(context.plugin(), 0L, delayBetweenStrikes);

        context.fx().playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    private void damageAtStrike(SpellContext context, Location strikeLoc, double damage, boolean friendlyFire) {
        for (var entity : strikeLoc.getWorld().getNearbyLivingEntities(strikeLoc, 3.0)) {
            if (entity.equals(context.caster()) && !friendlyFire)
                continue;
            entity.damage(damage, context.caster());
            context.fx().spawnParticles(entity.getLocation(), Particle.ELECTRIC_SPARK, 10, 0.2, 0.2, 0.2, 0.1);
        }
    }
}