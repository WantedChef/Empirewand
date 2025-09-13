package nl.wantedchef.empirewand.spell.lightning;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * A lightning spell that strikes the target block with ray tracing.
 * <p>
 * This spell uses modern ray tracing to find the target block and calls down
 * lightning at that location, dealing damage to nearby entities.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Modern ray tracing for accurate target selection</li>
 *   <li>Direct lightning strike at target location</li>
 *   <li>Area damage around the strike point</li>
 *   <li>Visual and audio feedback</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class ElementalLightningAttack extends Spell<Void> {

    /**
     * Configuration record for the ElementalLightningAttack spell.
     */
    private record Config(
        double maxRange,
        double damage,
        double damageRadius,
        boolean friendlyFire
    ) {}

    private static final double DEFAULT_MAX_RANGE = 30.0;
    private static final double DEFAULT_DAMAGE = 12.0;
    private static final double DEFAULT_DAMAGE_RADIUS = 3.0;
    private static final boolean DEFAULT_FRIENDLY_FIRE = false;

    private Config config = new Config(
        DEFAULT_MAX_RANGE,
        DEFAULT_DAMAGE,
        DEFAULT_DAMAGE_RADIUS,
        DEFAULT_FRIENDLY_FIRE
    );

    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new ElementalLightningAttack spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Elemental Lightning Attack";
            this.description = "Strikes lightning at the target location using ray tracing.";
            this.cooldown = Duration.ofSeconds(10);
            this.spellType = SpellType.LIGHTNING;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new ElementalLightningAttack(this);
        }
    }

    private ElementalLightningAttack(Builder builder) {
        super(builder);
    }

    @Override
    public void loadConfig(@NotNull ReadableConfig spellConfig) {
        Objects.requireNonNull(spellConfig, "SpellConfig cannot be null");
        super.loadConfig(spellConfig);
        
        this.config = new Config(
            spellConfig.getDouble("values.max-range", DEFAULT_MAX_RANGE),
            spellConfig.getDouble("values.damage", DEFAULT_DAMAGE),
            spellConfig.getDouble("values.damage-radius", DEFAULT_DAMAGE_RADIUS),
            spellConfig.getBoolean("values.friendly-fire", DEFAULT_FRIENDLY_FIRE)
        );
    }

    @Override
    @NotNull
    public String key() {
        return "elemental-lightning-attack";
    }

    @Override
    @NotNull
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    @Nullable
    protected Void executeSpell(SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();
        
        RayTraceResult result = player.rayTraceBlocks(config.maxRange, FluidCollisionMode.NEVER);
        if (result == null || result.getHitBlock() == null) {
            player.sendMessage("No target within range.");
            context.fx().fizzle(player);
            return null;
        }
        
        Location targetLocation = result.getHitBlock().getLocation().add(0.5, 1, 0.5);
        var world = targetLocation.getWorld();
        if (world == null) {
            return null;
        }
        
        world.strikeLightning(targetLocation);
        
        damageNearbyEntities(context, targetLocation);
        
        context.fx().playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect
    }

    /**
     * Damages entities near the lightning strike.
     *
     * @param context the spell context
     * @param strikeLoc the location of the lightning strike
     */
    private void damageNearbyEntities(SpellContext context, Location strikeLoc) {
        var world = strikeLoc.getWorld();
        if (world == null) return;
        
        for (var entity : world.getNearbyLivingEntities(strikeLoc, config.damageRadius)) {
            if (entity.equals(context.caster()) && !config.friendlyFire) {
                continue;
            }
            if (entity.isDead() || !entity.isValid()) {
                continue;
            }
            
            entity.damage(config.damage, context.caster());
            context.fx().spawnParticles(entity.getLocation(), Particle.ELECTRIC_SPARK, 15, 0.3, 0.3, 0.3, 0.1);
        }
    }
}