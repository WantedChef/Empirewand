package nl.wantedchef.empirewand.spell.defensive;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.core.task.TaskManager;
import nl.wantedchef.empirewand.framework.service.FxService;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Absorb - Absorb damage and convert to health from real Empirewand
 */
public class Absorb extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Absorb";
            this.description = "Absorb incoming damage and convert it to health";
            this.cooldown = Duration.ofSeconds(40);
            this.spellType = SpellType.MISC;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Absorb(this);
        }
    }

    private static final int DEFAULT_DURATION_TICKS = 120;
    private static final int DEFAULT_ABSORPTION_LEVEL = 3;
    private static final double DEFAULT_ABSORPTION_MULTIPLIER = 0.5;
    private static final double DEFAULT_FIELD_RADIUS = 1.5;
    private static final int PARTICLE_UPDATE_INTERVAL = 5;
    private static final int PULSE_INTERVAL = 20;

    private Absorb(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "absorb";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        final int durationTicks = spellConfig.getInt("values.duration", DEFAULT_DURATION_TICKS);
        final int absorptionLevel = spellConfig.getInt("values.absorption_level", DEFAULT_ABSORPTION_LEVEL);
        final double absorptionMultiplier = spellConfig.getDouble("values.absorption_multiplier", DEFAULT_ABSORPTION_MULTIPLIER);
        final double fieldRadius = spellConfig.getDouble("values.field_radius", DEFAULT_FIELD_RADIUS);

        // Apply absorption effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, durationTicks, absorptionLevel));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, durationTicks, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, durationTicks, 0));

        // Start absorption field monitoring using TaskManager
        startAbsorptionField(context, player, durationTicks, absorptionMultiplier, fieldRadius);

        // Initial effects through FxService
        final FxService fx = context.fx();
        fx.spawnParticles(player.getLocation(), Particle.TOTEM_OF_UNDYING, 50, 1.0, 1.0, 1.0, 0.1);
        fx.playSound(player, Sound.ITEM_TOTEM_USE, 0.8f, 1.5f);
        fx.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.8f);

        player.sendMessage("§e§lAbsorb §6field activated for " + (durationTicks / 20) + " seconds!");
    }

    private void startAbsorptionField(@NotNull SpellContext context, @NotNull Player player,
                                    int durationTicks, double absorptionMultiplier, double fieldRadius) {
        final EmpireWandPlugin plugin = context.plugin();
        final TaskManager taskManager = plugin.getTaskManager();
        final FxService fx = context.fx();

        // Use AtomicReference to track task state
        final AtomicReference<BukkitTask> taskRef = new AtomicReference<>();
        final AbsorptionFieldState state = new AbsorptionFieldState(player.getHealth());

        BukkitTask task = taskManager.runTaskTimer(() -> {
            // Check if effect should continue
            if (state.ticks >= durationTicks || !player.isOnline() || !player.isValid()) {
                finalizeAbsorptionField(player, state, taskRef.get());
                return;
            }

            updateAbsorptionField(player, state, fx, fieldRadius, absorptionMultiplier);
            state.ticks += PARTICLE_UPDATE_INTERVAL;

        }, 0L, PARTICLE_UPDATE_INTERVAL);

        taskRef.set(task);
    }

    private void updateAbsorptionField(@NotNull Player player, @NotNull AbsorptionFieldState state,
                                     @NotNull FxService fx, double fieldRadius, double absorptionMultiplier) {
        final double time = state.ticks * 0.1;
        final var location = player.getLocation();

        // Generate absorption field particles in optimized pattern
        generateAbsorptionFieldParticles(fx, location, time, fieldRadius);

        // Check for damage absorption
        final double currentHealth = player.getHealth();
        if (currentHealth < state.lastHealth) {
            handleDamageAbsorption(player, state, fx, currentHealth, absorptionMultiplier);
        }

        // Periodic pulse effects
        if (state.ticks % PULSE_INTERVAL == 0) {
            generatePulseEffects(fx, location);
        }

        state.lastHealth = currentHealth;
    }

    private void generateAbsorptionFieldParticles(@NotNull FxService fx, @NotNull org.bukkit.Location location,
                                                double time, double fieldRadius) {
        final double particleRadius = fieldRadius + Math.sin(time) * 0.3;
        final int particleCount = 6; // Reduced for performance

        for (int i = 0; i < particleCount; i++) {
            final double angle = (Math.PI * 2 * i / particleCount) + time;
            final double x = Math.cos(angle) * particleRadius;
            final double z = Math.sin(angle) * particleRadius;
            final double y = Math.sin(time * 2) * 0.5 + 1;

            final var particleLocation = location.clone().add(x, y, z);
            fx.spawnParticles(particleLocation, Particle.DUST, 1, 0, 0, 0, 0,
                new Particle.DustOptions(org.bukkit.Color.YELLOW, 1.0f));
        }
    }

    private void handleDamageAbsorption(@NotNull Player player, @NotNull AbsorptionFieldState state,
                                      @NotNull FxService fx, double currentHealth, double absorptionMultiplier) {
        final double damageAbsorbed = state.lastHealth - currentHealth;
        state.totalAbsorbedDamage += damageAbsorbed;

        // Convert absorbed damage to healing
        final double maxHealth = getMaxHealth(player);
        final double healingAmount = damageAbsorbed * absorptionMultiplier;
        player.setHealth(Math.min(currentHealth + healingAmount, maxHealth));

        // Absorption visual feedback
        final var location = player.getLocation();
        fx.spawnParticles(location.clone().add(0, 1, 0), Particle.HEART, 5, 0.3, 0.3, 0.3, 0);
        fx.spawnParticles(location, Particle.HAPPY_VILLAGER, 10, 0.5, 0.5, 0.5, 0);
        fx.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
    }

    private void generatePulseEffects(@NotNull FxService fx, @NotNull org.bukkit.Location location) {
        fx.spawnParticles(location, Particle.END_ROD, 20, 1.0, 1.0, 1.0, 0.05);
        fx.spawnParticles(location.clone().add(0, 1, 0), Particle.TOTEM_OF_UNDYING, 5, 0.3, 0.3, 0.3, 0.02);
    }

    private void finalizeAbsorptionField(@NotNull Player player, @NotNull AbsorptionFieldState state,
                                       @NotNull BukkitTask task) {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }

        if (player.isOnline()) {
            player.sendMessage("§7Absorption field has dissipated. Total absorbed: " +
                String.format("%.1f", state.totalAbsorbedDamage) + " damage!");
        }
    }

    /**
     * Internal state holder for absorption field tracking
     */
    private static class AbsorptionFieldState {
        int ticks = 0;
        double lastHealth;
        double totalAbsorbedDamage = 0.0;

        AbsorptionFieldState(double initialHealth) {
            this.lastHealth = initialHealth;
        }
    }
    
    private double getMaxHealth(org.bukkit.entity.LivingEntity entity) {
        var attr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }
}
