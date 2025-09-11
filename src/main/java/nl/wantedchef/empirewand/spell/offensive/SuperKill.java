package nl.wantedchef.empirewand.spell.offensive;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * SuperKill - Ultimate instant kill spell from real Empirewand
 */
public class SuperKill extends Spell<LivingEntity> {

    public static class Builder extends Spell.Builder<LivingEntity> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "SuperKill";
            this.description = "Instantly eliminate any target";
            this.cooldown = Duration.ofSeconds(120);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<LivingEntity> build() {
            return new SuperKill(this);
        }
    }

    private static final double DEFAULT_RANGE = 50.0;
    private static final boolean DEFAULT_BYPASS_ARMOR = true;

    private SuperKill(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "superkill";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(45);
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
        boolean bypassArmor = spellConfig.getBoolean("flags.bypass_armor", DEFAULT_BYPASS_ARMOR);
        
        // Visual effects - death ray
        Location start = player.getEyeLocation();
        Location end = target.getLocation().add(0, 1, 0);
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);
        
        for (double d = 0; d <= distance; d += 0.5) {
            Location particleLoc = start.clone().add(direction.clone().multiply(d));
            particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                new Particle.DustOptions(org.bukkit.Color.BLACK, 2.0f));
            particleLoc.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, particleLoc, 2, 0.1, 0.1, 0.1, 0);
        }
        
        // Death effect at target
        target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);
        target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation(), 30, 0.5, 0.5, 0.5, 0.05);
        
        // Sound effects
        context.fx().playSound(target.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
        context.fx().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 2.0f);
        
        // Execute the kill
        if (bypassArmor) {
            target.setHealth(0);
        } else {
            target.damage(999999, player);
        }
        
        String targetName = target instanceof Player ? ((Player) target).getName() : target.getType().name();
        player.sendMessage("§4§lSUPERKILL §cexecuted on " + targetName + "!");
    }
}
