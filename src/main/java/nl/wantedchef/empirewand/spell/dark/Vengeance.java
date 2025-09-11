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
 * Vengeance - Reflects damage back from real Empirewand
 */
public class Vengeance extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Vengeance";
            this.description = "Return damage to those who harm you";
            this.cooldown = Duration.ofSeconds(45);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new Vengeance(this);
        }
    }

    private static final int DEFAULT_DURATION = 200;
    private static final double DEFAULT_REFLECT_MULTIPLIER = 1.5;

    private Vengeance(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "vengeance";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(35);
    }

    @Override
    protected Void executeSpell(@NotNull SpellContext context) {
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void effect) {
        Player player = context.caster();
        int duration = spellConfig.getInt("values.duration", DEFAULT_DURATION);
        double reflectMultiplier = spellConfig.getDouble("values.reflect_multiplier", DEFAULT_REFLECT_MULTIPLIER);
        
        // Visual effect on activation
        player.getWorld().spawnParticle(Particle.WITCH, player.getLocation(), 50, 1, 1, 1, 0.1);
        context.fx().playSound(player, Sound.ENTITY_EVOKER_CAST_SPELL, 1.5f, 0.5f);
        player.sendMessage("§5§lVengeance §dactivated! Damage will be reflected for " + (duration/20) + " seconds!");
        
        // Store player's state for damage reflection
        new BukkitRunnable() {
            int ticks = 0;
            double lastHealth = player.getHealth();
            
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    player.sendMessage("§7Vengeance has ended.");
                    cancel();
                    return;
                }
                
                // Visual aura effect
                if (ticks % 10 == 0) {
                    player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.02);
                    player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation(), 5, 1, 1, 1, 0.1);
                }
                
                // Check if player took damage
                double currentHealth = player.getHealth();
                if (currentHealth < lastHealth) {
                    double damageTaken = lastHealth - currentHealth;
                    
                    // Find and damage nearby enemies
                    for (var entity : player.getWorld().getNearbyEntities(player.getLocation(), 15, 15, 15)) {
                        if (entity instanceof LivingEntity living && !living.equals(player)) {
                            living.damage(damageTaken * reflectMultiplier, player);
                            
                            // Visual effect on reflected damage
                            living.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, living.getLocation(), 10, 0.3, 0.5, 0.3, 0.05);
                            living.getWorld().spawnParticle(Particle.WITCH, living.getLocation(), 5, 0.2, 0.3, 0.2, 0);
                            context.fx().playSound(living.getLocation(), Sound.ENTITY_PHANTOM_HURT, 1.0f, 1.5f);
                        }
                    }
                    
                    player.sendMessage("§dVengeance reflected " + String.format("%.1f", damageTaken * reflectMultiplier) + " damage!");
                }
                
                lastHealth = currentHealth;
                ticks += 5;
            }
        }.runTaskTimer(context.plugin(), 0L, 5L);
        
    }
}
