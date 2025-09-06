package com.example.empirewand.listeners;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.example.empirewand.EmpireWandPlugin;
import com.example.empirewand.core.FxService;
import com.example.empirewand.core.Keys;
import com.example.empirewand.core.PerformanceMonitor;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = {
        "EI_EXPOSE_REP2" }, justification = "Retains plugin reference per Bukkit listener pattern; not exposing internal mutable state beyond required service access.")
public class EntityListener implements Listener {

    private final EmpireWandPlugin plugin;

    public EntityListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        PerformanceMonitor.TimingContext timing = PerformanceMonitor.startTiming("onProjectileHit");

        Projectile projectile = event.getEntity();
        String spellName = projectile.getPersistentDataContainer().get(Keys.PROJECTILE_SPELL,
                Keys.STRING_TYPE.getType());

        if (spellName == null) {
            timing.complete(1);
            return;
        }

        FxService fxService = plugin.getFxService();

        switch (spellName) {
            case "explosive" -> handleExplosiveSpell(projectile, fxService);
            case "glacial-spike" -> handleGlacialSpikeSpell(projectile, event, fxService);
            case "lifesteal" -> handleLifestealSpell(projectile, event, fxService);
            default -> {
                // Unknown or handled elsewhere; flush any batched particles and clean up arrows
                fxService.flushParticleBatch();
                if (projectile instanceof Arrow) {
                    projectile.remove();
                }
            }
        }

