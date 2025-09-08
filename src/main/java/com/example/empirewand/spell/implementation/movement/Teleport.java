package com.example.empirewand.spell.implementation.movement;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.EffectService;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import com.example.empirewand.visual.Afterimages;
import com.example.empirewand.visual.RingRenderer;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;

public class Teleport extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Teleport";
            this.description = "Teleports you to your target location.";
            this.manaCost = 5; // Example
            this.cooldown = java.time.Duration.ofSeconds(5);
            this.spellType = SpellType.MOVEMENT;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new Teleport(this);
        }
    }

    private Teleport(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "teleport";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double range = spellConfig.getDouble("values.range", 15.0);
        boolean requiresLineOfSight = spellConfig.getBoolean("flags.requires-los", true);

        Location targetLoc = getTargetLocation(player, range, requiresLineOfSight);
        if (targetLoc == null || !isLocationSafe(targetLoc)) {
            context.fx().fizzle(player);
            return null;
        }

        Location from = player.getLocation().clone();
        if (Afterimages.get() != null) Afterimages.get().record(from);
        context.fx().spawnParticles(from, Particle.PORTAL, 35, 0.5, 0.8, 0.5, 0.15);
        context.fx().playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        player.teleport(targetLoc);

        Location to = targetLoc.clone();
        if (Afterimages.get() != null) Afterimages.get().record(to);
        context.fx().spawnParticles(to, Particle.PORTAL, 45, 0.6, 1.0, 0.6, 0.2);
        new RingVisual(to, 0.3, 0.35).runTaskTimer(context.plugin(), 0L, 2L);
        context.fx().playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    private Location getTargetLocation(Player player, double range, boolean requiresLineOfSight) {
        BlockIterator iterator = new BlockIterator(player, (int) range);
        Location targetLoc = null;
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (block.getType().isSolid()) {
                targetLoc = block.getLocation().add(0, 1, 0);
                break;
            }
        }
        if (targetLoc == null) {
            targetLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(range));
        }
        return targetLoc;
    }

    private boolean isLocationSafe(Location location) {
        Block feetBlock = location.getBlock();
        Block headBlock = location.clone().add(0, 1, 0).getBlock();
        Block groundBlock = location.clone().add(0, -1, 0).getBlock();
        return !feetBlock.getType().isSolid() && !headBlock.getType().isSolid() && groundBlock.getType().isSolid();
    }

    private static class RingVisual extends BukkitRunnable {
        private final Location center;
        private double radius;
        private final double radiusStep;
        private int steps = 0;

        public RingVisual(Location center, double initialRadius, double radiusStep) {
            this.center = center;
            this.radius = initialRadius;
            this.radiusStep = radiusStep;
        }

        @Override
        public void run() {
            if (steps++ > 6 || center.getWorld() == null) {
                cancel();
                return;
            }
            RingRenderer.renderRing(center, radius, 32, Particle.CRIT);
            radius += radiusStep;
        }
    }
}