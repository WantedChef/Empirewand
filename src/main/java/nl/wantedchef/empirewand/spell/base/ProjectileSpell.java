package nl.wantedchef.empirewand.spell.base;

import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;

/**
 * Abstract base class for projectile-based spells.
 *
 * @param <P> The type of projectile this spell launches.
 */
public abstract class ProjectileSpell<P extends Projectile> extends Spell<Void> {

    protected final Class<P> projectileClass;

    protected ProjectileSpell(Builder<P> builder) {
        super(builder);
        this.projectileClass = (Class<P>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    /**
     * Gets the class of the projectile this spell uses.
     */
    @NotNull
    public Class<P> getProjectileClass() {
        return projectileClass;
    }

    /**
     * Launches the projectile for this spell.
     *
     * @param context The spell context.
     */
    protected abstract void launchProjectile(@NotNull SpellContext context);

    @Override
    protected Void executeSpell(@NotNull SpellContext context) {
        launchProjectile(context);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Projectile effects are handled in handleHit
    }

    /**
     * Handles the projectile hit event.
     *
     * @param context The spell context.
     * @param projectile The projectile that hit.
     * @param event The projectile hit event.
     */
    protected abstract void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile, @NotNull ProjectileHitEvent event);

    /**
     * Builder pattern for creating ProjectileSpell instances.
     */
    public static abstract class Builder<P extends Projectile> extends Spell.Builder<Void> {

        public Builder(EmpireWandAPI api, Class<P> projectileClass) {
            super(api);
            // Projectile spells typically have shorter cooldowns
            this.cooldown = java.time.Duration.ofSeconds(3);
            this.spellType = SpellType.PROJECTILE;
        }

        @NotNull
        @Override
        public abstract ProjectileSpell<P> build();
    }
}
