package nl.wantedchef.empirewand.spell.offensive;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Volley - Arrows at a target from real Empirewand
 */
public class Volley extends Spell<LivingEntity> {

    public static class Builder extends Spell.Builder<LivingEntity> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Volley";
            this.description = "Launch a volley of arrows at your target";
            this.cooldown = Duration.ofSeconds(12);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<LivingEntity> build() {
            return new Volley(this);
        }
    }

    private static final double DEFAULT_RANGE = 30.0;
    private static final int DEFAULT_ARROW_COUNT = 20;
    private static final double DEFAULT_DAMAGE_MULTIPLIER = 1.5;

    private Volley(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "volley";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(15);
    }

    @Override
    protected LivingEntity executeSpell(@NotNull SpellContext context) {
        Player player = context.caster();
        double range = spellConfig.getDouble("values.range", DEFAULT_RANGE);
        
        var target = player.getTargetEntity((int) range);
        if (target instanceof LivingEntity living) {
            return living;
        }
        
        player.sendMessage("§cNo valid target found!");
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull LivingEntity target) {
        if (target == null) return;
        
        Player player = context.caster();
        int arrowCount = spellConfig.getInt("values.arrow_count", DEFAULT_ARROW_COUNT);
        double damageMultiplier = spellConfig.getDouble("values.damage_multiplier", DEFAULT_DAMAGE_MULTIPLIER);
        
        Location startLoc = player.getEyeLocation().add(0, 3, 0);
        
        // Launch arrows in waves
        new BukkitRunnable() {
            int launched = 0;
            
            @Override
            public void run() {
                if (launched >= arrowCount || !target.isValid()) {
                    cancel();
                    return;
                }
                
                for (int i = 0; i < 4 && launched < arrowCount; i++) {
                    // Spawn arrow with slight spread
                    Location arrowLoc = startLoc.clone().add(
                        (Math.random() - 0.5) * 3,
                        (Math.random() - 0.5) * 2,
                        (Math.random() - 0.5) * 3
                    );
                    
                    Arrow arrow = player.getWorld().spawn(arrowLoc, Arrow.class);
                    arrow.setShooter(player);
                    arrow.setDamage(arrow.getDamage() * damageMultiplier);
                    arrow.setCritical(true);
                    arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
                    
                    // Calculate direction to target
                    Vector direction = target.getLocation().add(0, 1, 0).toVector()
                        .subtract(arrowLoc.toVector()).normalize();
                    arrow.setVelocity(direction.multiply(2.5));
                    
                    // Arrow trail
                    arrow.getWorld().spawnParticle(Particle.CRIT, arrow.getLocation(), 3, 0.1, 0.1, 0.1, 0);
                    
                    launched++;
                }
                
                // Sound effect
                context.fx().playSound(startLoc, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f + (float)launched/arrowCount);
            }
        }.runTaskTimer(context.plugin(), 0L, 3L);
        
        // Visual effects
        startLoc.getWorld().spawnParticle(Particle.FLASH, startLoc, 1, 0, 0, 0, 0);
        target.getWorld().spawnParticle(Particle.INSTANT_EFFECT, target.getLocation().add(0, 2, 0), 5, 0.5, 0.5, 0.5, 0);
        
        context.fx().playSound(player, Sound.ENTITY_SKELETON_SHOOT, 2.0f, 0.8f);
        
        String targetName = target instanceof Player ? ((Player) target).getName() : target.getType().name();
        player.sendMessage("§e§lVolley §6launched at " + targetName + "!");
    }
}
