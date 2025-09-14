package nl.wantedchef.empirewand.spell.ice;

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
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * FreezeRay - Continuous freezing beam
 */
public class FreezeRay extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Freeze Ray";
            this.description = "Fire a continuous freezing beam";
            this.cooldown = Duration.ofSeconds(12);
            this.spellType = SpellType.ICE;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new FreezeRay(this);
        }
    }

    private static final double DEFAULT_RANGE = 15.0;
    private static final double DEFAULT_DAMAGE_PER_TICK = 1.5;
    private static final int DEFAULT_DURATION_TICKS = 60;

    private FreezeRay(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "freeze-ray";
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
        double range = spellConfig.getDouble("values.range", DEFAULT_RANGE);
        double damagePerTick = spellConfig.getDouble("values.damage_per_tick", DEFAULT_DAMAGE_PER_TICK);
        int duration = spellConfig.getInt("values.duration_ticks", DEFAULT_DURATION_TICKS);
        
        context.fx().playSound(player, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.5f);
        player.sendMessage("§b§lFreeze Ray §3activated!");
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    cancel();
                    return;
                }
                
                Location start = player.getEyeLocation();
                Vector direction = start.getDirection();
                
                // Create beam effect
                for (double d = 0; d <= range; d += 0.5) {
                    Location particleLoc = start.clone().add(direction.clone().multiply(d));
                    particleLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0, 0, 0, 0);
                    particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(173, 216, 230), 1.0f));
                    
                    // Check for entity hit
                    for (var entity : particleLoc.getWorld().getNearbyEntities(particleLoc, 0.5, 0.5, 0.5)) {
                        if (entity instanceof LivingEntity living && !living.equals(player)) {
                            living.damage(damagePerTick, player);
                            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 3));
                            living.getWorld().spawnParticle(Particle.CRIT, living.getLocation(), 5, 0.3, 0.5, 0.3, 0.02);
                        }
                    }
                }
                
                if (ticks % 10 == 0) {
                    context.fx().playSound(player, Sound.BLOCK_SNOW_PLACE, 0.5f, 2.0f);
                }
                
                ticks += 2;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
    }
}
