package nl.wantedchef.empirewand.spell.movement;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Rocket - Launch yourself like a rocket from real Empirewand
 */
public class Rocket extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Rocket";
            this.description = "Launch yourself like a rocket";
            this.cooldown = Duration.ofSeconds(20);
            this.spellType = SpellType.MOVEMENT;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Rocket(this);
        }
    }

    private static final double DEFAULT_POWER = 3.0;
    private static final int DEFAULT_DURATION = 60;

    private Rocket(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "rocket";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(18);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        double power = spellConfig.getDouble("values.power", DEFAULT_POWER);
        int duration = spellConfig.getInt("values.duration", DEFAULT_DURATION);
        
        // Launch the player
        Vector direction = player.getLocation().getDirection();
        if (player.isSneaking()) {
            // Launch upward if sneaking
            direction = new Vector(0, 1, 0);
        }
        
        player.setVelocity(direction.multiply(power));
        
        // Rocket trail effect
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline() || player.isOnGround()) {
                    // Landing effect
                    player.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, 
                        player.getLocation(), 1, 0, 0, 0, 0);
                    context.fx().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
                    cancel();
                    return;
                }
                
                // Rocket trail
                player.getWorld().spawnParticle(Particle.FLAME, 
                    player.getLocation(), 10, 0.2, 0.2, 0.2, 0.05);
                player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, 
                    player.getLocation(), 5, 0.1, 0.1, 0.1, 0.02);
                player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, 
                    player.getLocation(), 3, 0.3, 0.3, 0.3, 0.1);
                
                // Boost effect
                if (ticks < 20) {
                    player.setVelocity(player.getVelocity().add(direction.clone().multiply(0.1)));
                }
                
                // Sound
                if (ticks % 5 == 0) {
                    context.fx().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.0f);
                }
                
                ticks += 2;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
        
        // Initial effects
        player.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, 
            player.getLocation(), 1, 0, 0, 0, 0);
        context.fx().playSound(player, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.5f, 0.8f);
        context.fx().playSound(player, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
        
        player.sendMessage("§c§lRocket §elaunch activated!");
        
        // Give temporary fire resistance
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, duration + 100, 0));
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.RESISTANCE, duration + 100, 1));
    }
}
