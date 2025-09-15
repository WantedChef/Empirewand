package nl.wantedchef.empirewand.spell.life;

import nl.wantedchef.empirewand.api.service.ConfigService;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.ProjectileSpell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;


public class LifeSteal extends ProjectileSpell<Snowball> {

    public static class Builder extends ProjectileSpell.Builder<Snowball> {
        public Builder(EmpireWandAPI api) {
            super(api, Snowball.class);
            this.name = "Life Steal";
            this.description = "Steals health from the target.";
            this.cooldown = java.time.Duration.ofSeconds(3);
            this.spellType = SpellType.LIFE;
            this.trailParticle = Particle.DUST; // Enable custom trail via override
            this.hitSound = Sound.ENTITY_GENERIC_DRINK;
        }

        @Override
        @NotNull
        public ProjectileSpell<Snowball> build() {
            return new LifeSteal(this);
        }
    }

    private LifeSteal(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "lifesteal";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected void launchProjectile(@NotNull SpellContext context) {
        super.launchProjectile(context);
        context.fx().playSound(context.caster(), Sound.ENTITY_WITCH_THROW, 0.8f, 1.2f);
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event) {
        if (!(event.getHitEntity() instanceof LivingEntity target))
            return;

        double damage = spellConfig.getDouble("values.damage", 4.0);
        double stealModifier = spellConfig.getDouble("values.steal-modifier", 0.5);
        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig()
                .getBoolean("features.friendly-fire", false);

        Player caster = context.caster();
        if (target.equals(caster) && !friendlyFire)
            return;

        target.damage(damage, caster);
        double stolenHealth = damage * stealModifier;
        var maxAttr = caster.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = maxAttr != null ? maxAttr.getValue() : 20.0;
        caster.setHealth(Math.min(maxHealth, caster.getHealth() + stolenHealth));

        context.fx().spawnParticles(caster.getLocation(), Particle.HEART, 5, 0.5, 1, 0.5, 0);
    }

    @Override
    protected void startTrailEffect(@NotNull Snowball projectile, @NotNull SpellContext context) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!projectile.isValid() || projectile.isDead()) {
                    cancel();
                    return;
                }
                projectile.getWorld().spawnParticle(
                        Particle.DUST,
                        projectile.getLocation(),
                        8,
                        0.08,
                        0.08,
                        0.08,
                        0,
                        new Particle.DustOptions(Color.fromRGB(170, 0, 0), 1.0f));
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }
}
