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
 * Shell - Defensive barrier from real Empirewand
 */
public class Shell extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Shell";
            this.description = "Create a protective shell around yourself";
            this.cooldown = Duration.ofSeconds(30);
            this.spellType = SpellType.UTILITY;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Shell(this);
        }
    }

    private static final int DEFAULT_DURATION = 200;
    private static final int DEFAULT_RESISTANCE_LEVEL = 2;

    private Shell(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "shell";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(15);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        int duration = spellConfig.getInt("values.duration", DEFAULT_DURATION);
        int resistanceLevel = spellConfig.getInt("values.resistance_level", DEFAULT_RESISTANCE_LEVEL);
        
        // Apply shell protection
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, resistanceLevel));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration, 2));
        
        // Visual shell effect
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    player.sendMessage("§7Shell protection has faded.");
                    cancel();
                    return;
                }
                
                // Shell particle effect
                for (double phi = 0; phi <= Math.PI; phi += Math.PI / 6) {
                    for (double theta = 0; theta < Math.PI * 2; theta += Math.PI / 6) {
                        double r = 1.2;
                        double x = r * Math.sin(phi) * Math.cos(theta);
                        double y = r * Math.cos(phi) + 1;
                        double z = r * Math.sin(phi) * Math.sin(theta);
                        
                        if (ticks % 10 == 0) {
                            player.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, 
                                player.getLocation().add(x, y, z), 1, 0, 0, 0, 0);
                        }
                    }
                }
                
                // Shimmer effect
                if (ticks % 20 == 0) {
                    player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, 
                        player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.05);
                }
                
                ticks += 5;
            }
        }.runTaskTimer(context.plugin(), 0L, 5L);
        
        // Initial effects
        player.getWorld().spawnParticle(Particle.SPLASH, 
            player.getLocation(), 50, 1, 1, 1, 0.1);
        context.fx().playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
        context.fx().playSound(player, Sound.ENTITY_TURTLE_EGG_CRACK, 1.5f, 0.8f);
        
        player.sendMessage("§3§lShell §bprotection activated for " + (duration/20) + " seconds!");
    }
}
