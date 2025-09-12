package nl.wantedchef.empirewand.spell.ice;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Petrify - Freeze target in place
 * Real Empirewand spell
 */
public class Petrify extends Spell<LivingEntity> {

    public static class Builder extends Spell.Builder<LivingEntity> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Petrify";
            this.description = "Freeze your target solid in ice";
            this.cooldown = Duration.ofSeconds(15);
            this.spellType = SpellType.ICE;
        }

        @Override
        @NotNull
        public Spell<LivingEntity> build() {
            return new Petrify(this);
        }
    }

    private static final double DEFAULT_RANGE = 20.0;
    private static final int DEFAULT_DURATION_TICKS = 100;
    private static final double DEFAULT_DAMAGE = 5.0;

    private Petrify(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "petrify";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(15);
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
        int duration = spellConfig.getInt("values.duration_ticks", DEFAULT_DURATION_TICKS);
        double damage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
        
        // Apply effects
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 255));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, duration, 128));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duration, 255));
        target.damage(damage, player);
        
        // Visual effects
        target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation(), 50, 0.5, 1, 0.5, 0.1);
        target.getWorld().spawnParticle(Particle.BLOCK_DUST, target.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0, org.bukkit.Material.ICE.createBlockData());
        
        // Sound effects
        context.fx().playSound(target.getLocation(), Sound.BLOCK_GLASS_PLACE, 1.5f, 0.5f);
        context.fx().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.0f);
        
        player.sendMessage("§b§lTarget Petrified §3for " + (duration/20) + " seconds!");
    }
}
