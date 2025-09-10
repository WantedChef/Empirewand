package nl.wantedchef.empirewand.spell.toggle.aura;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.base.PrereqInterface;
import nl.wantedchef.empirewand.spell.base.Spell;
import nl.wantedchef.empirewand.spell.base.SpellContext;
import nl.wantedchef.empirewand.spell.base.SpellType;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.core.EmpireWandPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * MephiAura - Persistent, toggleable aura that pulses proximity damage and pressure.
 * The aura automatically stops when switching to another spell.
 */
public final class MephiAura extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /*  DATA                                    */
    /* ---------------------------------------- */
    private final Map<UUID, AuraData> auras = new WeakHashMap<>();
    private EmpireWandPlugin plugin;

    /* ---------------------------------------- */
    /*  BUILDER                                 */
    /* ---------------------------------------- */
    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Mephi Aura";
            description = "Persistent, toggleable aura that pulses proximity damage and pressure. Automatically stops when switching spells.";
            cooldown = Duration.ofSeconds(12);
            spellType = SpellType.AURA;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new MephiAura(this);
        }
    }

    private MephiAura(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /*  SPELL API                               */
    /* ---------------------------------------- */
    @Override
    public String key() {
        return "mephi-aura";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        toggle(context.caster(), context);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect
    }

    /* ---------------------------------------- */
    /*  TOGGLE API                              */
    /* ---------------------------------------- */
    @Override
    public boolean isActive(Player player) {
        return auras.containsKey(player.getUniqueId());
    }

    @Override
    public void activate(Player player, SpellContext context) {
        if (isActive(player)) return;
        plugin = context.plugin();
        auras.put(player.getUniqueId(), new AuraData(player, context));
    }

    @Override
    public void deactivate(Player player, SpellContext context) {
        Optional.ofNullable(auras.remove(player.getUniqueId())).ifPresent(AuraData::stop);
    }

    @Override
    public void forceDeactivate(Player player) {
        deactivate(player, null);
    }

    /* ---------------------------------------- */
    /*  INTERNAL CLASS                          */
    /* ---------------------------------------- */
    private final class AuraData {
        private final Player player;
        private final BukkitTask ticker;
        private final double auraRadius;
        private final double pulseDamage;
        private final int pulseInterval;
        private int tickCounter = 0;

        AuraData(Player player, SpellContext context) {
            this.player = player;
            this.auraRadius = spellConfig.getDouble("values.radius", 5.0);
            this.pulseDamage = spellConfig.getDouble("values.pulse-damage", 2.0);
            this.pulseInterval = spellConfig.getInt("values.pulse-interval-ticks", 20); // 1 second default
            
            // Send activation message
            player.sendMessage(Component.text("§5⚡ §7Mephi Aura activated. Moving creates proximity damage."));
            
            // Start the ticker
            this.ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, 1); // Run every tick
        }

        void stop() {
            ticker.cancel();
            player.sendMessage(Component.text("§5⚡ §7Mephi Aura deactivated."));
        }

        private void tick() {
            if (!player.isOnline() || player.isDead()) {
                forceDeactivate(player);
                return;
            }

            // Increment tick counter
            tickCounter++;
            
            // Apply aura effects periodically
            if (tickCounter % 2 == 0) {
                spawnAuraParticles();
            }
            
            // Apply pulse damage periodically
            if (tickCounter % pulseInterval == 0) {
                applyPulseDamage();
            }
        }

        private void spawnAuraParticles() {
            Location loc = player.getLocation().add(0, 1, 0);
            World world = player.getWorld();
            
            // Create a ring of particles around the player
            for (int i = 0; i < 16; i++) {
                double angle = 2 * Math.PI * i / 16;
                double x = Math.cos(angle) * auraRadius;
                double z = Math.sin(angle) * auraRadius;
                Location particleLoc = loc.clone().add(x, 0, z);
                
                // Alternate between different particle types
                if (tickCounter % 4 < 2) {
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
                } else {
                    world.spawnParticle(Particle.SMOKE, particleLoc, 1, 0.1, 0.1, 0.1, 0.02);
                }
            }
            
            // Create occasional critical particles for visual effect
            if (Math.random() < 0.3) {
                world.spawnParticle(Particle.CRIT, loc, 5, 0.5, 0.5, 0.5, 0.05);
            }
        }

        private void applyPulseDamage() {
            Location playerLoc = player.getLocation();
            World world = player.getWorld();
            
            // Get nearby entities
            for (Entity entity : world.getNearbyEntities(playerLoc, auraRadius, auraRadius, auraRadius)) {
                if (entity instanceof LivingEntity && entity != player && !entity.isDead()) {
                    LivingEntity livingEntity = (LivingEntity) entity;
                    
                    // Apply damage
                    livingEntity.damage(pulseDamage, player);
                    
                    // Apply slight knockback away from player
                    Vector knockback = livingEntity.getLocation().toVector().subtract(playerLoc.toVector()).normalize().multiply(0.3);
                    knockback.setY(0.1); // Small upward component
                    livingEntity.setVelocity(livingEntity.getVelocity().add(knockback));
                    
                    // Apply wither effect briefly
                    livingEntity.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.WITHER, 
                        40, // 2 seconds
                        0, 
                        false, 
                        true
                    ));
                    
                    // Spawn damage particles
                    Location entityLoc = livingEntity.getLocation().add(0, 1, 0);
                    world.spawnParticle(Particle.DAMAGE_INDICATOR, entityLoc, 3, 0.2, 0.2, 0.2, 0.1);
                }
            }
            
            // Play pulse sound
            world.playSound(playerLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.5f);
        }
    }
}