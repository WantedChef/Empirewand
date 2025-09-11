package nl.wantedchef.empirewand.spell.offensive;

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
 * Wound - Inflict serious wounds from real Empirewand
 */
public class Wound extends Spell<LivingEntity> {

    public static class Builder extends Spell.Builder<LivingEntity> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Wound";
            this.description = "Inflict serious wounds on your target";
            this.cooldown = Duration.ofSeconds(8);
            this.spellType = SpellType.OFFENSIVE;
        }

        @Override
        @NotNull
        public Spell<LivingEntity> build() {
            return new Wound(this);
        }
    }

    private static final double DEFAULT_RANGE = 25.0;
    private static final double DEFAULT_DAMAGE = 15.0;
    private static final int DEFAULT_BLEED_DURATION = 100;

    private Wound(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "wound";
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
        double damage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
        int bleedDuration = spellConfig.getInt("values.bleed_duration", DEFAULT_BLEED_DURATION);
        
        // Inflict wound damage
        target.damage(damage, player);
        
        // Apply bleeding effects
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, bleedDuration, 1));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, bleedDuration, 1));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, bleedDuration/2, 0));
        
        // Visual effects - blood and wounds
        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, 
            target.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.1);
        target.getWorld().spawnParticle(Particle.DUST, 
            target.getLocation(), 20, 0.5, 0.5, 0.5, 0,
            new Particle.DustOptions(org.bukkit.Color.fromRGB(139, 0, 0), 1.5f));
        target.getWorld().spawnParticle(Particle.CRIMSON_SPORE, 
            target.getLocation(), 30, 0.5, 0.5, 0.5, 0.05);
        
        // Sound effects
        context.fx().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.5f, 0.8f);
        context.fx().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 1.5f);
        
        String targetName = target instanceof Player ? ((Player) target).getName() : target.getType().name();
        player.sendMessage("§c§lWound §4inflicted on " + targetName + "! They will bleed for " + (bleedDuration/20) + " seconds!");
    }
}
