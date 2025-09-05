package com.example.empirewand.spell.implementation;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public class Polymorph implements Spell {

    // Map polymorphed sheep UUID -> original entity UUID
    private final HashMap<UUID, UUID> polymorphedEntities = new HashMap<>();

    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Configurable duration with safe default (5s)
        int durationTicks = context.config().getSpellsConfig().getInt("polymorph.values.duration-ticks", 100);

        Entity looked = player.getTargetEntity(12);
        if (!(looked instanceof LivingEntity target) || target instanceof Player || !target.isValid() || target.isDead()) {
            context.fx().fizzle(player);
            return;
        }

        // Soften and hide the original entity
        target.setAI(false);
        target.setInvulnerable(true); // Can't be damaged while polymorphed
        target.setInvisible(true);
        target.setCollidable(false);

        // Visual cue
        context.fx().spawnParticles(target.getLocation().add(0, 1.0, 0), Particle.CLOUD, 12, 0.4, 0.4, 0.4, 0.01);
        context.fx().playSound(target.getLocation(), Sound.ENTITY_SHEEP_AMBIENT, 0.8f, 1.2f);

        Sheep sheep = (Sheep) target.getWorld().spawnEntity(target.getLocation(), EntityType.SHEEP);
        sheep.customName(Component.text("Polymorphed"));
        sheep.setCustomNameVisible(true);

        polymorphedEntities.put(sheep.getUniqueId(), target.getUniqueId());

        new BukkitRunnable() {
            @Override
            public void run() {
                // If the sheep is gone or the mapping was cleared (e.g., sheep died -> original killed), do nothing
                UUID originalId = polymorphedEntities.get(sheep.getUniqueId());
                if (originalId == null) {
                    return;
                }

                // Normal expiry: remove sheep and restore original
                if (sheep.isValid() && !sheep.isDead()) {
                    sheep.remove();
                }

                // Restore original if it still exists
                if (target.isValid() && !target.isDead()) {
                    target.setAI(true);
                    target.setInvulnerable(false);
                    target.setInvisible(false);
                    target.setCollidable(true);
                    context.fx().spawnParticles(target.getLocation().add(0, 1.0, 0), Particle.CLOUD, 10, 0.3, 0.3, 0.3, 0.01);
                }

                polymorphedEntities.remove(sheep.getUniqueId());
            }
        }.runTaskLater(context.plugin(), Math.max(1L, durationTicks));
    }

    @Override
    public String getName() {
        return "polymorph";
    }

    public HashMap<UUID, UUID> getPolymorphedEntities() {
        return polymorphedEntities;
    }
}
