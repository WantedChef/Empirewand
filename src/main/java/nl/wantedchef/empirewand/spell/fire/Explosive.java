package nl.wantedchef.empirewand.spell.fire;

import nl.wantedchef.empirewand.api.EmpireWandAPI;

import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.ProjectileSpell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

/**
 * A fast-casting offensive spell that launches an explosive wither skull projectile.
 * <p>
 * The projectile inherits explosive properties (yield and incendiary behavior) from the
 * spell configuration and embeds metadata to associate the projectile with its caster and
 * originating spell. Visual and audio feedback is provided on launch and impact.
 * <p>
 * Features:
 * <ul>
 *   <li>Configurable explosion radius (yield) and fire ignition behavior.</li>
 *   <li>Projectile metadata linking for hit detection and ownership.</li>
 *   <li>Trail particles and launch/impact effects via the shared FX system.</li>
 * </ul>
 *
 * Usage example:
 * <pre>{@code
 * ProjectileSpell<WitherSkull> explosive = new Explosive.Builder(api)
 *     .name("Explosive")
 *     .description("Launches an explosive skull.")
 *     .cooldown(java.time.Duration.ofSeconds(8))
 *     .build();
 * }</pre>
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
    @NotNull
    public String key() {
        return "explosive";
    }

    @Override
    @NotNull
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    /**
     * Launches the wither skull projectile with configured explosive properties.
     *
     * @param context active spell context; must not be null
     */
    protected void launchProjectile(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");

        Player caster = context.caster();
        float yield = (float) spellConfig.getDouble("values.radius", DEFAULT_RADIUS);
        boolean setsFire = spellConfig.getBoolean("flags.sets-fire", DEFAULT_SETS_FIRE);

        context.fx().playSound(caster, Sound.ENTITY_WITHER_SHOOT, LAUNCH_SOUND_VOLUME, LAUNCH_SOUND_PITCH);

        Location eye = caster.getEyeLocation();
        if (eye == null) {
            context.fx().fizzle(caster);
            return;
        }

        caster.launchProjectile(WitherSkull.class, eye.getDirection(), skull -> {
            skull.setYield(yield);
            skull.setIsIncendiary(setsFire);
            skull.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, key());
            skull.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING,
                    caster.getUniqueId().toString());
        });
    }

    @Override
    /**
     * Handles projectile impact effects by triggering a shared impact visual.
     */
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event) {
        context.fx().impact(projectile.getLocation());
    }
}
