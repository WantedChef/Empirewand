package nl.wantedchef.empirewand.spell.heal;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.attribute.Attribute;
import org.bukkit.Color;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * DivineHeal - Ultimate healing spell
 */
public class DivineHeal extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Divine Heal";
            this.description = "Channel divine power to heal and protect all allies";
            this.cooldown = Duration.ofSeconds(60);
            this.spellType = SpellType.HEAL;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new DivineHeal(this);
        }
    }

    private static final double DEFAULT_RADIUS = 15.0;
    private static final int DEFAULT_DURATION = 200;

    private DivineHeal(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "divineheal";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(40);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        double radius = spellConfig.getDouble("values.radius", DEFAULT_RADIUS);
        int duration = spellConfig.getInt("values.duration", DEFAULT_DURATION);
        boolean celestialSanctuary = spellConfig.getBoolean("flags.celestial_sanctuary", true);
        boolean angelicPresence = spellConfig.getBoolean("flags.angelic_presence", true);
        
        // Start the ultimate divine healing sanctuary
        startCelestialSanctuary(context, player, radius, duration, celestialSanctuary, angelicPresence);
    }
    
    private void startCelestialSanctuary(SpellContext context, Player caster, double radius, int duration, boolean celestialSanctuary, boolean angelicPresence) {
        Location center = caster.getLocation();
        
        // Create celestial sanctuary setup
        createCelestialSetup(context, center, radius);
        
        // Start the divine sanctuary process
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= duration) {
                    // Complete the celestial sanctuary
                    completeCelestialSanctuary(context, center, radius);
                    cancel();
                    return;
                }
                
                double progress = (double) ticks / duration;
                
                // Celestial sanctuary aura
                if (celestialSanctuary) {
                    createCelestialAura(context, center, radius, progress);
                }
                
                // Angel wing effects
                if (angelicPresence && ticks % 15 == 0) {
                    createAngelWings(context, center, radius, progress);
                }
                
                // Divine presence healing
                if (ticks % 20 == 0) {
                    performDivineHealing(center, radius, angelicPresence, ticks / 20);
                }
                
                // Sanctuary sounds
                if (ticks % 20 == 0) {
                    context.fx().playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.5f + (float)(progress * 0.5f));
                }
                if (ticks % 40 == 0) {
                    context.fx().playSound(center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f, 2.0f);
                }
                
                ticks += 5;
            }
        }.runTaskTimer(context.plugin(), 0L, 5L);
        
        // Initial celestial activation
        context.fx().playSound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 2.0f, 1.0f);
        context.fx().playSound(center, Sound.ENTITY_EVOKER_CAST_SPELL, 1.5f, 1.5f);
        context.fx().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);
        context.fx().spawnParticles(center.clone().add(0, 2, 0), Particle.FLASH, 3, 1, 1, 1, 0);
        
        caster.sendMessage("§6§l👼 CELESTIAL SANCTUARY §eactivated! Divine presence for " + (duration/20) + " seconds!");
    }
    
    private double getMaxHealth(org.bukkit.entity.LivingEntity entity) {
        var attr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }
    
    private void createCelestialSetup(SpellContext context, Location center, double radius) {
        // Create divine sanctuary foundation with celestial effects
        for (int i = 0; i < 30; i++) {
            final int step = i;
            context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                // Outer celestial circle
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                    double x = Math.cos(angle) * radius * 1.2;
                    double z = Math.sin(angle) * radius * 1.2;
                    Location celestialLoc = center.clone().add(x, 0.3, z);
                    
                    context.fx().spawnParticles(celestialLoc, Particle.DUST, 2, 0.1, 0.1, 0.1, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.2f)); // Pure White
                    context.fx().spawnParticles(celestialLoc, Particle.END_ROD, 1, 0.1, 0.1, 0.1, 0.02);
                }
                
                // Central divine pillar
                for (int h = 0; h < 15; h++) {
                    Location pillarLoc = center.clone().add(0, h * 0.6, 0);
                    context.fx().spawnParticles(pillarLoc, Particle.TOTEM_OF_UNDYING, 2, 0.2, 0.2, 0.2, 0.02);
                    context.fx().spawnParticles(pillarLoc, Particle.END_ROD, 3, 0.3, 0.3, 0.3, 0.05);
                }
            }, step * 2L);
        }
    }
    
    private void createCelestialAura(SpellContext context, Location center, double radius, double progress) {
        // Rotating celestial energy rings
        double angle = progress * Math.PI * 2;
        
        for (int ring = 1; ring <= 3; ring++) {
            double ringRadius = radius * ring / 3;
            for (int i = 0; i < 8; i++) {
                double particleAngle = angle + (i * Math.PI / 4) + (ring * Math.PI / 6);
                double x = Math.cos(particleAngle) * ringRadius;
                double z = Math.sin(particleAngle) * ringRadius;
                double y = Math.sin(progress * Math.PI * 3 + i) * 0.8 + 1.5;
                
                Location auraLoc = center.clone().add(x, y, z);
                context.fx().spawnParticles(auraLoc, Particle.DUST, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f));
                context.fx().spawnParticles(auraLoc, Particle.END_ROD, 1, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }
    
    private void createAngelWings(SpellContext context, Location center, double radius, double progress) {
        // Create angelic wing formations
        for (int angel = 0; angel < 4; angel++) {
            double angelAngle = (2 * Math.PI * angel / 4) + progress * Math.PI;
            double angelX = Math.cos(angelAngle) * radius * 1.1;
            double angelZ = Math.sin(angelAngle) * radius * 1.1;
            double angelY = 3 + Math.sin(progress * Math.PI * 2 + angel) * 1;
            
            Location angelLoc = center.clone().add(angelX, angelY, angelZ);
            
            // Angel wings
            for (int wing = 0; wing < 6; wing++) {
                double wingAngle = angelAngle + (wing < 3 ? Math.PI/2 : -Math.PI/2);
                double wingSpread = (wing % 3) * 0.4;
                double wingX = Math.cos(wingAngle) * wingSpread;
                double wingZ = Math.sin(wingAngle) * wingSpread;
                
                Location wingLoc = angelLoc.clone().add(wingX, -wing * 0.1, wingZ);
                context.fx().spawnParticles(wingLoc, Particle.DUST, 2, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.3f));
            }
            
            // Angel halo
            for (double haloAngle = 0; haloAngle < Math.PI * 2; haloAngle += Math.PI / 6) {
                double haloX = Math.cos(haloAngle) * 0.6;
                double haloZ = Math.sin(haloAngle) * 0.6;
                Location haloLoc = angelLoc.clone().add(haloX, 0.4, haloZ);
                context.fx().spawnParticles(haloLoc, Particle.DUST, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.0f));
            }
        }
    }
    
    private void performDivineHealing(Location center, double radius, boolean angelicPresence, int wave) {
        // Enhanced divine healing for all entities
        for (var entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof Player targetPlayer) {
                // Powerful healing
                double max = getMaxHealth(targetPlayer);
                targetPlayer.setHealth(Math.min(targetPlayer.getHealth() + 2, max));
                
                // Divine blessings
                if (wave % 2 == 0) {
                    targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, 2));
                    targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 1));
                    targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 120, 3));
                }
                
                if (angelicPresence && wave % 3 == 0) {
                    targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 1));
                    targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1));
                }
                
                // Remove all negative effects
                targetPlayer.setFireTicks(0);
                targetPlayer.removePotionEffect(PotionEffectType.POISON);
                targetPlayer.removePotionEffect(PotionEffectType.WITHER);
                targetPlayer.removePotionEffect(PotionEffectType.WEAKNESS);
                targetPlayer.removePotionEffect(PotionEffectType.SLOWNESS);
                
                // Individual blessing
                targetPlayer.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, 
                    targetPlayer.getLocation().add(0, 2, 0), 8, 0.4, 0.4, 0.4, 0.1);
            }
        }
    }
    
    private void completeCelestialSanctuary(SpellContext context, Location center, double radius) {
        // Spectacular celestial completion
        context.fx().spawnParticles(center.clone().add(0, 3, 0), Particle.EXPLOSION_EMITTER, 8, 3, 3, 3, 0);
        context.fx().spawnParticles(center.clone().add(0, 3, 0), Particle.TOTEM_OF_UNDYING, 200, 4, 4, 4, 0.8);
        context.fx().spawnParticles(center.clone().add(0, 3, 0), Particle.END_ROD, 150, 3, 6, 3, 0.6);
        
        // Angel ascension
        for (int i = 0; i < 8; i++) {
            final int angel = i;
            context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                double angle = (2 * Math.PI * angel / 8);
                double x = Math.cos(angle) * radius * 1.5;
                double z = Math.sin(angle) * radius * 1.5;
                
                for (int h = 0; h < 15; h++) {
                    Location ascensionLoc = center.clone().add(x, h * 0.8, z);
                    context.fx().spawnParticles(ascensionLoc, Particle.DUST, 3, 0.2, 0.2, 0.2, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.5f));
                }
            }, angel * 4L);
        }
        
        // Divine completion sounds
        context.fx().playSound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 3.0f, 1.0f);
        context.fx().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 2.5f, 1.2f);
        context.fx().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.8f);
        
        // Divine message
        for (Player player : center.getWorld().getPlayers()) {
            if (player.getLocation().distance(center) <= radius * 2) {
                player.sendMessage("§6§l👼 CELESTIAL SANCTUARY §ecomplete! Divine blessings linger...");
            }
        }
    }
}
