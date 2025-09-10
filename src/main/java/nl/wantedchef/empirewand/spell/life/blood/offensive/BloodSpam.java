package nl.wantedchef.empirewand.spell.life.blood.offensive;

import nl.wantedchef.empirewand.api.EmpireWandAPI;

import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.spell.base.PrereqInterface;
import nl.wantedchef.empirewand.spell.base.ProjectileSpell;
import nl.wantedchef.empirewand.spell.base.SpellContext;
import nl.wantedchef.empirewand.spell.base.SpellType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BloodSpam extends ProjectileSpell<Snowball> {

    public static class Builder extends ProjectileSpell.Builder<Snowball> {
        public Builder(EmpireWandAPI api) {
            super(api, Snowball.class);
            this.name = "Blood Spam";
            this.description = "Launches a burst of blood projectiles.";
            this.cooldown = java.time.Duration.ofSeconds(6);
            this.spellType = SpellType.LIFE;
            this.trailParticle = null; // Custom trail
        }

        @Override
        @NotNull
        public ProjectileSpell<Snowball> build() {
            return new BloodSpam(this);
        }
    }

    private BloodSpam(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "blood-spam";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(SpellContext context) {
        Player caster = context.caster();
        int projectileCount = spellConfig.getInt("values.projectile-count", 8);
        int delayTicks = spellConfig.getInt("values.delay-ticks", 2);

        new BukkitRunnable() {
            private int launched = 0;

            @Override
            public void run() {
                if (launched >= projectileCount) {
                    this.cancel();
                    return;
                }
                launchSingleProjectile(caster, context);
                launched++;
            }
        }.runTaskTimer(context.plugin(), 0L, delayTicks);

        return null;
    }

    private void launchSingleProjectile(Player caster, SpellContext context) {
        double damage = spellConfig.getDouble("values.damage", 1.0);
        int trailSteps = spellConfig.getInt("values.trail-steps", 6);
        int redDustCount = spellConfig.getInt("values.red-dust-count", 5);

        Vector spread = new Vector((Math.random() - 0.5) * 0.3, (Math.random() - 0.5) * 0.3,
                (Math.random() - 0.5) * 0.3);
        Vector direction = caster.getEyeLocation().getDirection().add(spread).normalize();

        caster.launchProjectile(Snowball.class, direction.multiply(1.3), projectile -> {
            projectile.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, key());
            projectile.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING,
                    caster.getUniqueId().toString());
            projectile.getPersistentDataContainer().set(Keys.DAMAGE, PersistentDataType.DOUBLE, damage);
            new BloodTrail(projectile, trailSteps, redDustCount, context).runTaskTimer(context.plugin(), 0L, 1L);
        });
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event) {
        Double damage = projectile.getPersistentDataContainer().get(Keys.DAMAGE, PersistentDataType.DOUBLE);
        if (damage == null || !(event.getHitEntity() instanceof LivingEntity living)) {
            return;
        }

        living.damage(damage, context.caster());
        context.fx().spawnParticles(living.getLocation(), Particle.DUST, 8, 0.2, 0.2, 0.2, 0,
                new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.0f));
    }

    private class BloodTrail extends BukkitRunnable {
        private final Projectile projectile;
        private final int trailSteps;
        private final int particleCount;
        private final java.util.Deque<org.bukkit.Location> history = new java.util.ArrayDeque<>();
        private final SpellContext context;

        public BloodTrail(Projectile projectile, int trailSteps, int particleCount, SpellContext context) {
            this.projectile = projectile;
            this.trailSteps = trailSteps;
            this.particleCount = particleCount;
            this.context = context;
        }

        @Override
        public void run() {
            if (!projectile.isValid() || projectile.isDead()) {
                cancel();
                return;
            }
            history.addFirst(projectile.getLocation().clone());
            if (history.size() > trailSteps)
                history.removeLast();

            for (org.bukkit.Location loc : history) {
                context.fx().spawnParticles(loc, Particle.DUST, particleCount, 0.02, 0.02, 0.02, 0,
                        new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.0f));
            }
        }
    }
}
