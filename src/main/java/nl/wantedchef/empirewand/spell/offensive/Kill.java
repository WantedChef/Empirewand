package nl.wantedchef.empirewand.spell.offensive;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Kill - Instant kill spell from real Empirewand
 */
public class Kill extends Spell<LivingEntity> {

    public static class Builder extends Spell.Builder<LivingEntity> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Kill";
            this.description = "Instantly kill your target";
            this.cooldown = Duration.ofSeconds(60);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<LivingEntity> build() {
            return new Kill(this);
        }
    }

    private static final double DEFAULT_RANGE = 30.0;
    private static final double DEFAULT_DAMAGE = 1000.0;

    private Kill(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "kill";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(35);
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
        
        // Visual effects - death beam
        Location start = player.getEyeLocation();
        Location end = target.getLocation().add(0, 1, 0);
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);
        
        for (double d = 0; d <= distance; d += 0.5) {
            Location particleLoc = start.clone().add(direction.clone().multiply(d));
            particleLoc.getWorld().spawnParticle(Particle.CRIT, particleLoc, 1, 0, 0, 0, 0);
            particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                new Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
        }
        
        // Death effect
        target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);
        target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation(), 20, 0.3, 0.3, 0.3, 0.02);
        
        // Sound effects
        context.fx().playSound(target.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1.5f, 0.5f);
        context.fx().playSound(target.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 1.5f);
        
        // Kill the target
        target.damage(damage, player);
        
        String targetName = target instanceof Player ? ((Player) target).getName() : target.getType().name();
        player.sendMessage("§c§lKILL §4executed on " + targetName + "!");
    }
}
