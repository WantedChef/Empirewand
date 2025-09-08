package com.example.empirewand.spell.implementation.control;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.EffectService;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class Polymorph extends Spell<Void> {

    private final Map<UUID, UUID> polymorphedEntities = new HashMap<>();

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Polymorph";
            this.description = "Transforms a target entity into a sheep.";
            this.manaCost = 12;
            this.cooldown = java.time.Duration.ofSeconds(25);
            this.spellType = SpellType.CONTROL;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new Polymorph(this);
        }
    }

    private Polymorph(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "polymorph";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        int durationTicks = spellConfig.getInt("values.duration-ticks", 100);

        Entity looked = player.getTargetEntity(12);
        if (!(looked instanceof LivingEntity target) || target instanceof Player || !target.isValid() || target.isDead()) {
            context.fx().fizzle(player);
            return null;
        }

        target.setAI(false);
        target.setInvulnerable(true);
        target.setInvisible(true);
        target.setCollidable(false);

        context.fx().spawnParticles(target.getLocation().add(0, 1.0, 0), Particle.CLOUD, 12, 0.4, 0.4, 0.4, 0.01);
        context.fx().playSound(target.getLocation(), Sound.ENTITY_SHEEP_AMBIENT, 0.8f, 1.2f);

        Sheep sheep = (Sheep) target.getWorld().spawnEntity(target.getLocation(), EntityType.SHEEP);
        sheep.customName(Component.text(context.plugin().getTextService().getMessage("polymorph-name")));
        sheep.setCustomNameVisible(true);

        polymorphedEntities.put(sheep.getUniqueId(), target.getUniqueId());

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(durationTicks * 50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            UUID originalId = polymorphedEntities.remove(sheep.getUniqueId());
            if (originalId == null) {
                return;
            }

            if (sheep.isValid() && !sheep.isDead()) {
                sheep.remove();
            }

            Entity originalEntity = Bukkit.getEntity(originalId);
            if (originalEntity instanceof LivingEntity originalLivingEntity && originalLivingEntity.isValid() && !originalLivingEntity.isDead()) {
                originalLivingEntity.setAI(true);
                originalLivingEntity.setInvulnerable(false);
                originalLivingEntity.setInvisible(false);
                originalLivingEntity.setCollidable(true);
                context.fx().spawnParticles(originalLivingEntity.getLocation().add(0, 1.0, 0), Particle.CLOUD, 10, 0.3, 0.3, 0.3, 0.01);
            }
        });

        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    public Map<UUID, UUID> getPolymorphedEntities() {
        return Collections.unmodifiableMap(polymorphedEntities);
    }

    public void cleanup() {
        for (Map.Entry<UUID, UUID> entry : new HashMap<>(polymorphedEntities).entrySet()) {
            UUID sheepId = entry.getKey();
            UUID originalId = entry.getValue();

            Entity sheep = Bukkit.getEntity(sheepId);
            if (sheep != null && sheep.isValid()) {
                sheep.remove();
            }

            Entity original = Bukkit.getEntity(originalId);
            if (original instanceof LivingEntity living && living.isValid() && !living.isDead()) {
                living.setAI(true);
                living.setInvulnerable(false);
                living.setInvisible(false);
                living.setCollidable(true);
            }
        }
        polymorphedEntities.clear();
    }
}