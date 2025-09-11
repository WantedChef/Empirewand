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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Wither - Apply wither effect from real Empirewand
 */
public class Wither extends Spell<LivingEntity> {

    public static class Builder extends Spell.Builder<LivingEntity> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Wither";
            this.description = "Inflict wither curse upon your target";
            this.cooldown = Duration.ofSeconds(8);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<LivingEntity> build() {
            return new Wither(this);
        }
    }

    private static final double DEFAULT_RANGE = 25.0;
    private static final int DEFAULT_DURATION = 200;
    private static final int DEFAULT_AMPLIFIER = 2;

    private Wither(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "wither";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(20);
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
        int duration = spellConfig.getInt("values.duration", DEFAULT_DURATION);
        int amplifier = spellConfig.getInt("values.amplifier", DEFAULT_AMPLIFIER);
        
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration, amplifier));
        
        // Visual effects
        target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);
        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 10, 0.2, 0.3, 0.2, 0.01);
        
        // Sound effects
        context.fx().playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 2.0f);
        context.fx().playSound(target.getLocation(), Sound.ENTITY_WITHER_HURT, 0.8f, 1.5f);
        
        player.sendMessage("§5§lWither §dcurse applied!");
    }
}
