package com.example.empirewand.spell.implementation.dark;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import com.example.empirewand.visual.Afterimages;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;

public class ShadowStep extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Shadow Step";
            this.description = "Short-range blink with afterimages.";
            this.manaCost = 8;
            this.cooldown = java.time.Duration.ofSeconds(12);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new ShadowStep(this);
        }
    }

    private ShadowStep(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "shadow-step";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @NotNull Void executeSpell(SpellContext context) {
        Player player = context.caster();
        double range = spellConfig.getDouble("values.range", 10.0);
        boolean requiresLOS = spellConfig.getBoolean("flags.requires-los", true);
        int echoSamples = spellConfig.getInt("visual.echo-samples", 6);

        Location target = getTargetLocation(player, range, requiresLOS);
        if (!isLocationSafe(target)) {
            context.fx().fizzle(player);
            return null;
        }

        Location from = player.getLocation().clone();
        if (Afterimages.get() != null) {
            for (int i = 0; i < echoSamples; i++) {
                double t = (i + 1) / (double) (echoSamples + 1);
                Afterimages.get().record(lerp(from, target, t));
            }
            Afterimages.get().record(from);
        }

        context.fx().spawnParticles(from, Particle.CLOUD, 25, 0.4, 0.6, 0.4, 0.02);
        context.fx().playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.4f);

        player.teleport(target);

        context.fx().spawnParticles(target, Particle.CLOUD, 30, 0.5, 0.8, 0.5, 0.05);
        context.fx().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.7f);

        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    private Location lerp(Location a, Location b, double t) {
        return new Location(a.getWorld(),
                a.getX() + (b.getX() - a.getX()) * t,
                a.getY() + (b.getY() - a.getY()) * t,
                a.getZ() + (b.getZ() - a.getZ()) * t,
                a.getYaw() + (b.getYaw() - a.getYaw()) * (float) t,
                a.getPitch() + (b.getPitch() - a.getPitch()) * (float) t);
    }

    private Location getTargetLocation(Player player, double range, boolean requiresLineOfSight) {
        BlockIterator iterator = new BlockIterator(player, (int) range);
        Location targetLoc = null;
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (block.getType().isSolid()) {
                targetLoc = block.getLocation();
                targetLoc.setY(targetLoc.getY() + 1);
                break;
            }
        }
        if (targetLoc == null) {
            targetLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(range));
        }
        var world = player.getWorld();
        targetLoc.setY(Math.max(world.getMinHeight(), Math.min(world.getMaxHeight(), targetLoc.getY())));
        return targetLoc;
    }

    private boolean isLocationSafe(Location location) {
        var feet = location.getBlock();
        var head = location.clone().add(0, 1, 0).getBlock();
        var ground = location.clone().add(0, -1, 0).getBlock();
        return !feet.getType().isSolid() && !head.getType().isSolid() && ground.getType().isSolid();
    }
}