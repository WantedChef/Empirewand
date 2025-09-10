package nl.wantedchef.empirewand.spell.dark.damage;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.*;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * ShadowStrike - A short shadow sprint/teleport toward or alongside the target
 * with burst impact, intended for quick openers and disengagement in close range.
 */
public class ShadowStrike extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Shadow Strike";
            this.description = "Short shadow sprint/teleport toward or alongside target with burst impact. Ideal for quick openers and disengagement.";
            this.cooldown = Duration.ofSeconds(8);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new ShadowStrike(this);
        }
    }

    private ShadowStrike(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "shadow-strike";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player caster = context.caster();
        double range = spellConfig.getDouble("values.range", 15.0);
        double teleportDistance = spellConfig.getDouble("values.teleport-distance", 5.0);
        double damage = spellConfig.getDouble("values.damage", 4.0);
        int blindnessDuration = spellConfig.getInt("values.blindness-duration-ticks", 40); // 2 seconds

        // Get target entity
        Entity targetEntity = caster.getTargetEntity((int) range);
        if (!(targetEntity instanceof LivingEntity)) {
            context.fx().showError(caster, "no-target");
            return null;
        }
        
        LivingEntity target = (LivingEntity) targetEntity;
        
        // Check if target is valid
        if (target.isDead() || !target.isValid()) {
            context.fx().showError(caster, "invalid-target");
            return null;
        }

        // Calculate teleport position (toward target but offset to the side)
        Location casterLoc = caster.getLocation();
        Location targetLoc = target.getLocation();
        
        Vector direction = targetLoc.toVector().subtract(casterLoc.toVector()).normalize();
        Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        
        // Position slightly to the side and in front of the target
        Location teleportLoc = targetLoc.clone().add(direction.clone().multiply(-1.5)).add(right.multiply(1.5));
        teleportLoc.setY(targetLoc.getY() + 0.5); // Slightly above ground
        
        // Teleport caster
        caster.teleport(teleportLoc);
        
        // Apply effects to target
        target.damage(damage, caster);
        
        // Apply blindness/debuffs
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.BLINDNESS,
            blindnessDuration,
            0,
            false,
            true
        ));
        
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOWNESS,
            blindnessDuration,
            1,
            false,
            true
        ));
        
        // Apply brief speed boost to caster
        caster.addPotionEffect(new PotionEffect(
            PotionEffectType.SPEED,
            20, // 1 second
            2,
            false,
            false
        ));

        // Spawn shadow strike effects
        spawnShadowStrikeEffects(context, casterLoc, teleportLoc, targetLoc);
        
        // Play sound effects
        context.fx().playSound(casterLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        context.fx().playSound(teleportLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        context.fx().playSound(targetLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
        
        caster.sendMessage("§5⚡ §7Shadow Strike executed on: " + target.getName());
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect - all handled in executeSpell
    }
    
    private void spawnShadowStrikeEffects(SpellContext context, Location fromLoc, Location toLoc, Location targetLoc) {
        World world = fromLoc.getWorld();
        
        // Create particle trail from caster to teleport location
        Vector direction = toLoc.toVector().subtract(fromLoc.toVector()).normalize();
        Location currentLoc = fromLoc.clone();
        double distance = fromLoc.distance(toLoc);
        
        for (double d = 0; d < distance; d += 0.5) {
            currentLoc.add(direction.clone().multiply(0.5));
            world.spawnParticle(Particle.SMOKE, currentLoc, 2, 0.1, 0.1, 0.1, 0.02);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, currentLoc, 1, 0.1, 0.1, 0.1, 0.01);
        }
        
        // Create impact particles at target location
        context.fx().spawnParticles(targetLoc, Particle.EXPLOSION, 10, 0.5, 0.5, 0.5, 0.1);
        context.fx().spawnParticles(targetLoc, Particle.SMOKE, 15, 1.0, 1.0, 1.0, 0.05);
        context.fx().spawnParticles(targetLoc, Particle.SOUL_FIRE_FLAME, 8, 0.8, 0.8, 0.8, 0.03);
        
        // Create particles at teleport arrival location
        context.fx().spawnParticles(toLoc, Particle.PORTAL, 20, 0.5, 0.5, 0.5, 0.1);
    }
}