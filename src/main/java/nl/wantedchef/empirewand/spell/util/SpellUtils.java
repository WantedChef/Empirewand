package nl.wantedchef.empirewand.spell.util;

import nl.wantedchef.empirewand.core.config.ReadableConfig;
import nl.wantedchef.empirewand.spell.SpellContext;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for common spell operations to reduce code duplication.
 * 
 * <p>This class provides static utility methods for common patterns found across
 * spell implementations, including configuration loading, effect creation, and
 * mathematical calculations.</p>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * <pre>{@code
 * // Load configuration with defaults
 * double damage = SpellUtils.getConfigDouble(spellConfig, "values.damage", 5.0);
 * int count = SpellUtils.getConfigInt(spellConfig, "values.count", 3);
 * 
 * // Create visual effects
 * SpellUtils.playSoundAtLocation(context, location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
 * SpellUtils.spawnParticles(context, location, Particle.EXPLOSION, 10, 0.5, 0.5, 0.5, 0.1);
 * 
 * // Mathematical utilities
 * Location randomLocation = SpellUtils.getRandomLocationInRadius(center, radius);
 * double distance = SpellUtils.calculateDistance(location1, location2);
 * }</pre>
 * 
 * @since 1.1.1
 * @author EmpireWand Team
 */
public final class SpellUtils {
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private SpellUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    // ==================== Configuration Utilities ====================
    
    /**
     * Gets a double value from spell configuration with a default fallback.
     * 
     * @param config the spell configuration
     * @param path the configuration path
     * @param defaultValue the default value if not found
     * @return the configuration value or default
     */
    public static double getConfigDouble(@NotNull ReadableConfig config, @NotNull String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }
    
    /**
     * Gets an integer value from spell configuration with a default fallback.
     * 
     * @param config the spell configuration
     * @param path the configuration path
     * @param defaultValue the default value if not found
     * @return the configuration value or default
     */
    public static int getConfigInt(@NotNull ReadableConfig config, @NotNull String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }
    
    /**
     * Gets a boolean value from spell configuration with a default fallback.
     * 
     * @param config the spell configuration
     * @param path the configuration path
     * @param defaultValue the default value if not found
     * @return the configuration value or default
     */
    public static boolean getConfigBoolean(@NotNull ReadableConfig config, @NotNull String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }
    
    /**
     * Gets a string value from spell configuration with a default fallback.
     * 
     * @param config the spell configuration
     * @param path the configuration path
     * @param defaultValue the default value if not found
     * @return the configuration value or default
     */
    @NotNull
    public static String getConfigString(@NotNull ReadableConfig config, @NotNull String path, @NotNull String defaultValue) {
        return config.getString(path, defaultValue);
    }
    
    // ==================== Effect Utilities ====================
    
    /**
     * Plays a sound effect at a specific location using the spell context's FX service.
     * 
     * @param context the spell context
     * @param location the location to play the sound
     * @param sound the sound to play
     * @param volume the volume (0.0 to 1.0)
     * @param pitch the pitch (0.5 to 2.0)
     */
    public static void playSoundAtLocation(@NotNull SpellContext context, @NotNull Location location, 
                                         @NotNull Sound sound, float volume, float pitch) {
        context.fx().playSound(location, sound, volume, pitch);
    }
    
    /**
     * Plays a sound effect at the caster's location using the spell context's FX service.
     * 
     * @param context the spell context
     * @param sound the sound to play
     * @param volume the volume (0.0 to 1.0)
     * @param pitch the pitch (0.5 to 2.0)
     */
    public static void playSoundAtCaster(@NotNull SpellContext context, @NotNull Sound sound, float volume, float pitch) {
        context.fx().playSound(context.caster().getLocation(), sound, volume, pitch);
    }
    
    /**
     * Spawns particles at a specific location using the spell context's FX service.
     * 
     * @param context the spell context
     * @param location the location to spawn particles
     * @param particle the particle type
     * @param count the number of particles
     * @param offsetX the X offset
     * @param offsetY the Y offset
     * @param offsetZ the Z offset
     * @param speed the particle speed
     */
    public static void spawnParticles(@NotNull SpellContext context, @NotNull Location location, 
                                    @NotNull Particle particle, int count, double offsetX, double offsetY, 
                                    double offsetZ, double speed) {
        context.fx().spawnParticles(location, particle, count, offsetX, offsetY, offsetZ, speed);
    }
    
    /**
     * Spawns particles at a specific location with uniform offset using the spell context's FX service.
     * 
     * @param context the spell context
     * @param location the location to spawn particles
     * @param particle the particle type
     * @param count the number of particles
     * @param offset the uniform offset for all axes
     * @param speed the particle speed
     */
    public static void spawnParticles(@NotNull SpellContext context, @NotNull Location location, 
                                    @NotNull Particle particle, int count, double offset, double speed) {
        context.fx().spawnParticles(location, particle, count, offset, offset, offset, speed);
    }
    
    // ==================== Mathematical Utilities ====================
    
    /**
     * Calculates the distance between two locations.
     * 
     * @param loc1 the first location
     * @param loc2 the second location
     * @return the distance between the locations
     */
    public static double calculateDistance(@NotNull Location loc1, @NotNull Location loc2) {
        return loc1.distance(loc2);
    }
    
    /**
     * Calculates the distance between two locations in 2D (ignoring Y coordinate).
     * 
     * @param loc1 the first location
     * @param loc2 the second location
     * @return the 2D distance between the locations
     */
    public static double calculateDistance2D(@NotNull Location loc1, @NotNull Location loc2) {
        double dx = loc1.getX() - loc2.getX();
        double dz = loc1.getZ() - loc2.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
    
    /**
     * Gets a random location within a specified radius of a center point.
     * Uses proper distribution to ensure uniform coverage of the circular area.
     * 
     * @param center the center location
     * @param radius the radius
     * @return a random location within the radius
     */
    @NotNull
    public static Location getRandomLocationInRadius(@NotNull Location center, double radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // Use proper distribution for uniform coverage
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = Math.sqrt(random.nextDouble()) * radius;
        
        double x = center.getX() + distance * Math.cos(angle);
        double z = center.getZ() + distance * Math.sin(angle);
        
        return new Location(center.getWorld(), x, center.getY(), z);
    }
    
    /**
     * Gets a random location within a specified radius of a center point at a specific Y level.
     * 
     * @param center the center location
     * @param radius the radius
     * @param yOffset the Y offset from the center
     * @return a random location within the radius at the specified Y level
     */
    @NotNull
    public static Location getRandomLocationInRadius(@NotNull Location center, double radius, double yOffset) {
        Location randomLoc = getRandomLocationInRadius(center, radius);
        randomLoc.setY(center.getY() + yOffset);
        return randomLoc;
    }
    
    /**
     * Clamps a value between a minimum and maximum.
     * 
     * @param value the value to clamp
     * @param min the minimum value
     * @param max the maximum value
     * @return the clamped value
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Clamps an integer value between a minimum and maximum.
     * 
     * @param value the value to clamp
     * @param min the minimum value
     * @param max the maximum value
     * @return the clamped value
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Linear interpolation between two values.
     * 
     * @param start the start value
     * @param end the end value
     * @param factor the interpolation factor (0.0 to 1.0)
     * @return the interpolated value
     */
    public static double lerp(double start, double end, double factor) {
        return start + (end - start) * factor;
    }
    
    // ==================== Validation Utilities ====================
    
    /**
     * Validates that a location is within a certain distance of the caster.
     * 
     * @param caster the caster
     * @param location the location to validate
     * @param maxDistance the maximum allowed distance
     * @return true if the location is within range
     */
    public static boolean isWithinRange(@NotNull Player caster, @NotNull Location location, double maxDistance) {
        return calculateDistance(caster.getLocation(), location) <= maxDistance;
    }
    
    /**
     * Validates that a location is within a certain 2D distance of the caster.
     * 
     * @param caster the caster
     * @param location the location to validate
     * @param maxDistance the maximum allowed distance
     * @return true if the location is within range
     */
    public static boolean isWithinRange2D(@NotNull Player caster, @NotNull Location location, double maxDistance) {
        return calculateDistance2D(caster.getLocation(), location) <= maxDistance;
    }
    
    /**
     * Ensures a location is within a certain distance of the caster, clamping if necessary.
     * 
     * @param caster the caster
     * @param location the location to validate
     * @param maxDistance the maximum allowed distance
     * @return the validated (possibly clamped) location
     */
    @NotNull
    public static Location ensureWithinRange(@NotNull Player caster, @NotNull Location location, double maxDistance) {
        double distance = calculateDistance(caster.getLocation(), location);
        if (distance <= maxDistance) {
            return location;
        }
        
        // Clamp the location to the maximum distance
        Location casterLoc = caster.getLocation();
        double factor = maxDistance / distance;
        
        double newX = casterLoc.getX() + (location.getX() - casterLoc.getX()) * factor;
        double newY = casterLoc.getY() + (location.getY() - casterLoc.getY()) * factor;
        double newZ = casterLoc.getZ() + (location.getZ() - casterLoc.getZ()) * factor;
        
        return new Location(location.getWorld(), newX, newY, newZ);
    }
    
    // ==================== String Utilities ====================
    
    /**
     * Converts a spell key to a display name by replacing hyphens with spaces and capitalizing.
     * 
     * @param spellKey the spell key (e.g., "fire-ball")
     * @return the display name (e.g., "Fire Ball")
     */
    @NotNull
    public static String keyToDisplayName(@NotNull String spellKey) {
        if (spellKey.isEmpty()) {
            return spellKey;
        }
        
        String[] words = spellKey.split("-");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            
            String word = words[i];
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * Converts a display name to a spell key by replacing spaces with hyphens and lowercasing.
     * 
     * @param displayName the display name (e.g., "Fire Ball")
     * @return the spell key (e.g., "fire-ball")
     */
    @NotNull
    public static String displayNameToKey(@NotNull String displayName) {
        return displayName.toLowerCase().replaceAll("\\s+", "-");
    }
}
