package nl.wantedchef.empirewand.spell.offensive;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Shuriken - Throw magical shurikens from real Empirewand
 */
public class Shuriken extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Shuriken";
            this.description = "Throw magical shurikens that pierce through enemies";
            this.cooldown = Duration.ofSeconds(8);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Shuriken(this);
        }
    }

    private static final int DEFAULT_SHURIKEN_COUNT = 5;
    private static final double DEFAULT_DAMAGE = 8.0;
    private static final double DEFAULT_RANGE = 20.0;
    private static final double DEFAULT_SPEED = 2.0;

    private Shuriken(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "shuriken";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(10);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }
    
    private Vector rotateVector(Vector vector, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        return new Vector(x, vector.getY(), z);
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        int shurikenCount = spellConfig.getInt("values.shuriken_count", DEFAULT_SHURIKEN_COUNT);
        double damage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
        double range = spellConfig.getDouble("values.range", DEFAULT_RANGE);
        double speed = spellConfig.getDouble("values.speed", DEFAULT_SPEED);
        
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection();
        
        // Launch shurikens in a spread pattern
        for (int i = 0; i < shurikenCount; i++) {
            final int index = i;
            context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                // Calculate spread angle
                double spreadAngle = (index - shurikenCount/2.0) * 0.1;
                Vector spreadDir = rotateVector(direction.clone(), spreadAngle);
                
                // Create shuriken (invisible armor stand with item)
                ArmorStand shuriken = player.getWorld().spawn(start.clone().add(spreadDir.clone().multiply(0.5)), ArmorStand.class);
                shuriken.setInvisible(true);
                shuriken.setGravity(false);
                shuriken.setInvulnerable(true);
                shuriken.setSmall(true);
                shuriken.setMarker(true);
                shuriken.getEquipment().setHelmet(new ItemStack(Material.NETHER_STAR));
                shuriken.setHeadPose(new EulerAngle(0, 0, 0));
                
                // Launch shuriken
                new BukkitRunnable() {
                    double traveled = 0;
                    int rotation = 0;
                    
                    @Override
                    public void run() {
                        if (traveled >= range || !shuriken.isValid()) {
                            shuriken.remove();
                            cancel();
                            return;
                        }
                        
                        // Move shuriken
                        Location newLoc = shuriken.getLocation().add(spreadDir.clone().multiply(speed));
                        shuriken.teleport(newLoc);
                        
                        // Rotate shuriken
                        rotation += 30;
                        shuriken.setHeadPose(new EulerAngle(0, Math.toRadians(rotation), 0));
                        
                        // Particle trail
                        shuriken.getWorld().spawnParticle(Particle.CRIT, shuriken.getLocation(), 2, 0.05, 0.05, 0.05, 0);
                        shuriken.getWorld().spawnParticle(Particle.SWEEP_ATTACK, shuriken.getLocation(), 1, 0, 0, 0, 0);
                        
                        // Damage nearby entities
                        for (var entity : shuriken.getWorld().getNearbyEntities(shuriken.getLocation(), 1, 1, 1)) {
                            if (entity instanceof LivingEntity living && !living.equals(player) && !living.equals(shuriken)) {
                                living.damage(damage, player);
                                living.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, living.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.05);
                                context.fx().playSound(living.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.5f);
                            }
                        }
                        
                        traveled += speed;
                    }
                }.runTaskTimer(context.plugin(), 0L, 1L);
                
                // Launch sound
                context.fx().playSound(start, Sound.ENTITY_ARROW_SHOOT, 1.0f, 2.0f);
                context.fx().playSound(start, Sound.ITEM_TRIDENT_THROW, 0.8f, 1.5f);
            }, i * 2L);
        }
        
        // Visual effect
        start.getWorld().spawnParticle(Particle.FLASH, start, 1, 0, 0, 0, 0);
        
        player.sendMessage("§7§lShuriken §8barrage launched!");
    }
}
