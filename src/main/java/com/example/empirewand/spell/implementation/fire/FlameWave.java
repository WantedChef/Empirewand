package com.example.empirewand.spell.implementation.fire;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.ConfigService;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class FlameWave extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Flame Wave";
            this.description = "Unleashes a wave of fire in a cone.";
            this.cooldown = java.time.Duration.ofSeconds(6);
            this.spellType = SpellType.FIRE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new FlameWave(this);
        }
    }

    private FlameWave(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "flame-wave";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double range = spellConfig.getDouble("values.range", 6.0);
        double coneAngle = spellConfig.getDouble("values.cone-angle-degrees", 60.0);
        double damage = spellConfig.getDouble("values.damage", 4.0);
        int fireTicks = spellConfig.getInt("values.fire-ticks", 80);
        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig()
                .getBoolean("features.friendly-fire", false);

        List<LivingEntity> targets = getEntitiesInCone(player, range, coneAngle);

        for (LivingEntity target : targets) {
            if (target.equals(player) && !friendlyFire)
                continue;
            if (target.isDead() || !target.isValid())
                continue;

            target.damage(damage, player);
            target.setFireTicks(fireTicks);

            context.fx().spawnParticles(target.getLocation(), Particle.FLAME, 10, 0.2, 0.2, 0.2, 0.05);
            context.fx().spawnParticles(target.getLocation(), Particle.FLAME, 10, 0.2, 0.2, 0.2, 0.05);
            context.fx().spawnParticles(target.getLocation(), Particle.SMOKE, 5, 0.2, 0.2, 0.2, 0.1);
        }

        context.fx().playSound(player, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.8f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    private List<LivingEntity> getEntitiesInCone(Player player, double range, double coneAngle) {
        List<LivingEntity> targets = new ArrayList<>();
        Location playerLoc = player.getEyeLocation();
        Vector playerDir = playerLoc.getDirection().normalize();

        for (var entity : player.getWorld().getNearbyEntities(playerLoc, range, range, range)) {
            if (entity instanceof LivingEntity living) {
                Vector toEntity = living.getEyeLocation().toVector().subtract(playerLoc.toVector());
                if (toEntity.lengthSquared() > range * range)
                    continue;

                if (playerDir.angle(toEntity.normalize()) < Math.toRadians(coneAngle / 2.0)) {
                    targets.add(living);
                }
            }
        }
        return targets;
    }
}