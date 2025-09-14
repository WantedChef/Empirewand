package nl.wantedchef.empirewand.spell.movement.flight;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Lift - Lift entities into the air from real Empirewand
 */
public class Lift extends Spell<Entity> {

    public static class Builder extends Spell.Builder<Entity> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Lift";
            this.description = "Lift a target high into the air";
            this.cooldown = Duration.ofSeconds(10);
            this.spellType = SpellType.MOVEMENT;
        }

        @Override
        @NotNull
        public Spell<Entity> build() {
            return new Lift(this);
        }
    }

    private static final double DEFAULT_RANGE = 20.0;
    private static final double DEFAULT_LIFT_POWER = 1.5;

    private Lift(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "lift";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(8);
    }

    @Override
    protected Entity executeSpell(@NotNull SpellContext context) {
        Player player = context.caster();
        double range = spellConfig.getDouble("values.range", DEFAULT_RANGE);
        
        var target = player.getTargetEntity((int) range);
        if (target != null) {
            return target;
        }
        
        player.sendMessage("\u00A7cNo target found!");
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Entity target) {
        if (target == null) return;
        
        Player player = context.caster();
        double liftPower = spellConfig.getDouble("values.lift_power", DEFAULT_LIFT_POWER);
        
        // Lift the target
        Vector lift = new Vector(0, liftPower, 0);
        target.setVelocity(target.getVelocity().add(lift));
        
        // Add slow falling if it's a living entity
        if (target instanceof LivingEntity living) {
            living.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SLOW_FALLING, 100, 0));
        }
        
        // Visual effects
        target.getWorld().spawnParticle(Particle.CLOUD, 
            target.getLocation(), 20, 0.5, 0.2, 0.5, 0.05);
        target.getWorld().spawnParticle(Particle.END_ROD, 
            target.getLocation(), 15, 0.3, 0.5, 0.3, 0.02);
        
        // Sound effects
        context.fx().playSound(target.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.5f, 1.0f);
        context.fx().playSound(target.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 1.5f);
        
        String targetName = target instanceof Player ? ((Player) target).getName() : target.getType().name();
        player.sendMessage("\u00A7f\u00A7lLifted \u00A77" + targetName + " into the air!");
    }
}
