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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * ShadowAura - Toggleable shadow aura with continuous DOT/pressure in a radius.
 * Stops immediately when switching spells for clear rotation rules.
 */
public final class ShadowAura extends Spell<Void> implements ToggleableSpell {

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
            name = "Shadow Aura";
            description = "Toggleable shadow aura with continuous DOT/pressure in a radius. Stops immediately when switching spells.";
            cooldown = Duration.ofSeconds(15);
            spellType = SpellType.AURA;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new ShadowAura(this);
        }
    }

    private ShadowAura(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /*  SPELL API                               */
    /* ---------------------------------------- */
    @Override
    public String key() {
        return "shadow-aura";
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
        private final BukkitTask dotTicker;
        private final double auraRadius;
        private final double dotDamage;
        private final int dotInterval;
        private int tickCounter = 0;

        AuraData(Player player, SpellContext context) {
            this.player = player;
            this.auraRadius = spellConfig.getDouble("values.radius", 6.0);
            this.dotDamage = spellConfig.getDouble("values.dot-damage", 1.0);
            this.dotInterval = spellConfig.getInt("values.dot-interval-ticks", 10); // 0.5 seconds default
            
            // Send activation message
            player.sendMessage(Component.text("§5⚡ §7Shadow Aura activated. Moving creates shadow pressure."));
            
            // Start the main ticker
            this.ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, 1); // Run every tick
            
            // Start DOT ticker
            this.dotTicker = Bukkit.getScheduler().runTaskTimer(plugin, this::applyDot, 0, dotInterval);
        }

        void stop() {
            ticker.cancel();
            dotTicker.cancel();
            player.sendMessage(Component.text("§5⚡ §7Shadow Aura deactivated."));
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
        }
        
        private void spawnAuraParticles() {
            Location loc = player.getLocation().add(0, 1, 0);
            World world = player.getWorld();
            
            // Create a ring of particles around the player
            for (int i = 0; i < 18; i++) {
                double angle = 2 * Math.PI * i / 18;
                double x = Math.cos(angle) * auraRadius;
                double z = Math.sin(angle) * auraRadius;
                Location particleLoc = loc.clone().add(x, 0, z);
                
                // Alternate between different particle types
                if (tickCounter % 6 < 2) {
                    world.spawnParticle(Particle.SMOKE, particleLoc, 1, 0.1, 0.1, 0.1, 0.02);
                } else if (tickCounter % 6 < 4) {
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
                } else {
                    world.spawnParticle(Particle.ASH, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }
            
            // Create occasional portal particles for shadow effect
            if (Math.random() < 0.3) {
                world.spawnParticle(Particle.PORTAL, loc, 5, 0.8, 0.8, 0.8, 0.1);
            }
            
            // Create end rod particles for mystical effect
            if (Math.random() < 0.2) {
                world.spawnParticle(Particle.END_ROD, loc, 2, 0.5, 0.5, 0.5, 0.02);
            }
        }
        
        private void applyDot() {
            Location playerLoc = player.getLocation();
            World world = player.getWorld();
            
            // Get nearby entities
            for (Entity entity : world.getNearbyEntities(playerLoc, auraRadius, auraRadius, auraRadius)) {
                if (entity instanceof LivingEntity && entity != player && !entity.isDead()) {
                    LivingEntity livingEntity = (LivingEntity) entity;
                    
                    // Apply DOT damage
                    livingEntity.damage(dotDamage, player);
                    
                    // Apply wither effect
                    livingEntity.addPotionEffect(new PotionEffect(
                        PotionEffectType.WITHER,
                        40, // 2 seconds
                        0,
                        false,
                        true
                    ));
                    
                    // Apply slow effect
                    livingEntity.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS,
                        30, // 1.5 seconds
                        0,
                        false,
                        true
                    ));
                    
                    // Spawn damage particles
                    Location entityLoc = livingEntity.getLocation().add(0, 1, 0);
                    world.spawnParticle(Particle.DAMAGE_INDICATOR, entityLoc, 2, 0.2, 0.2, 0.2, 0.1);
                    world.spawnParticle(Particle.SMOKE, entityLoc, 3, 0.3, 0.3, 0.3, 0.03);
                }
            }
            
            // Play ambient sound occasionally
            if (Math.random() < 0.3) {
                world.playSound(playerLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.3f, 2.0f);
            }
        }
    }
}