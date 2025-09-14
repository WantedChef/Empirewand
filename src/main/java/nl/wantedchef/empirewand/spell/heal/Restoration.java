package nl.wantedchef.empirewand.spell.heal;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.attribute.Attribute;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Restoration - Complete restoration spell
 */
public class Restoration extends Spell<LivingEntity> {

    public static class Builder extends Spell.Builder<LivingEntity> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Restoration";
            this.description = "Completely restore a target's health and status";
            this.cooldown = Duration.ofSeconds(25);
            this.spellType = SpellType.HEAL;
        }

        @Override
        @NotNull
        public Spell<LivingEntity> build() {
            return new Restoration(this);
        }
    }

    private static final double DEFAULT_RANGE = 30.0;
    private static final boolean DEFAULT_FULL_RESTORE = true;

    private Restoration(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "restoration";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected LivingEntity executeSpell(@NotNull SpellContext context) {
        Player player = context.caster();
        double range = spellConfig.getDouble("values.range", DEFAULT_RANGE);
        
        var target = player.getTargetEntity((int) range);
        if (target instanceof LivingEntity living) {
            return living;
        }
        
        // If no target, heal self
        return player;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull LivingEntity target) {
        Player player = context.caster();
        boolean fullRestore = spellConfig.getBoolean("flags.full_restore", DEFAULT_FULL_RESTORE);
        
        // Full health restoration
        if (fullRestore) {
            target.setHealth(getMaxHealth(target));
        } else {
            target.setHealth(Math.min(target.getHealth() + 20, getMaxHealth(target)));
        }
        
        // Remove all negative effects
        target.getActivePotionEffects().forEach(effect -> {
            if (isNegativeEffect(effect.getType())) {
                target.removePotionEffect(effect.getType());
            }
        });
        
        // Add beneficial effects
        target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
        target.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 400, 1));
        target.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 0));
        
        // Clear fire
        target.setFireTicks(0);
        
        // Restore hunger for players
        if (target instanceof Player targetPlayer) {
            targetPlayer.setFoodLevel(20);
            targetPlayer.setSaturation(20);
            targetPlayer.setExhaustion(0);
        }
        
        // Visual effects
        for (int i = 0; i < 3; i++) {
            final int wave = i;
            context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                target.getWorld().spawnParticle(Particle.END_ROD, 
                    target.getLocation().add(0, 1 + wave * 0.5, 0), 15, 0.3, 0.3, 0.3, 0.05);
                target.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, 
                    target.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
            }, i * 5L);
        }
        
        // Sound effects
        context.fx().playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        context.fx().playSound(target.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
        
        String targetName = target instanceof Player ? ((Player) target).getName() : target.getType().name();
        player.sendMessage("§d§lRestoration §5complete on " + targetName + "!");
    }
    
    private boolean isNegativeEffect(PotionEffectType type) {
        return type.equals(PotionEffectType.POISON) ||
               type.equals(PotionEffectType.WITHER) ||
               type.equals(PotionEffectType.WEAKNESS) ||
               type.equals(PotionEffectType.SLOWNESS) ||
               type.equals(PotionEffectType.MINING_FATIGUE) ||
               type.equals(PotionEffectType.BLINDNESS) ||
               type.equals(PotionEffectType.NAUSEA) ||
               type.equals(PotionEffectType.HUNGER) ||
               type.equals(PotionEffectType.UNLUCK) ||
               type.equals(PotionEffectType.BAD_OMEN);
    }
    
    private double getMaxHealth(LivingEntity entity) {
        var attr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }
}
