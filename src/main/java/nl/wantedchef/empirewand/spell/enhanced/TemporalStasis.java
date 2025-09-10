package nl.wantedchef.empirewand.spell.enhanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A powerful spell that freezes time for all enemies in a large radius,
 * rendering them completely immobile for a duration.
 */
public class TemporalStasis extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Temporal Stasis";
            this.description = "Freezes time for all enemies in a large radius, rendering them completely immobile.";
            this.cooldown = java.time.Duration.ofSeconds(60);
            this.spellType = SpellType.CONTROL;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new TemporalStasis(this);
        }
    }

    private TemporalStasis(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "temporal-stasis";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(SpellContext context) {
        Player player = context.caster();

        // Configuration
        double radius = spellConfig.getDouble("values.radius", 20.0);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 100);
        boolean affectsPlayers = spellConfig.getBoolean("flags.affects-players", true);

        // Find all entities in radius
        List<LivingEntity> affectedEntities = new ArrayList<>();
        for (LivingEntity entity : player.getWorld().getNearbyLivingEntities(player.getLocation(), radius, radius, radius)) {
            if (entity.equals(player)) continue;
            if (entity instanceof Player && !affectsPlayers) continue;
            if (entity.isDead() || !entity.isValid()) continue;
            affectedEntities.add(entity);
        }

        // Apply temporal stasis effect
        for (LivingEntity entity : affectedEntities) {
            // Apply slowness and jump boost potions to simulate frozen movement
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 9, false, false));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, durationTicks, 128, false, false));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, durationTicks, 1, false, false));
            
            // Visual effects
            context.fx().spawnParticles(entity.getLocation(), Particle.CLOUD, 20, 0.5, 1, 0.5, 0.05);
            context.fx().spawnParticles(entity.getLocation(), Particle.END_ROD, 5, 0.3, 1, 0.3, 0.01);
        }

        // Visual effect for the caster
        context.fx().spawnParticles(player.getLocation(), Particle.PORTAL, 50, 2, 2, 2, 0.1);
        context.fx().playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.5f);

        // Create a visual representation of the time freeze
        new TimeFreezeVisual(context, player.getLocation(), radius, durationTicks)
                .runTaskTimer(context.plugin(), 0L, 5L);

        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled in executeSpell
    }

    private static class TimeFreezeVisual extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final double radius;
        private final int durationTicks;
        private int ticks = 0;
        private final int maxTicks;

        public TimeFreezeVisual(SpellContext context, Location center, double radius, int durationTicks) {
            this.context = context;
            this.center = center;
            this.radius = radius;
            this.durationTicks = durationTicks;
            this.maxTicks = durationTicks / 5; // Convert to our tick interval
        }

        @Override
        public void run() {
            if (ticks >= maxTicks) {
                this.cancel();
                context.fx().playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
                return;
            }

            // Create expanding rings of particles
            double currentRadius = radius * (ticks / (double) maxTicks);
            for (int i = 0; i < 36; i++) {
                double angle = 2 * Math.PI * i / 36;
                double x = currentRadius * Math.cos(angle);
                double z = currentRadius * Math.sin(angle);
                Location particleLoc = center.clone().add(x, 1, z);
                center.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
            }

            // Create vertical particle streams
            if (ticks % 4 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = 2 * Math.PI * i / 8;
                    double x = (radius * 0.5) * Math.cos(angle);
                    double z = (radius * 0.5) * Math.sin(angle);
                    Location particleLoc = center.clone().add(x, 0, z);
                    
                    for (int y = 0; y < 10; y++) {
                        Location streamLoc = particleLoc.clone().add(0, y * 0.5, 0);
                        center.getWorld().spawnParticle(Particle.CLOUD, streamLoc, 1, 0, 0, 0, 0);
                    }
                }
            }

            ticks++;
        }
    }
}