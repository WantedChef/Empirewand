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
import org.bukkit.Location;
import org.bukkit.Color;
import org.bukkit.scheduler.BukkitRunnable;
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
        
        // Start the spectacular restoration matrix process
        startRestorationMatrix(context, target, player, fullRestore);
    }
    
    private void startRestorationMatrix(SpellContext context, LivingEntity target, Player caster, boolean fullRestore) {
        Location center = target.getLocation();
        
        // Create restoration matrix setup
        createRestorationMatrix(context, center);
        
        // Start the restoration sequence
        new BukkitRunnable() {
            int ticks = 0;
            final int matrixDuration = 80; // 4 seconds
            boolean healingComplete = false;
            
            @Override
            public void run() {
                if (ticks >= matrixDuration) {
                    // Complete the restoration
                    completeRestoration(context, target, caster);
                    cancel();
                    return;
                }
                
                Location targetLoc = target.getLocation();
                double progress = (double) ticks / matrixDuration;
                
                // Purification waves
                if (ticks % 15 == 0) {
                    createPurificationWave(context, targetLoc, ticks / 15);
                }
                
                // Restoration matrix particles
                createMatrixParticles(context, targetLoc, progress);
                
                // Gradually heal and restore
                if (ticks % 20 == 0 && !healingComplete) {
                    performGradualRestoration(target, fullRestore, ticks / 20);
                    if (ticks >= 60) { // Complete healing in first 3 seconds
                        healingComplete = true;
                    }
                }
                
                // Matrix sounds
                if (ticks % 16 == 0) {
                    context.fx().playSound(targetLoc, Sound.BLOCK_BEACON_AMBIENT, 0.7f, 1.5f + (float)(progress * 0.5f));
                }
                if (ticks % 25 == 0) {
                    context.fx().playSound(targetLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.8f);
                }
                
                ticks += 2;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
        
        // Initial matrix activation
        context.fx().playSound(center, Sound.ENTITY_EVOKER_CAST_SPELL, 1.5f, 1.2f);
        context.fx().playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 0.8f);
        context.fx().spawnParticles(center.clone().add(0, 1, 0), Particle.FLASH, 2, 0.5, 0.5, 0.5, 0);
        
        String targetName = target instanceof Player ? ((Player) target).getName() : target.getType().name();
        caster.sendMessage("§d§l✨ Restoration Matrix §5activating on " + targetName + "...");
    }
    
    private void createRestorationMatrix(SpellContext context, Location center) {
        // Create complex restoration matrix pattern
        for (int i = 0; i < 20; i++) {
            final int step = i;
            context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                // Outer restoration circle
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 12) {
                    double radius = 4;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location matrixLoc = center.clone().add(x, 0.2, z);
                    
                    context.fx().spawnParticles(matrixLoc, Particle.DUST, 1, 0.1, 0.1, 0.1, 0,
                        new Particle.DustOptions(Color.fromRGB(147, 112, 219), 1.0f)); // Medium Slate Blue
                }
                
                // Inner energy circle
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    double radius = 2;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location energyLoc = center.clone().add(x, 0.5, z);
                    
                    context.fx().spawnParticles(energyLoc, Particle.END_ROD, 1, 0.1, 0.1, 0.1, 0.02);
                }
                
                // Central restoration focus
                if (step % 3 == 0) {
                    context.fx().spawnParticles(center.clone().add(0, 1, 0), Particle.TOTEM_OF_UNDYING, 3, 0.3, 0.3, 0.3, 0.05);
                }
            }, step * 3L);
        }
    }
    
    private void createPurificationWave(SpellContext context, Location center, int waveNumber) {
        // Create expanding purification waves
        for (int i = 0; i < 20; i++) {
            final int step = i;
            context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                double radius = step * 0.3;
                
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    double y = Math.sin(radius * 0.5) * 0.5 + 1;
                    
                    Location waveLoc = center.clone().add(x, y, z);
                    
                    // Purification particles
                    context.fx().spawnParticles(waveLoc, Particle.DUST, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.8f)); // White
                    context.fx().spawnParticles(waveLoc, Particle.END_ROD, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }, step);
        }
        
        // Wave sound
        context.fx().playSound(center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 2.0f);
    }
    
    private void createMatrixParticles(SpellContext context, Location center, double progress) {
        // Rotating restoration energy
        double angle = progress * Math.PI * 4; // Multiple rotations
        
        for (int i = 0; i < 6; i++) {
            double orbAngle = angle + (i * Math.PI / 3);
            double radius = 2.5 + Math.sin(progress * Math.PI * 2) * 0.5;
            double x = Math.cos(orbAngle) * radius;
            double z = Math.sin(orbAngle) * radius;
            double y = Math.sin(progress * Math.PI * 3) * 1 + 2;
            
            Location orbLoc = center.clone().add(x, y, z);
            
            // Restoration orbs
            context.fx().spawnParticles(orbLoc, Particle.TOTEM_OF_UNDYING, 1, 0, 0, 0, 0);
            context.fx().spawnParticles(orbLoc, Particle.ENCHANT, 2, 0.2, 0.2, 0.2, 0.05);
        }
        
        // Central healing column
        for (int h = 0; h < 8; h++) {
            double y = h * 0.5 + Math.sin(progress * Math.PI * 4 + h) * 0.3;
            Location columnLoc = center.clone().add(0, y, 0);
            
            context.fx().spawnParticles(columnLoc, Particle.END_ROD, 1, 0.1, 0.1, 0.1, 0.02);
            if (h % 2 == 0) {
                context.fx().spawnParticles(columnLoc, Particle.DUST, 2, 0.2, 0.2, 0.2, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 182, 193), 1.0f)); // Light Pink
            }
        }
    }
    
    private void performGradualRestoration(LivingEntity target, boolean fullRestore, int phase) {
        // Gradual restoration over multiple phases
        if (phase == 0) {
            // Remove negative effects first
            target.getActivePotionEffects().forEach(effect -> {
                if (isNegativeEffect(effect.getType())) {
                    target.removePotionEffect(effect.getType());
                }
            });
            target.setFireTicks(0);
        } else if (phase == 1) {
            // Restore health
            if (fullRestore) {
                target.setHealth(getMaxHealth(target));
            } else {
                target.setHealth(Math.min(target.getHealth() + 20, getMaxHealth(target)));
            }
        } else if (phase == 2) {
            // Add beneficial effects
            target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 300, 2));
            target.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 600, 2));
            target.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 400, 1));
            target.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 300, 0));
        } else if (phase == 3) {
            // Restore hunger and vitals for players
            if (target instanceof Player targetPlayer) {
                targetPlayer.setFoodLevel(20);
                targetPlayer.setSaturation(20);
                targetPlayer.setExhaustion(0);
                targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 100, 1));
            }
        }
    }
    
    private void completeRestoration(SpellContext context, LivingEntity target, Player caster) {
        Location center = target.getLocation();
        
        // Spectacular rebirth explosion
        createRebirthExplosion(context, center);
        
        // Completion sounds
        context.fx().playSound(caster, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 1.0f);
        context.fx().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 1.2f);
        context.fx().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.5f);
        context.fx().playSound(center, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 2.0f);
        
        String targetName = target instanceof Player ? ((Player) target).getName() : target.getType().name();
        caster.sendMessage("§d§l✨ Restoration Matrix §5complete! " + targetName + " §5has been fully restored!");
        
        if (target instanceof Player targetPlayer && !targetPlayer.equals(caster)) {
            targetPlayer.sendMessage("§d§l✨ You have been completely restored by " + caster.getName() + "!");
        }
    }
    
    private void createRebirthExplosion(SpellContext context, Location center) {
        // Massive restoration explosion
        context.fx().spawnParticles(center.clone().add(0, 1, 0), Particle.TOTEM_OF_UNDYING, 150, 3, 3, 3, 0.5);
        context.fx().spawnParticles(center.clone().add(0, 1, 0), Particle.END_ROD, 100, 2.5, 4, 2.5, 0.3);
        context.fx().spawnParticles(center.clone().add(0, 1, 0), Particle.ENCHANT, 200, 4, 4, 4, 0.8);
        
        // Rainbow rebirth particles
        Color[] colors = {
            Color.fromRGB(255, 0, 0),    // Red
            Color.fromRGB(255, 165, 0),  // Orange
            Color.fromRGB(255, 255, 0),  // Yellow
            Color.fromRGB(0, 255, 0),    // Green
            Color.fromRGB(0, 191, 255),  // Deep Sky Blue
            Color.fromRGB(138, 43, 226), // Blue Violet
            Color.fromRGB(255, 20, 147)  // Deep Pink
        };
        
        for (int i = 0; i < colors.length; i++) {
            final Color color = colors[i];
            final int delay = i * 3;
            
            context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                    double x = Math.cos(angle) * 5;
                    double z = Math.sin(angle) * 5;
                    Location colorLoc = center.clone().add(x, 2, z);
                    
                    context.fx().spawnParticles(colorLoc, Particle.DUST, 3, 0.3, 0.3, 0.3, 0,
                        new Particle.DustOptions(color, 1.5f));
                }
            }, delay);
        }
        
        // Ascending restoration pillars
        for (int i = 0; i < 8; i++) {
            final int pillar = i;
            context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                double angle = (2 * Math.PI * pillar / 8);
                double x = Math.cos(angle) * 6;
                double z = Math.sin(angle) * 6;
                
                for (int h = 0; h < 12; h++) {
                    Location pillarLoc = center.clone().add(x, h * 0.5, z);
                    context.fx().spawnParticles(pillarLoc, Particle.END_ROD, 2, 0.1, 0.1, 0.1, 0.02);
                    context.fx().spawnParticles(pillarLoc, Particle.TOTEM_OF_UNDYING, 1, 0.2, 0.2, 0.2, 0.01);
                }
            }, pillar * 4L);
        }
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
