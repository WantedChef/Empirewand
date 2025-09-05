package com.example.empirewand.listeners;

import com.example.empirewand.EmpireWandPlugin;
import com.example.empirewand.core.FxService;
import com.example.empirewand.core.Keys;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Arrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class EntityListener implements Listener {

    private final EmpireWandPlugin plugin;

    public EntityListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        String spellName = projectile.getPersistentDataContainer().get(Keys.PROJECTILE_SPELL, PersistentDataType.STRING);

        if (spellName == null) {
            return;
        }

        FxService fxService = plugin.getFxService();
        if (spellName.equals("comet")) {
            fxService.spawnParticles(projectile.getLocation(), Particle.EXPLOSION, 30, 0.5, 0.5, 0.5, 0.1);
            fxService.playSound(projectile.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
        } else if (spellName.equals("explosive")) {
            fxService.spawnParticles(projectile.getLocation(), Particle.EXPLOSION_EMITTER, 5, 0, 0, 0, 0);
            fxService.spawnParticles(projectile.getLocation(), Particle.LARGE_SMOKE, 20, 1, 1, 1, 0.1);
            fxService.playSound(projectile.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
        } else if (spellName.equals("glacial-spike")) {
            // Apply slow on hit entity based on config flags, then shatter effect and clean up projectile
            if (event.getHitEntity() instanceof LivingEntity target) {
                boolean hitPlayers = plugin.getConfigService().getSpellsConfig().getBoolean("glacial-spike.flags.hit-players", true);
                boolean hitMobs = plugin.getConfigService().getSpellsConfig().getBoolean("glacial-spike.flags.hit-mobs", true);
                boolean isPlayer = target instanceof Player;
                if ((isPlayer && hitPlayers) || (!isPlayer && hitMobs)) {
                    int slowTicks = plugin.getConfigService().getSpellsConfig().getInt("glacial-spike.values.slow-duration-ticks", 80);
                    int slowAmp = plugin.getConfigService().getSpellsConfig().getInt("glacial-spike.values.slow-amplifier", 2);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowTicks, slowAmp));
                }
            }
            // ITEM_CRACK requires data; use FxService overload with data
            fxService.spawnParticles(projectile.getLocation(), Particle.ITEM, 30, 0.2, 0.2, 0.2, 0.1, new ItemStack(Material.ICE));
            fxService.playSound(projectile.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
            // Remove arrow to avoid it sticking around
            if (projectile instanceof Arrow) {
                projectile.remove();
            }
        } else if (spellName.equals("lifesteal")) {
            if (event.getHitEntity() instanceof LivingEntity target) {
                String ownerUUID = projectile.getPersistentDataContainer().get(Keys.PROJECTILE_OWNER, PersistentDataType.STRING);
                if (ownerUUID != null) {
                    Player caster = Bukkit.getPlayer(UUID.fromString(ownerUUID));
                    if (caster != null) {
                        // Respect friendly fire: skip if target is the caster and FF disabled
                        boolean friendlyFire = plugin.getConfigService().getConfig().getBoolean("features.friendly-fire", false);
                        boolean hitPlayers = plugin.getConfigService().getSpellsConfig().getBoolean("lifesteal.flags.hit-players", true);
                        boolean hitMobs = plugin.getConfigService().getSpellsConfig().getBoolean("lifesteal.flags.hit-mobs", true);
                        boolean isPlayer = target instanceof Player;
                        boolean allowedByFlags = (isPlayer && hitPlayers) || (!isPlayer && hitMobs);
                        boolean hittingSelfWhenNoFF = (target instanceof Player tgtPlayer) && !friendlyFire && tgtPlayer.getUniqueId().equals(caster.getUniqueId());
                        if (allowedByFlags && !hittingSelfWhenNoFF) {
                            double damage = plugin.getConfigService().getSpellsConfig().getDouble("lifesteal.values.damage", 6.0);
                            target.damage(damage, caster);
                            var maxAttr = caster.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                            double maxHealth = maxAttr != null ? maxAttr.getValue() : 20.0;
                            double currentHealth = caster.getHealth();
                            caster.setHealth(Math.min(maxHealth, currentHealth + (damage / 2.0)));
                        }
                    }
                }
            }
            fxService.playSound(projectile.getLocation(), Sound.ENTITY_VEX_CHARGE, 1.0f, 1.0f);
            // Snowball can linger briefly; ensure cleanup
            projectile.remove();
        }

        // TODO: Add impact effects for other spells
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        com.example.empirewand.spell.implementation.Polymorph polymorphSpell = (com.example.empirewand.spell.implementation.Polymorph) plugin.getSpellRegistry().getSpell("polymorph");
        if (polymorphSpell != null && polymorphSpell.getPolymorphedEntities().containsKey(deadEntity.getUniqueId())) {
            UUID originalEntityUUID = polymorphSpell.getPolymorphedEntities().get(deadEntity.getUniqueId());
            LivingEntity originalEntity = (LivingEntity) Bukkit.getEntity(originalEntityUUID);
            if (originalEntity != null) {
                originalEntity.setHealth(0);
            }
            polymorphSpell.getPolymorphedEntities().remove(deadEntity.getUniqueId());
        }
    }
}
