package nl.wantedchef.empirewand.spell.control;

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
 * Levitate - Hard CC spell that holds a target in the air, following the caster's view direction.
 * The effect persists until the spell is recast on the same target or switched to another spell.
 */
public final class Levitate extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /*  DATA                                    */
    /* ---------------------------------------- */
    private final Map<UUID, LevitationData> levitations = new WeakHashMap<>();
    private EmpireWandPlugin plugin;

    /* ---------------------------------------- */
    /*  BUILDER                                 */
    /* ---------------------------------------- */
    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Levitate";
            description = "Hard-CC target in the air, following caster's view direction. Effect stops only when recast or spell is switched.";
            cooldown = Duration.ofSeconds(15);
            spellType = SpellType.CONTROL;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new Levitate(this);
        }
    }

    private Levitate(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /*  SPELL API                               */
    /* ---------------------------------------- */
    @Override
    public String key() {
        return "levitate";
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
            // Recast - toggle off
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
    /*  TOGGLE API                              */
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
        if (levitations.containsKey(targetId)) return; // Already levitating
        
        levitations.put(targetId, new LevitationData(target, caster, context));
    }

    @Override
    public void deactivate(Player player, SpellContext context) {
        // Find and deactivate any levitation by this player
        levitations.entrySet().removeIf(entry -> {
            LevitationData data = entry.getValue();
            if (data.caster.equals(player)) {
                data.stop();
                return true;
            }
            return false;
        });
    }
    
    public void deactivate(LivingEntity target, SpellContext context) {
        Optional.ofNullable(levitations.remove(target.getUniqueId())).ifPresent(LevitationData::stop);
    }

    @Override
    public void forceDeactivate(Player player) {
        deactivate(player, null);
    }
    
    public void forceDeactivate(LivingEntity target) {
        deactivate(target, null);
    }

    /* ---------------------------------------- */
    /*  INTERNAL CLASS                          */
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
            this.offsetPosition = casterLoc.clone().add(direction.multiply(3)).add(right.multiply(1.5)).add(new Vector(0, levitationHeight, 0));
            
            // Send activation message
            caster.sendMessage(Component.text("§b⚡ §7Levitating target: " + target.getName()));
            
            // Start the ticker
            this.ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, 1); // Run every tick
        }

        void stop() {
            ticker.cancel();
            target.setGravity(true);
            caster.sendMessage(Component.text("§b⚡ §7Levitate released: " + target.getName()));
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
            
            // Apply levitation particles
            spawnLevitationParticles();
        }

        private void spawnLevitationParticles() {
            Location loc = target.getLocation().add(0, 1, 0);
            World world = target.getWorld();
            
            // Create enchantment particles around the target
            world.spawnParticle(Particle.ENCHANT, loc, 5, 0.5, 0.5, 0.5, 0.1);
            
            // Create cloud particles
            world.spawnParticle(Particle.CLOUD, loc, 3, 0.3, 0.3, 0.3, 0.05);
            
            // Create end rod particles for magical effect
            world.spawnParticle(Particle.END_ROD, loc, 2, 0.2, 0.2, 0.2, 0.02);
        }
    }
}