package nl.wantedchef.empirewand.spell.heal;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

/**
 * Heals the caster with enhanced visual and sound effects.
 */
public class HealEnhanced extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Enhanced Heal";
            this.description = "Heals the caster with enhanced visual effects.";
            this.cooldown = java.time.Duration.ofSeconds(5);
            this.spellType = SpellType.HEAL;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new HealEnhanced(this);
        }
    }

    private HealEnhanced(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "heal-enhanced";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        double healAmount = spellConfig.getDouble("values.heal-amount", 10.0);
        var maxAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = maxAttr != null ? maxAttr.getValue() : 20.0;

        // Heal the player
        player.setHealth(Math.min(maxHealth, player.getHealth() + healAmount));

        // Start enhanced healing effect
        new HealingEffect(context, player).runTaskTimer(context.plugin(), 0L, 1L);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    /**
     * A runnable that creates enhanced healing effects.
     */
    private static class HealingEffect extends BukkitRunnable {
        private final SpellContext context;
        private final Player player;
        private int ticks = 0;
        private static final int DURATION = 40; // 2 seconds

        HealingEffect(SpellContext context, Player player) {
            this.context = context;
            this.player = player;
        }

        @Override
        public void run() {
            if (ticks >= DURATION || !player.isValid() || player.isDead()) {
                cancel();
                return;
            }

            // Create spiral healing effect
            double radius = 1.0 + Math.sin(ticks * 0.3) * 0.5;
            double height = 0.5 + Math.sin(ticks * 0.5) * 0.3;
            
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i / 8) + (ticks * 0.5);
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                org.bukkit.Location particleLoc = player.getLocation().add(x, height, z);
                
                // Use enchantment rune particles for magical effect
                player.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 1, 0, 0, 0, 0);
                
                // Add some sparkle particles
                if (ticks % 5 == 0) {
                    player.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0.1, 0.1, 0.1, 0);
                }
            }

            // Create rising particles
            if (ticks % 3 == 0) {
                org.bukkit.Location baseLoc = player.getLocation().add(0, 0.1, 0);
                for (int i = 0; i < 5; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double distance = Math.random() * 1.5;
                    double x = distance * Math.cos(angle);
                    double z = distance * Math.sin(angle);
                    org.bukkit.Location particleLoc = baseLoc.clone().add(x, 0, z);
                    player.getWorld().spawnParticle(Particle.HEART, particleLoc, 1, 0, 0.5, 0, 0.05);
                }
            }

            // Sound effects
            if (ticks == 0) {
                // Initial healing sound
                context.fx().playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.5f);
            } else if (ticks == DURATION / 2) {
                // Midpoint sound
                context.fx().playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.8f);
            } else if (ticks == DURATION - 1) {
                // Completion sound
                context.fx().playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
            }

            ticks++;
        }
    }
}