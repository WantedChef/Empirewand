package nl.wantedchef.empirewand.spell.misc;

import nl.wantedchef.empirewand.api.EmpireWandAPI;

import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class EtherealForm extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Ethereal Form";
            this.description = "Become intangible and fall slowly.";
            this.cooldown = java.time.Duration.ofSeconds(30);
            this.spellType = SpellType.MISC;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new EtherealForm(this);
        }
    }

    private EtherealForm(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "ethereal-form";
    }

    @Override
    public Component displayName() {
        return Component.text("Ethereal Form").color(TextColor.color(147, 112, 219));
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        if (player.getPersistentDataContainer().has(Keys.ETHEREAL_ACTIVE)) {
            cleanupEtherealForm(context, player);
            return null;
        }

        int duration = spellConfig.getInt("values.duration-ticks", 100);

        player.setCollidable(false);
        player.setInvulnerable(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1, false, true, true));

        Location loc = player.getLocation();
        context.fx().spawnParticles(loc.add(0, 1.0, 0), Particle.END_ROD, 16, 0.4, 0.4, 0.4, 0.01);
        context.fx().playSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.3f);

        player.getPersistentDataContainer().set(Keys.ETHEREAL_ACTIVE, Keys.BYTE_TYPE.getType(), (byte) 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupEtherealForm(context, player);
            }
        }.runTaskLater(context.plugin(), duration);

        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    private void cleanupEtherealForm(SpellContext context, Player player) {
        if (player == null || !player.isValid() || !player.getPersistentDataContainer().has(Keys.ETHEREAL_ACTIVE)) {
            return;
        }
        player.setCollidable(true);
        player.setInvulnerable(false);
        player.getPersistentDataContainer().remove(Keys.ETHEREAL_ACTIVE);
        context.fx().playSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.8f);
    }
}
