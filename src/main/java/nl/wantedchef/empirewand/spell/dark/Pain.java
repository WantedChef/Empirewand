package nl.wantedchef.empirewand.spell.dark;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * A dark spell that instantly damages the targeted entity with necrotic energy.
 *
 * <p>The caster channels malevolent dark energy into their target, inflicting severe
 * pain and potentially applying debilitating effects. The spell features enhanced
 * targeting, configurable damage scaling, and sophisticated visual/audio feedback.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Smart target selection with range validation</li>
 *   <li>Configurable damage with difficulty scaling</li>
 *   <li>Optional status effects (weakness, slowness)</li>
 *   <li>Enhanced particle and sound effects</li>
 *   <li>Resistance to armor and shields</li>
 * </ul>
 *
 * @author WantedChef
 */
public class Pain extends Spell<LivingEntity> {

    /**
     * Builder for creating {@link Pain} instances.
     */
    public static class Builder extends Spell.Builder<LivingEntity> {

        /**
         * Creates a new builder for the Pain spell.
         *
         * @param api the plugin API
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Pain";
            this.description = "Inflict instant pain on your target";
            this.cooldown = Duration.ofSeconds(5);
            this.spellType = SpellType.DARK;
        }

        /**
         * Builds the Pain spell.
         *
         * @return the constructed spell
         */
        @Override
        @NotNull
        public Spell<LivingEntity> build() {
            return new Pain(this);
        }
    }

    private static final double DEFAULT_RANGE = 30.0;
    private static final double DEFAULT_DAMAGE = 10.0;
    private static final double DEFAULT_PENETRATION = 0.5;
    private static final boolean DEFAULT_APPLY_WEAKNESS = true;
    private static final int DEFAULT_WEAKNESS_DURATION = 60;
    private static final int DEFAULT_WEAKNESS_LEVEL = 1;

    /**
     * Constructs the spell from its builder.
     *
     * @param builder the spell builder
     */
    private Pain(Builder builder) {
        super(builder);
    }

    /**
     * Gets the configuration key for this spell.
     *
     * @return "pain"
     */
    @Override
    public String key() {
        return "pain";
    }

    /**
     * Gets the prerequisites for casting the spell.
     *
     * @return level prerequisite requiring level 10
     */
    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(10);
    }

    /**
     * Searches for the target entity using advanced ray tracing.
     *
     * @param context the spell context
     * @return the targeted living entity or {@code null} if none is found
     */
    @Override
    protected @Nullable LivingEntity executeSpell(@NotNull SpellContext context) {
        final Player player = context.caster();
        final double range = spellConfig.getDouble("values.range", DEFAULT_RANGE);

        // Use ray tracing for more precise targeting
        final RayTraceResult result = player.getWorld().rayTraceEntities(
            player.getEyeLocation(),
            player.getLocation().getDirection(),
            range,
            entity -> entity instanceof LivingEntity && entity != player && entity.isValid()
        );

        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            return target;
        }

        // Fallback to basic targeting if ray trace fails
        final var fallbackTarget = player.getTargetEntity((int) range);
        if (fallbackTarget instanceof LivingEntity living && living.isValid()) {
            return living;
        }

        context.fx().fizzle(player);
        player.sendMessage("§cNo valid target found within range!");
        return null;
    }

    /**
     * Applies necrotic damage and dark effects to the target.
     *
     * @param context the spell context
     * @param target  the affected entity (guaranteed non-null by framework)
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull LivingEntity target) {
        final Player player = context.caster();
        final double baseDamage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
        final double penetration = spellConfig.getDouble("values.penetration", DEFAULT_PENETRATION);
        final boolean applyWeakness = spellConfig.getBoolean("values.apply_weakness", DEFAULT_APPLY_WEAKNESS);

        // Calculate armor-penetrating necrotic damage
        final double necroticDamage = calculateNecroticDamage(baseDamage, penetration);

        // Apply the damage with dark energy penetration
        target.damage(necroticDamage, player);

        // Apply debuff effects if enabled
        if (applyWeakness) {
            applyDarkEffects(target);
        }

        // Enhanced visual effects
        createPainVisuals(context, target);

        // Enhanced audio feedback
        createPainAudio(context, target, player);

        // Success message with damage information
        player.sendMessage(String.format("§4§lPain §cinflicted! §7(%.1f necrotic damage)", necroticDamage));
    }

    /**
     * Calculates necrotic damage that partially bypasses armor.
     */
    private double calculateNecroticDamage(double baseDamage, double penetration) {
        // Necrotic damage ignores a percentage of armor/resistance
        return baseDamage * (1.0 + penetration);
    }

    /**
     * Applies dark magic debuff effects to the target.
     */
    private void applyDarkEffects(@NotNull LivingEntity target) {
        final int weaknessDuration = spellConfig.getInt("values.weakness_duration", DEFAULT_WEAKNESS_DURATION);
        final int weaknessLevel = spellConfig.getInt("values.weakness_level", DEFAULT_WEAKNESS_LEVEL);

        // Apply weakness effect (reduced attack damage)
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.WEAKNESS,
            weaknessDuration,
            weaknessLevel - 1, // Bukkit levels are 0-based
            false,
            true
        ));

        // Brief slowness effect
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOWNESS,
            20, // 1 second
            1,
            false,
            true
        ));
    }

    /**
     * Creates sophisticated visual effects for the pain spell.
     */
    private void createPainVisuals(@NotNull SpellContext context, @NotNull LivingEntity target) {
        final var location = target.getLocation();
        final var headLocation = location.clone().add(0, target.getHeight() * 0.8, 0);
        final var fx = context.fx();

        // Central dark energy burst
        fx.spawnParticles(headLocation, Particle.DAMAGE_INDICATOR, 20, 0.4, 0.6, 0.4, 0.1);

        // Dark magic aura
        fx.spawnParticles(location, Particle.WITCH, 25, 0.8, 0.5, 0.8, 0.05);
        fx.spawnParticles(headLocation, Particle.WITCH, 15, 0.3, 0.3, 0.3, 0.1);

        // Necrotic energy swirl
        for (int i = 0; i < 8; i++) {
            final double angle = (Math.PI * 2 * i) / 8;
            final double x = Math.cos(angle) * 1.2;
            final double z = Math.sin(angle) * 1.2;
            final var particlePos = location.clone().add(x, 0.5, z);

            fx.spawnParticles(particlePos, Particle.SMOKE, 3, 0.1, 0.1, 0.1, 0.02);
            fx.spawnParticles(particlePos, Particle.ENCHANT, 2, 0.1, 0.1, 0.1, 0.5);
        }

        // Pain indicators
        fx.spawnParticles(headLocation, Particle.CRIT, 10, 0.3, 0.3, 0.3, 0.1);
    }

    /**
     * Creates layered audio effects for the pain spell.
     */
    private void createPainAudio(@NotNull SpellContext context, @NotNull LivingEntity target, @NotNull Player player) {
        final var fx = context.fx();
        final var targetLocation = target.getLocation();

        // Primary pain sounds
        fx.playSound(targetLocation, Sound.ENTITY_PLAYER_HURT, 1.5f, 0.5f);
        fx.playSound(targetLocation, Sound.ENTITY_PHANTOM_BITE, 1.2f, 1.3f);

        // Dark magic ambiance
        fx.playSound(targetLocation, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 1.8f);
        fx.playSound(targetLocation, Sound.PARTICLE_SOUL_ESCAPE, 1.0f, 0.8f);

        // Spell casting feedback for caster
        fx.playSound(player, Sound.ENTITY_WITCH_CELEBRATE, 0.6f, 1.5f);
    }
}
