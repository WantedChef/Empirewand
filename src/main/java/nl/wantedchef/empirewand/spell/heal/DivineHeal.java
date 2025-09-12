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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * DivineHeal - Ultimate healing spell
 */
public class DivineHeal extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Divine Heal";
            this.description = "Channel divine power to heal and protect all allies";
            this.cooldown = Duration.ofSeconds(60);
            this.spellType = SpellType.HEAL;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new DivineHeal(this);
        }
    }

    private static final double DEFAULT_RADIUS = 15.0;
    private static final int DEFAULT_DURATION = 200;

    private DivineHeal(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "divineheal";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(40);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        double radius = spellConfig.getDouble("values.radius", DEFAULT_RADIUS);
        int duration = spellConfig.getInt("values.duration", DEFAULT_DURATION);
        
        Location center = player.getLocation();
        
        // Create divine pillar effect
        for (int y = 0; y < 10; y++) {
            final int height = y;
            context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                center.getWorld().spawnParticle(Particle.END_ROD, 
                    center.clone().add(0, height, 0), 10, 0.5, 0, 0.5, 0.05);
                center.getWorld().spawnParticle(Particle.TOTEM, 
                    center.clone().add(0, height, 0), 5, 0.3, 0, 0.3, 0.02);
            }, y * 2L);
        }
        
        // Heal and buff all nearby allies
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }
                
                // Divine aura particles
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location loc = center.clone().add(x, 0.5, z);
                    loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 1, 0, 0, 0, 0);
                }
                
                // Heal and buff allies
                for (var entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                    if (entity instanceof Player targetPlayer) {
                        // Heal
                        targetPlayer.setHealth(Math.min(targetPlayer.getHealth() + 1, targetPlayer.getMaxHealth()));
                        
                        // Apply divine protection
                        if (ticks % 40 == 0) {
                            targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                            targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 1));
                            targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 2));
                            targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 0));
                        }
                        
                        // Remove negative effects
                        targetPlayer.setFireTicks(0);
                        if (ticks % 20 == 0) {
                            targetPlayer.removePotionEffect(PotionEffectType.POISON);
                            targetPlayer.removePotionEffect(PotionEffectType.WITHER);
                        }
                        
                        // Individual blessing effect
                        if (ticks % 10 == 0) {
                            targetPlayer.getWorld().spawnParticle(Particle.END_ROD, 
                                targetPlayer.getLocation().add(0, 2, 0), 3, 0.2, 0.2, 0.2, 0.02);
                        }
                    }
                }
                
                if (ticks % 20 == 0) {
                    context.fx().playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.5f);
                }
                
                ticks += 5;
            }
        }.runTaskTimer(context.plugin(), 0L, 5L);
        
        // Initial effects
        context.fx().playSound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 1.0f);
        context.fx().playSound(center, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.5f);
        
        player.sendMessage("§6§lDivine Heal §echanneled! Blessing area for " + (duration/20) + " seconds!");
    }
}
