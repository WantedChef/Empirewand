package nl.wantedchef.empirewand.spell.movement;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Phase - Phase through solid blocks from real Empirewand
 */
public class Phase extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Phase";
            this.description = "Phase through solid blocks like a ghost";
            this.cooldown = Duration.ofSeconds(30);
            this.spellType = SpellType.MOVEMENT;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new Phase(this);
        }
    }

    private static final int DEFAULT_DURATION = 100;

    private Phase(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "phase";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(25);
    }

    @Override
    protected Void executeSpell(@NotNull SpellContext context) {
        Player player = context.caster();
        int duration = spellConfig.getInt("values.duration", DEFAULT_DURATION);
        
        GameMode originalMode = player.getGameMode();
        
        // Set to spectator mode for phasing
        player.setGameMode(GameMode.SPECTATOR);
        
        // Visual effect
        player.getWorld().spawnParticle(Particle.PORTAL, 
            player.getLocation(), 50, 0.5, 1, 0.5, 0.1);
        
        // Sound effects
        context.fx().playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        context.fx().playSound(player, Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 2.0f);
        
        player.sendMessage("\u00A75\u00A7lPhase \u00A7dactivated for " + (duration/20) + " seconds!");
        
        // Phase effect
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    // Return to original mode
                    player.setGameMode(originalMode);
                    player.getWorld().spawnParticle(Particle.PORTAL, 
                        player.getLocation(), 30, 0.5, 1, 0.5, 0.05);
                    context.fx().playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    player.sendMessage("\u00A77Phase has ended.");
                    cancel();
                    return;
                }
                
                // Continuous phase particles
                player.getWorld().spawnParticle(Particle.PORTAL, 
                    player.getLocation(), 3, 0.2, 0.5, 0.2, 0.02);
                player.getWorld().spawnParticle(Particle.WITCH, 
                    player.getLocation(), 2, 0.3, 0.3, 0.3, 0);
                
                ticks += 5;
            }
        }.runTaskTimer(context.plugin(), 0L, 5L);
        
        return null;
    }

    protected void applyEffect(@NotNull SpellContext context, Void effect) {
        // No synchronous effect needed
    }

    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effect handled in executeSpell
    }
}
