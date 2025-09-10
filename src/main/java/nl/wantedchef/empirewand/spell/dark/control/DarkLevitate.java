package nl.wantedchef.empirewand.spell.dark.control;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.base.PrereqInterface;
import nl.wantedchef.empirewand.spell.base.Spell;
import nl.wantedchef.empirewand.spell.base.SpellContext;
import nl.wantedchef.empirewand.spell.base.SpellType;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.core.EmpireWandPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * DarkLevitate - Mechanically identical to Levitate but with dark particles and
 * an extra "throw" impulse
 * when released for aggressive spacing in PvP.
 */
public final class DarkLevitate extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /* DATA */
    /* ---------------------------------------- */
    private final Map<UUID, LevitationData> levitations = new WeakHashMap<>();
    private EmpireWandPlugin plugin;

    /* ---------------------------------------- */
    /* BUILDER */
    /* ---------------------------------------- */
    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Dark Levitate";
            description = "Dark version of Levitate with extra throw impulse on release. Effect stops only when recast or spell is switched.";
            cooldown = Duration.ofSeconds(15);
            spellType = SpellType.DARK;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new DarkLevitate(this);
        }
    }

    private DarkLevitate(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /* SPELL API */
    /* ---------------------------------------- */
    @Override
    public String key() {
        return "dark-levitate";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player caster = context.caster();
        double range = spellConfig.getDouble("values.range", 15.0);

        // Get target entity
        Entity targetEntity = caster.getTargetEntity((int) range);
        if (!(targetEntity instanceof LivingEntity)) {
            context.fx().showError(caster, "no-target");
            return null;
        }

        LivingEntity target = (LivingEntity) targetEntity;

        // Check if target is already levitated by this caster
        UUID targetId = target.getUniqueId();
        if (levitations.containsKey(targetId)) {
            // Recast - toggle off with throw impulse
            deactivate(target, context);
        } else {
            // New target - activate
            plugin = context.plugin();
            activate(target, caster, context);
        }

        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect
    }

    /* ---------------------------------------- */
    /* TOGGLE API */
    /* ---------------------------------------- */
    @Override
    public boolean isActive(Player player) {
        // For this spell, we check if any target is being levitated by this player
        return levitations.values().stream().anyMatch(data -> data.caster.equals(player));
    }

    public boolean isTargetLevitated(LivingEntity target) {
        return levitations.containsKey(target.getUniqueId());
    }

    @Override
    public void activate(Player player, SpellContext context) {
        // This method is for player-based toggle spells
        // For target-based spells, we use the overloaded activate method
    }

    public void activate(LivingEntity target, Player caster, SpellContext context) {
        UUID targetId = target.getUniqueId();
        if (levitations.containsKey(targetId))
            return; // Already levitating

        levitations.put(targetId, new LevitationData(target, caster, context));
    }

    @Override
    public void deactivate(Player player, SpellContext context) {
        // Find and deactivate any levitation by this player with throw impulse
        levitations.entrySet().removeIf(entry -> {
            LevitationData data = entry.getValue();
            if (data.caster.equals(player)) {
                data.stopWithThrow();
                return true;
            }
            return false;
        });
    }

    public void deactivate(LivingEntity target, SpellContext context) {
        Optional.ofNullable(levitations.remove(target.getUniqueId())).ifPresent(LevitationData::stopWithThrow);
    }

    @Override
    public void forceDeactivate(Player player) {
        deactivate(player, null);
    }

    public void forceDeactivate(LivingEntity target) {
        deactivate(target, null);
    }

    /* ---------------------------------------- */
    /* INTERNAL CLASS */
    /* ---------------------------------------- */
    private final class LevitationData {
        private final LivingEntity target;
        private final Player caster;
        private final BukkitTask ticker;
        private final Location offsetPosition;
        private final double levitationHeight;

        LevitationData(LivingEntity target, Player caster, SpellContext context) {
            this.target = target;
            this.caster = caster;

            // Calculate offset position relative to caster's view direction
            Location casterLoc = caster.getLocation();
            Location targetLoc = target.getLocation();

            // Set levitation height above ground
            this.levitationHeight = 3.0;

            // Store offset from caster to target
            Vector direction = casterLoc.getDirection();
            Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
            Vector up = right.clone().crossProduct(direction).normalize();

            // Position target in front and slightly to the right of caster
            this.offsetPosition = casterLoc.clone().add(direction.multiply(3)).add(right.multiply(1.5))
                    .add(new Vector(0, levitationHeight, 0));

            // Send activation message
            caster.sendMessage(Component.text("§5⚡ §7Dark Levitating target: " + target.getName()));

            // Start the ticker
            this.ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, 1); // Run every tick
        }

        void stopWithThrow() {
            ticker.cancel();
            target.setGravity(true);

            // Apply throw impulse in caster's direction
            Vector throwDirection = caster.getLocation().getDirection();
            double throwPower = spellConfig.getDouble("values.throw-power", 1.5);
            target.setVelocity(throwDirection.multiply(throwPower).setY(0.5));

            // Spawn dark particles for the throw effect
            spawnThrowParticles();

            caster.sendMessage(Component.text("§5⚡ §7Dark Levitate released: " + target.getName()));
        }

        private void tick() {
            if (!target.isValid() || target.isDead() || !caster.isOnline()) {
                forceDeactivate(target);
                return;
            }

            // Update target position based on caster's view direction
            Location casterLoc = caster.getLocation();
            Vector direction = casterLoc.getDirection();
            Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
            Vector up = right.clone().crossProduct(direction).normalize();

            // Calculate new position
            Location newPosition = casterLoc.clone()
                    .add(direction.multiply(3))
                    .add(right.multiply(1.5))
                    .add(new Vector(0, levitationHeight, 0));

            // Teleport target to new position
            target.teleport(newPosition);

            // Disable gravity so target stays in position
            target.setGravity(false);

            // Apply dark levitation particles
            spawnDarkLevitationParticles();
        }

        private void spawnDarkLevitationParticles() {
            Location loc = target.getLocation().add(0, 1, 0);
            World world = target.getWorld();

            // Create smoke particles around the target
            world.spawnParticle(Particle.SMOKE, loc, 5, 0.5, 0.5, 0.5, 0.05);

            // Create soul fire particles
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 3, 0.3, 0.3, 0.3, 0.02);

            // Create ash particles for dark effect
            world.spawnParticle(Particle.ASH, loc, 2, 0.2, 0.2, 0.2, 0.01);

            // Create dripstone particles for dark magic
            world.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, loc, 2, 0.3, 0.3, 0.3, 0.01);
        }

        private void spawnThrowParticles() {
            Location loc = target.getLocation().add(0, 1, 0);
            World world = target.getWorld();

            // Create explosion particles
            world.spawnParticle(Particle.EXPLOSION, loc, 10, 0.5, 0.5, 0.5, 0.1);

            // Create smoke particles
            world.spawnParticle(Particle.SMOKE, loc, 15, 0.8, 0.8, 0.8, 0.05);

            // Create soul fire particles
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 8, 0.5, 0.5, 0.5, 0.03);
        }
    }
}