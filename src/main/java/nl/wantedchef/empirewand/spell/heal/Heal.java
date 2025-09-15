package nl.wantedchef.empirewand.spell.heal;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Heal - Basic healing spell that restores player health with visual effects.
 * Features proper resource management and optimized particle effects.
 */
public class Heal extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Heal";
            this.description = "Heals the caster";
            this.cooldown = Duration.ofSeconds(5);
            this.spellType = SpellType.HEAL;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new Heal(this);
        }
    }

    // Configuration defaults
    private static final double DEFAULT_HEAL_AMOUNT = 8.0;

    // Effect constants
    private static final int WAVE_COUNT = 3;
    private static final int WAVE_DELAY_TICKS = 5;
    private static final double ANGLE_INCREMENT = Math.PI / 12;
    private static final int HEART_PARTICLES = 3;
    private static final int DIVINE_PARTICLES = 15;
    private static final int COLUMN_HEIGHT = 6;

    private Heal(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "heal";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(@NotNull SpellContext context) {
        Player player = context.caster();
        double healAmount = spellConfig.getDouble("values.heal-amount", DEFAULT_HEAL_AMOUNT);

        // Get max health safely
        var maxAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = maxAttr != null ? maxAttr.getValue() : 20.0;

        // Apply healing
        player.setHealth(Math.min(maxHealth, player.getHealth() + healAmount));

        // Create visual effects
        createHealingEffects(context, player.getLocation(), healAmount);

        // Sound effects
        context.fx().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f);
        context.fx().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.6f);
        context.fx().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f, 1.8f);

        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in executeSpell
    }

    /**
     * Creates healing visual effects with proper task management.
     */
    private void createHealingEffects(SpellContext context, Location center, double healAmount) {
        List<BukkitTask> tasks = new ArrayList<>();

        // Create healing waves
        for (int wave = 0; wave < WAVE_COUNT; wave++) {
            final int waveNum = wave;

            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    double radius = (waveNum + 1) * 1.5;

                    // Heart particles in expanding ring
                    for (double angle = 0; angle < Math.PI * 2; angle += ANGLE_INCREMENT) {
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        Location loc = center.clone().add(x, 0.5, z);

                        center.getWorld().spawnParticle(Particle.HEART, loc, HEART_PARTICLES,
                            0.2, 0.3, 0.2, 0.02);
                    }
                }
            }.runTaskLater(context.plugin(), wave * WAVE_DELAY_TICKS);

            tasks.add(task);
        }

        // Divine column effect
        BukkitTask columnTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (int y = 0; y < COLUMN_HEIGHT; y++) {
                    Location columnLoc = center.clone().add(0, y, 0);
                    center.getWorld().spawnParticle(Particle.END_ROD, columnLoc, DIVINE_PARTICLES,
                        0.3, 0.1, 0.3, 0.05);
                }
            }
        }.runTaskLater(context.plugin(), 2L);

        tasks.add(columnTask);

        // Register all tasks for cleanup
        if (context.plugin() instanceof nl.wantedchef.empirewand.EmpireWandPlugin plugin) {
            for (BukkitTask task : tasks) {
                plugin.getTaskManager().registerTask(task);
            }
        }
    }
}