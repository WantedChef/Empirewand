package nl.wantedchef.empirewand.spell.heal;

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
import org.bukkit.attribute.Attribute;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Prayer - Heal yourself from real Empirewand
 */
public class Prayer extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Prayer";
            this.description = "Heal yourself through divine prayer";
            this.cooldown = Duration.ofSeconds(8);
            this.spellType = SpellType.HEAL;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Prayer(this);
        }
    }

    private static final double DEFAULT_HEAL_AMOUNT = 10.0;
    private static final boolean DEFAULT_REMOVE_EFFECTS = true;

    private Prayer(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "prayer";
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
        double healAmount = spellConfig.getDouble("values.heal_amount", DEFAULT_HEAL_AMOUNT);
        boolean removeEffects = spellConfig.getBoolean("flags.remove_effects", DEFAULT_REMOVE_EFFECTS);
        
        // Heal the player
        double currentHealth = player.getHealth();
        double maxHealth = getMaxHealth(player);
        player.setHealth(Math.min(currentHealth + healAmount, maxHealth));
        
        // Remove negative effects
        if (removeEffects) {
            player.removePotionEffect(PotionEffectType.POISON);
            player.removePotionEffect(PotionEffectType.WITHER);
            player.removePotionEffect(PotionEffectType.WEAKNESS);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        }
        
        // Add absorption
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 0));
        
        // Visual effects
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, 
            player.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.END_ROD, 
            player.getLocation().add(0, 2, 0), 20, 0.3, 0.3, 0.3, 0.05);
        
        // Sound effects
        context.fx().playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        context.fx().playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.2f);
        
        player.sendMessage("§e§lPrayer §6healed you for " + String.format("%.1f", healAmount) + " health!");
    }

    private double getMaxHealth(org.bukkit.entity.LivingEntity entity) {
        var attr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }
}
