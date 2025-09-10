package nl.wantedchef.empirewand.spell.heal;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LifeReap extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Life Reap";
            this.description = "Damages enemies in a cone and heals you for each enemy hit.";
            this.cooldown = java.time.Duration.ofSeconds(10);
            this.spellType = SpellType.LIFE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new LifeReap(this);
        }
    }

    private LifeReap(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "life-reap";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Levenszuiger");
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double damage = spellConfig.getDouble("values.damage", 4.0);
        double healPerTarget = spellConfig.getDouble("values.heal-per-target", 0.8);
        double range = spellConfig.getDouble("values.range", 5.0);
        double angleDegrees = spellConfig.getDouble("values.angle-degrees", 120.0);
        boolean hitPlayers = spellConfig.getBoolean("flags.hit-players", true);
        boolean hitMobs = spellConfig.getBoolean("flags.hit-mobs", true);

        List<LivingEntity> targets = getEntitiesInCone(player, range, angleDegrees);
        targets.removeIf(
                entity -> (entity instanceof Player && !hitPlayers) || (!(entity instanceof Player) && !hitMobs));

        if (targets.isEmpty()) {
            context.fx().fizzle(player);
            return null;
        }

        double totalHeal = 0.0;
        for (LivingEntity target : targets) {
            target.damage(damage, player);
            totalHeal += healPerTarget;
        }

        double maxHealth = 20.0; // Default max health
        var maxHealthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealth = maxHealthAttr.getValue();
        }
        player.setHealth(Math.min(maxHealth, player.getHealth() + totalHeal));

        context.fx().playSound(player, Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.8f);
        spawnSweepParticles(player);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    private List<LivingEntity> getEntitiesInCone(Player player, double range, double angleDegrees) {
        List<LivingEntity> targets = new ArrayList<>();
        Vector playerDir = player.getEyeLocation().getDirection().normalize();
        double angleRadians = Math.toRadians(angleDegrees / 2.0);

        for (LivingEntity entity : player.getWorld().getLivingEntities()) {
            if (entity.equals(player) || entity.isDead() || !entity.isValid())
                continue;
            if (entity.getLocation().distanceSquared(player.getLocation()) > range * range)
                continue;

            Vector toEntity = entity.getEyeLocation().toVector().subtract(player.getEyeLocation().toVector())
                    .normalize();
            if (playerDir.angle(toEntity) <= angleRadians) {
                targets.add(entity);
            }
        }
        return targets;
    }

    private void spawnSweepParticles(Player player) {
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
        Vector direction = player.getLocation().getDirection().multiply(0.5);
        for (int i = 0; i < 10; i++) {
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(direction.clone().multiply(i)), 5,
                    new Particle.DustOptions(Color.fromRGB(64, 0, 0), 1.0f));
        }
    }
}
