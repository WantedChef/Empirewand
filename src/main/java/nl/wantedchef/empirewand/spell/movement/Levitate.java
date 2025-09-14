package nl.wantedchef.empirewand.spell.movement;

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
 * Levitate - Float in the air from real Empirewand
 */
public class Levitate extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Levitate";
            this.description = "Float gracefully through the air";
            this.cooldown = Duration.ofSeconds(15);
            this.spellType = SpellType.MOVEMENT;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Levitate(this);
        }
    }

    private static final int DEFAULT_DURATION = 200;
    private static final int DEFAULT_AMPLIFIER = 1;

    private Levitate(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "levitate";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(12);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        Player player = context.caster();
        if (player == null) {
            return null;
        }

        int duration = spellConfig.getInt("values.duration", DEFAULT_DURATION);
        int amplifier = spellConfig.getInt("values.amplifier", DEFAULT_AMPLIFIER);
        
        // Apply levitation
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, duration, amplifier));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration + 100, 0));
        
        // Visual effects
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    cancel();
                    return;
                }
                
                // Cloud particles under feet
                player.getWorld().spawnParticle(Particle.CLOUD, 
                    player.getLocation().subtract(0, 0.5, 0), 5, 0.3, 0.1, 0.3, 0.02);
                player.getWorld().spawnParticle(Particle.END_ROD, 
                    player.getLocation(), 2, 0.2, 0.1, 0.2, 0.01);
                
                ticks += 5;
            }
        }.runTaskTimer(context.plugin(), 0L, 5L);
        
        // Sound effects
        context.fx().playSound(player, Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.5f);
        context.fx().playSound(player, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 2.0f);
        
        player.sendMessage("\u00A7f\u00A7lLevitate \u00A77activated for " + (duration/20) + " seconds!");
        
        return player;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        // Effects are applied in executeSpell for instant spells
    }
}
