package com.example.empirewand.spell.implementation.fire;

import com.example.empirewand.api.EmpireWandAPI;

import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class CometShower extends Spell<Void> {

    public record Config(int cometCount, double radius, double explosionYield, int delayTicks) {}

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Comet Shower";
            this.description = "Rains down comets on a target area.";
            this.manaCost = 25; // Example value
            this.cooldown = java.time.Duration.ofSeconds(30);
            this.spellType = SpellType.FIRE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new CometShower(this);
        }
    }

    private final Config config;

    private CometShower(Builder builder) {
        super(builder);
        this.config = new Config(
                spellConfig.getInt("values.comet-count", 5),
                spellConfig.getDouble("values.radius", 8.0),
                spellConfig.getDouble("values.yield", 2.6),
                spellConfig.getInt("values.delay-ticks", 6)
        );
    }

    @Override
    public String key() {
        return "comet-shower";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player caster = context.caster();
        Location targetLocation = findTargetLocation(caster);

        new BukkitRunnable() {
            private int launched = 0;

            @Override
            public void run() {
                if (launched >= config.cometCount) {
                    this.cancel();
                    return;
                }
                launchComet(caster, targetLocation, config.radius, config.explosionYield, context);
                launched++;
            }
        }.runTaskTimer(context.plugin(), 0L, config.delayTicks);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    private Location findTargetLocation(Player caster) {
        RayTraceResult rayTrace = caster.getWorld().rayTraceBlocks(caster.getEyeLocation(), caster.getEyeLocation().getDirection(), 25.0);
        if (rayTrace != null && rayTrace.getHitPosition() != null) {
            return rayTrace.getHitPosition().toLocation(caster.getWorld());
        }
        return caster.getEyeLocation().add(caster.getEyeLocation().getDirection().multiply(20));
    }

    private void launchComet(Player caster, Location center, double radius, double explosionYield, SpellContext context) {
        double angle = Math.random() * 2 * Math.PI;
        double distance = Math.random() * radius;
        double x = center.getX() + distance * Math.cos(angle);
        double z = center.getZ() + distance * Math.sin(angle);
        Location spawnLoc = new Location(center.getWorld(), x, center.getY() + 12, z);

        LargeFireball comet = caster.getWorld().spawn(spawnLoc, LargeFireball.class, c -> {
            c.setYield((float) explosionYield);
            c.setIsIncendiary(false);
            c.setDirection(new Vector(0, -1, 0));
            c.setShooter(caster);
        });

        context.fx().spawnParticles(spawnLoc, Particle.FLAME, 10, 0.2, 0.2, 0.2, 0.1);
        context.fx().spawnParticles(spawnLoc, Particle.LAVA, 5, 0.1, 0.1, 0.1, 0.05);
    }
}