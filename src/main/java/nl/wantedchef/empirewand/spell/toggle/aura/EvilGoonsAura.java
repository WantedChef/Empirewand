package nl.wantedchef.empirewand.spell.toggle.aura;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A passive aura spell that deals scalable damage to nearby enemies.
 * <p>
 * This spell creates an evil aura around the caster that continuously damages
 * nearby living entities. The damage scales based on distance, with maximum
 * damage dealt to entities directly adjacent to the caster.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Passive damage aura around the caster</li>
 *   <li>Distance-based damage scaling</li>
 *   <li>Performance-optimized with global task management</li>
 *   <li>Visual particle effects for feedback</li>
 *   <li>Excludes tamed animals and other players</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class EvilGoonsAura extends Spell<Void> implements ToggleableSpell {

    /**
     * Configuration record for the EvilGoonsAura spell.
     */
    private record Config(
        double range,
        double maxDamage,
        int checkIntervalTicks
    ) {}

    private static final double DEFAULT_RANGE = 5.0;
    private static final double DEFAULT_MAX_DAMAGE = 3.0;
    private static final int DEFAULT_CHECK_INTERVAL_TICKS = 15;

    private Config config = new Config(
        DEFAULT_RANGE,
        DEFAULT_MAX_DAMAGE,
        DEFAULT_CHECK_INTERVAL_TICKS
    );

    private static final Set<UUID> activeAuras = ConcurrentHashMap.newKeySet();
    private static BukkitTask globalTask = null;
    private static EmpireWandAPI globalApi = null;

    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new EvilGoonsAura spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Evil Goons Aura";
            this.description = "Passive aura that damages nearby enemies based on distance.";
            this.cooldown = Duration.ofSeconds(1);
            this.spellType = SpellType.AURA;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new EvilGoonsAura(this);
        }
    }

    private EvilGoonsAura(Builder builder) {
        super(builder);
        if (globalApi == null) {
            globalApi = this.api;
        }
    }

    @Override
    public void loadConfig(@NotNull ReadableConfig spellConfig) {
        Objects.requireNonNull(spellConfig, "SpellConfig cannot be null");
        super.loadConfig(spellConfig);
        
        this.config = new Config(
            spellConfig.getDouble("values.range", DEFAULT_RANGE),
            spellConfig.getDouble("values.max-damage", DEFAULT_MAX_DAMAGE),
            spellConfig.getInt("values.check-interval-ticks", DEFAULT_CHECK_INTERVAL_TICKS)
        );
    }

    @Override
    @NotNull
    public String key() {
        return "evil-goons-aura";
    }

    @Override
    @NotNull
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    @Nullable
    protected Void executeSpell(SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();
        
        if (isActive(player)) {
            deactivate(player, context);
            player.sendMessage("Evil aura deactivated.");
        } else {
            activate(player, context);
            player.sendMessage("Evil aura activated!");
        }
        
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Toggle effect handled in executeSpell
    }

    @Override
    public boolean isActive(Player player) {
        return activeAuras.contains(player.getUniqueId());
    }

    @Override
    public void activate(Player player, SpellContext context) {
        activeAuras.add(player.getUniqueId());
        startGlobalTask(context);
        context.fx().spawnParticles(player.getLocation(), Particle.SOUL_FIRE_FLAME, 20, 1, 1, 1, 0.05);
    }

    @Override
    public void deactivate(Player player, SpellContext context) {
        activeAuras.remove(player.getUniqueId());
        if (activeAuras.isEmpty()) {
            stopGlobalTask();
        }
        context.fx().spawnParticles(player.getLocation(), Particle.SMOKE, 15, 1, 1, 1, 0.05);
    }

    @Override
    public void forceDeactivate(Player player) {
        activeAuras.remove(player.getUniqueId());
        if (activeAuras.isEmpty()) {
            stopGlobalTask();
        }
    }

    /**
     * Starts the global aura task if it's not already running.
     *
     * @param context the spell context for task management
     */
    private void startGlobalTask(SpellContext context) {
        if (globalTask == null || globalTask.isCancelled()) {
            globalTask = context.plugin().getTaskManager().runTaskTimer(
                new AuraTask(),
                0L, config.checkIntervalTicks
            );
        }
    }

    /**
     * Stops the global aura task if it's running.
     */
    private static void stopGlobalTask() {
        if (globalTask != null && !globalTask.isCancelled()) {
            globalTask.cancel();
            globalTask = null;
        }
    }

    /**
     * The global task that processes all active auras.
     * <p>
     * This task runs periodically and processes damage for all players with active auras,
     * providing better performance than individual tasks per player.
     */
    private class AuraTask extends BukkitRunnable {
        @Override
        public void run() {
            if (activeAuras.isEmpty()) {
                cancel();
                globalTask = null;
                return;
            }

            for (UUID playerId : activeAuras) {
                Player player = Bukkit.getPlayer(playerId);
                if (player == null || !player.isOnline() || player.isDead()) {
                    activeAuras.remove(playerId);
                    continue;
                }

                processAuraForPlayer(player);
            }
        }

        /**
         * Processes the aura effect for a specific player.
         *
         * @param player the player with an active aura
         */
        private void processAuraForPlayer(Player player) {
            var location = player.getLocation();
            var world = location.getWorld();
            if (world == null) return;

            for (var entity : world.getNearbyLivingEntities(location, config.range)) {
                if (entity.equals(player)) continue;
                if (entity instanceof Player) continue;
                if (entity instanceof Tameable tameable && tameable.isTamed()) continue;
                if (entity.isDead() || !entity.isValid()) continue;

                double distance = location.distance(entity.getLocation());
                if (distance > config.range) continue;

                double damage = config.maxDamage * (1 - (distance / config.range));
                if (damage > 0) {
                    entity.damage(damage, player);
                    
                    if (globalApi != null) {
                        var fx = globalApi.getService(nl.wantedchef.empirewand.framework.service.FxService.class);
                        fx.spawnParticles(entity.getLocation(), Particle.CRIT, 1, 0.1, 0.1, 0.1, 0.05);
                    }
                }
            }
        }
    }

    /**
     * Cleanup method to ensure tasks are stopped when the plugin is disabled.
     */
    public static void cleanup() {
        activeAuras.clear();
        stopGlobalTask();
        globalApi = null;
    }
}