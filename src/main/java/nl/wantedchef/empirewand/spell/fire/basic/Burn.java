package nl.wantedchef.empirewand.spell.fire.basic;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Burn - Standard fire damage spell from real Empirewand
 */
public class Burn extends Spell<LivingEntity> {

    public static class Builder extends Spell.Builder<LivingEntity> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Burn";
            this.description = "Ignite your target with intense flames";
            this.cooldown = Duration.ofSeconds(3);
            this.spellType = SpellType.FIRE;
        }

        @Override
        @NotNull
        public Spell<LivingEntity> build() {
            return new Burn(this);
        }
    }

    private static final double DEFAULT_DAMAGE = 8.0;
    private static final int DEFAULT_FIRE_TICKS = 100;
    private static final double DEFAULT_RANGE = 20.0;

    private Burn(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "burn";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
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
        double damage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
        int fireTicks = spellConfig.getInt("values.fire_ticks", DEFAULT_FIRE_TICKS);
        
        target.damage(damage, player);
        target.setFireTicks(fireTicks);
        
        // Visual effects
        target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);
        target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation(), 15, 0.2, 0.3, 0.2, 0.02);
        
        // Sound effects
        context.fx().playSound(target.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.2f);
        context.fx().playSound(target.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.8f, 1.0f);
        
        player.sendMessage("§6Target ignited with §lBurn§r§6!");
    }
}
