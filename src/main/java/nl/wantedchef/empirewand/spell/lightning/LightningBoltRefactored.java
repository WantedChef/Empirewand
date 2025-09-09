package nl.wantedchef.empirewand.spell.lightning;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.service.ConfigService;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Calls down a powerful lightning bolt on your target.
 * Refactored for better performance and code quality.
 */
public class LightningBoltRefactored extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Lightning Bolt";
            this.description = "Calls down a powerful lightning bolt on your target.";
            this.cooldown = java.time.Duration.ofSeconds(10);
            this.spellType = SpellType.LIGHTNING;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new LightningBoltRefactored(this);
        }
    }

    private LightningBoltRefactored(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "lightning-bolt";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double range = spellConfig.getDouble("values.range", 20.0);
        double damage = spellConfig.getDouble("values.damage", 24.0);
        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig()
                .getBoolean("features.friendly-fire", false);

        // Validate target
        LivingEntity target = getValidTarget(player, range);
        if (target == null) {
            context.fx().fizzle(player);
            return null;
        }

        Location targetLoc = target.getLocation();
        
        // Validate location (can lightning strike here?)
        if (!canLightningStrike(targetLoc)) {
            context.fx().fizzle(player);
            return null;
        }

        // Strike lightning and damage entities
        target.getWorld().strikeLightning(targetLoc);
        damageNearbyEntities(context, targetLoc, damage, friendlyFire);

        context.fx().playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    /**
     * Gets a valid target for the lightning bolt.
     *
     * @param player The player casting the spell.
     * @param range  The range to search for targets.
     * @return A valid living entity target, or null if none found.
     */
    private LivingEntity getValidTarget(Player player, double range) {
        var targetEntity = player.getTargetEntity((int) range);
        if (!(targetEntity instanceof LivingEntity)) {
            return null;
        }
        
        LivingEntity target = (LivingEntity) targetEntity;
        return target.isDead() || !target.isValid() ? null : target;
    }

    /**
     * Checks if lightning can strike at the given location.
     *
     * @param location The location to check.
     * @return true if lightning can strike there, false otherwise.
     */
    private boolean canLightningStrike(Location location) {
        return location.getWorld().getHighestBlockYAt(location) >= location.getY();
    }

    /**
     * Damages entities near the lightning strike.
     *
     * @param context      The spell context.
     * @param strikeLoc    The location of the lightning strike.
     * @param damage       The amount of damage to deal.
     * @param friendlyFire Whether friendly fire is enabled.
     */
    private void damageNearbyEntities(SpellContext context, Location strikeLoc, double damage, boolean friendlyFire) {
        strikeLoc.getWorld().getNearbyLivingEntities(strikeLoc, 2.0).stream()
                .filter(entity -> !entity.equals(context.caster()) || friendlyFire)
                .forEach(entity -> {
                    entity.damage(damage, context.caster());
                    context.fx().spawnParticles(entity.getLocation(), Particle.ELECTRIC_SPARK, 15, 0.3, 0.3, 0.3, 0.1);
                });
    }
}