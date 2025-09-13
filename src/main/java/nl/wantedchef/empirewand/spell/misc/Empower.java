package nl.wantedchef.empirewand.spell.misc;

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
 * Empower - Buffs your spells from real Empirewand
 */
public class Empower extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Empower";
            this.description = "Empower yourself with increased magical abilities";
            this.cooldown = Duration.ofSeconds(45);
            this.spellType = SpellType.MISC;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Empower(this);
        }
    }

    private static final int DEFAULT_DURATION = 400;

    private Empower(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "empower";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(25);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        int duration = spellConfig.getInt("values.duration", DEFAULT_DURATION);
        
        // Apply empowerment effects (stronger levels)
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 2)); // Increased from 1 to 2
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 2)); // Increased from 1 to 2
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, duration, 2)); // Increased from 1 to 2
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 2)); // Increased from 1 to 2
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 1)); // Increased from 0 to 1
        
        // Visual effect over time
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    player.sendMessage("§7Empowerment has faded.");
                    cancel();
                    return;
                }
                
                // Aura particles
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 4) {
                    double x = Math.cos(angle) * 0.5;
                    double z = Math.sin(angle) * 0.5;
                    double y = Math.sin(ticks * 0.1) * 0.5 + 1;
                    player.getWorld().spawnParticle(Particle.ENCHANT, 
                        player.getLocation().add(x, y, z), 1, 0, 0, 0, 0);
                }
                
                // Power particles
                if (ticks % 10 == 0) {
                    player.getWorld().spawnParticle(Particle.WITCH, 
                        player.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0);
                    player.getWorld().spawnParticle(Particle.CRIT, 
                        player.getLocation(), 10, 0.5, 0.5, 0.5, 0.02);
                }
                
                ticks += 5;
            }
        }.runTaskTimer(context.plugin(), 0L, 5L);
        
        // Initial effects (more noticeable)
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, 
            player.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
        player.getWorld().spawnParticle(Particle.END_ROD, 
            player.getLocation(), 100, 1.5, 1.5, 1.5, 0.2);
        player.getWorld().spawnParticle(Particle.DRAGON_BREATH, 
            player.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);
        
        context.fx().playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 0.8f);
        context.fx().playSound(player, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.5f, 1.5f);
        
        player.sendMessage("§6§l✨ EMPOWER ACTIVATED! ✨");
        player.sendMessage("§eStrength, Speed, Haste, Jump Boost, and Resistance increased!");
        player.sendMessage("§7Duration: " + (duration/20) + " seconds");
    }
}
