package nl.wantedchef.empirewand.spell.defensive;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.attribute.Attribute;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

import java.time.Duration;

/**
 * Absorb - Absorb damage and convert to health from real Empirewand
 */
public class Absorb extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Absorb";
            this.description = "Absorb incoming damage and convert it to health";
            this.cooldown = Duration.ofSeconds(40);
            this.spellType = SpellType.MISC;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Absorb(this);
        }
    }

    private static final int DEFAULT_DURATION = 120;
    private static final int DEFAULT_ABSORPTION_LEVEL = 3;

    private Absorb(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "absorb";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        int duration = spellConfig.getInt("values.duration", DEFAULT_DURATION);
        int absorptionLevel = spellConfig.getInt("values.absorption_level", DEFAULT_ABSORPTION_LEVEL);
        
        // Apply absorption effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration, absorptionLevel));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 0));
        
        // Visual absorption field
        new BukkitRunnable() {
            int ticks = 0;
            double lastHealth = player.getHealth();
            double absorbedDamage = 0;
            final org.bukkit.World world = Objects.requireNonNull(player.getWorld(), "world");
            
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    player.sendMessage("§7Absorption field has dissipated. Total absorbed: " + String.format("%.1f", absorbedDamage) + " damage!");
                    cancel();
                    return;
                }
                
                // Absorption field particles
                double time = ticks * 0.1;
                final var baseNow = Objects.requireNonNull(player.getLocation(), "location");
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 6) {
                    double radius = 1.5 + Math.sin(time) * 0.3;
                    double x = Math.cos(angle + time) * radius;
                    double z = Math.sin(angle + time) * radius;
                    double y = Math.sin(time * 2) * 0.5 + 1;

                    final var loc = baseNow.clone().add(x, y, z);
                    world.spawnParticle(Particle.DUST,
                        loc, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(org.bukkit.Color.YELLOW, 1.0f));
                }
                
                // Check if damage was absorbed
                double currentHealth = player.getHealth();
                if (currentHealth < lastHealth) {
                    double damageAbsorbed = lastHealth - currentHealth;
                    absorbedDamage += damageAbsorbed;
                    
                    // Convert some damage to healing
                    double max = getMaxHealth(player);
                    player.setHealth(Math.min(currentHealth + damageAbsorbed * 0.5, max));
                    
                    // Absorption effect
                    final var head = baseNow.clone().add(0, 1, 0);
                    world.spawnParticle(Particle.HEART,
                        head, 5, 0.3, 0.3, 0.3, 0);
                    world.spawnParticle(Particle.HAPPY_VILLAGER,
                        baseNow, 10, 0.5, 0.5, 0.5, 0);
                    context.fx().playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                }
                
                // Pulsing effect
                if (ticks % 20 == 0) {
                    final var base = baseNow;
                    world.spawnParticle(Particle.END_ROD,
                        base, 20, 1, 1, 1, 0.05);
                    final var head = baseNow.clone().add(0, 1, 0);
                    world.spawnParticle(Particle.TOTEM_OF_UNDYING,
                        head, 5, 0.3, 0.3, 0.3, 0.02);
                }
                
                lastHealth = player.getHealth();
                ticks += 5;
            }
        }.runTaskTimer(context.plugin(), 0L, 5L);
        
        // Initial effects
        {
            final var world0 = Objects.requireNonNull(player.getWorld(), "world");
            final var base0 = Objects.requireNonNull(player.getLocation(), "location");
            world0.spawnParticle(Particle.TOTEM_OF_UNDYING,
                base0, 50, 1, 1, 1, 0.1);
        }
        context.fx().playSound(player, Sound.ITEM_TOTEM_USE, 0.8f, 1.5f);
        context.fx().playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.8f);
        
        player.sendMessage("§e§lAbsorb §6field activated for " + (duration/20) + " seconds!");
    }
    
    private double getMaxHealth(org.bukkit.entity.LivingEntity entity) {
        var attr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }
}
