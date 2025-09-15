package nl.wantedchef.empirewand.spell.ice;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * ArcticBlast - Creates a massive ice explosion with expanding waves of frost.
 * Features proper resource management and optimized particle effects.
 */
public class ArcticBlast extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Arctic Blast";
            this.description = "Unleash a devastating arctic explosion";
            this.cooldown = Duration.ofSeconds(30);
            this.spellType = SpellType.ICE;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new ArcticBlast(this);
        }
    }

    // Configuration defaults
    private static final double DEFAULT_RADIUS = 10.0;
    private static final double DEFAULT_DAMAGE = 20.0;
    private static final int DEFAULT_FREEZE_DURATION = 100;
    private static final int DEFAULT_WEAKNESS_DURATION = 80;
    private static final int DEFAULT_FATIGUE_DURATION = 80;

    // Effect constants
    private static final int WAVE_COUNT = 3;
    private static final int WAVE_DELAY_TICKS = 5;
    private static final double ANGLE_INCREMENT = Math.PI / 32;
    private static final int SNOWFLAKE_PARTICLES = 3;
    private static final int ICE_PARTICLES = 2;
    private static final int DAMAGE_PARTICLES = 20;

    private ArcticBlast(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "arctic-blast";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(25);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        // Load configuration
        double radius = spellConfig.getDouble("values.radius", DEFAULT_RADIUS);
        double damage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
        int freezeDuration = spellConfig.getInt("values.freeze_duration", DEFAULT_FREEZE_DURATION);
        int weaknessDuration = spellConfig.getInt("values.weakness_duration_ticks", DEFAULT_WEAKNESS_DURATION);
        int weaknessAmplifier = spellConfig.getInt("values.weakness_amplifier", 1);
        int fatigueDuration = spellConfig.getInt("values.fatigue_duration_ticks", DEFAULT_FATIGUE_DURATION);
        int fatigueAmplifier = spellConfig.getInt("values.fatigue_amplifier", 1);

        Location center = player.getLocation();

        // Initial sound effect
        context.fx().playSound(center, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 2.0f, 2.0f);

        // Create blast wave task
        List<BukkitTask> tasks = new ArrayList<>();

        for (int i = 0; i < WAVE_COUNT; i++) {
            final int wave = i;

            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    double waveRadius = radius * (wave + 1) / WAVE_COUNT;
                    double previousRadius = wave > 0 ? radius * wave / WAVE_COUNT : 0;

                    // Create particle ring
                    for (double angle = 0; angle < Math.PI * 2; angle += ANGLE_INCREMENT) {
                        double x = Math.cos(angle) * waveRadius;
                        double z = Math.sin(angle) * waveRadius;
                        Location loc = center.clone().add(x, 0.5, z);

                        // Spawn particles
                        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, SNOWFLAKE_PARTICLES,
                            0.1, 0.5, 0.1, 0.05);
                        loc.getWorld().spawnParticle(Particle.BLOCK, loc, ICE_PARTICLES,
                            0.1, 0.3, 0.1, 0, Material.ICE.createBlockData());
                    }

                    // Damage and freeze entities in wave
                    for (var entity : center.getWorld().getNearbyEntities(center, waveRadius, waveRadius, waveRadius)) {
                        if (entity instanceof LivingEntity living && !living.equals(player) && !living.isDead()) {
                            double distance = living.getLocation().distance(center);

                            // Only affect entities in this wave ring
                            if (distance <= waveRadius && distance > previousRadius) {
                                // Calculate damage based on distance and core bonus
                                double coreBonusRadius = waveRadius * 0.35;
                                double baseDamage = damage * (1 - distance / radius);
                                double bonusDamage = distance <= coreBonusRadius ? damage * 0.25 : 0;
                                double totalDamage = baseDamage + bonusDamage;

                                living.damage(totalDamage, player);
                                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, freezeDuration, 4));
                                living.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, weaknessDuration, weaknessAmplifier));
                                living.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, fatigueDuration, fatigueAmplifier));

                                // Damage particles
                                living.getWorld().spawnParticle(Particle.SNOWFLAKE, living.getLocation(), DAMAGE_PARTICLES,
                                    0.5, 1, 0.5, 0.1);
                            }
                        }
                    }

                    // Wave sound effect
                    context.fx().playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 2.0f, 0.5f + wave * 0.2f);
                }
            }.runTaskLater(context.plugin(), i * WAVE_DELAY_TICKS);

            tasks.add(task);
        }

        // Register all tasks for cleanup
        if (context.plugin() instanceof nl.wantedchef.empirewand.EmpireWandPlugin plugin) {
            for (BukkitTask task : tasks) {
                plugin.getTaskManager().registerTask(task);
            }
        }
    }
}