        timing.complete(15); // Projectile hit should complete within 15ms
    }

    private void handleExplosiveSpell(Projectile projectile, FxService fxService) {
        PerformanceMonitor.TimingContext timing = PerformanceMonitor.startTiming("handleExplosiveSpell");

        // Batch particle effects for better performance
        fxService.batchParticles(projectile.getLocation(), Particle.EXPLOSION_EMITTER, 5, 0, 0, 0, 0);
        fxService.batchParticles(projectile.getLocation(), Particle.LARGE_SMOKE, 20, 1, 1, 1, 0.1);
        fxService.flushParticleBatch(); // Execute all batched particles at once

        fxService.playSound(projectile.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);

        // Configurable AoE damage around impact
        double damage = plugin.getConfigService().getSpellsConfig().getDouble("explosive.values.damage", 12.0);
        double radius = plugin.getConfigService().getSpellsConfig().getDouble("explosive.values.radius", 4.0);
        boolean hitPlayers = plugin.getConfigService().getSpellsConfig().getBoolean("explosive.flags.hit-players",
                true);
        boolean hitMobs = plugin.getConfigService().getSpellsConfig().getBoolean("explosive.flags.hit-mobs", true);

        String ownerUUID = projectile.getPersistentDataContainer().get(Keys.PROJECTILE_OWNER,
                Keys.STRING_TYPE.getType());
        Player caster = ownerUUID != null ? Bukkit.getPlayer(UUID.fromString(ownerUUID)) : null;
        boolean friendlyFire = plugin.getConfigService().getConfig().getBoolean("features.friendly-fire", false);

        projectile.getWorld().getNearbyEntities(projectile.getLocation(), radius, radius, radius).forEach(e -> {
            if (e instanceof LivingEntity le) {
                boolean isPlayer = le instanceof Player;
                if ((isPlayer && !hitPlayers) || (!isPlayer && !hitMobs))
                    return;
                if (caster != null && !friendlyFire && (le instanceof Player p)
                        && p.getUniqueId().equals(caster.getUniqueId()))
                    return;
                le.damage(damage, caster != null ? caster : projectile);
            }
        });

        timing.complete(20); // Explosive spell should complete within 20ms
    }

    private void handleGlacialSpikeSpell(Projectile projectile, ProjectileHitEvent event, FxService fxService) {
        // Apply slow on hit entity based on config flags, then shatter effect and clean
        // up projectile
        if (event.getHitEntity() instanceof LivingEntity target) {
            boolean hitPlayers = plugin.getConfigService().getSpellsConfig()
                    .getBoolean("glacial-spike.flags.hit-players", true);
            boolean hitMobs = plugin.getConfigService().getSpellsConfig().getBoolean("glacial-spike.flags.hit-mobs",
                    true);
            boolean isPlayer = target instanceof Player;
            if ((isPlayer && hitPlayers) || (!isPlayer && hitMobs)) {
                int slowTicks = plugin.getConfigService().getSpellsConfig()
                        .getInt("glacial-spike.values.slow-duration-ticks", 80);
                int slowAmp = plugin.getConfigService().getSpellsConfig().getInt("glacial-spike.values.slow-amplifier",
                        2);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowTicks, slowAmp));
            }
        }
        // ITEM_CRACK requires data; use FxService overload with data
        fxService.spawnParticles(projectile.getLocation(), Particle.ITEM, 30, 0.2, 0.2, 0.2, 0.1,
                new ItemStack(Material.ICE));
        fxService.playSound(projectile.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
        // Remove arrow to avoid it sticking around
        if (projectile instanceof Arrow) {
            projectile.remove();
        }
    }

    private void handleLifestealSpell(Projectile projectile, ProjectileHitEvent event, FxService fxService) {
        if (event.getHitEntity() instanceof LivingEntity target) {
            String ownerUUID = projectile.getPersistentDataContainer().get(Keys.PROJECTILE_OWNER,
                    Keys.STRING_TYPE.getType());
            if (ownerUUID != null) {
                Player caster = Bukkit.getPlayer(UUID.fromString(ownerUUID));
                if (caster != null && canDamageTarget(caster, target)) {
                    double damage = plugin.getConfigService().getSpellsConfig().getDouble("lifesteal.values.damage",
                            6.0);
                    target.damage(damage, caster);
                    var maxAttr = caster.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    double maxHealth = maxAttr != null ? maxAttr.getValue() : 20.0;
                    double currentHealth = caster.getHealth();
                    caster.setHealth(Math.min(maxHealth, currentHealth + (damage / 2.0)));
                }
            }
        }
        fxService.playSound(projectile.getLocation(), Sound.ENTITY_VEX_CHARGE, 1.0f, 1.0f);
        // Snowball can linger briefly; ensure cleanup
        projectile.remove();
    }

    private boolean canDamageTarget(Player caster, LivingEntity target) {
        // Respect friendly fire: skip if target is the caster and FF disabled
        boolean friendlyFire = plugin.getConfigService().getConfig().getBoolean("features.friendly-fire", false);
        boolean hitPlayers = plugin.getConfigService().getSpellsConfig().getBoolean("lifesteal.flags.hit-players",
                true);
        boolean hitMobs = plugin.getConfigService().getSpellsConfig().getBoolean("lifesteal.flags.hit-mobs", true);
        boolean isPlayer = target instanceof Player;
        boolean allowedByFlags = (isPlayer && hitPlayers) || (!isPlayer && hitMobs);
        boolean hittingSelfWhenNoFF = (target instanceof Player tgtPlayer) && !friendlyFire
                && tgtPlayer.getUniqueId().equals(caster.getUniqueId());
        return allowedByFlags && !hittingSelfWhenNoFF;
    }

    @EventHandler(ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != DamageCause.FALL)
            return;
        if (!(event.getEntity() instanceof Player player))
            return;

        // Cancel fall damage if tagged as ethereal and not expired.
        var pdc = player.getPersistentDataContainer();
        boolean etherealFlag = pdc.has(Keys.ETHEREAL_ACTIVE, Keys.BYTE_TYPE.getType());
        Long expires = pdc.get(Keys.ETHEREAL_EXPIRES_TICK, Keys.LONG_TYPE.getType());
        long now = player.getWorld().getFullTime();
        boolean activeByTime = (expires != null) && now <= expires;

        if (etherealFlag && activeByTime) {
            event.setCancelled(true);
            // Soft landing FX
            plugin.getFxService().spawnParticles(player.getLocation(), Particle.END_ROD, 8, 0.2, 0.1, 0.2, 0.0);
            plugin.getFxService().playSound(player, Sound.BLOCK_AMETHYST_BLOCK_FALL, 0.5f, 1.5f);
        }

        // Cleanup if we detect expiry has passed but flags remain (failsafe)
        if (etherealFlag && expires != null && now > expires) {
            pdc.remove(Keys.ETHEREAL_ACTIVE);
            pdc.remove(Keys.ETHEREAL_EXPIRES_TICK);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        com.example.empirewand.spell.implementation.Polymorph polymorphSpell = (com.example.empirewand.spell.implementation.Polymorph) plugin
                .getSpellRegistry().getSpell("polymorph");
        if (polymorphSpell != null && polymorphSpell.getPolymorphedEntities().containsKey(deadEntity.getUniqueId())) {
            UUID originalEntityUUID = polymorphSpell.getPolymorphedEntities().get(deadEntity.getUniqueId());
            LivingEntity originalEntity = (LivingEntity) Bukkit.getEntity(originalEntityUUID);
            if (originalEntity != null) {
                originalEntity.setHealth(0);
            }
            polymorphSpell.getPolymorphedEntities().remove(deadEntity.getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        var entity = event.getEntity();
        String spellName = entity.getPersistentDataContainer().get(Keys.PROJECTILE_SPELL, Keys.STRING_TYPE.getType());
        if (!"explosive".equals(spellName))
            return;

        boolean blockDamage = plugin.getConfigService().getSpellsConfig().getBoolean("explosive.flags.block-damage",
                false);
        if (!blockDamage) {
            // Prevent blocks from being destroyed by our Explosive spell
            event.blockList().clear();
        }
    }
}
