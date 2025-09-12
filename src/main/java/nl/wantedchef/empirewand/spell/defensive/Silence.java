package nl.wantedchef.empirewand.spell.defensive;

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
 * Silence - Silence enemies from real Empirewand
 */
public class Silence extends Spell<LivingEntity> {

    public static class Builder extends Spell.Builder<LivingEntity> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Silence";
            this.description = "Silence your target, preventing them from using abilities";
            this.cooldown = Duration.ofSeconds(20);
            this.spellType = SpellType.MISC;
        }

        @Override
        @NotNull
        public Spell<LivingEntity> build() {
            return new Silence(this);
        }
    }

    private static final double DEFAULT_RANGE = 25.0;
    private static final int DEFAULT_DURATION = 100;

    private Silence(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "silence";
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
        
        // Apply silence effects (weakening and slowing abilities)
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 2));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duration, 3));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 1));
        
        // Visual effects - silence aura
        for (int i = 0; i < 3; i++) {
            final int wave = i;
            context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                target.getWorld().spawnParticle(Particle.WITCH, 
                    target.getLocation().add(0, 1 + wave * 0.3, 0), 15, 0.3, 0.3, 0.3, 0.02);
                target.getWorld().spawnParticle(Particle.SMOKE, 
                    target.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.01);
            }, i * 3L);
        }
        
        // Mute effect
        target.getWorld().spawnParticle(Particle.BLOCK_MARKER, 
            target.getLocation().add(0, 2, 0), 1, 0, 0, 0, 0);
        
        // Sound effects (ironic for a silence spell)
        context.fx().playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.3f, 2.0f);
        context.fx().playSound(target.getLocation(), Sound.AMBIENT_CAVE, 1.0f, 0.5f);
        
        String targetName = target instanceof Player ? ((Player) target).getName() : target.getType().name();
        player.sendMessage("§5§lSilence §dcast on " + targetName + " for " + (duration/20) + " seconds!");
    }
}
