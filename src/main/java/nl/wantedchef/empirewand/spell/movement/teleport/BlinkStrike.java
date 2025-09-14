package nl.wantedchef.empirewand.spell.movement.teleport;

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
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class BlinkStrike extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Blink Strike";
            this.description = "Teleport behind your target and strike.";
            this.cooldown = java.time.Duration.ofSeconds(12);
            this.spellType = SpellType.MOVEMENT;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new BlinkStrike(this);
        }
    }

    private BlinkStrike(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "blink-strike";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double range = spellConfig.getDouble("values.range", 15.0);
        double behind = spellConfig.getDouble("values.behind-distance", 1.5);
        double damage = spellConfig.getDouble("values.damage", 10.0);

        var looked = player.getTargetEntity((int) range);
        if (!(looked instanceof LivingEntity target) || target.isDead() || !target.isValid()) {
            context.fx().fizzle(player);
            return null;
        }

        Location targetLoc = target.getLocation();
        Vector backDir = targetLoc.getDirection().normalize().multiply(-behind);
        Location blinkLoc = targetLoc.clone().add(backDir);
        blinkLoc.setYaw(targetLoc.getYaw());
        blinkLoc.setPitch(targetLoc.getPitch());

        if (!isLocationSafe(blinkLoc)) {
            context.fx().fizzle(player);
            return null;
        }

        context.fx().spawnParticles(player.getLocation(), Particle.PORTAL, 20, 0.3, 0.3, 0.3, 0.1);
        context.fx().playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        player.teleport(blinkLoc);

        target.damage(damage, player);
        context.fx().spawnParticles(target.getLocation(), Particle.CRIT, 15, 0.2, 0.2, 0.2, 0.01);
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
