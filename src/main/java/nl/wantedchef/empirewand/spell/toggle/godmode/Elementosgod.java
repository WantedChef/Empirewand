package nl.wantedchef.empirewand.spell.toggle.godmode;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

/**
 * Elementosgod – A toggleable divine stance that reflects incoming damage back
 * to attackers while healing the caster for half of the prevented amount.
 * A subtle halo follows the caster's head to indicate the stance is active.
 *
 * All values are configurable under {@code spells.elementosgod.*} in
 * {@code spells.yml}.
 *
 * @author WantedChef
 */
public final class Elementosgod extends Spell<Void> implements ToggleableSpell {

    private static final String ACTIVE_KEY = "elementosgod_active";
    private static final String REFLECT_META = "elementosgod_reflect";

    private final Map<UUID, HaloData> halos = new WeakHashMap<>();
    private final Map<UUID, Long> lastToggle = new WeakHashMap<>();

    /**
     * Builder for creating {@link Elementosgod} instances.
     */
    public static class Builder extends Spell.Builder<Void> {

        /**
         * Creates a new builder for the Elementosgod spell.
         *
         * @param api the plugin API
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "God Mode";
            description = "Reflects damage and heals while a golden halo hovers above you.";
            cooldown = Duration.ofSeconds(2);
            spellType = SpellType.AURA;
        }

        /**
         * Builds the Elementosgod spell.
         *
         * @return the constructed spell
         */
        @Override
        public @NotNull Spell<Void> build() {
            return new Elementosgod(this);
        }
    }

    /**
     * Constructs the spell from its builder.
     *
     * @param builder the spell builder
     */
    private Elementosgod(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "elementosgod";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        toggle(context.caster(), context);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect
    }

    @Override
    public boolean isActive(@NotNull Player player) {
        return player.hasMetadata(ACTIVE_KEY);
    }

    @Override
    public void activate(@NotNull Player player, SpellContext context) {
        long now = System.currentTimeMillis();
        long reToggle = cfgInt("cooldown-retoggle-ticks", 20) * 50L;
        long last = lastToggle.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < reToggle) {
            context.fx().fizzle(player);
            return;
        }

        double healFactor = cfgDouble("effects.heal-factor", 0.5);
        double reflectMultiplier = cfgDouble("effects.reflect-damage", 1.0);
        player.setMetadata(ACTIVE_KEY, new FixedMetadataValue(context.plugin(), new GodData(healFactor, reflectMultiplier)));

        if (cfgBool("particles.show-halo", true)) {
            halos.put(player.getUniqueId(), new HaloData(player, context));
        }

        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.8f);
        lastToggle.put(player.getUniqueId(), now);
    }

    @Override
    public void deactivate(@NotNull Player player, SpellContext context) {
        var plugin = context != null ? context.plugin() : JavaPlugin.getProvidingPlugin(Elementosgod.class);
        player.removeMetadata(ACTIVE_KEY, plugin);
        Optional.ofNullable(halos.remove(player.getUniqueId())).ifPresent(HaloData::stop);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.2f);
        lastToggle.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @Override
    public void forceDeactivate(@NotNull Player player) {
        deactivate(player, null);
    }

    @Override
    public int getMaxDuration() {
        return cfgInt("max-duration-ticks", -1);
    }

    /**
     * Handles damage events to reflect damage and heal the caster.
     *
     * @param event the damage event
     */
    public static void handleDamageEvent(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }
        var metaList = target.getMetadata(ACTIVE_KEY);
        if (metaList.isEmpty()) {
            return;
        }
        Object obj = metaList.get(0).value();
        if (!(obj instanceof GodData data)) {
            return;
        }

        var plugin = JavaPlugin.getProvidingPlugin(Elementosgod.class);
        if (target.hasMetadata(REFLECT_META)) {
            target.removeMetadata(REFLECT_META, plugin);
            return;
        }

        Entity damager = event.getDamager();
        LivingEntity attacker = null;
        if (damager instanceof LivingEntity le) {
            attacker = le;
        } else if (damager instanceof Projectile proj && proj.getShooter() instanceof LivingEntity shooter) {
            attacker = shooter;
        }
        if (attacker == null) {
            return;
        }

        double damage = event.getDamage();
        event.setCancelled(true);

        attacker.setMetadata(REFLECT_META, new FixedMetadataValue(plugin, true));
        attacker.damage(damage * data.reflectMultiplier(), target);
        attacker.removeMetadata(REFLECT_META, plugin);

        double heal = damage * data.healFactor();
        double max = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        target.setHealth(Math.min(max, target.getHealth() + heal));
    }

    private record GodData(double healFactor, double reflectMultiplier) {
    }

    private final class HaloData {
        private final Player player;
        private final BukkitTask task;
        private final double radius;
        private final double height;

        HaloData(Player player, SpellContext context) {
            this.player = player;
            this.radius = cfgDouble("particles.halo-radius", 0.4);
            this.height = cfgDouble("particles.halo-height", 1.1);
            this.task = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 0L, 5L);
        }

        private void tick() {
            if (!player.isOnline() || player.isDead()) {
                forceDeactivate(player);
                return;
            }
            Location base = player.getLocation().add(0, height, 0);
            World world = base.getWorld();
            if (world == null) {
                return;
            }
            int points = 12;
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                world.spawnParticle(Particle.END_ROD, base.getX() + x, base.getY(), base.getZ() + z, 1, 0, 0, 0, 0);
            }
        }

        private void stop() {
            task.cancel();
        }
    }

    private int cfgInt(String path, int def) {
        return spellConfig.getInt(path, def);
    }

    private double cfgDouble(String path, double def) {
        return spellConfig.getDouble(path, def);
    }

    private boolean cfgBool(String path, boolean def) {
        return spellConfig.getBoolean(path, def);
    }
}
