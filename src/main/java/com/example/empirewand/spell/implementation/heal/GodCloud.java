package com.example.empirewand.spell.implementation.heal;

import com.example.empirewand.api.EmpireWandAPI;

import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class GodCloud extends Spell<Void> {

    private static final String METADATA_KEY = "god_cloud_active";

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "God Cloud";
            this.description = "Creates a cloud under your feet when flying.";
            this.manaCost = 10; // Example
            this.cooldown = java.time.Duration.ofSeconds(45);
            this.spellType = SpellType.HEAL;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new GodCloud(this);
        }
    }

    private GodCloud(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "god-cloud";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player caster = context.caster();
        if (caster.hasMetadata(METADATA_KEY)) {
            caster.removeMetadata(METADATA_KEY, context.plugin());
            return null;
        }

        int durationTicks = spellConfig.getInt("values.duration-ticks", 600);
        int particleInterval = spellConfig.getInt("values.particle-interval", 2);
        int particleCount = spellConfig.getInt("values.particle-count", 6);
        double haloRadius = spellConfig.getDouble("halo.radius", 2.8);
        int haloPoints = spellConfig.getInt("halo.points", 24);
        int wispCount = spellConfig.getInt("wisp.count", 4);

        caster.setMetadata(METADATA_KEY, new FixedMetadataValue(context.plugin(), true));
        new CloudEffectTask(caster, durationTicks, particleInterval, particleCount, haloRadius, haloPoints, wispCount, context).runTaskTimer(context.plugin(), 0L, particleInterval);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    private class CloudEffectTask extends BukkitRunnable {
        private final Player player;
        private final int duration;
        private final int count;
        private final double haloRadius;
        private final int haloPoints;
        private final int wispCount;
        private final SpellContext context;
        private int ticks = 0;

        public CloudEffectTask(Player player, int duration, int interval, int count, double haloRadius, int haloPoints, int wispCount, SpellContext context) {
            this.player = player;
            this.duration = duration;
            this.count = count;
            this.haloRadius = haloRadius;
            this.haloPoints = haloPoints;
            this.wispCount = wispCount;
            this.context = context;
        }

        @Override
        public void run() {
            if (ticks >= duration || !player.isValid() || !player.hasMetadata(METADATA_KEY)) {
                player.removeMetadata(METADATA_KEY, context.plugin());
                this.cancel();
                return;
            }

            if (player.isFlying() || player.isGliding()) {
                Location base = player.getLocation();
                context.fx().spawnParticles(base.clone().add(0, -0.9, 0), Particle.CLOUD, count, 0.3, 0.05, 0.3, 0);

                double angleOffset = (ticks / 8.0);
                for (int i = 0; i < haloPoints; i++) {
                    double a = (2 * Math.PI * i / haloPoints) + angleOffset;
                    base.getWorld().spawnParticle(Particle.CRIT, base.getX() + (Math.cos(a) * haloRadius), base.getY() + 0.2, base.getZ() + (Math.sin(a) * haloRadius), 1, 0, 0, 0, 0);
                }

                if (ticks % 40 == 0) {
                    context.fx().playSound(player.getLocation(), Sound.AMBIENT_CAVE, 0.3f, 1.0f);
                }
            }
            ticks += 2; // Since interval is 2
        }
    }
}