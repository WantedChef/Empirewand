package nl.wantedchef.empirewand.spell.fire;

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
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Inferno - Massive area fire damage spell
 */
public class Inferno extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Inferno";
            this.description = "Unleash a devastating inferno around you";
            this.cooldown = Duration.ofSeconds(45);
            this.spellType = SpellType.FIRE;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Inferno(this);
        }
    }

    private static final double DEFAULT_RADIUS = 8.0;
    private static final double DEFAULT_DAMAGE = 15.0;
    private static final int DEFAULT_DURATION_TICKS = 60;

    private Inferno(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "inferno";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(30);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        Player player = context.caster();
        if (player == null) {
            return null;
        }

        double radius = spellConfig.getDouble("values.radius", DEFAULT_RADIUS);
        double damage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
        int duration = spellConfig.getInt("values.duration_ticks", DEFAULT_DURATION_TICKS);
        
        Location center = player.getLocation();
        
        // Create inferno effect
        context.fx().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
        player.sendMessage("§c§lINFERNO UNLEASHED!");
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }
                
                // Fire ring effect
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location loc = center.clone().add(x, 0, z);
                    
                    loc.getWorld().spawnParticle(Particle.FLAME, loc, 5, 0.2, 1, 0.2, 0.05);
                    loc.getWorld().spawnParticle(Particle.LAVA, loc, 1, 0.2, 0.5, 0.2, 0);
                }
                
                // Damage entities
                if (ticks % 10 == 0) {
                    for (var entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                        if (entity instanceof LivingEntity living && !living.equals(player)) {
                            living.damage(damage / 3, player);
                            living.setFireTicks(40);
                            living.getWorld().spawnParticle(Particle.FLAME, living.getLocation(), 10, 0.3, 0.5, 0.3, 0.02);
                        }
                    }
                }
                
                ticks += 2;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
        
        return player;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        // Effects are applied in executeSpell for instant spells
    }
}
