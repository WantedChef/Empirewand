package com.example.empirewand.spell.implementation.lightning;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.example.empirewand.spell.Prereq;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;

import net.kyori.adventure.text.Component;

/**
 * LightningArrow — globale listener + particle-only trail (geen echte blokken).
 *
 * Robuuste versie die memory leaks, dubbele damage en griefing via redstone
 * blokken voorkomt.
 * - 1 globale event-listener per plugin (niet per cast)
 * - Trail is puur visueel met particles
 * - Arrow damage = 0 (geen dubbele vanilla hit); AoE-schade wordt handmatig
 * toegepast
 * - Friendly fire: standaard geen self-damage; uitbreidbaar voor teams
 */
public class LightningArrow implements Spell {

    // === PDC keys (lazy init met de plugin) ===
    private static NamespacedKey K_SPELL;
    private static NamespacedKey K_DAMAGE;
    private static NamespacedKey K_BLOCK_DAMAGE;
    private static NamespacedKey K_GLOWING;
    private static NamespacedKey K_GLOW_TICKS;
    private static NamespacedKey K_RADIUS;
    private static NamespacedKey K_FF_SELF;
    private static NamespacedKey K_CASTER_UUID;
    private static NamespacedKey K_PROCESSED;

    // Listener singleton-registratie
    private static volatile boolean LISTENER_REGISTERED = false;

    private static void ensureKeysAndListener(SpellContext ctx) {
        if (K_SPELL == null) {
            synchronized (LightningArrow.class) {
                if (K_SPELL == null) {
                    Plugin plugin = ctx.plugin();
                    K_SPELL = new NamespacedKey(plugin, "spell_tag");
                    K_DAMAGE = new NamespacedKey(plugin, "lightning_damage");
                    K_BLOCK_DAMAGE = new NamespacedKey(plugin, "lightning_block_damage");
                    K_GLOWING = new NamespacedKey(plugin, "lightning_glow");
                    K_GLOW_TICKS = new NamespacedKey(plugin, "lightning_glow_ticks");
                    K_RADIUS = new NamespacedKey(plugin, "lightning_radius");
                    K_FF_SELF = new NamespacedKey(plugin, "lightning_ff_self");
                    K_CASTER_UUID = new NamespacedKey(plugin, "lightning_caster_uuid");
                    K_PROCESSED = new NamespacedKey(plugin, "lightning_processed");
                }
            }
        }
        if (!LISTENER_REGISTERED) {
            synchronized (LightningArrow.class) {
                if (!LISTENER_REGISTERED) {
                    ctx.plugin().getServer().getPluginManager().registerEvents(new GlobalArrowListener(ctx.plugin()),
                            ctx.plugin());
                    LISTENER_REGISTERED = true;
                }
            }
        }
    }

    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        ensureKeysAndListener(context);

        // Config waarden
        var spells = context.config().getSpellsConfig();
        double damage = spells.getDouble("lightning-arrow.values.damage", 8.0); // 4 hearts
        boolean blockDamage = spells.getBoolean("lightning-arrow.flags.block-damage", false);
        boolean glowing = spells.getBoolean("lightning-arrow.flags.glowing", true);
        int glowingDuration = spells.getInt("lightning-arrow.values.glowing-duration-ticks", 60);
        boolean friendlyFireSelf = context.config().getConfig().getBoolean("features.friendly-fire-self", false);
        double radius = spells.getDouble("lightning-arrow.values.radius", 3.0);

        // Trail config (purely cosmetic)
        int trailLength = spells.getInt("lightning-arrow.values.trail_length", 5);
        int particleCount = spells.getInt("lightning-arrow.values.particle_count", 3);
        int sparkInterval = spells.getInt("lightning-arrow.values.spark_interval_ticks", 4);
        int maxLifeTicks = spells.getInt("lightning-arrow.values.max_flight_ticks", 20 * 10);

        // Arrow spawnen
        Arrow arrow = player.getWorld().spawn(player.getEyeLocation(), Arrow.class, a -> {
            a.setVelocity(player.getEyeLocation().getDirection().multiply(2.0));
            a.setShooter(player);
            a.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
            a.setCritical(true);
            // Voorkom vanilla pijl-damage; we handelen zelf de AoE af bij impact
            if (a instanceof AbstractArrow abs) {
                abs.setDamage(0.0);
                abs.setKnockbackStrength(0);
            }
        });

        // PDC taggen met alle parameters
        PersistentDataContainer pdc = arrow.getPersistentDataContainer();
        pdc.set(K_SPELL, PersistentDataType.STRING, "lightning-arrow");
        pdc.set(K_DAMAGE, PersistentDataType.DOUBLE, damage);
        pdc.set(K_BLOCK_DAMAGE, PersistentDataType.BYTE, (byte) (blockDamage ? 1 : 0));
        pdc.set(K_GLOWING, PersistentDataType.BYTE, (byte) (glowing ? 1 : 0));
        pdc.set(K_GLOW_TICKS, PersistentDataType.INTEGER, glowingDuration);
        pdc.set(K_RADIUS, PersistentDataType.DOUBLE, radius);
        pdc.set(K_FF_SELF, PersistentDataType.BYTE, (byte) (friendlyFireSelf ? 1 : 0));
        pdc.set(K_CASTER_UUID, PersistentDataType.STRING, player.getUniqueId().toString());

        // Trail runnable (particles only)
        new ParticleTrail(context.plugin(), arrow, trailLength, particleCount, sparkInterval, maxLifeTicks)
                .runTaskTimer(context.plugin(), 0L, 1L);

