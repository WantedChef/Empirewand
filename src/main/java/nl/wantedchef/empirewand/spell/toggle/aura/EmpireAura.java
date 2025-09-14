package nl.wantedchef.empirewand.spell.toggle.aura;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * EmpireAura – Configurable toggleable aura granting strength & resistance with
 * particle ring and a damaging defensive field.
 * <p>
 * All gameplay values are driven through {@code spells.yml} under the prefix:
 * <pre>spells.empire-aura.*</pre>
 *
 * @author WantedChef
 */
public final class EmpireAura extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
 /* DATA */
 /* ---------------------------------------- */
    private final Map<UUID, AuraData> auras = new WeakHashMap<>();

    /* ---------------------------------------- */
 /* BUILDER */
 /* ---------------------------------------- */
    /**
     * Builder for creating {@link EmpireAura} instances.
     */
    public static class Builder extends Spell.Builder<Void> {

        /**
         * Creates a new builder for the Empire Aura spell.
         *
         * @param api the plugin API
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Empire Aura";
            description = "Omring jezelf met een krachtige aura.";
            cooldown = Duration.ofSeconds(25); // Applied by framework (do not manual set)
            spellType = SpellType.AURA;
        }

        /**
         * Builds the Empire Aura spell.
         *
         * @return the constructed spell
         */
        @Override
        public @NotNull Spell<Void> build() {
            return new EmpireAura(this);
        }
    }

    /**
     * Constructs the spell from its builder.
     *
     * @param builder the spell builder
     */
    private EmpireAura(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
 /* SPELL API */
 /* ---------------------------------------- */
    /**
     * Gets the configuration key for this spell.
     *
     * @return "empire-aura"
     */
    @Override
    public String key() {
        return "empire-aura";
    }

    /**
     * Returns the casting prerequisites.
     *
     * @return a no-op prerequisite
     */
    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    /**
     * Toggles the aura for the caster.
     *
     * @param context the spell context
     * @return always {@code null}
     */
    @Override
    protected Void executeSpell(SpellContext context) {
        toggle(context.caster(), context);
        return null;
    }

    /**
     * No additional effect handling is required.
     *
     * @param context the spell context
     * @param result  unused
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect not required
    }

    /* ---------------------------------------- */
 /* TOGGLE API */
 /* ---------------------------------------- */
    /**
     * Checks whether the aura is currently active for a player.
     *
     * @param player the player to check
     * @return {@code true} if active
     */
    @Override
    public boolean isActive(Player player) {
        return auras.containsKey(player.getUniqueId());
    }

    /**
     * Activates the aura for a player.
     *
     * @param player  the player
     * @param context the spell context
     */
    @Override
    public void activate(Player player, SpellContext context) {
        if (isActive(player)) {
            return;
        }
        auras.put(player.getUniqueId(), new AuraData(player, context));
    }

    /**
     * Deactivates the aura for a player.
     *
     * @param player  the player
     * @param context the spell context
     */
    @Override
    public void deactivate(Player player, SpellContext context) {
        Optional.ofNullable(auras.remove(player.getUniqueId())).ifPresent(AuraData::stop);
    }

    /**
     * Forcefully deactivates the aura without context.
     *
     * @param player the player
     */
    @Override
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public void forceDeactivate(Player player) {
        deactivate(player, null);
    }

    /**
     * Gets the maximum duration of the aura in ticks.
     *
     * @return max duration or -1 for infinite
     */
    @Override
    public int getMaxDuration() {
        return cfgInt("max-duration-ticks", -1);
    }

    /* ---------------------------------------- */
 /* INTERNAL CLASS */
 /* ---------------------------------------- */
    private final class AuraData {

        private final Player player;
        private final BukkitTask ticker;
        private final double radius;
        private final double damage;
        private final int confDur;
        private final double push;
        private final PotionEffectType confusion;

        AuraData(Player player, SpellContext context) {
            this.player = player;
            this.radius = cfgDouble("particles.radius", 9.0d);
            this.damage = cfgDouble("effects.aoe-damage", 3.0d);
            this.confDur = cfgInt("effects.confusion-duration-ticks", 100);
            this.push = cfgDouble("effects.push-strength", 1.8d);
            this.confusion = effectType("CONFUSION", "NAUSEA");
            applyEffects();
            sendMessage(cfgString("messages.activate", "&a⚔ Empire Aura geactiveerd."));
            spawnActivationBurst();
            long period = cfgLong("particles.period-ticks", 5L);
            this.ticker = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 0L, Math.max(1L, period));
        }

        void stop() {
            ticker.cancel();
            removeEffects();
            sendMessage(cfgString("messages.deactivate", "&c⚔ Empire Aura gedeactiveerd."));
            spawnDeactivationBurst();
        }

        private void tick() {
            if (!player.isOnline()) {
                forceDeactivate(player);
                return;
            }
            int max = getMaxDuration();
            if (max > 0 && player.getTicksLived() % max == 0) {
                // Auto timeout
                forceDeactivate(player);
                return;
            }
            spawnRingParticles();

            World world = player.getWorld();
            if (world == null) {
                forceDeactivate(player);
                return;
            }
            Location playerLoc = player.getLocation();
            Vector playerVec = playerLoc.toVector();
            for (LivingEntity entity : world.getNearbyLivingEntities(playerLoc, radius, e -> !e.equals(player))) {
                entity.damage(damage, player);
                if (confusion != null) {
                    entity.addPotionEffect(new PotionEffect(confusion, confDur, 0, false, false, true));
                }
                Vector vec = entity.getLocation().toVector().subtract(playerVec);

                // Check for zero-length vector to avoid NaN/infinite values when normalizing
                if (vec.lengthSquared() > 0.0001) { // Small threshold to avoid division by zero
                    vec.normalize();
                    vec.setY(0.4);
                    vec.multiply(push);

                    // Additional safety check to ensure all values are finite
                    if (Double.isFinite(vec.getX()) && Double.isFinite(vec.getY()) && Double.isFinite(vec.getZ())) {
                        entity.setVelocity(vec);
                    }
                } else {
                    // If entities are too close, push them in a random horizontal direction
                    double angle = Math.random() * 2 * Math.PI;
                    vec = new Vector(Math.cos(angle) * push, 0.4, Math.sin(angle) * push);
                    entity.setVelocity(vec);
                }
            }
        }

        private void applyEffects() {
            int dur = Integer.MAX_VALUE; // Long-lived until toggle off
            int strengthAmp = Math.max(0, cfgInt("effects.strength-amplifier", 0));
            int resistAmp = Math.max(0, cfgInt("effects.resistance-amplifier", 0));
            PotionEffectType strength = effectType("STRENGTH", "INCREASE_DAMAGE");
            PotionEffectType resistance = effectType("RESISTANCE", "DAMAGE_RESISTANCE");
            if (strength != null) {
                player.addPotionEffect(new PotionEffect(strength, dur, strengthAmp, false, false, true));
            }
            if (resistance != null) {
                player.addPotionEffect(new PotionEffect(resistance, dur, resistAmp, false, false, true));
            }
            if (cfgBool("effects.regeneration.enable", false)) {
                int regenAmp = Math.max(0, cfgInt("effects.regeneration.amplifier", 0));
                PotionEffectType regen = effectType("REGENERATION");
                if (regen != null) {
                    player.addPotionEffect(new PotionEffect(regen, dur, regenAmp, false, false, true));
                }
            }
        }

        private void removeEffects() {
            PotionEffectType strength = effectType("STRENGTH", "INCREASE_DAMAGE");
            PotionEffectType resistance = effectType("RESISTANCE", "DAMAGE_RESISTANCE");
            PotionEffectType regen = effectType("REGENERATION");
            if (strength != null) {
                player.removePotionEffect(strength);
            }
            if (resistance != null) {
                player.removePotionEffect(resistance);
            }
            if (regen != null) {
                player.removePotionEffect(regen);
            }
        }

        private void spawnRingParticles() {
            Location loc = player.getLocation();
            int points = Math.max(4, cfgInt("particles.ring-points", 10));
            double radius = cfgDouble("particles.radius", 9.0d);
            String particleName = cfgString("particles.type", "CRIT");
            Particle particle;
            try {
                particle = Particle.valueOf(particleName);
            } catch (IllegalArgumentException ex) {
                particle = Particle.CRIT;
            }

            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                double x = loc.getX() + radius * Math.cos(angle);
                double z = loc.getZ() + radius * Math.sin(angle);
                if (loc.getWorld() != null) {
                    loc.getWorld().spawnParticle(particle, x, loc.getY() + 1, z, 1, 0, 0, 0, 0);
                }
            }

            // Optional colored dust core
            if (cfgBool("particles.core-dust.enable", false)) {
                int r = cfgInt("particles.core-dust.r", 20);
                int g = cfgInt("particles.core-dust.g", 20);
                int b = cfgInt("particles.core-dust.b", 20);
                float size = (float) cfgDouble("particles.core-dust.size", 1.2);
                if (loc.getWorld() != null) {
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0,
                            new Particle.DustOptions(Color.fromRGB(clamp(r), clamp(g), clamp(b)), size));
                }
            }
        }

        private void spawnActivationBurst() {
            if (!cfgBool("particles.activation.enable", true)) {
                return;
            }
            Location loc = player.getLocation().clone().add(0, 1, 0);
            String type = cfgString("particles.activation.type", "CRIT");
            Particle p;
            try {
                p = Particle.valueOf(type);
            } catch (IllegalArgumentException ex) {
                p = Particle.CRIT;
            }
            if (loc.getWorld() != null) {
                loc.getWorld().spawnParticle(p, loc, cfgInt("particles.activation.count", 30), .5, .5, .5, .01);
            }
        }

        private void spawnDeactivationBurst() {
            if (!cfgBool("particles.deactivation.enable", true)) {
                return;
            }
            Location loc = player.getLocation().clone().add(0, 1, 0);
            String type = cfgString("particles.deactivation.type", "SMOKE");
            Particle p;
            try {
                p = Particle.valueOf(type);
            } catch (IllegalArgumentException ex) {
                p = Particle.SMOKE;
            }
            if (loc.getWorld() != null) {
                loc.getWorld().spawnParticle(p, loc, cfgInt("particles.deactivation.count", 20), .4, .4, .4, .02);
            }
        }

        /* helpers */
        private void sendMessage(String msg) {
            if (msg == null || msg.isEmpty()) {
                return;
            }
            player.sendMessage(Component.text(msg.replace('&', '§')));
        }
    }

    /* ---------------------------------------- */
 /* CONFIG HELPERS */
 /* ---------------------------------------- */
    private String cfgString(String path, String def) {
        return spellConfig.getString(key() + "." + path, def);
    }

    private int cfgInt(String path, int def) {
        return spellConfig.getInt(key() + "." + path, def);
    }

    private long cfgLong(String path, long def) {
        return spellConfig.getLong(key() + "." + path, def);
    }

    private double cfgDouble(String path, double def) {
        return spellConfig.getDouble(key() + "." + path, def);
    }

    private boolean cfgBool(String path, boolean def) {
        return spellConfig.getBoolean(key() + "." + path, def);
    }

    private int clamp(int c) {
        return Math.max(0, Math.min(255, c));
    }

    private PotionEffectType effectType(String primary, String... fallbacks) {
        PotionEffectType type = PotionEffectType.getByName(primary);
        if (type != null) {
            return type;
        }
        if (fallbacks != null) {
            for (String fb : fallbacks) {
                type = PotionEffectType.getByName(fb);
                if (type != null) {
                    return type;
                }
            }
        }
        return null; // gracefully handle version differences
    }
}
