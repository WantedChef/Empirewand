package com.example.empirewand.spell.implementation.fire;

import com.example.empirewand.api.EmpireWandAPI;

import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class BlazeLaunch extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Blaze Launch";
            this.description = "Launches you forward, leaving a trail of fire.";
            this.cooldown = java.time.Duration.ofSeconds(10);
            this.spellType = SpellType.FIRE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new BlazeLaunch(this);
        }
    }

    private BlazeLaunch(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "blaze-launch";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double power = spellConfig.getDouble("values.power", 1.8);
        int trailDuration = spellConfig.getInt("values.trail-duration-ticks", 40);

        Vector direction = player.getEyeLocation().getDirection().normalize();
        player.setVelocity(direction.multiply(power));

        startFireTrail(player, trailDuration, context);

        context.fx().playSound(player, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    private void startFireTrail(Player player, int duration, SpellContext context) {
        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration || !player.isValid() || player.isDead()) {
                    this.cancel();
                    return;
                }

                Location playerLoc = player.getLocation();
                context.fx().spawnParticles(playerLoc, Particle.FLAME, 10, 0.2, 0.2, 0.2, 0.05);

                if (ticks % 20 == 0) {
                    context.fx().playSound(playerLoc, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 1.0f);
                }
                ticks++;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }
}