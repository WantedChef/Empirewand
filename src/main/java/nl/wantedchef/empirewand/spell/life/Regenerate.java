package nl.wantedchef.empirewand.spell.life;

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
 * Regenerate - Powerful regeneration spell
 */
public class Regenerate extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Regenerate";
            this.description = "Grant powerful regeneration over time";
            this.cooldown = Duration.ofSeconds(30);
            this.spellType = SpellType.LIFE;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Regenerate(this);
        }
    }

    private static final int DEFAULT_DURATION = 300;
    private static final int DEFAULT_AMPLIFIER = 2;
    private static final double DEFAULT_HEAL_AMOUNT = 2.0;

    private Regenerate(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "regenerate";
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
        int amplifier = spellConfig.getInt("values.amplifier", DEFAULT_AMPLIFIER);
        double healAmount = spellConfig.getDouble("values.heal_amount", DEFAULT_HEAL_AMOUNT);
        
        // Apply regeneration
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, amplifier));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, duration/2, 0));
        
        // Visual effect over time
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    cancel();
                    return;
                }
                
                // Healing particles
                player.getWorld().spawnParticle(Particle.HEART, 
                    player.getLocation().add(0, 1, 0), 2, 0.3, 0.3, 0.3, 0);
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, 
                    player.getLocation(), 3, 0.5, 0.5, 0.5, 0);
                
                // Periodic extra healing
                if (ticks % 40 == 0) {
                    double currentHealth = player.getHealth();
                    double maxHealth = player.getMaxHealth();
                    if (currentHealth < maxHealth) {
                        player.setHealth(Math.min(currentHealth + healAmount, maxHealth));
                        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, 
                            player.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.1);
                    }
                }
                
                ticks += 10;
            }
        }.runTaskTimer(context.plugin(), 0L, 10L);
        
        // Effects
        context.fx().playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        context.fx().playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, 
            player.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.1);
        
        player.sendMessage("§a§lRegenerate §2activated for " + (duration/20) + " seconds!");
    }
}
