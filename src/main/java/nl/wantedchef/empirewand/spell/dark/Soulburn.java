package nl.wantedchef.empirewand.spell.dark;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Soulburn - Burns the soul directly from real Empirewand
 */
public class Soulburn extends Spell<LivingEntity> {

    public static class Builder extends Spell.Builder<LivingEntity> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Soulburn";
            this.description = "Burn your enemy's soul directly";
            this.cooldown = Duration.ofSeconds(15);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<LivingEntity> build() {
            return new Soulburn(this);
        }
    }

    private static final double DEFAULT_RANGE = 20.0;
    private static final double DEFAULT_DAMAGE = 25.0;
    private static final int DEFAULT_DURATION_TICKS = 60;

    private Soulburn(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "soulburn";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(25);
    }

    @Override
    protected LivingEntity executeSpell(@NotNull SpellContext context) {
        Player player = context.caster();
        double range = spellConfig.getDouble("values.range", DEFAULT_RANGE);
        
        var target = player.getTargetEntity((int) range);
        if (target instanceof LivingEntity living) {
            return living;
        }
        
        player.sendMessage("§cNo valid target found!");
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull LivingEntity target) {
        if (target == null) return;
        
        Player player = context.caster();
        double totalDamage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
        int duration = spellConfig.getInt("values.duration_ticks", DEFAULT_DURATION_TICKS);
        
        context.fx().playSound(target.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1.0f, 0.5f);
        player.sendMessage("§5§lSoulburn §dignited on target!");
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= duration || !target.isValid() || target.isDead()) {
                    cancel();
                    return;
                }
                
                // Damage over time
                target.damage(totalDamage / (duration / 10), player);
                
                // Purple flame effect
                target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
                target.getWorld().spawnParticle(Particle.WITCH, target.getLocation(), 5, 0.2, 0.3, 0.2, 0);
                
                if (ticks % 20 == 0) {
                    context.fx().playSound(target.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.8f, 0.5f);
                }
                
                ticks += 10;
            }
        }.runTaskTimer(context.plugin(), 0L, 10L);
    }
}
