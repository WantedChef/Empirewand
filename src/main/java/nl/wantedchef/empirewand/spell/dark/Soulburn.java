package nl.wantedchef.empirewand.spell.dark;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * A dark curse that ignites an enemy's soul, causing damage over time.
 *
 * <p>The spell locks onto a nearby target and burns their essence with
 * purple flames and ghastly screams.</p>
 *
 * @author WantedChef
 */
public class Soulburn extends Spell<LivingEntity> {

    /**
     * Builder for creating {@link Soulburn} instances.
     */
    public static class Builder extends Spell.Builder<LivingEntity> {

        /**
         * Creates a new builder for the Soulburn spell.
         *
         * @param api the plugin API
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Soulburn";
            this.description = "Burn your enemy's soul directly";
            this.cooldown = Duration.ofSeconds(15);
            this.spellType = SpellType.DARK;
        }

        /**
         * Builds the Soulburn spell.
         *
         * @return the constructed spell
         */
        @Override
        @NotNull
        public Spell<LivingEntity> build() {
            return new Soulburn(this);
        }
    }

    private static final double DEFAULT_RANGE = 20.0;
    private static final double DEFAULT_DAMAGE = 25.0;
    private static final int DEFAULT_DURATION_TICKS = 60;

    /**
     * Constructs the spell from its builder.
     *
     * @param builder the spell builder
     */
    private Soulburn(Builder builder) {
        super(builder);
    }

    /**
     * Gets the configuration key for this spell.
     *
     * @return "soulburn"
     */
    @Override
    public String key() {
        return "soulburn";
    }

    /**
     * Gets the prerequisites for casting the spell.
     *
     * @return level prerequisite requiring level 25
     */
    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(25);
    }

    /**
     * Searches for a target and applies the soulburn effect.
     *
     * @param context the spell context
     * @return the targeted entity or {@code null} if none
     */
    @Override
    protected LivingEntity executeSpell(@NotNull SpellContext context) {
        Player player = context.caster();
        if (player == null) {
            return null;
        }

        double range = spellConfig.getDouble("values.range", DEFAULT_RANGE);
        
        // First try to get the player's targeted entity
        var target = player.getTargetEntity((int) range);
        if (target instanceof LivingEntity living && !living.equals(player)) {
            applySoulburnEffect(context, living);
            return living;
        }
        
        // If no direct target, find the closest living entity in range
        LivingEntity closest = null;
        double closestDistance = range;
        
        for (var entity : player.getWorld().getNearbyEntities(player.getLocation(), range, range, range)) {
            if (entity instanceof LivingEntity living && !living.equals(player) && living.isValid() && !living.isDead()) {
                double distance = player.getLocation().distance(living.getLocation());
                if (distance < closestDistance) {
                    closest = living;
                    closestDistance = distance;
                }
            }
        }
        
        if (closest != null) {
            applySoulburnEffect(context, closest);
            return closest;
        }
        
        player.sendMessage("§cNo valid target found within range!");
        return null;
    }

    /**
     * Applies the burning effect over time to the target.
     *
     * @param context the spell context
     * @param target  the entity whose soul is burning
     */
    private void applySoulburnEffect(@NotNull SpellContext context, @NotNull LivingEntity target) {
        Player player = context.caster();
        double totalDamage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
        int duration = spellConfig.getInt("values.duration_ticks", DEFAULT_DURATION_TICKS);
        
        context.fx().playSound(target.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1.0f, 0.5f);
        player.sendMessage("§5§lSoulburn §dignited on target!");
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= duration || !target.isValid() || target.isDead()) {
                    cancel();
                    return;
                }
                
                // Damage over time
                target.damage(totalDamage / (duration / 10), player);
                
                // Purple flame effect
                target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
                target.getWorld().spawnParticle(Particle.WITCH, target.getLocation(), 5, 0.2, 0.3, 0.2, 0);
                
                if (ticks % 20 == 0) {
                    context.fx().playSound(target.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.8f, 0.5f);
                }
                
                ticks += 10;
            }
        }.runTaskTimer(context.plugin(), 0L, 10L);
    }

    /**
     * No additional handling is required; damage occurs during execution.
     *
     * @param context the spell context
     * @param target  the affected entity
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull LivingEntity target) {
        // Effects are applied in executeSpell
    }
}
