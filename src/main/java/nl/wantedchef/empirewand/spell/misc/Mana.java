package nl.wantedchef.empirewand.spell.misc;

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
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Mana - Restores mana/energy from real Empirewand
 */
public class Mana extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Mana";
            this.description = "Restore your magical energy and reduce cooldowns";
            this.cooldown = Duration.ofSeconds(60);
            this.spellType = SpellType.MISC;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Mana(this);
        }
    }

    private static final int DEFAULT_FOOD_RESTORE = 20;
    private static final float DEFAULT_SATURATION = 20.0f;
    private static final int DEFAULT_XP_LEVELS = 5;

    private Mana(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "mana";
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
        int foodRestore = spellConfig.getInt("mana.values.food_restore", DEFAULT_FOOD_RESTORE);
        float saturation = (float) spellConfig.getDouble("mana.values.saturation", DEFAULT_SATURATION);
        int xpLevels = spellConfig.getInt("mana.values.xp_levels", DEFAULT_XP_LEVELS);
        
        // Restore "mana" (represented by food, saturation, and XP)
        player.setFoodLevel(Math.min(player.getFoodLevel() + foodRestore, 20));
        player.setSaturation(saturation);
        player.setExhaustion(0);
        player.giveExpLevels(xpLevels);
        
        // Clear negative effects that drain energy
        player.removePotionEffect(PotionEffectType.HUNGER);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        
        // Add energy boost
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 200, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 200, 0));
        
        // Visual effects - mana restoration
        for (int i = 0; i < 3; i++) {
            final int wave = i;
            context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                // Rising mana particles
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    double radius = 0.5;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    double y = wave * 0.5;
                    
                    player.getWorld().spawnParticle(Particle.ENCHANT, 
                        player.getLocation().add(x, y, z), 3, 0.1, 0.1, 0.1, 0);
                    player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, 
                        player.getLocation().add(x, y, z), 1, 0.1, 0.1, 0.1, 0.02);
                }
                
                context.fx().playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f + wave * 0.2f);
            }, i * 5L);
        }
        
        // Main effects
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, 
            player.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.05);
        player.getWorld().spawnParticle(Particle.ENCHANT, 
            player.getLocation(), 50, 1, 1, 1, 0.1);
        
        context.fx().playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        context.fx().playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.2f);
        
        player.sendMessage("§b§lMana §3restored! +" + xpLevels + " experience levels!");
    }
}
