package nl.wantedchef.empirewand.spell.fire;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * MagmaWave - Sends a wave of magma forward
 */
public class MagmaWave extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Magma Wave";
            this.description = "Send a devastating wave of magma forward";
            this.cooldown = Duration.ofSeconds(20);
            this.spellType = SpellType.FIRE;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new MagmaWave(this);
        }
    }

    private static final double DEFAULT_DAMAGE = 12.0;
    private static final double DEFAULT_RANGE = 15.0;
    private static final double DEFAULT_WIDTH = 5.0;

    private MagmaWave(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "magmawave";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(20);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        double damage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
        double range = spellConfig.getDouble("values.range", DEFAULT_RANGE);
        double width = spellConfig.getDouble("values.width", DEFAULT_WIDTH);
        
        Vector direction = player.getLocation().getDirection().normalize();
        Location start = player.getLocation();
        
        context.fx().playSound(start, Sound.ENTITY_BLAZE_SHOOT, 2.0f, 0.5f);
        player.sendMessage("§6§lMagma Wave §eunleashed!");
        
        new BukkitRunnable() {
            double distance = 0;
            
            @Override
            public void run() {
                if (distance >= range) {
                    cancel();
                    return;
                }
                
                Location waveLoc = start.clone().add(direction.clone().multiply(distance));
                
                // Create wave effect
                for (double w = -width/2; w <= width/2; w += 0.5) {
                    Vector perpendicular = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
                    Location particleLoc = waveLoc.clone().add(perpendicular.clone().multiply(w));
                    
                    particleLoc.getWorld().spawnParticle(Particle.LAVA, particleLoc, 3, 0.1, 0.5, 0.1, 0);
                    particleLoc.getWorld().spawnParticle(Particle.FLAME, particleLoc, 5, 0.2, 0.3, 0.2, 0.02);
                    
                    // Set temporary magma blocks on ground
                    if (particleLoc.getBlock().getType() == Material.AIR && 
                        particleLoc.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
                        particleLoc.getBlock().setType(Material.MAGMA_BLOCK);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (particleLoc.getBlock().getType() == Material.MAGMA_BLOCK) {
                                    particleLoc.getBlock().setType(Material.AIR);
                                }
                            }
                        }.runTaskLater(context.plugin(), 60L);
                    }
                }
                
                // Damage entities in wave
                for (var entity : waveLoc.getWorld().getNearbyEntities(waveLoc, width/2, 2, width/2)) {
                    if (entity instanceof LivingEntity living && !living.equals(player)) {
                        living.damage(damage, player);
                        living.setFireTicks(80);
                        living.setVelocity(living.getVelocity().add(direction.clone().multiply(0.5).setY(0.3)));
                    }
                }
                
                distance += 1;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
    }
}
