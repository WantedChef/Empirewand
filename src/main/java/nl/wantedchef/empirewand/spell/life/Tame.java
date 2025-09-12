package nl.wantedchef.empirewand.spell.life;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Tame - Tame animals from real Empirewand
 */
public class Tame extends Spell<Animals> {

    public static class Builder extends Spell.Builder<Animals> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Tame";
            this.description = "Instantly tame an animal";
            this.cooldown = Duration.ofSeconds(10);
            this.spellType = SpellType.LIFE;
        }

        @Override
        @NotNull
        public Spell<Animals> build() {
            return new Tame(this);
        }
    }

    private static final double DEFAULT_RANGE = 20.0;

    private Tame(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "tame";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(3);
    }

    @Override
    protected Animals executeSpell(@NotNull SpellContext context) {
        Player player = context.caster();
        double range = spellConfig.getDouble("values.range", DEFAULT_RANGE);
        
        var target = player.getTargetEntity((int) range);
        if (target instanceof Animals animal && target instanceof Tameable) {
            return animal;
        }
        
        player.sendMessage("§cNo tameable animal found!");
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Animals target) {
        if (target == null) return;
        
        Player player = context.caster();
        
        if (target instanceof Tameable tameable) {
            // Tame the animal
            tameable.setTamed(true);
            tameable.setOwner(player);
            
            // Heal the animal
            if (target instanceof LivingEntity living) {
                living.setHealth(living.getMaxHealth());
            }
            
            // Visual effects
            target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0);
            target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, target.getLocation(), 15, 0.5, 0.5, 0.5, 0);
            
            // Sound effects
            context.fx().playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            
            // Special effects for specific animals
            if (target instanceof Wolf wolf) {
                wolf.setCollarColor(org.bukkit.DyeColor.RED);
                context.fx().playSound(target.getLocation(), Sound.ENTITY_WOLF_WHINE, 1.0f, 1.0f);
            } else if (target instanceof Cat cat) {
                context.fx().playSound(target.getLocation(), Sound.ENTITY_CAT_PURR, 1.0f, 1.0f);
            } else if (target instanceof Horse horse) {
                horse.setJumpStrength(1.0);
                context.fx().playSound(target.getLocation(), Sound.ENTITY_HORSE_AMBIENT, 1.0f, 1.0f);
            }
            
            player.sendMessage("§a§lTamed §2" + target.getType().name().toLowerCase().replace("_", " ") + "!");
        }
    }
}
