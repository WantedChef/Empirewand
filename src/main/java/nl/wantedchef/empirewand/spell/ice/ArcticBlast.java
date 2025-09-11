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
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * ArcticBlast - Massive ice explosion
 */
public class ArcticBlast extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Arctic Blast";
            this.description = "Unleash a devastating arctic explosion";
            this.cooldown = Duration.ofSeconds(30);
            this.spellType = SpellType.ICE;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new ArcticBlast(this);
        }
    }

    private static final double DEFAULT_RADIUS = 10.0;
    private static final double DEFAULT_DAMAGE = 20.0;
    private static final int DEFAULT_FREEZE_DURATION = 100;

    private ArcticBlast(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "arcticblast";
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
        double radius = spellConfig.getDouble("values.radius", DEFAULT_RADIUS);
        double damage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
        int freezeDuration = spellConfig.getInt("values.freeze_duration", DEFAULT_FREEZE_DURATION);
        
        Location center = player.getLocation();
        
        // Create blast effect
        for (int i = 0; i < 3; i++) {
            final int wave = i;
            context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                double waveRadius = radius * (wave + 1) / 3;
                
                // Particle ring
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 32) {
                    double x = Math.cos(angle) * waveRadius;
                    double z = Math.sin(angle) * waveRadius;
                    Location loc = center.clone().add(x, 0.5, z);
                    
                    loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 3, 0.1, 0.5, 0.1, 0.05);
                    loc.getWorld().spawnParticle(Particle.BLOCK_DUST, loc, 2, 0.1, 0.3, 0.1, 0, org.bukkit.Material.ICE.createBlockData());
                }
                
                // Damage and freeze entities in wave
                for (var entity : center.getWorld().getNearbyEntities(center, waveRadius, waveRadius, waveRadius)) {
                    if (entity instanceof LivingEntity living && !living.equals(player)) {
                        double distance = living.getLocation().distance(center);
                        if (distance <= waveRadius && distance > (wave > 0 ? radius * wave / 3 : 0)) {
                            living.damage(damage * (1 - distance / radius), player);
                            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, freezeDuration, 4));
                            living.getWorld().spawnParticle(Particle.SNOWFLAKE, living.getLocation(), 20, 0.5, 1, 0.5, 0.1);
                        }
                    }
                }
                
                context.fx().playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 2.0f, 0.5f + wave * 0.2f);
            }, i * 5L);
        }
        
        context.fx().playSound(center, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 2.0f, 2.0f);
        player.sendMessage("§b§lARCTIC BLAST §3unleashed!");
    }
}
