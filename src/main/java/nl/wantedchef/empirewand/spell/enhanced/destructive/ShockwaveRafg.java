package nl.wantedchef.empirewand.spell.enhanced.destructive;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A devastating AoE attack that deals armor-piercing percentage-based damage.
 * <p>
 * This spell creates a destructive shockwave that expands outward from the caster,
 * dealing percentage-based damage that bypasses armor and nearly kills enemies.
 * The spell features a visual expanding ring effect and has a very long cooldown
 * due to its devastating nature.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Devastating AoE attack with long cooldown</li>
 *   <li>Armor-piercing percentage-based damage</li>
 *   <li>Visual expanding shockwave ring effect</li>
 *   <li>Configurable damage percentage</li>
 *   <li>Excludes tamed animals and other players based on friendly fire settings</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class ShockwaveRafg extends Spell<Void> {

    /**
     * Configuration record for the ShockwaveRafg spell.
     */
    private record Config(
        double range,
        double damagePercentage,
        boolean friendlyFire
    ) {}

    private static final double DEFAULT_RANGE = 15.0;
    private static final double DEFAULT_DAMAGE_PERCENTAGE = 0.90;
    private static final boolean DEFAULT_FRIENDLY_FIRE = false;

    private Config config = new Config(
        DEFAULT_RANGE,
        DEFAULT_DAMAGE_PERCENTAGE,
        DEFAULT_FRIENDLY_FIRE
    );

    private static final Set<UUID> processingEntities = ConcurrentHashMap.newKeySet();

    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new ShockwaveRafg spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Shockwave (Rafg)";
            this.description = "Devastating AoE attack with armor-piercing percentage damage.";
            this.cooldown = Duration.ofMinutes(2); // 120 seconds
            this.spellType = SpellType.ENHANCED;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new ShockwaveRafg(this);
        }
    }

    private ShockwaveRafg(Builder builder) {
        super(builder);
    }

    @Override
    public void loadConfig(@NotNull ReadableConfig spellConfig) {
        Objects.requireNonNull(spellConfig, "SpellConfig cannot be null");
        super.loadConfig(spellConfig);
        
        this.config = new Config(
            spellConfig.getDouble("values.range", DEFAULT_RANGE),
            spellConfig.getDouble("values.damage-percentage", DEFAULT_DAMAGE_PERCENTAGE),
            spellConfig.getBoolean("values.friendly-fire", DEFAULT_FRIENDLY_FIRE)
        );
    }

    @Override
    @NotNull
    public String key() {
        return "shockwave-rafg";
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
        Location center = player.getLocation();
        var world = center.getWorld();
        if (world == null) {
            return null;
        }

        createShockwaveEffect(context, center);
        
        context.plugin().getTaskManager().runTaskLater(() -> {
            applyShockwaveDamage(context, center);
        }, 40L); // 2 second delay for visual buildup

        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled asynchronously
    }

    /**
     * Creates the visual shockwave effect expanding outward from the center.
     *
     * @param context the spell context
     * @param center the center location of the shockwave
     */
    private void createShockwaveEffect(SpellContext context, Location center) {
        context.plugin().getTaskManager().runTaskTimer(
            new BukkitRunnable() {
                private double currentRadius = 0.0;
                private int ticks = 0;

                @Override
                public void run() {
                    if (ticks >= 40 || currentRadius >= config.range) { // 2 seconds
                        this.cancel();
                        return;
                    }

                    currentRadius += config.range / 40.0; // Expand over 2 seconds
                    
                    for (int i = 0; i < 360; i += 10) { // 10 degrees per particle
                        double angle = Math.toRadians(i);
                        double x = center.getX() + currentRadius * Math.cos(angle);
                        double z = center.getZ() + currentRadius * Math.sin(angle);
                        Location particleLocation = new Location(center.getWorld(), x, center.getY(), z);
                        
                        context.fx().spawnParticles(particleLocation, Particle.EXPLOSION, 1, 0, 0, 0, 0);
                        
                        if (i % 30 == 0) { // Every 30 degrees, add extra effects
                            context.fx().spawnParticles(particleLocation, Particle.FLASH, 1, 0, 0, 0, 0);
                        }
                    }
                    
                    if (ticks % 10 == 0) { // Every 0.5 seconds, play sound
                        context.fx().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f + (ticks * 0.02f));
                    }
                    
                    ticks++;
                }
            },
            0L, 1L
        );
    }

    /**
     * Applies the devastating damage to all entities within range.
     *
     * @param context the spell context
     * @param center the center location of the shockwave
     */
    private void applyShockwaveDamage(SpellContext context, Location center) {
        var world = center.getWorld();
        if (world == null) return;

        // Register damage interceptor
        DamageInterceptor interceptor = new DamageInterceptor(context.caster());
        context.plugin().getServer().getPluginManager().registerEvents(interceptor, context.plugin());

        try {
            for (var entity : world.getNearbyLivingEntities(center, config.range)) {
                if (entity.equals(context.caster()) && !config.friendlyFire) {
                    continue;
                }
                if (entity instanceof Player && !config.friendlyFire) {
                    continue;
                }
                if (entity instanceof Tameable tameable && tameable.isTamed() && !config.friendlyFire) {
                    continue;
                }
                if (entity.isDead() || !entity.isValid()) {
                    continue;
                }

                double maxHealth = getMaxHealth(entity);
                double targetDamage = maxHealth * config.damagePercentage;
                
                processingEntities.add(entity.getUniqueId());
                
                entity.damage(1000.0, context.caster()); // High damage that will be intercepted
                
                context.fx().spawnParticles(entity.getLocation(), Particle.EXPLOSION, 20, 0.5, 0.5, 0.5, 0.1);
                context.fx().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
            }
        } finally {
            // Unregister interceptor after a short delay
            context.plugin().getTaskManager().runTaskLater(() -> {
                HandlerList.unregisterAll(interceptor);
                processingEntities.clear();
            }, 5L);
        }
    }

    /**
     * Damage interceptor to apply percentage-based damage instead of the high damage value.
     */
    private class DamageInterceptor implements Listener {
        private final Player caster;

        public DamageInterceptor(Player caster) {
            this.caster = caster;
        }

        @EventHandler
        public void onEntityDamage(EntityDamageEvent event) {
            if (!(event.getEntity() instanceof LivingEntity entity)) return;
            if (!processingEntities.contains(entity.getUniqueId())) return;
            if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

            double maxHealth = entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            double targetDamage = maxHealth * config.damagePercentage;
            
            event.setDamage(targetDamage);
            
            processingEntities.remove(entity.getUniqueId());
        }
    }
    
    private double getMaxHealth(org.bukkit.entity.LivingEntity entity) {
        var attr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }
}