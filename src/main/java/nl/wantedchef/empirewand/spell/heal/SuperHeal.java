package nl.wantedchef.empirewand.spell.heal;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * SuperHeal - Powerful healing spell that fully heals the caster and nearby allies.
 * Features proper resource management and spectacular healing effects.
 */
public class SuperHeal extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "SuperHeal";
            this.description = "Fully heal yourself and nearby allies";
            this.cooldown = Duration.ofSeconds(45);
            this.spellType = SpellType.HEAL;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new SuperHeal(this);
        }
    }

    // Configuration defaults
    private static final double DEFAULT_RADIUS = 10.0;
    private static final boolean DEFAULT_HEAL_FULL = true;

    // Effect constants
    private static final int EFFECT_DURATION_TICKS = 60;
    private static final int EFFECT_INTERVAL_TICKS = 3;
    private static final int REGENERATION_DURATION = 100;
    private static final int ABSORPTION_DURATION = 600;

    private SuperHeal(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "superheal";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(25);
    }

    @Override
    protected Void executeSpell(@NotNull SpellContext context) {
        Player player = context.caster();
        double radius = spellConfig.getDouble("values.radius", DEFAULT_RADIUS);
        boolean healFull = spellConfig.getBoolean("flags.heal_full", DEFAULT_HEAL_FULL);
        boolean divineIntervention = spellConfig.getBoolean("flags.divine_intervention", true);

        Location center = player.getLocation();

        // Immediate healing and effects
        healNearbyEntities(context, center, radius, healFull);

        // Create divine intervention effects if enabled
        if (divineIntervention) {
            createDivineEffects(context, center, radius);
        }

        // Sound effects
        context.fx().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 1.0f);
        context.fx().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.2f);
        context.fx().playSound(center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2.0f, 0.8f);

        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in executeSpell
    }

    /**
     * Heals all nearby entities.
     */
    private void healNearbyEntities(SpellContext context, Location center, double radius, boolean healFull) {
        for (var entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity living && !living.isDead()) {
                // Get max health
                var maxAttr = living.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                double maxHealth = maxAttr != null ? maxAttr.getValue() : 20.0;

                if (healFull) {
                    living.setHealth(maxHealth);
                } else {
                    double healAmount = maxHealth * 0.8; // 80% heal
                    living.setHealth(Math.min(maxHealth, living.getHealth() + healAmount));
                }

                // Add beneficial effects
                living.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, REGENERATION_DURATION, 1));
                living.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, ABSORPTION_DURATION, 1));

                // Remove negative effects
                living.removePotionEffect(PotionEffectType.POISON);
                living.removePotionEffect(PotionEffectType.WITHER);
                living.removePotionEffect(PotionEffectType.SLOWNESS);
                living.removePotionEffect(PotionEffectType.WEAKNESS);

                // Healing particles
                living.getWorld().spawnParticle(Particle.HEART, living.getLocation().add(0, 1, 0), 10,
                    0.5, 0.5, 0.5, 0.1);
            }
        }
    }

    /**
     * Creates divine intervention visual effects.
     */
    private void createDivineEffects(SpellContext context, Location center, double radius) {
        BukkitTask effectTask = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= EFFECT_DURATION_TICKS) {
                    cancel();
                    return;
                }

                // Divine column of light
                for (int y = 0; y < 8; y++) {
                    Location columnLoc = center.clone().add(0, y, 0);
                    center.getWorld().spawnParticle(Particle.END_ROD, columnLoc, 3,
                        0.3, 0.1, 0.3, 0.02);
                }

                // Healing aura ring
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location ringLoc = center.clone().add(x, 1, z);

                    center.getWorld().spawnParticle(Particle.DUST, ringLoc, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.5f));
                }

                // Floating hearts
                if (ticks % 8 == 0) {
                    Location heartLoc = center.clone().add(
                        (Math.random() - 0.5) * radius * 2,
                        Math.random() * 3,
                        (Math.random() - 0.5) * radius * 2
                    );
                    center.getWorld().spawnParticle(Particle.HEART, heartLoc, 1, 0, 0, 0, 0);
                }

                ticks += EFFECT_INTERVAL_TICKS;
            }
        }.runTaskTimer(context.plugin(), 0L, EFFECT_INTERVAL_TICKS);

        // Register task for cleanup
        if (context.plugin() instanceof nl.wantedchef.empirewand.EmpireWandPlugin plugin) {
            plugin.getTaskManager().registerTask(effectTask);
        }
    }
}