package nl.wantedchef.empirewand.spell.fire;

import nl.wantedchef.empirewand.api.EmpireWandAPI;

import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.ProjectileSpell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * A spell that launches an explosive skull projectile.
 */
public class Explosive extends ProjectileSpell<WitherSkull> {

    /**
     * The builder for the Explosive spell.
     */
    public static class Builder extends ProjectileSpell.Builder<WitherSkull> {
        /**
         * Creates a new builder for the Explosive spell.
         *
         * @param api The EmpireWandAPI instance.
         */
        public Builder(EmpireWandAPI api) {
            super(api, WitherSkull.class);
            this.name = "Explosive";
            this.description = "Launches an explosive skull.";
            this.cooldown = java.time.Duration.ofSeconds(8);
            this.spellType = SpellType.FIRE;
            this.trailParticle = Particle.SMOKE;
        }

        @Override
        @NotNull
        public ProjectileSpell<WitherSkull> build() {
            return new Explosive(this);
        }
    }

    private static final double DEFAULT_RADIUS = 4.0;
    private static final boolean DEFAULT_SETS_FIRE = false;
    private static final float LAUNCH_SOUND_VOLUME = 1.0f;
    private static final float LAUNCH_SOUND_PITCH = 1.0f;

    private Explosive(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "explosive";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected void launchProjectile(@NotNull SpellContext context) {
        Player caster = context.caster();
        float yield = (float) spellConfig.getDouble("values.radius", DEFAULT_RADIUS);
        boolean setsFire = spellConfig.getBoolean("flags.sets-fire", DEFAULT_SETS_FIRE);

        context.fx().playSound(caster, Sound.ENTITY_WITHER_SHOOT, LAUNCH_SOUND_VOLUME, LAUNCH_SOUND_PITCH);
        caster.launchProjectile(WitherSkull.class, caster.getEyeLocation().getDirection(), skull -> {
            skull.setYield(yield);
            skull.setIsIncendiary(setsFire);
            skull.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, key());
            skull.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING,
                    caster.getUniqueId().toString());
        });
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event) {
        context.fx().impact(projectile.getLocation());
    }
}
