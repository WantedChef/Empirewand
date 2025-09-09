package nl.wantedchef.empirewand.spell.misc;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.service.ConfigService;

import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class ExplosionWave extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Explosion Wave";
            this.description = "Creates an explosive wave that damages and knocks back entities in a cone.";
            this.cooldown = java.time.Duration.ofSeconds(12);
            this.spellType = SpellType.MISC;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new ExplosionWave(this);
        }
    }

    private ExplosionWave(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "explosion-wave";
    }

    @Override
    public Component displayName() {
        return Component.text("Explosion Wave").color(TextColor.color(255, 69, 0));
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double range = spellConfig.getDouble("values.range", 8.0);
        double coneAngle = spellConfig.getDouble("values.cone-angle-degrees", 70.0);
        double baseDamage = spellConfig.getDouble("values.damage", 6.0);
        double knockbackStrength = spellConfig.getDouble("values.knockback-strength", 0.9);
        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig()
                .getBoolean("features.friendly-fire", false);
        float explosionPower = (float) spellConfig.getDouble("values.explosion-power", 1.5f);

        List<LivingEntity> targets = getEntitiesInCone(player, range, coneAngle);

        for (LivingEntity target : targets) {
            if (target.equals(player) && !friendlyFire)
                continue;

            double distance = player.getLocation().distance(target.getLocation());
            double damageMultiplier = 1.0 - (distance / range);
            target.damage(baseDamage * damageMultiplier, player);

            Vector kbDirection = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
            kbDirection.multiply(knockbackStrength * damageMultiplier).setY(0.4 + (0.2 * damageMultiplier));
            target.setVelocity(kbDirection);

            context.fx().spawnParticles(target.getLocation(), Particle.EXPLOSION, 8, 0.3, 0.3, 0.3, 0.15);
        }

        player.getWorld().createExplosion(player.getLocation(), explosionPower, false, false);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    private List<LivingEntity> getEntitiesInCone(Player player, double range, double coneAngle) {
        List<LivingEntity> targets = new ArrayList<>();
        Vector playerDir = player.getEyeLocation().getDirection().normalize();
        double angleRadians = Math.toRadians(coneAngle / 2.0);

        for (LivingEntity entity : player.getWorld().getLivingEntities()) {
            if (entity.equals(player) || entity.getLocation().distanceSquared(player.getLocation()) > range * range)
                continue;

            Vector toEntity = entity.getEyeLocation().toVector().subtract(player.getEyeLocation().toVector())
                    .normalize();
            if (playerDir.angle(toEntity) <= angleRadians) {
                targets.add(entity);
            }
        }
        return targets;
    }
}
