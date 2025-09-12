package nl.wantedchef.empirewand.spell.heal;

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
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * SuperHeal - Powerful healing spell from real Empirewand
 */
public class SuperHeal extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "SuperHeal";
            this.description = "Fully heal yourself and nearby allies";
            this.cooldown = Duration.ofSeconds(45);
            this.spellType = SpellType.HEAL;
        }

        @NotNull
        public Spell<Void> build() {
            return new SuperHeal(this);
        }
    }

    private static final double DEFAULT_RADIUS = 10.0;
    private static final boolean DEFAULT_HEAL_FULL = true;

    private SuperHeal(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "superheal";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(25);
    }

    @Override
    protected Void executeSpell(@NotNull SpellContext context) {
        Player player = context.caster();
        double radius = spellConfig.getDouble("values.radius", DEFAULT_RADIUS);
        boolean healFull = spellConfig.getBoolean("flags.heal_full", DEFAULT_HEAL_FULL);
        
        Location center = player.getLocation();
        int healedCount = 0;
        
        // Heal all nearby players and allies
        for (var entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof Player targetPlayer) {
                if (healFull) {
                    targetPlayer.setHealth(targetPlayer.getMaxHealth());
                } else {
                    targetPlayer.setHealth(Math.min(targetPlayer.getHealth() + 20, targetPlayer.getMaxHealth()));
                }
                targetPlayer.setFoodLevel(20);
                targetPlayer.setSaturation(20);
                targetPlayer.setFireTicks(0);
                
                // Remove all negative effects
                targetPlayer.getActivePotionEffects().forEach(effect -> {
                    if (isNegativeEffect(effect.getType())) {
                        targetPlayer.removePotionEffect(effect.getType());
                    }
                });
                
                // Individual heal effect
                targetPlayer.getWorld().spawnParticle(Particle.HEART, 
                    targetPlayer.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0);
                
                healedCount++;
            } else if (entity instanceof LivingEntity living && isAlly(player, living)) {
                living.setHealth(living.getMaxHealth());
                living.setFireTicks(0);
                healedCount++;
            }
        }
        
        // Grand healing effect
        for (int i = 0; i < 3; i++) {
            final int wave = i;
            context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                double waveRadius = radius * (wave + 1) / 3;
                
                // Particle ring
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                    double x = Math.cos(angle) * waveRadius;
                    double z = Math.sin(angle) * waveRadius;
                    Location loc = center.clone().add(x, 1, z);
                    
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0);
                    loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 1, 0, 0, 0, 0.05);
                }
                
                context.fx().playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 1.0f + wave * 0.2f);
            }, i * 5L);
        }
        
        context.fx().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 1.0f);
        player.sendMessage("§6§lSuperHeal §ehealed " + healedCount + " targets!");
        
        return null;
    }
    
    private boolean isNegativeEffect(org.bukkit.potion.PotionEffectType type) {
        return type.equals(org.bukkit.potion.PotionEffectType.POISON) ||
               type.equals(org.bukkit.potion.PotionEffectType.WITHER) ||
               type.equals(org.bukkit.potion.PotionEffectType.WEAKNESS) ||
               type.equals(org.bukkit.potion.PotionEffectType.SLOWNESS) ||
               type.equals(org.bukkit.potion.PotionEffectType.MINING_FATIGUE) ||
               type.equals(org.bukkit.potion.PotionEffectType.BLINDNESS) ||
               type.equals(org.bukkit.potion.PotionEffectType.NAUSEA) ||
               type.equals(org.bukkit.potion.PotionEffectType.HUNGER);
    }
    
    private boolean isAlly(Player player, LivingEntity entity) {
        // Check if entity is tamed by player
        if (entity instanceof org.bukkit.entity.Tameable tameable) {
            return tameable.isTamed() && tameable.getOwner() != null && 
                   tameable.getOwner().getUniqueId().equals(player.getUniqueId());
        }
        return false;
    }

    protected void applyEffect(@NotNull SpellContext context, Void effect) {
        // No synchronous effect needed
    }

    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effect handled in executeSpell
    }
}
