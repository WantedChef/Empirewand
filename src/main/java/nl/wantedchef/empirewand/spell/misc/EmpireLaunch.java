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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import java.time.Duration;

public class EmpireLaunch extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Empire Launch";
            this.description = "Launches the caster into the air with enhanced mobility and fall protection.";
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

        double powerUp = spellConfig.getDouble("values.power-up", 2.5);
        double forwardMult = spellConfig.getDouble("values.forward-multiplier", 1.0);
        int knockbackImmunityTicks = spellConfig.getInt("values.knockback-immunity-ticks", 100); // 5 seconds
        int trailTicks = spellConfig.getInt("values.trail-duration-ticks", 200); // 10 seconds

        Vector dir = caster.getEyeLocation().getDirection().normalize();
        Vector launchVector = dir.multiply(forwardMult).setY(powerUp);
        caster.setVelocity(launchVector);

        // Add knockback immunity using SLOW_FALLING effect for fall protection
        caster.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, knockbackImmunityTicks, 0, true, false));
        // Add resistance for damage protection
        caster.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, knockbackImmunityTicks, 2, true, false));

        // Launch FX
        Location launchLoc = caster.getLocation();
        if (launchLoc == null) {
            return null; // Cannot proceed without a location
        }
        context.fx().spawnParticles(launchLoc.clone().add(0, 1, 0), Particle.EXPLOSION, 10, 1.0, 1.0, 1.0, 0.1);
        context.fx().playSound(launchLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);

        // Trail effect
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > trailTicks || !caster.isOnline() || caster.isDead()) {
                    cancel();
                    return;
                }
                Location trailLoc = caster.getLocation();
                if (trailLoc != null) {
                    context.fx().spawnParticles(trailLoc.clone().add(0, 1, 0), Particle.FLAME, 5, 0.3, 0.3, 0.3, 0.05);
                    context.fx().spawnParticles(trailLoc.clone().add(0, 1, 0), Particle.SMOKE, 3, 0.2, 0.2, 0.2, 0.02);
                }
                ticks++;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);

        // Landing impact (check for ground)
        new BukkitRunnable() {
            @org.jetbrains.annotations.Nullable Location lastLoc = caster.getLocation() != null ? caster.getLocation().clone() : null;
            @Override
            public void run() {
                if (!caster.isOnline() || caster.isDead()) {
                    cancel();
                    return;
                }
                Location currLoc = caster.getLocation();
                if (currLoc == null) {
                    cancel();
                    return;
                }
                if (lastLoc != null && Math.abs(currLoc.getY() - lastLoc.getY()) < 0.1 && currLoc.getY() < lastLoc.getY()) { // Landed
                    // Impact FX
                    Location impactLoc = currLoc.clone().subtract(0, 1, 0);
                    context.fx().spawnParticles(impactLoc, Particle.EXPLOSION, 5, 2.0, 0.5, 2.0, 0.1);
                    context.fx().spawnParticles(impactLoc, Particle.DUST, 20, 1.0, 0.1, 1.0, 0.1, 
                        new Particle.DustOptions(org.bukkit.Color.GRAY, 2.0f));
                    context.fx().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.0f);
                    
                    // Small crater (replace nearby dirt/grass with cracked stone if possible)
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            if (dx * dx + dz * dz <= 4) {
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
