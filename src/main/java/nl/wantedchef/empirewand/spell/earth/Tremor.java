package nl.wantedchef.empirewand.spell.earth;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Tremor - Earth shaking spell
 */
public class Tremor extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Tremor";
            this.description = "Shake the earth beneath your enemies";
            this.cooldown = Duration.ofSeconds(12);
            this.spellType = SpellType.EARTH;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Tremor(this);
        }
    }

    private static final double DEFAULT_RADIUS = 8.0;
    private static final double DEFAULT_DAMAGE = 15.0;
    private static final double DEFAULT_KNOCKUP = 1.2;

    private Tremor(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "tremor";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(15);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        double radius = spellConfig.getDouble("values.radius", DEFAULT_RADIUS);
        double damage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
        double knockup = spellConfig.getDouble("values.knockup", DEFAULT_KNOCKUP);
        
        Location center = player.getLocation();
        
        // Create tremor effect
        for (double r = 1; r <= radius; r += 1) {
            final double currentRadius = r;
            context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                // Particle ring
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                    double x = Math.cos(angle) * currentRadius;
                    double z = Math.sin(angle) * currentRadius;
                    Location loc = center.clone().add(x, 0, z);
                    
                    loc.getWorld().spawnParticle(Particle.DUST, loc, 5, 0.1, 0.1, 0.1, 0,
                        new Particle.DustOptions(org.bukkit.Color.fromRGB(139, 90, 43), 3.0f));
                    loc.getWorld().spawnParticle(Particle.DUST, loc, 3, 0.1, 0.1, 0.1, 0,
                        org.bukkit.Material.DIRT.createBlockData());
                    loc.getWorld().spawnParticle(Particle.CRIT, loc, 2, 0.2, 0.2, 0.2, 0.1);
                }
                
                // Damage and knock up entities in ring
                int entitiesHit = 0;
                for (var entity : center.getWorld().getNearbyEntities(center, currentRadius, 3, currentRadius)) {
                    if (entity instanceof LivingEntity living && !living.equals(player)) {
                        double distance = living.getLocation().distance(center);
                        if (Math.abs(distance - currentRadius) < 1.5) {
                            living.damage(damage, player);
                            living.setVelocity(new Vector(0, knockup, 0));
                            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                            entitiesHit++;
                            
                            // Visual feedback for hit
                            living.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, living.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);
                        }
                    }
                }
                
                if (entitiesHit > 0) {
                    player.sendMessage("§6Tremor hit " + entitiesHit + " entities at radius " + String.format("%.1f", currentRadius));
                }
                
                context.fx().playSound(center, Sound.BLOCK_STONE_BREAK, 1.5f, 0.5f + (float)(currentRadius / radius) * 0.5f);
            }, (long)(r * 2));
        }
        
        context.fx().playSound(center, Sound.ENTITY_IRON_GOLEM_STEP, 2.0f, 0.5f);
        player.sendMessage("§6§lTremor §eshaking the ground!");
    }
}
