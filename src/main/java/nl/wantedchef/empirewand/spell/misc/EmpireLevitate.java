package nl.wantedchef.empirewand.spell.misc;

import nl.wantedchef.empirewand.api.EmpireWandAPI;

import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

public class EmpireLevitate extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Empire Levitate";
            this.description = "Levitates a target entity.";
            this.cooldown = java.time.Duration.ofSeconds(20);
            this.spellType = SpellType.MISC;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new EmpireLevitate(this);
        }
    }

    private EmpireLevitate(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "empire-levitate";
    }

    @Override
    public Component displayName() {
        return Component.text("Empire Levitate").color(TextColor.color(138, 43, 226));
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player caster = context.caster();

        int duration = spellConfig.getInt("values.duration-ticks", 200); // Extended from 60 to 200 ticks (10 seconds)
        int amplifier = spellConfig.getInt("values.amplifier", 0);
        double maxRange = spellConfig.getDouble("values.max-range", 15.0);
        double bossHealthThreshold = spellConfig.getDouble("values.boss-health-threshold", 100.0);
        boolean affectPlayers = spellConfig.getBoolean("values.affect-players", true); // Changed default to true
        boolean takeoverEnabled = spellConfig.getBoolean("values.takeover-enabled", true);

        LivingEntity target = caster; // Default to self-cast

        // Try to find a target if takeover is enabled
        if (takeoverEnabled) {
            RayTraceResult rayTrace = caster.rayTraceEntities((int) maxRange, false);
            if (rayTrace != null && rayTrace.getHitEntity() instanceof LivingEntity potentialTarget && !potentialTarget.isDead()) {
                if (!isBoss(potentialTarget, bossHealthThreshold) && (affectPlayers || !(potentialTarget instanceof Player))) {
                    target = potentialTarget;
                }
            }
        }

        // Apply levitation effect
        target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, duration, amplifier, false, true, true));

        // If takeover is enabled and we have a target that's not the caster, start takeover control
        if (takeoverEnabled && target != caster) {
            startTakeoverControl(context, caster, target, duration);
        }

        Location effectLocation = target.getLocation().add(0, 1, 0);
        context.fx().spawnParticles(effectLocation, Particle.CLOUD, 30, 0.5, 0.5, 0.5, 0.1);
        context.fx().spawnParticles(effectLocation, Particle.ENCHANT, 20, 0.3, 0.3, 0.3, 0.05);
        context.fx().playSound(effectLocation, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    private void startTakeoverControl(SpellContext context, Player caster, LivingEntity target, int duration) {
        // Create a task that allows the caster to control the target's movement
        new org.bukkit.scheduler.BukkitRunnable() {
            private int ticksRemaining = duration / 20; // Convert to seconds for control intervals
            private final Location lastCasterLocation = caster.getLocation().clone();

            @Override
            public void run() {
                if (ticksRemaining <= 0 || !target.isValid() || target.isDead()) {
                    this.cancel();
                    return;
                }

                // Move target towards caster's facing direction if caster has moved
                Location currentCasterLocation = caster.getLocation();
                if (currentCasterLocation.distance(lastCasterLocation) > 0.5) {
                    // Calculate movement vector based on caster's look direction
                    org.bukkit.util.Vector direction = currentCasterLocation.getDirection().normalize();
                    Location targetLocation = target.getLocation();

                    // Move target in the direction the caster is facing
                    Location newLocation = targetLocation.add(direction.multiply(0.8));
                    newLocation.setY(targetLocation.getY()); // Keep same height

                    // Only move if the new location is safe (not in blocks)
                    if (newLocation.getBlock().isEmpty() && newLocation.clone().add(0, 1, 0).getBlock().isEmpty()) {
                        target.teleport(newLocation);

                        // Visual effect for takeover movement
                        context.fx().spawnParticles(newLocation.add(0, 1, 0), Particle.ENCHANT, 5, 0.2, 0.2, 0.2, 0.02);
                    }

                    lastCasterLocation.setX(currentCasterLocation.getX());
                    lastCasterLocation.setY(currentCasterLocation.getY());
                    lastCasterLocation.setZ(currentCasterLocation.getZ());
                }

                ticksRemaining--;
            }
        }.runTaskTimer(context.plugin(), 0L, 20L); // Run every second
    }

    private boolean isBoss(LivingEntity entity, double healthThreshold) {
        AttributeInstance maxHealthAttr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return maxHealthAttr != null && maxHealthAttr.getBaseValue() > healthThreshold;
    }
}
