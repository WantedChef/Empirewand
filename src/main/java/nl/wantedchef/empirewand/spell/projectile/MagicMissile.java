package nl.wantedchef.empirewand.spell.projectile;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import java.time.Duration;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class MagicMissile extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Magic Missile";
            this.description = "Fires a sequence of magical beams at the target.";
            this.cooldown = Duration.ofMillis(4000);
            this.spellType = SpellType.PROJECTILE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new MagicMissile(this);
        }
    }

    private MagicMissile(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "magic-missile";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double damage = spellConfig.getDouble("values.damage-per-missile", 3.0);
        int missiles = spellConfig.getInt("values.missile-count", 3);
        int delay = spellConfig.getInt("values.delay-ticks", 7);
        boolean requiresLos = spellConfig.getBoolean("flags.requires-los", true);

        Entity lookedAt = player.getTargetEntity(20);
        if (!(lookedAt instanceof LivingEntity target) || target.isDead() || !target.isValid()) {
            context.fx().fizzle(player);
            return null;
        }

        context.fx().playSound(player, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.2f);

        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count >= missiles || target.isDead() || !target.isValid() || !player.isValid()) {
                    this.cancel();
                    return;
                }

                boolean blocked = requiresLos && !player.hasLineOfSight(target);
                drawBeam(context, player, target, blocked);
                if (!blocked) {
                    target.damage(damage, player);
                }
                count++;
            }
        }.runTaskTimer(context.plugin(), 0L, Math.max(1, delay));
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    private void drawBeam(SpellContext context, Player from, LivingEntity to, boolean blocked) {
        context.fx().trail(from.getEyeLocation(), to.getEyeLocation(), Particle.CRIT, blocked ? 1 : 4);
        if (!blocked) {
            context.fx().trail(from.getEyeLocation(), to.getEyeLocation(), Particle.END_ROD, 1);
        }
    }
}
