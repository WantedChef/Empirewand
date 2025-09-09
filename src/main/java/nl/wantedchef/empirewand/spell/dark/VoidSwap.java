package nl.wantedchef.empirewand.spell.dark;

import nl.wantedchef.empirewand.api.EmpireWandAPI;

import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class VoidSwap extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Void Swap";
            this.description = "Swap positions with a target.";
            this.cooldown = java.time.Duration.ofSeconds(25);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new VoidSwap(this);
        }
    }

    private VoidSwap(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "void-swap";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double range = spellConfig.getDouble("values.range", 15.0);

        var looked = player.getTargetEntity((int) range);
        if (!(looked instanceof LivingEntity target) || target.isDead() || !target.isValid()) {
            context.fx().fizzle(player);
            return null;
        }

        if (player.isInsideVehicle() || target.isInsideVehicle() || !player.getPassengers().isEmpty()
                || !target.getPassengers().isEmpty()) {
            context.fx().fizzle(player);
            return null;
        }

        Location a = player.getLocation();
        Location b = target.getLocation();
        if (!isLocationSafe(a) || !isLocationSafe(b)) {
            context.fx().fizzle(player);
            return null;
        }

        context.fx().spawnParticles(a, Particle.PORTAL, 25, 0.4, 0.8, 0.4, 0.05);
        context.fx().spawnParticles(b, Particle.PORTAL, 25, 0.4, 0.8, 0.4, 0.05);
        context.fx().playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        player.teleport(b);
        target.teleport(a);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    private boolean isLocationSafe(Location location) {
        Block feet = location.getBlock();
        Block head = location.clone().add(0, 1, 0).getBlock();
        Block ground = location.clone().add(0, -1, 0).getBlock();
        return !feet.getType().isSolid() && !head.getType().isSolid() && ground.getType().isSolid();
    }
}
