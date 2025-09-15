package nl.wantedchef.empirewand.spell.misc;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import java.time.Duration;

public class EmpireLaunch extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Empire Launch";
            this.description = "Launches the targeted living entity into the air with great force.";
            this.cooldown = Duration.ofSeconds(10);
            this.spellType = SpellType.MISC;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new EmpireLaunch(this);
        }
    }

    private EmpireLaunch(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "empire-launch";
    }

    @Override
    public Component displayName() {
        return Component.text("Empire Launch").color(TextColor.color(255, 215, 0));
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

        @Override
    protected Void executeSpell(SpellContext context) {
        Player caster = context.caster();

        // Find targeted living entity
        double range = spellConfig.getDouble("values.range", 20.0);

        // Validate caster has valid location and world
        var eyeLocation = caster.getEyeLocation();
        if (eyeLocation == null || eyeLocation.getWorld() == null) {
            caster.sendMessage(Component.text("Cannot launch from current location!")
                .color(net.kyori.adventure.text.format.NamedTextColor.RED));
            return null;
        }

        var rayTrace = eyeLocation.getWorld().rayTraceEntities(
            eyeLocation,
            eyeLocation.getDirection(),
            range,
            entity -> entity instanceof org.bukkit.entity.LivingEntity &&
                     entity != caster &&
                     !entity.isDead()
        );

        if (rayTrace == null || rayTrace.getHitEntity() == null) {
            // No target found
            caster.sendMessage(Component.text("No living entity in range to launch!")
                .color(net.kyori.adventure.text.format.NamedTextColor.RED));
            return null;
        }

        org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) rayTrace.getHitEntity();

        double powerUp = spellConfig.getDouble("values.power-up", 3.0);
        double forwardMult = spellConfig.getDouble("values.forward-multiplier", 1.5);
        int trailTicks = spellConfig.getInt("values.trail-duration-ticks", 200); // 10 seconds

        Vector dir = caster.getEyeLocation().getDirection().normalize();
        Vector launchVector = dir.multiply(forwardMult).setY(powerUp);
        target.setVelocity(launchVector);

        // Launch FX at target location
        Location launchLoc = target.getLocation();
        if (launchLoc == null) {
            return null; // Cannot proceed without a location
        }
        context.fx().spawnParticles(launchLoc.clone().add(0, 1, 0), Particle.EXPLOSION, 15, 1.5, 1.5, 1.5, 0.2);
        context.fx().playSound(launchLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.2f, 0.4f);

        // Trail effect following the target
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > trailTicks || !target.isValid() || target.isDead()) {
                    cancel();
                    return;
                }
                Location trailLoc = target.getLocation();
                if (trailLoc != null) {
                    context.fx().spawnParticles(trailLoc.clone().add(0, 1, 0), Particle.FLAME, 8, 0.4, 0.4, 0.4, 0.08);
                    context.fx().spawnParticles(trailLoc.clone().add(0, 1, 0), Particle.SMOKE, 5, 0.3, 0.3, 0.3, 0.03);
                }
                ticks++;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);

        // Landing impact (check for ground) - track the target
        new BukkitRunnable() {
            @org.jetbrains.annotations.Nullable Location lastLoc = target.getLocation() != null ? target.getLocation().clone() : null;
            @Override
            public void run() {
                if (!target.isValid() || target.isDead()) {
                    cancel();
                    return;
                }
                Location currLoc = target.getLocation();
                if (currLoc == null) {
                    cancel();
                    return;
                }
                if (lastLoc != null && Math.abs(currLoc.getY() - lastLoc.getY()) < 0.1 && currLoc.getY() < lastLoc.getY()) { // Landed
                    // Impact FX
                    Location impactLoc = currLoc.clone().subtract(0, 1, 0);
                    context.fx().spawnParticles(impactLoc, Particle.EXPLOSION, 8, 2.5, 0.8, 2.5, 0.15);
                    context.fx().spawnParticles(impactLoc, Particle.DUST, 30, 1.5, 0.2, 1.5, 0.12,
                        new Particle.DustOptions(org.bukkit.Color.GRAY, 2.5f));
                    context.fx().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.9f);

                    // Small crater (replace nearby dirt/grass with cracked stone if possible)
                    for (int dx = -3; dx <= 3; dx++) {
                        for (int dz = -3; dz <= 3; dz++) {
                            if (dx * dx + dz * dz <= 9) {
                                Location craterLoc = impactLoc.clone().add(dx, -1, dz);
                                if (craterLoc.getBlock().getType() == Material.GRASS_BLOCK ||
                                    craterLoc.getBlock().getType() == Material.DIRT) {
                                    craterLoc.getBlock().setType(Material.CRACKED_STONE_BRICKS);
                                }
                            }
                        }
                    }
                    cancel();
                }
                lastLoc = currLoc.clone();
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);

        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }
}
