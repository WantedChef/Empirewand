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
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Pain - Direct pain spell from real Empirewand
 */
public class Pain extends Spell<LivingEntity> {

    public static class Builder extends Spell.Builder<LivingEntity> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Pain";
            this.description = "Inflict instant pain on your target";
            this.cooldown = Duration.ofSeconds(5);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<LivingEntity> build() {
            return new Pain(this);
        }
    }

    private static final double DEFAULT_RANGE = 30.0;
    private static final double DEFAULT_DAMAGE = 10.0;

    private Pain(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "pain";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(10);
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
        
        target.damage(damage, player);
        
        // Visual effects
        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.05);
        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0);
        
        // Sound effects
        context.fx().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.5f, 0.5f);
        context.fx().playSound(target.getLocation(), Sound.ENTITY_PHANTOM_BITE, 1.0f, 1.5f);
        
        player.sendMessage("§4§lPain §cinflicted!");
    }
}