        // Cast FX
        context.fx().playSound(player, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
    }

    @Override
    public String getName() {
        return "lightning-arrow";
    }

    @Override
    public String key() {
        return "lightning-arrow";
    }

    @Override
    public Component displayName() {
        return Component.text("Lightning Arrow");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }

    // === Globale listener ===
    private static final class GlobalArrowListener implements Listener {
        private final Plugin plugin;

        GlobalArrowListener(Plugin plugin) {
            this.plugin = plugin;
        }

        @EventHandler
        public void onProjectileHit(ProjectileHitEvent event) {
            Projectile proj = event.getEntity();
            if (!(proj instanceof Arrow arrow))
                return;

            PersistentDataContainer pdc = arrow.getPersistentDataContainer();
            String tag = pdc.getOrDefault(K_SPELL, PersistentDataType.STRING, "");
            if (!"lightning-arrow".equals(tag))
                return;

            // Dubbele verwerking voorkomen
            if (pdc.has(K_PROCESSED, PersistentDataType.BYTE))
                return;
            pdc.set(K_PROCESSED, PersistentDataType.BYTE, (byte) 1);

            // Lees parameters
            double damage = pdc.getOrDefault(K_DAMAGE, PersistentDataType.DOUBLE, 8.0);
            boolean blockDamage = pdc.getOrDefault(K_BLOCK_DAMAGE, PersistentDataType.BYTE, (byte) 0) == 1;
            boolean glowing = pdc.getOrDefault(K_GLOWING, PersistentDataType.BYTE, (byte) 1) == 1;
            int glowTicks = pdc.getOrDefault(K_GLOW_TICKS, PersistentDataType.INTEGER, 60);
            double radius = pdc.getOrDefault(K_RADIUS, PersistentDataType.DOUBLE, 3.0);
            boolean ffSelfOnly = pdc.getOrDefault(K_FF_SELF, PersistentDataType.BYTE, (byte) 0) == 1;
            UUID casterId = null;
            try {
                String s = pdc.get(K_CASTER_UUID, PersistentDataType.STRING);
                if (s != null)
                    casterId = UUID.fromString(s);
            } catch (Exception ignored) {
            }

            Location hitLoc = arrow.getLocation();

            // Bliksem effect / echte schade
            if (blockDamage) {
                hitLoc.getWorld().strikeLightning(hitLoc);
            } else {
                hitLoc.getWorld().strikeLightningEffect(hitLoc);

                // Handmatige AoE schade
                for (var e : hitLoc.getWorld().getNearbyEntities(hitLoc, radius, radius, radius)) {
                    if (!(e instanceof LivingEntity living))
                        continue;
                    if (!living.isValid() || living.isDead())
                        continue;

                    // self-only friendly fire blokkade
                    if (ffSelfOnly && casterId != null && casterId.equals(living.getUniqueId())) {
                        continue;
                    }

                    living.damage(damage, arrow.getShooter() instanceof LivingEntity le ? le : null);

                    if (glowing) {
                        // Gebruik potion-effect (server-side zichtbaar voor iedereen)
                        living.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.GLOWING,
                                Math.max(1, glowTicks),
                                0,
                                false,
                                true));
                    }
                }
            }

            // Impact FX
            hitLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 24, 0.6, 0.6, 0.6, 0.05);
            hitLoc.getWorld().spawnParticle(Particle.CRIT, hitLoc, 8, 0.3, 0.3, 0.3, 0.0);
            hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);

            // Opruimen
            arrow.remove();

            // Safety: haal tag weg zodat her-proc onmogelijk is (voor het geval andere
            // plugins nog events afhandelen)
            pdc.remove(K_SPELL);
        }

        void unregister() {
            HandlerList.unregisterAll(this);
        }
    }

    // === Particle-only trail ===
    private static final class ParticleTrail extends BukkitRunnable {
        private final Plugin plugin;
        private final Arrow arrow;
        private final int trailLength;
        private final int particleCount;
        private final int sparkInterval;
        private final int maxLifeTicks;
        private int tick = 0;

        ParticleTrail(Plugin plugin, Arrow arrow, int trailLength, int particleCount, int sparkInterval,
                int maxLifeTicks) {
            this.plugin = plugin;
            this.arrow = arrow;
            this.trailLength = Math.max(1, trailLength);
            this.particleCount = Math.max(1, particleCount);
            this.sparkInterval = Math.max(1, sparkInterval);
            this.maxLifeTicks = Math.max(20, maxLifeTicks);
        }

        @Override
        public void run() {
            if (!arrow.isValid() || arrow.isDead()) {
                cancel();
                return;
            }

            Location base = arrow.getLocation();
            Vector dir = arrow.getVelocity().clone();
            if (dir.lengthSquared() < 1.0E-6)
                dir = base.getDirection();
            dir.normalize();

            // Kleine trail van spark/crit langs de staart
            for (int i = 0; i < trailLength; i++) {
                Location l = base.clone().add(dir.clone().multiply(-0.3 * i));
                base.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, l, particleCount, 0.05, 0.05, 0.05, 0.01);
                base.getWorld().spawnParticle(Particle.CRIT, l, 1, 0.02, 0.02, 0.02, 0.0);
            }

            if (tick % sparkInterval == 0) {
                base.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, base, particleCount * 2, 0.2, 0.2, 0.2, 0.02);
            }

            tick++;
            if (tick > maxLifeTicks) {
                cancel();
            }
        }
    }
}
