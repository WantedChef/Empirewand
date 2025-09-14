package nl.wantedchef.empirewand.spell.defensive;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Reflect - Reflect projectiles and damage from real Empirewand
 */
public class Reflect extends Spell<Player> {

    private static final Set<UUID> reflectingPlayers = new HashSet<>();

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Reflect";
            this.description = "Reflect incoming projectiles and damage back to attackers";
            this.cooldown = Duration.ofSeconds(35);
            this.spellType = SpellType.MISC;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Reflect(this);
        }
    }

    private static final int DEFAULT_DURATION = 150;
    private static final double DEFAULT_REFLECT_CHANCE = 0.75;

    private Reflect(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "reflect";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }
    
    public static boolean isReflecting(Player player) {
        return reflectingPlayers.contains(player.getUniqueId());
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        int duration = spellConfig.getInt("values.duration", DEFAULT_DURATION);
        double reflectChance = spellConfig.getDouble("values.reflect_chance", DEFAULT_REFLECT_CHANCE);
        
        // Add player to reflecting set
        reflectingPlayers.add(player.getUniqueId());
        
        // Visual effect - mirror shield
        new BukkitRunnable() {
            int ticks = 0;
            final org.bukkit.World world = Objects.requireNonNull(player.getWorld(), "world");
            
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    reflectingPlayers.remove(player.getUniqueId());
                    player.sendMessage("§7Reflect shield has dissipated.");
                    cancel();
                    return;
                }
                final var baseNow = Objects.requireNonNull(player.getLocation(), "location");
                
                // Mirror shield particles
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    for (double y = 0; y <= 2; y += 0.5) {
                        double radius = 1.5 - y * 0.2;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        
                        if (Math.random() < 0.3) {
                            final var loc = baseNow.clone().add(x, y, z);
                            world.spawnParticle(Particle.END_ROD,
                                loc, 1, 0, 0, 0, 0);
                        }
                    }
                }
                
                // Reflection gleam
                if (ticks % 20 == 0) {
                    final var head = baseNow.clone().add(0, 1, 0);
                    world.spawnParticle(Particle.FLASH,
                        head, 1, 0, 0, 0, 0);
                    final var base = baseNow;
                    world.spawnParticle(Particle.ENCHANT,
                        base, 15, 1, 1, 1, 0.05);
                }
                
                // Check for nearby projectiles to reflect
                player.getNearbyEntities(3, 3, 3).forEach(entity -> {
                    if (entity instanceof Projectile projectile && Math.random() < reflectChance) {
                        final var shooter = projectile.getShooter();
                        if (!(shooter instanceof Player shooterPlayer && shooterPlayer.equals(player))) {
                            // Reflect the projectile
                            projectile.setVelocity(projectile.getVelocity().multiply(-1.5));
                            projectile.setShooter(player);
                            
                            // Visual effect
                            projectile.getWorld().spawnParticle(Particle.FLASH, 
                                projectile.getLocation(), 1, 0, 0, 0, 0);
                            context.fx().playSound(projectile.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 2.0f);
                        }
                    }
                });
                
                ticks += 5;
            }
        }.runTaskTimer(context.plugin(), 0L, 5L);
        
        // Initial effects
        {
            final var world = Objects.requireNonNull(player.getWorld(), "world");
            final var base = Objects.requireNonNull(player.getLocation(), "location");
            world.spawnParticle(Particle.FIREWORK,
                base, 50, 1, 1, 1, 0.1);
        }
        context.fx().playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);
        context.fx().playSound(player, Sound.ITEM_SHIELD_BLOCK, 1.5f, 1.0f);
        
        player.sendMessage("§e§lReflect §6shield activated for " + (duration/20) + " seconds!");
    }
}
