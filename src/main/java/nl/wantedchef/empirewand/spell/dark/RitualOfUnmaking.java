package nl.wantedchef.empirewand.spell.dark;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.core.config.ReadableConfig;

import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class RitualOfUnmaking extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Ritual of Unmaking";
            this.description = "Channels a destructive ritual that damages and weakens nearby enemies.";
            this.cooldown = java.time.Duration.ofSeconds(40);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new RitualOfUnmaking(this);
        }
    }

    private Config config;

    private RitualOfUnmaking(Builder builder) {
        super(builder);
        // Config will be initialized lazily when first accessed
    }

    private Config getConfig() {
        if (config == null) {
            // This will be called after loadConfig has been called
            config = new Config(spellConfig);
        }
        return config;
    }

    @Override
    public String key() {
        return "ritual-of-unmaking";
    }

    @Override
    public Component displayName() {
        return Component.text("Ritueel van Ontering");
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        new ChannelTask(context, player, getConfig().channelTicks, getConfig().radius, getConfig().damage,
                getConfig().weaknessDuration,
                getConfig().weaknessAmplifier, getConfig().hitPlayers, getConfig().hitMobs)
                .runTaskTimer(context.plugin(), 0L, 1L);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    private class ChannelTask extends BukkitRunnable {
        private final SpellContext context;
        private final Player player;
        private final int channelTicks;
        private final double radius;
        private final double damage;
        private final int weaknessDuration;
        private final int weaknessAmplifier;
        private final boolean hitPlayers;
        private final boolean hitMobs;
        private int ticksPassed = 0;

        public ChannelTask(SpellContext context, Player player, int channelTicks, double radius, double damage,
                int weaknessDuration, int weaknessAmplifier, boolean hitPlayers, boolean hitMobs) {
            this.context = context;
            this.player = player;
            this.channelTicks = channelTicks;
            this.radius = radius;
            this.damage = damage;
            this.weaknessDuration = weaknessDuration;
            this.weaknessAmplifier = weaknessAmplifier;
            this.hitPlayers = hitPlayers;
            this.hitMobs = hitMobs;
        }

        @Override
        public void run() {
            if (!player.isValid() || player.isDead() || (player.getLastDamage() > 0 && ticksPassed > 0)) {
                this.cancel();
                context.fx().fizzle(player);
                return;
            }

            spawnCircleParticles(player, radius);
            ticksPassed++;

            if (ticksPassed >= channelTicks) {
                this.cancel();
                onFinish();
            }
        }

        private void onFinish() {
            List<LivingEntity> targets = player.getWorld().getLivingEntities().stream()
                    .filter(entity -> entity.getLocation() != null && entity.getLocation().distance(player.getLocation()) <= radius)
                    .filter(entity -> !entity.equals(player))
                    .filter(entity -> (entity instanceof Player && hitPlayers)
                            || (!(entity instanceof Player) && hitMobs))
                    .toList();

            for (LivingEntity target : targets) {
                target.damage(damage, player);
                target.addPotionEffect(
                        new PotionEffect(PotionEffectType.WEAKNESS, weaknessDuration, weaknessAmplifier));
            }

            context.fx().playSound(player, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);
            spawnBurstParticles(player, radius);
        }

        private void spawnCircleParticles(Player player, double radius) {
            for (int i = 0; i < 36; i++) {
                double angle = 2 * Math.PI * i / 36;
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(x, 0.1, z), 1, 0, 0, 0, 0);
            }
        }

        private void spawnBurstParticles(Player player, double radius) {
            for (int i = 0; i < 50; i++) {
                double x = (Math.random() - 0.5) * radius * 2;
                double z = (Math.random() - 0.5) * radius * 2;
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(x, 1, z), 1, 0, 0, 0, 0.1);
            }
        }
    }

    private static class Config {
        private final int channelTicks;
        private final double radius;
        private final double damage;
        private final int weaknessDuration;
        private final int weaknessAmplifier;
        private final boolean hitPlayers;
        private final boolean hitMobs;

        public Config(ReadableConfig config) {
            this.channelTicks = config.getInt("values.channel-ticks", 40);
            this.radius = config.getDouble("values.radius", 6.0);
            this.damage = config.getDouble("values.damage", 8.0);
            this.weaknessDuration = config.getInt("values.weakness-duration-ticks", 120);
            this.weaknessAmplifier = config.getInt("values.weakness-amplifier", 0);
            this.hitPlayers = config.getBoolean("flags.hit-players", true);
            this.hitMobs = config.getBoolean("flags.hit-mobs", true);
        }
    }
}
