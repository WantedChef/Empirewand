package nl.wantedchef.empirewand.spell;

import org.bukkit.Location;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.service.ConfigService;
import nl.wantedchef.empirewand.framework.service.FxService;

import java.util.Objects;

/**
 * Immutable context object passed to spells during casting.
 * <p>
 * This record provides spells with access to core services and contextual information
 * needed for execution, including:
 * <ul>
 *   <li>Plugin instance for accessing plugin services</li>
 *   <li>Player caster who initiated the spell</li>
 *   <li>Configuration service for accessing settings</li>
 *   <li>Effects service for visual/audio effects</li>
 *   <li>Optional target entity or location</li>
 *   <li>Spell key for identification</li>
 * </ul>
 *
 * <p>
 * <strong>Immutability:</strong> This record is immutable and thread-safe.
 * All Location objects are defensively cloned to prevent external modification.
 *
 * <p>
 * <strong>Factory Methods:</strong> Use the provided factory methods for
 * convenient construction:
 * <ul>
 *   <li>{@link #targeted(EmpireWandPlugin, Player, ConfigService, FxService, LivingEntity, String)} - for entity-targeted spells</li>
 *   <li>{@link #location(EmpireWandPlugin, Player, ConfigService, FxService, Location, String)} - for location-based spells</li>
 * </ul>
 *
 * @param plugin the EmpireWandPlugin instance
 * @param caster the player casting the spell
 * @param config the configuration service
 * @param fx the effects service for visual/audio effects
 * @param target the targeted living entity, may be null
 * @param targetLocation the targeted location, may be null
 * @param spellKey the unique key identifying this spell instance
 * @since 1.0.0
 */
@SuppressFBWarnings(value = { "EI_EXPOSE_REP", "EI_EXPOSE_REP2" }, 
    justification = "Location is defensively cloned on construction and accessor; record components kept for concise immutable API")
public record SpellContext(
        EmpireWandPlugin plugin,
        Player caster,
        ConfigService config,
        FxService fx,
        LivingEntity target,
        Location targetLocation,
        String spellKey) {

    /**
     * Canonical constructor with defensive cloning for targetLocation.
     * Ensures immutability by creating defensive copies of mutable Bukkit objects.
     *
     * @param plugin the EmpireWandPlugin instance
     * @param caster the player casting the spell
     * @param config the configuration service
     * @param fx the effects service
     * @param target the targeted living entity
     * @param targetLocation the targeted location
     * @param spellKey the unique spell key
     * @throws NullPointerException if plugin, caster, config, or fx is null
     */
    public SpellContext {
        Objects.requireNonNull(plugin, "Plugin cannot be null");
        Objects.requireNonNull(caster, "Caster cannot be null");
        Objects.requireNonNull(config, "Config service cannot be null");
        Objects.requireNonNull(fx, "FX service cannot be null");
        
        if (targetLocation != null) {
            targetLocation = targetLocation.clone();
        }
    }

    /**
     * Convenience constructor for spells without specific targets.
     * <p>
     * Creates a SpellContext with null target, targetLocation, and spellKey.
     *
     * @param plugin the EmpireWandPlugin instance
     * @param caster the player casting the spell
     * @param config the configuration service
     * @param fx the effects service
     * @throws NullPointerException if any parameter is null
     */
    public SpellContext(EmpireWandPlugin plugin, Player caster, ConfigService config, FxService fx) {
        this(plugin, caster, config, fx, null, null, null);
    }

    /**
     * Factory method for creating a targeted spell context.
     * <p>
     * Creates a context for spells that target a specific living entity.
     * The target's location is automatically captured and defensively cloned.
     *
     * @param plugin the EmpireWandPlugin instance
     * @param caster the player casting the spell
     * @param config the configuration service
     * @param fx the effects service
     * @param target the entity being targeted, may be null
     * @param spellKey the unique key for this spell instance
     * @return a new SpellContext instance
     * @throws NullPointerException if plugin, caster, config, or fx is null
     */
    public static SpellContext targeted(EmpireWandPlugin plugin, Player caster, ConfigService config, FxService fx,
            LivingEntity target, String spellKey) {
        Location loc = target != null ? target.getLocation().clone() : null;
        return new SpellContext(plugin, caster, config, fx, target, loc, spellKey);
    }

    /**
     * Factory method for creating a location-based spell context.
     * <p>
     * Creates a context for spells that target a specific location in the world.
     * The location is defensively cloned to prevent external modification.
     *
     * @param plugin the EmpireWandPlugin instance
     * @param caster the player casting the spell
     * @param config the configuration service
     * @param fx the effects service
     * @param location the target location, may be null
     * @param spellKey the unique key for this spell instance
     * @return a new SpellContext instance
     * @throws NullPointerException if plugin, caster, config, or fx is null
     */
    public static SpellContext location(EmpireWandPlugin plugin, Player caster, ConfigService config, FxService fx,
            Location location, String spellKey) {
        return new SpellContext(plugin, caster, config, fx, null, 
            location != null ? location.clone() : null, spellKey);
    }

    /**
     * Returns the target location with defensive cloning.
     * <p>
     * This method ensures that the returned Location is a defensive copy,
     * preventing external modification of the internally stored location.
     *
     * @return a defensive clone of the target location, or null if no location was provided
     */
    @Override
    @SuppressFBWarnings(value = { "EI_EXPOSE_REP" }, 
        justification = "Location is defensively cloned before returning to ensure immutability")
    public Location targetLocation() {
        return targetLocation != null ? targetLocation.clone() : null;
    }

    /**
     * Checks if this context has a valid target entity.
     *
     * @return true if a target entity is present, false otherwise
     */
    public boolean hasTarget() {
        return target != null && target.isValid();
    }

    /**
     * Checks if this context has a valid target location.
     *
     * @return true if a target location is present, false otherwise
     */
    public boolean hasTargetLocation() {
        return targetLocation != null && targetLocation.getWorld() != null;
    }

    /**
     * Gets the distance between the caster and the target location.
     * <p>
     * Returns 0.0 if no target location is available.
     *
     * @return the distance in blocks, or 0.0 if no target location
     */
    public double getTargetDistance() {
        if (!hasTargetLocation()) {
            return 0.0;
        }
        return caster.getLocation().distance(targetLocation);
    }
}
