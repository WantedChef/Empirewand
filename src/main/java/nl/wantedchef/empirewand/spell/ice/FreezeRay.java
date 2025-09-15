package nl.wantedchef.empirewand.spell.ice;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * FreezeRay - Fires a continuous freezing beam that damages and slows enemies.
 * Features proper resource cleanup and optimized beam calculations.
 */
public class FreezeRay extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Freeze Ray";
            this.description = "Fire a continuous freezing beam";
            this.cooldown = Duration.ofSeconds(12);
            this.spellType = SpellType.ICE;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new FreezeRay(this);
        }
    }

    // Configuration defaults
    private static final double DEFAULT_RANGE = 15.0;
    private static final double DEFAULT_DAMAGE_PER_TICK = 1.5;
    private static final int DEFAULT_DURATION_TICKS = 60;
    private static final int DEFAULT_MAX_HITS_PER_TICK = 6;

    // Effect constants
    private static final double BEAM_STEP = 0.5;
    private static final double HIT_RADIUS = 0.45;
    private static final int SLOWNESS_DURATION = 40;
    private static final int SLOWNESS_AMPLIFIER = 3;
    private static final int CRIT_PARTICLES = 4;
    private static final int SOUND_INTERVAL = 12;

    private FreezeRay(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "freeze-ray";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(18);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        // Load configuration
        double range = spellConfig.getDouble("values.range", DEFAULT_RANGE);
        double damagePerTick = spellConfig.getDouble("values.damage_per_tick", DEFAULT_DAMAGE_PER_TICK);
        int duration = spellConfig.getInt("values.duration_ticks", DEFAULT_DURATION_TICKS);
        int maxHitsPerTick = spellConfig.getInt("values.max_hits_per_tick", DEFAULT_MAX_HITS_PER_TICK);

        // Initial sound effect
        context.fx().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.5f);

        // Create freeze ray task
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    cancel();
                    return;
                }

                Location start = player.getEyeLocation();
                Vector direction = start.getDirection().normalize();

                int hits = 0;

                // Create beam effect
                for (double d = 0; d <= range; d += BEAM_STEP) {
                    Location point = start.clone().add(direction.clone().multiply(d));

                    // Stop ray if it hits a solid block
                    if (!point.getBlock().isPassable()) {
                        break;
                    }

                    // Spawn beam particles
                    point.getWorld().spawnParticle(Particle.SNOWFLAKE, point, 1, 0.02, 0.02, 0.02, 0);
                    point.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(173, 216, 230), 1.0f));

                    // Check for entity hits (with per-tick limit)
                    if (hits < maxHitsPerTick) {
                        for (var entity : point.getWorld().getNearbyEntities(point, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
                            if (hits >= maxHitsPerTick) break;

                            if (entity instanceof LivingEntity living && !living.equals(player) && !living.isDead()) {
                                living.damage(damagePerTick, player);
                                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, SLOWNESS_DURATION, SLOWNESS_AMPLIFIER));

                                // Hit particles
                                living.getWorld().spawnParticle(Particle.CRIT, living.getLocation(), CRIT_PARTICLES,
                                    0.25, 0.4, 0.25, 0.01);
                                hits++;
                            }
                        }
                    }
                }

                // Periodic sound effect
                if (ticks % SOUND_INTERVAL == 0) {
                    context.fx().playSound(player.getLocation(), Sound.BLOCK_SNOW_PLACE, 0.5f, 2.0f);
                }

                ticks += 2;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);

        // Register task for cleanup
        if (context.plugin() instanceof nl.wantedchef.empirewand.EmpireWandPlugin plugin) {
            plugin.getTaskManager().registerTask(task);
        }
    }
}