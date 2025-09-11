package nl.wantedchef.empirewand.spell.earth;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Sandblast - Hurls sand at enemies from real Empirewand
 */
public class Sandblast extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Sandblast";
            this.description = "Blast your enemies with a torrent of sand";
            this.cooldown = Duration.ofSeconds(8);
            this.spellType = SpellType.EARTH;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Sandblast(this);
        }
    }

    private static final double DEFAULT_DAMAGE = 8.0;
    private static final double DEFAULT_RANGE = 20.0;
    private static final int DEFAULT_SAND_COUNT = 15;

    private Sandblast(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "sandblast";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(8);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        double damage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
        double range = spellConfig.getDouble("values.range", DEFAULT_RANGE);
        int sandCount = spellConfig.getInt("values.sand_count", DEFAULT_SAND_COUNT);
        
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection();
        
        // Launch sand blocks
        for (int i = 0; i < sandCount; i++) {
            Vector velocity = direction.clone().add(new Vector(
                (Math.random() - 0.5) * 0.3,
                (Math.random() - 0.5) * 0.3,
                (Math.random() - 0.5) * 0.3
            )).multiply(1.5);
            
            FallingBlock sand = player.getWorld().spawnFallingBlock(start, Material.SAND.createBlockData());
            sand.setVelocity(velocity);
            sand.setDropItem(false);
            sand.setHurtEntities(true);
        }
        
        // Particle and sound effects
        start.getWorld().spawnParticle(Particle.DUST, start, 50, 0.5, 0.5, 0.5, 0.1,
            new Particle.DustOptions(org.bukkit.Color.fromRGB(194, 178, 128), 2.0f));
        context.fx().playSound(start, Sound.BLOCK_SAND_BREAK, 2.0f, 0.8f);
        context.fx().playSound(start, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.5f, 1.0f);
        
        // Check for hits
        for (var entity : start.getWorld().getNearbyEntities(start, range, range, range)) {
            if (entity instanceof LivingEntity living && !living.equals(player)) {
                Vector toTarget = living.getLocation().toVector().subtract(start.toVector());
                if (toTarget.normalize().dot(direction) > 0.5 && toTarget.length() <= range) {
                    living.damage(damage, player);
                    living.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.BLINDNESS, 60, 0));
                }
            }
        }
        
        player.sendMessage("§e§lSandblast §6launched!");
    }
}
