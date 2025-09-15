package nl.wantedchef.empirewand.spell.heal;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.attribute.Attribute;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Prayer - Heal yourself from real Empirewand
 */
public class Prayer extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Prayer";
            this.description = "Heal yourself through divine prayer";
            this.cooldown = Duration.ofSeconds(8);
            this.spellType = SpellType.HEAL;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Prayer(this);
        }
    }

    private static final double DEFAULT_HEAL_AMOUNT = 10.0;
    private static final boolean DEFAULT_REMOVE_EFFECTS = true;

    private Prayer(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "prayer";
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
        double healAmount = spellConfig.getDouble("values.heal_amount", DEFAULT_HEAL_AMOUNT);
        boolean removeEffects = spellConfig.getBoolean("flags.remove_effects", DEFAULT_REMOVE_EFFECTS);
        
        // Start channeling prayer ritual
        startPrayerChanneling(context, player, healAmount, removeEffects);
    }
    
    private void startPrayerChanneling(SpellContext context, Player player, double healAmount, boolean removeEffects) {
        Location center = player.getLocation();
        
        // Create divine prayer circle
        createPrayerCircle(context, center);
        
        // Start channeling effects
        new BukkitRunnable() {
            int ticks = 0;
            final int channelDuration = 40; // 2 seconds
            
            @Override
            public void run() {
                if (ticks >= channelDuration || !player.isOnline()) {
                    // Complete the prayer
                    completePrayer(context, player, healAmount, removeEffects);
                    cancel();
                    return;
                }
                
                // Ascending prayer particles
                double progress = (double) ticks / channelDuration;
                Location playerLoc = player.getLocation();
                
                // Rising prayer energy
                for (int i = 0; i < 3; i++) {
                    double y = progress * 8 + i * 0.5;
                    Location prayerLoc = playerLoc.clone().add(0, y, 0);
                    
                    context.fx().spawnParticles(prayerLoc, Particle.END_ROD, 1, 0.1, 0.1, 0.1, 0.02);
                    context.fx().spawnParticles(prayerLoc, Particle.ENCHANT, 2, 0.2, 0.2, 0.2, 0.05);
                }
                
                // Blessing aura around player
                double angle = ticks * 0.3;
                for (int i = 0; i < 4; i++) {
                    double orbAngle = angle + (i * Math.PI / 2);
                    double x = Math.cos(orbAngle) * 1.5;
                    double z = Math.sin(orbAngle) * 1.5;
                    double y = Math.sin(ticks * 0.2) * 0.5 + 1.5;
                    
                    Location orbLoc = playerLoc.clone().add(x, y, z);
                    context.fx().spawnParticles(orbLoc, Particle.TOTEM_OF_UNDYING, 1, 0, 0, 0, 0);
                }
                
                // Channeling sounds
                if (ticks % 10 == 0) {
                    context.fx().playSound(playerLoc, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.5f + (float)progress * 0.5f);
                }
                
                ticks += 2;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
        
        // Initial prayer sounds
        context.fx().playSound(center, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 1.8f);
        context.fx().playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.3f);
        
        player.sendMessage("§e§l🙏 Prayer §6channeling divine healing...");
    }
    
    private void createPrayerCircle(SpellContext context, Location center) {
        // Create sacred prayer circle
        for (int i = 0; i < 8; i++) {
            final int step = i;
            context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                    double radius = 2.5;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location circleLoc = center.clone().add(x, 0.1, z);
                    
                    context.fx().spawnParticles(circleLoc, Particle.HAPPY_VILLAGER, 1, 0, 0, 0, 0);
                    if (step % 2 == 0) {
                        context.fx().spawnParticles(circleLoc, Particle.END_ROD, 1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
            }, step * 3L);
        }
    }
    
    private void completePrayer(SpellContext context, Player player, double healAmount, boolean removeEffects) {
        // Heal the player
        double currentHealth = player.getHealth();
        double maxHealth = getMaxHealth(player);
        player.setHealth(Math.min(currentHealth + healAmount, maxHealth));
        
        // Remove negative effects
        if (removeEffects) {
            player.removePotionEffect(PotionEffectType.POISON);
            player.removePotionEffect(PotionEffectType.WITHER);
            player.removePotionEffect(PotionEffectType.WEAKNESS);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.NAUSEA);
        }
        
        // Add enhanced blessings
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 400, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 0));
        
        // Divine completion effects
        createDivineCompletion(context, player.getLocation());
        
        // Completion sounds
        context.fx().playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 1.8f);
        context.fx().playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.5f);
        context.fx().playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1.2f, 1.4f);
        
        player.sendMessage("§e§l🙏 Prayer §6answered! §a+" + String.format("%.1f", healAmount) + " ❤ §6+ Divine Blessings!");
    }
    
    private void createDivineCompletion(SpellContext context, Location center) {
        // Massive divine explosion
        context.fx().spawnParticles(center.clone().add(0, 1, 0), Particle.TOTEM_OF_UNDYING, 100, 2, 2, 2, 0.3);
        context.fx().spawnParticles(center.clone().add(0, 1, 0), Particle.END_ROD, 50, 1.5, 3, 1.5, 0.2);
        context.fx().spawnParticles(center.clone().add(0, 1, 0), Particle.ENCHANT, 80, 2, 2, 2, 0.5);
        
        // Divine light beams
        for (int i = 0; i < 12; i++) {
            final int beam = i;
            context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                double angle = (2 * Math.PI * beam / 12);
                double x = Math.cos(angle) * 3;
                double z = Math.sin(angle) * 3;
                
                for (int h = 0; h < 6; h++) {
                    Location beamLoc = center.clone().add(x, h * 0.8, z);
                    context.fx().spawnParticles(beamLoc, Particle.END_ROD, 2, 0.1, 0.1, 0.1, 0.02);
                }
            }, beam * 2L);
        }
    }

    private double getMaxHealth(org.bukkit.entity.LivingEntity entity) {
        var attr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }
}
