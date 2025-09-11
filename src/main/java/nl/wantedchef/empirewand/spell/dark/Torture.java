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
 * Torture - Inflict extreme pain from real Empirewand
 */
public class Torture extends Spell<LivingEntity> {

    public static class Builder extends Spell.Builder<LivingEntity> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Torture";
            this.description = "Inflict unbearable torture upon your enemy";
            this.cooldown = Duration.ofSeconds(20);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<LivingEntity> build() {
            return new Torture(this);
        }
    }

    private static final double DEFAULT_RANGE = 15.0;
    private static final double DEFAULT_DAMAGE = 15.0;
    private static final int DEFAULT_DURATION = 100;

    private Torture(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "torture";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(30);
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
        int duration = spellConfig.getInt("values.duration", DEFAULT_DURATION);
        
        // Apply multiple negative effects
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 2));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 2));
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, duration, 1));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duration, 3));
        target.damage(damage, player);
        
        // Visual effects
        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        target.getWorld().spawnParticle(Particle.CRIMSON_SPORE, target.getLocation(), 30, 0.5, 1, 0.5, 0.05);
        
        // Sound effects
        context.fx().playSound(target.getLocation(), Sound.ENTITY_VILLAGER_HURT, 2.0f, 0.5f);
        context.fx().playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 1.5f);
        
        player.sendMessage("§4§lTorture §cinflicted!");
    }
}
