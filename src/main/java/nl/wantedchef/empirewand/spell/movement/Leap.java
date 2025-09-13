package nl.wantedchef.empirewand.spell.movement;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Leap extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Leap";
            this.description = "Leaps you forward.";
            this.cooldown = java.time.Duration.ofSeconds(2);
            this.spellType = SpellType.MOVEMENT;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new Leap(this);
        }
    }

    private Leap(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "leap";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        
        // Track consecutive leaps (max 3)
        String leapCountKey = "leap_consecutive_count";
        int consecutiveLeaps = player.getMetadata(leapCountKey).stream()
            .filter(meta -> meta.getOwningPlugin() == context.plugin())
            .mapToInt(meta -> meta.asInt())
            .findFirst()
            .orElse(0);
        
        if (consecutiveLeaps >= 3) {
            context.fx().fizzle(player);
            player.sendMessage("§cYou cannot leap again so soon!");
            return null;
        }
        
        double multiplier = spellConfig.getDouble("values.velocity-multiplier", 1.5);
        double verticalBoost = spellConfig.getDouble("values.vertical-boost", 1.0);

        player.setVelocity(player.getLocation().getDirection().normalize().multiply(multiplier).setY(verticalBoost));

        // Update consecutive leap count
        player.setMetadata(leapCountKey, new org.bukkit.metadata.FixedMetadataValue(context.plugin(), consecutiveLeaps + 1));
        
        // Reset count after 5 seconds
        context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
            int currentCount = player.getMetadata(leapCountKey).stream()
                .filter(meta -> meta.getOwningPlugin() == context.plugin())
                .mapToInt(meta -> meta.asInt())
                .findFirst()
                .orElse(0);
            if (currentCount > 0) {
                player.setMetadata(leapCountKey, new org.bukkit.metadata.FixedMetadataValue(context.plugin(), currentCount - 1));
            }
        }, 100L); // 5 seconds

        context.fx().spawnParticles(player.getLocation(), Particle.CLOUD, 16, 0.3, 0.1, 0.3, 0.02);
        context.fx().playSound(player, Sound.ENTITY_RABBIT_JUMP, 0.8f, 1.2f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }
}
