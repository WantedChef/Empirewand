package nl.wantedchef.empirewand.spell.defensive;

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
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Invincible - Ultimate defense from real Empirewand
 */
public class Invincible extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Invincible";
            this.description = "Become completely invincible for a short time";
            this.cooldown = Duration.ofSeconds(180);
            this.spellType = SpellType.UTILITY;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Invincible(this);
        }
    }

    private static final int DEFAULT_DURATION = 80;

    private Invincible(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "invincible";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(50);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        int duration = spellConfig.getInt("values.duration", DEFAULT_DURATION);
        
        // Apply invincibility
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 255));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration, 5));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 3));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 2));
        
        // Visual effect - diamond armor
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    // End effect
                    player.getWorld().spawnParticle(Particle.FLASH, 
                        player.getLocation().add(0, 1, 0), 3, 0, 0, 0, 0);
                    context.fx().playSound(player, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
                    player.sendMessage("§7Invincibility has ended.");
                    cancel();
                    return;
                }
                
                // Diamond armor particles
                for (double y = 0; y <= 2; y += 0.5) {
                    for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 4) {
                        double x = Math.cos(angle) * 0.6;
                        double z = Math.sin(angle) * 0.6;
                        
                        player.getWorld().spawnParticle(Particle.DUST, 
                            player.getLocation().add(x, y, z), 1, 0, 0, 0, 0,
                            new Particle.DustOptions(org.bukkit.Color.AQUA, 1.5f));
                    }
                }
                
                // Power aura
                if (ticks % 10 == 0) {
                    player.getWorld().spawnParticle(Particle.TOTEM, 
                        player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
                    player.getWorld().spawnParticle(Particle.END_ROD, 
                        player.getLocation(), 10, 0.5, 0.5, 0.5, 0.05);
                }
                
                ticks += 5;
            }
        }.runTaskTimer(context.plugin(), 0L, 5L);
        
        // Initial effects
        player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, 
            player.getLocation().add(0, 1, 0), 3, 0, 0, 0, 0);
        player.getWorld().spawnParticle(Particle.FIREWORK, 
            player.getLocation(), 100, 1, 1, 1, 0.2);
        
        context.fx().playSound(player, Sound.ITEM_TOTEM_USE, 1.5f, 0.8f);
        context.fx().playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        
        player.sendMessage("§b§lINVINCIBLE §3MODE ACTIVATED for " + (duration/20) + " seconds!");
    }
}
;
    }
}
